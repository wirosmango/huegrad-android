package ru.huegrad.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.huegrad.app.ui.AppSettings
import ru.huegrad.app.ui.NoInsets

private data class Choice(val value: String, val title: String)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    if (AppSettings.navStyle == "drawer") {
                        androidx.compose.material3.IconButton(onClick = { AppSettings.openDrawer?.invoke() }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Открыть меню")
                        }
                    } else CookieBackButton(onClick = onBack)
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Нижняя панель навигации", style = MaterialTheme.typography.titleMedium)
            ChoiceCard(
                choices = listOf(Choice("floating", "Плавающая"), Choice("md3", "MD3"), Choice("drawer", "Боковое меню")),
                selected = AppSettings.navStyle,
                onSelect = { AppSettings.setNavStyle(context, it) },
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSwitch("Подписи под иконками", "Показывать названия разделов", AppSettings.navLabels) { AppSettings.setNavLabels(context, it) }
                    HorizontalDivider()
                    SettingSwitch("Фон панели", "Показывать подложку и тень панели", AppSettings.navPanelBackground) { AppSettings.setNavPanelBackground(context, it) }
                }
            }

            Text("Тема приложения", style = MaterialTheme.typography.titleMedium)
            ChoiceCard(
                choices = listOf(
                    Choice("system", "Системная"),
                    Choice("light", "Светлая"),
                    Choice("dark", "Тёмная"),
                    Choice("amoled", "AMOLED"),
                ),
                selected = AppSettings.themeMode,
                onSelect = { AppSettings.setThemeMode(context, it) },
            )

            Text("Шрифт", style = MaterialTheme.typography.titleMedium)
            Text("Выберите шрифт интерфейса приложения.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceCard(
                choices = listOf(Choice("system", "Системный"), Choice("google_sans", "Google Sans")),
                selected = AppSettings.fontMode,
                onSelect = { AppSettings.setFontMode(context, it) },
            )

            Text("Акцентный цвет", style = MaterialTheme.typography.titleMedium)
            Text("Material You использует цвета обоев устройства (Android 12 и новее).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceCard(
                choices = listOf(
                    Choice("material_you", "Material You"), Choice("violet", "Фиолетовый"), Choice("blue", "Синий"),
                    Choice("green", "Зелёный"), Choice("orange", "Оранжевый"), Choice("pink", "Розовый"), Choice("red", "Красный"),
                ),
                selected = AppSettings.accent,
                onSelect = { AppSettings.setAccent(context, it) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
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

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceCard(choices: List<Choice>, selected: String, onSelect: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            choices.chunked(3).forEach { rowChoices ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowChoices.forEach { choice ->
                        FilterChip(
                            selected = selected == choice.value,
                            onClick = { onSelect(choice.value) },
                            label = { Text(choice.title) },
                        )
                    }
                }
            }
        }
    }
}
