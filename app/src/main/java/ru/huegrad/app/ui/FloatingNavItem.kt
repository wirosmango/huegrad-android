package ru.huegrad.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Пункт плавающей панели в стиле «подпись только у выбранного»: у выбранного пункта пилюля расширяется
 * и показывает значок с подписью в одну строку, у остальных остаётся один значок.
 *
 * Вся анимация идёт от одного значения progress (0..1) без пружины, поэтому ширина, цвет и подпись
 * меняются синхронно и без рывков. Подпись не появляется и не исчезает скачком: её ширина плавно растёт
 * от нуля, а прозрачность и позиция меняются только на этапе отрисовки, без лишних перекомпоновок.
 */
@Composable
fun RowScope.FloatingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val progress = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "navItemProgress",
    )
    val scheme = MaterialTheme.colorScheme
    val p = progress.value
    val tint = lerp(scheme.onSurfaceVariant, scheme.primary, p)

    Row(
        modifier = Modifier
            .weight(1f + 1.4f * p)
            .fillMaxHeight()
            .padding(5.dp)
            .background(scheme.primary.copy(alpha = 0.22f * p), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        // Подпись измеряется в полную ширину, но занимает место пропорционально progress и обрезается по краю
        Box(
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                    val width = (placeable.width * progress.value).roundToInt()
                    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .graphicsLayer { alpha = progress.value },
        ) {
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
