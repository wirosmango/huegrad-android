package ru.huegrad.app.forum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.huegrad.app.BuildConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class UploadException(message: String) : IOException(message)

/** Вложение сообщения. В посте хранится в поле attachments как {url, type}, как на сайте. */
data class Attachment(val url: String, val isVideo: Boolean) {
    fun toMap(): Map<String, String> = mapOf("url" to url, "type" to if (isVideo) "video" else "image")
}

/**
 * Загрузка фото и видео в Cloudinary без ключей в приложении: unsigned upload через upload preset.
 * По умолчанию cloud name и preset те же, что в forum.js сайта (см. app/build.gradle.kts), их можно
 * переопределить в local.properties. Лимиты и сжатие фото повторяют сайт.
 */
object CloudinaryUploader {
    const val MAX_IMAGE_BYTES = 15L * 1024 * 1024 // до сжатия
    const val MAX_VIDEO_BYTES = 15L * 1024 * 1024
    private const val MAX_SIDE = 1600
    private const val COMPRESS_FROM_BYTES = 1_500_000L

    val isConfigured: Boolean
        get() = BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() && BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()

    /** Загружает фото или видео по uri. onProgress получает долю 0..1, если размер файла известен. */
    suspend fun upload(context: Context, uri: Uri, onProgress: (Float) -> Unit = {}): Attachment = withContext(Dispatchers.IO) {
        if (!isConfigured) throw UploadException("Cloudinary не настроен: заполните cloudinary.* в local.properties")

        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val isVideo = mime.startsWith("video/")
        if (!isVideo && !mime.startsWith("image/")) throw UploadException("Можно прикреплять только изображения и видео")
        val limit = if (isVideo) MAX_VIDEO_BYTES else MAX_IMAGE_BYTES
        val tooBig = if (isVideo) "Видео больше 15 МБ" else "Изображение больше 15 МБ"

        val originalSize = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
        if (originalSize != null && originalSize > limit) throw UploadException(tooBig)

        // Большие фото уменьшаем перед загрузкой, как на сайте (до 1600 px, JPEG 85%)
        val compressed = if (isVideo) null else compressImage(context, uri, mime, originalSize)
        val sendMime = if (compressed != null) "image/jpeg" else mime
        val size = compressed?.size?.toLong() ?: originalSize

        val boundary = "----huegrad${System.currentTimeMillis()}"
        // auto/upload, как на сайте: тип (фото или видео) Cloudinary определяет сам и возвращает в resource_type
        val url = URL("https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/auto/upload")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 120_000
            setChunkedStreamingMode(0)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            conn.outputStream.use { out ->
                out.writeText("--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                out.writeText("${BuildConfig.CLOUDINARY_UPLOAD_PRESET}\r\n")
                out.writeText(
                    "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n" +
                        "Content-Type: $sendMime\r\n\r\n"
                )
                val input = compressed?.let { ByteArrayInputStream(it) }
                    ?: resolver.openInputStream(uri)
                    ?: throw UploadException("Не удалось открыть файл")
                input.use { stream ->
                    val buf = ByteArray(16 * 1024)
                    var total = 0L
                    var reported = -1
                    while (true) {
                        val n = stream.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > limit) throw UploadException(tooBig)
                        out.write(buf, 0, n)
                        if (size != null && size > 0) {
                            val percent = (total * 100 / size).toInt().coerceIn(0, 100)
                            if (percent != reported) {
                                reported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
                out.writeText("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (code !in 200..299) {
                val reason = json?.optJSONObject("error")?.optString("message").orEmpty()
                throw UploadException(reason.ifBlank { "Cloudinary ответил кодом $code" })
            }
            val secureUrl = json?.optString("secure_url")?.takeIf { it.startsWith("https://") }
                ?: throw UploadException("Cloudinary не вернул ссылку на файл")
            Attachment(secureUrl, isVideo = json.optString("resource_type") == "video")
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Уменьшает большое фото. null означает «загружать как есть»: gif, фото уже небольшое, сжатие не дало выигрыша
     * или Android старше 9 (там нет ImageDecoder, который сам учитывает поворот кадра).
     */
    private fun compressImage(context: Context, uri: Uri, mime: String, originalSize: Long?): ByteArray? {
        if (Build.VERSION.SDK_INT < 28 || mime == "image/gif" || mime == "image/svg+xml") return null
        return try {
            var scale = 1f
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(w, h))
                if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            if (scale == 1f && (originalSize ?: Long.MAX_VALUE) < COMPRESS_FROM_BYTES) {
                bitmap.recycle()
                return null
            }
            // Прозрачные области в JPEG станут белыми, как на сайте
            val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
            bitmap.recycle()
            val out = ByteArrayOutputStream()
            flat.compress(Bitmap.CompressFormat.JPEG, 85, out)
            flat.recycle()
            val bytes = out.toByteArray()
            if (originalSize != null && bytes.size >= originalSize) null else bytes
        } catch (e: Exception) {
            null
        }
    }

    private fun OutputStream.writeText(s: String) = write(s.toByteArray(Charsets.UTF_8))
}

/** Похоже ли значение на ссылку на картинку: расширение файла или картинка Cloudinary. */
fun isImageUrl(url: String): Boolean {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    if (listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif", ".bmp").any { path.endsWith(it) }) return true
    return "res.cloudinary.com" in path && "/image/upload/" in path
}

/** Старые сообщения могут хранить http-ссылку Cloudinary, а Android блокирует http. */
fun secureImageUrl(url: String): String =
    if (url.startsWith("http://") && "cloudinary.com" in url) "https://" + url.removePrefix("http://") else url

/** Ссылка на видео: расширение файла или видео Cloudinary. */
fun isVideoUrl(url: String): Boolean {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    if (listOf(".mp4", ".webm", ".mov", ".m4v", ".3gp", ".mkv").any { path.endsWith(it) }) return true
    return "res.cloudinary.com" in path && "/video/upload/" in path
}

/** Кадр-превью для видео Cloudinary (jpg первого кадра), для чужих ссылок превью нет. */
fun videoThumbnailUrl(url: String): String? {
    val base = url.substringBefore('?')
    if ("res.cloudinary.com" !in base || "/video/upload/" !in base) return null
    return base.replace("/video/upload/", "/video/upload/so_0,w_720,c_limit/").substringBeforeLast('.') + ".jpg"
}

/** Уменьшенная копия фото для ленты, как на сайте; оригинал открывается в просмотре. */
fun feedImageUrl(url: String): String =
    if ("res.cloudinary.com" in url && "/upload/" in url && "/upload/w_" !in url) {
        url.replace("/upload/", "/upload/w_900,c_limit,q_auto,f_auto/")
    } else {
        url
    }
