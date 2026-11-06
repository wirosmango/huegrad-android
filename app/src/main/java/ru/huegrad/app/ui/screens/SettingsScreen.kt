package ru.huegrad.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.AppSettings
import ru.huegrad.app.ui.NoInsets

private const val PAGE_NAVIGATION = "navigation"
private const val PAGE_APPEARANCE = "appearance"

/**
 * Настройки в духе приложения «Настройки» Android 16: на главной странице категории,
 * внутри каждой категории пункты собраны в скруглённые группы.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    // null = главная страница настроек, иначе открыта подстраница
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (page) {
                            PAGE_NAVIGATION -> "Навигация"
                            PAGE_APPEARANCE -> "Оформление"
                            else -> "Настройки"
                        },
                    )
                },
                navigationIcon = {
                    when {
                        page != null -> CookieBackButton(onClick = { page = null })
                        AppSettings.navStyle == "drawer" -> IconButton(onClick = { AppSettings.openDrawer?.invoke() }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Открыть меню")
                        }
                        else -> CookieBackButton(onClick = onBack)
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = page,
            modifier = Modifier.padding(padding).fillMaxSize(),
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "settingsPage",
        ) { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (current) {
                    PAGE_NAVIGATION -> NavigationSettings()
                    PAGE_APPEARANCE -> AppearanceSettings()
                    else -> SettingsRoot(onOpen = { page = it })
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsRoot(onOpen: (String) -> Unit) {
    ExpressiveListGroup {
        ExpressiveListItem(
            title = "Навигация",
            subtitle = "Нижняя панель, её форма и фон, панель ввода на форуме",
            icon = Icons.Rounded.Navigation,
            onClick = { onOpen(PAGE_NAVIGATION) },
            showDivider = true,
        )
        ExpressiveListItem(
            title = "Оформление",
            subtitle = "Тема, шрифт и акцентный цвет",
            icon = Icons.Rounded.Palette,
            onClick = { onOpen(PAGE_APPEARANCE) },
        )
    }
}

@Composable
private fun NavigationSettings() {
    val context = LocalContext.current

    SectionTitle("Нижняя панель навигации")
    ExpressiveChoiceGroup(
        choices = listOf("floating" to "Плавающая", "md3" to "MD3", "drawer" to "Боковое меню"),
        selected = AppSettings.navStyle,
        onSelect = { AppSettings.setNavStyle(context, it) },
    )

    SectionTitle("Форма плавающей панели")
    ExpressiveChoiceGroup(
        choices = listOf("rounded" to "Круглая", "rectangle" to "Прямоугольная"),
        selected = AppSettings.navShape,
        onSelect = { AppSettings.setNavShape(context, it) },
    )

    SectionTitle("Вид панели")
    ExpressiveListGroup {
        SwitchItem(
            title = "Подписи под иконками",
            subtitle = "Показывать названия разделов",
            checked = AppSettings.navLabels,
            onChange = { AppSettings.setNavLabels(context, it) },
            showDivider = true,
        )
        SwitchItem(
            title = "Фон панели",
            subtitle = "Показывать подложку и тень панели",
            checked = AppSettings.navPanelBackground,
            onChange = { AppSettings.setNavPanelBackground(context, it) },
        )
    }

    SectionTitle("Панель ввода на форуме")
    ExpressiveChoiceGroup(
        choices = listOf("floating" to "Плавающая", "minimal" to "Минимализм"),
        selected = AppSettings.inputStyle,
        onSelect = { AppSettings.setInputStyle(context, it) },
    )
}

@Composable
private fun AppearanceSettings() {
    val context = LocalContext.current

    SectionTitle("Тема приложения")
    ExpressiveChoiceGroup(
        choices = listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная", "amoled" to "AMOLED"),
        selected = AppSettings.themeMode,
        onSelect = { AppSettings.setThemeMode(context, it) },
    )

    SectionTitle("Шрифт", "Выберите шрифт интерфейса приложения.")
    ExpressiveChoiceGroup(
        choices = listOf("system" to "Системный", "google_sans" to "Google Sans"),
        selected = AppSettings.fontMode,
        onSelect = { AppSettings.setFontMode(context, it) },
    )

    SectionTitle("Акцентный цвет", "Material You использует цвета обоев устройства (Android 12 и новее).")
    ExpressiveChoiceGroup(
        choices = listOf(
            "material_you" to "Material You", "violet" to "Фиолетовый", "blue" to "Синий",
            "green" to "Зелёный", "orange" to "Оранжевый", "pink" to "Розовый", "red" to "Красный",
        ),
        selected = AppSettings.accent,
        onSelect = { AppSettings.setAccent(context, it) },
    )
}

/** Заголовок раздела, как в Настройках Android: небольшой текст акцентного цвета над группой. */
@Composable
private fun SectionTitle(title: String, description: String? = null) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, showDivider: Boolean = false) {
    ExpressiveListItem(
        title = title,
        subtitle = subtitle,
        onClick = { onChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        showDivider = showDivider,
    )
}

@Composable
private fun CookieBackButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "cookieRotation")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cookieRotationAngle",
    )

    val cookieFill = MaterialTheme.colorScheme.primaryContainer
    val playColor = MaterialTheme.colorScheme.onPrimaryContainer

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(start = 4.dp, end = 8.dp)
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(42.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.43f
            val path = Path()
            val lobes = 12
            val pointsPerLobe = 12
            val totalPoints = lobes * pointsPerLobe

            // Cookie12Sided: 12 мягких, округлых выступов.
            for (i in 0..totalPoints) {
                val angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i / totalPoints)
                val wave = (0.5 + 0.5 * kotlin.math.cos(lobes * angle)).toFloat()
                val smoothWave = wave * wave * (3f - 2f * wave)
                val r = radius * (0.92f + 0.08f * smoothWave)
                val point = Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * r,
                    y = center.y + kotlin.math.sin(angle).toFloat() * r,
                )
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()

            rotate(rotation.value, pivot = center) {
                drawPath(path = path, color = cookieFill)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = playColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
