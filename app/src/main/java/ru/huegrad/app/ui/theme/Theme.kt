@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package ru.huegrad.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.huegrad.app.R

private val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private fun Typography.withGoogleSans(): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = GoogleSans),
    displayMedium = displayMedium.copy(fontFamily = GoogleSans),
    displaySmall = displaySmall.copy(fontFamily = GoogleSans),
    headlineLarge = headlineLarge.copy(fontFamily = GoogleSans),
    headlineMedium = headlineMedium.copy(fontFamily = GoogleSans),
    headlineSmall = headlineSmall.copy(fontFamily = GoogleSans),
    titleLarge = titleLarge.copy(fontFamily = GoogleSans),
    titleMedium = titleMedium.copy(fontFamily = GoogleSans),
    titleSmall = titleSmall.copy(fontFamily = GoogleSans),
    bodyLarge = bodyLarge.copy(fontFamily = GoogleSans),
    bodyMedium = bodyMedium.copy(fontFamily = GoogleSans),
    bodySmall = bodySmall.copy(fontFamily = GoogleSans),
    labelLarge = labelLarge.copy(fontFamily = GoogleSans),
    labelMedium = labelMedium.copy(fontFamily = GoogleSans),
    labelSmall = labelSmall.copy(fontFamily = GoogleSans),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = GoogleSans),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = GoogleSans),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = GoogleSans),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = GoogleSans),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = GoogleSans),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = GoogleSans),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = GoogleSans),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = GoogleSans),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = GoogleSans),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = GoogleSans),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = GoogleSans),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = GoogleSans),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = GoogleSans),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = GoogleSans),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = GoogleSans),
)

private fun accentColor(key: String): Color = when (key) {
    "blue" -> Color(0xFF4D8DFF)
    "green" -> Color(0xFF36A875)
    "orange" -> Color(0xFFE58A35)
    "pink" -> Color(0xFFE66AA8)
    "red" -> Color(0xFFE45C64)
    else -> Color(0xFFB69CFF)
}

@Composable
fun HuegradTheme(
    themeMode: String = "system",
    accent: String = "violet",
    fontMode: String = "google_sans",
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val darkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }

    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    // AMOLED keeps the actual app background pure black (#000000).
    // Cards/containers remain slightly raised so the UI still has hierarchy.
    val themeBase = if (themeMode == "amoled") base.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF1A1A1A),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = Color(0xFF202020),
    ) else base

    val primary = accentColor(accent)
    val colors = if (accent == "material_you") themeBase else themeBase.copy(
        primary = primary,
        onPrimary = if (accent == "orange" || accent == "pink" || accent == "green") Color(0xFF171717) else Color.White,
        primaryContainer = primary.copy(alpha = 0.24f),
        onPrimaryContainer = if (darkTheme) Color.White else Color(0xFF211A30),
        secondary = primary,
        secondaryContainer = primary.copy(alpha = 0.18f),
        tertiary = primary,
    )

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = if (fontMode == "google_sans") Typography().withGoogleSans() else Typography(),
        content = content,
    )
}
