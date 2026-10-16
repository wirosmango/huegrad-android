@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.abs

/**
 * Аватар автора на форуме, как на сайте:
 *  - вошёл через Google (есть https-фото): круглое фото;
 *  - аноним, вход по почте или фото не загрузилось: «печенье» Cookie9Sided с первой буквой ника
 *    на фоне цвета, который выводится из ника (hsl(h 55% 45%), тот же хэш, что в avatarColor() на сайте).
 */
@Composable
fun ForumAvatar(
    nickname: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    // Как на сайте: при ошибке загрузки фото показываем букву
    var photoFailed by remember(photoUrl) { mutableStateOf(false) }
    val showPhoto = !photoFailed && photoUrl != null && photoUrl.startsWith("https://")

    if (showPhoto) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Аватар $nickname",
            contentScale = ContentScale.Crop,
            onError = { photoFailed = true },
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        // toShape() сам @Composable и запоминает форму внутри, оборачивать в remember не нужно
        val shape = MaterialShapes.Cookie9Sided.toShape()
        // Размер буквы 45% от размера аватарки и не зависит от масштаба шрифта системы
        val fontSize = with(LocalDensity.current) { (size * 0.45f).toSp() }
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(avatarColor(nickname)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarLetter(nickname),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/** Первая буква ника в верхнем регистре, «?» для пустого ника. */
internal fun avatarLetter(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
}

/**
 * Тот же расчёт, что в avatarColor(name) на сайте:
 * h = (h * 31 + код символа) % 360 по символам ника, цвет hsl(h 55% 45%).
 */
internal fun avatarColor(name: String): Color {
    var h = 0
    var i = 0
    while (i < name.length) {
        val cp = name.codePointAt(i)
        // В JS это charCodeAt(0) от символа: вне BMP берётся первая половина суррогатной пары
        val unit = if (cp > 0xFFFF) 0xD800 + ((cp - 0x10000) shr 10) else cp
        h = (h * 31 + unit) % 360
        i += Character.charCount(cp)
    }
    return hslColor(h.toFloat(), 0.55f, 0.45f)
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
