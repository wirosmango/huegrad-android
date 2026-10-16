package ru.huegrad.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small persistent store for appearance and navigation preferences. */
object AppSettings {
    var themeMode by mutableStateOf("system")
        private set
    var accent by mutableStateOf("violet")
        private set
    var fontMode by mutableStateOf("google_sans")
        private set
    var navLabels by mutableStateOf(false)
        private set
    var navStyle by mutableStateOf("floating")
        private set
    var navPanelBackground by mutableStateOf(true)
        private set

    /** Set by the app shell so every page toolbar can open the drawer. */
    var openDrawer: (() -> Unit)? = null

    fun load(context: Context) {
        val p = context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE)
        themeMode = p.getString("theme_mode", "system") ?: "system"
        accent = p.getString("accent", "violet") ?: "violet"
        fontMode = p.getString("font_mode", "google_sans") ?: "google_sans"
        navLabels = p.getBoolean("nav_labels", false)
        navStyle = p.getString("nav_style", "floating") ?: "floating"
        navPanelBackground = p.getBoolean("nav_panel_background", true)
    }

    fun setThemeMode(context: Context, value: String) {
        themeMode = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putString("theme_mode", value).apply()
    }
    fun setFontMode(context: Context, value: String) {
        fontMode = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putString("font_mode", value).apply()
    }
    fun setAccent(context: Context, value: String) {
        accent = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putString("accent", value).apply()
    }
    fun setNavLabels(context: Context, value: Boolean) {
        navLabels = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putBoolean("nav_labels", value).apply()
    }
    fun setNavStyle(context: Context, value: String) {
        navStyle = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putString("nav_style", value).apply()
    }
    fun setNavPanelBackground(context: Context, value: Boolean) {
        navPanelBackground = value
        context.getSharedPreferences("huegrad_settings", Context.MODE_PRIVATE).edit().putBoolean("nav_panel_background", value).apply()
    }
}
