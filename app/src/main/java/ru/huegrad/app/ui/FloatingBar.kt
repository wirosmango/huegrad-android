package ru.huegrad.app.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Боковой отступ плавающей пилюли от края экрана. Плавающая кнопка использует тот же отступ, чтобы совпасть с ней. */
val FloatingBarSideInset = 20.dp

/** Пустой отступ сверху и снизу внутри контейнера панели, вокруг самой пилюли. */
val FloatingBarVerticalInset = 8.dp

/** Зазор между плавающей кнопкой и верхним краем пилюли (или панели). */
val FloatingBarGap = 12.dp

/**
 * Нижний отступ плавающей кнопки экрана, от низа экрана до её нижнего края.
 *
 * [bottomBarInset] — LocalBottomBarInset, нижняя граница занятой панелью области.
 * В режиме floating это высота всего контейнера панели, он больше пилюли на [FloatingBarVerticalInset]
 * сверху и снизу, поэтому верх пилюли лежит на эту величину ниже.
 */
fun floatingActionBottom(navStyle: String, bottomBarInset: Dp): Dp = when (navStyle) {
    // NavigationBar: контент уже заканчивается над ней, кнопка просто на зазор выше
    "md3" -> FloatingBarGap
    // Панели нет, кнопка стоит над системной навигацией
    "drawer" -> bottomBarInset + FloatingBarGap
    // Плавающая пилюля: её верхний край на FloatingBarVerticalInset ниже верха контейнера панели
    else -> (bottomBarInset - FloatingBarVerticalInset).coerceAtLeast(0.dp) + FloatingBarGap
}
