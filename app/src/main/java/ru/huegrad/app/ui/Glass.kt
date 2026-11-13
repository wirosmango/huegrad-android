package ru.huegrad.app.ui

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Матовое стекло: фон из hazeSource под элементом размывается (Android 12+), сверху тонировка, лёгкий шум
 * и тонкая рамка. На более старых версиях размытия нет, и элемент почти непрозрачный, чтобы текст читался.
 * Используется плавающей панелью навигации и плавающей панелью ввода на форуме (настройки → навигация).
 */
@Composable
fun Modifier.frostedGlass(haze: HazeState, shape: Shape, bordered: Boolean = true): Modifier {
    val base = MaterialTheme.colorScheme.surface
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    return this
        .clip(shape)
        .hazeEffect(state = haze) {
            backgroundColor = base
            blurRadius = 24.dp
            noiseFactor = 0.06f
            tints = listOf(HazeTint(glass.copy(alpha = 0.55f)))
            fallbackTint = HazeTint(glass.copy(alpha = 0.92f))
        }
        .then(if (bordered) Modifier.border(1.dp, outline, shape) else Modifier)
}
