package ru.huegrad.app.forum

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.huegrad.app.BuildConfig
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class UploadException(message: String) : IOException(message)

/**
 * Загрузка фото в Cloudinary без ключей в приложении: unsigned upload через upload preset.
 * Cloud name и имя пресета задаются в local.properties (см. app/build.gradle.kts), в репозиторий не попадают.
 * В Cloudinary: Settings → Upload → Upload presets → Add, Signing mode = Unsigned.
 */
object CloudinaryUploader {
    /** Лимит бесплатного плана Cloudinary на одно изображение. */
    const val MAX_BYTES = 10L * 1024 * 1024

    val isConfigured: Boolean
        get() = BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() && BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()

    /** Загружает картинку по uri и возвращает https-ссылку на неё. */
    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw UploadException("Cloudinary не настроен: заполните cloudinary.* в local.properties")

        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        if (!mime.startsWith("image/")) throw UploadException("Можно прикрепить только изображение")

        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
        if (size != null && size > MAX_BYTES) throw UploadException("Фото больше 10 МБ")

        val boundary = "----huegrad${System.currentTimeMillis()}"
        val url = URL("https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/image/upload")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setChunkedStreamingMode(0)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            conn.outputStream.use { out ->
                out.writeText("--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                out.writeText("${BuildConfig.CLOUDINARY_UPLOAD_PRESET}\r\n")
                out.writeText(
                    "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\n" +
                        "Content-Type: $mime\r\n\r\n"
                )
                val input = resolver.openInputStream(uri) ?: throw UploadException("Не удалось открыть файл")
                input.use { stream ->
                    val buf = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val n = stream.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) throw UploadException("Фото больше 10 МБ")
                        out.write(buf, 0, n)
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
            json?.optString("secure_url")?.takeIf { it.startsWith("https://") }
                ?: throw UploadException("Cloudinary не вернул ссылку на фото")
        } finally {
            conn.disconnect()
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
