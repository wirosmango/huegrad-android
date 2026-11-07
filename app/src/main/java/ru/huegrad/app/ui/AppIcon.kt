package ru.huegrad.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import ru.huegrad.app.R

/**
 * Выбор значка приложения. Фон адаптивной иконки задаётся ресурсом и не меняется на лету,
 * поэтому в манифесте для каждого сочетания «значок + цвет» есть свой activity-alias
 * (IconVillageGreen, IconCitySynagogue... ), а включённым остаётся только один.
 */
object AppIcon {
    class IconOption(val id: String, val title: String, val hint: String?, @DrawableRes val glyph: Int)
    class ColorOption(val id: String, val title: String, val color: Color)

    const val DEFAULT_ICON = "village"
    const val DEFAULT_COLOR = "green"

    val icons = listOf(
        IconOption("village", "Посёлок", "по умолчанию", R.drawable.holiday_village_24),
        IconOption("city", "Город", "старая", R.drawable.location_city_24),
        IconOption("synagogue", "Синагога", null, R.drawable.synagogue_24),
    )

    val colors = listOf(
        ColorOption("green", "Зелёный", Color(0xFF2E7D32)),
        ColorOption("blue", "Синий", Color(0xFF1565C0)),
        ColorOption("purple", "Фиолетовый", Color(0xFF6A1B9A)),
        ColorOption("red", "Красный", Color(0xFFC62828)),
        ColorOption("orange", "Оранжевый", Color(0xFFE65100)),
        ColorOption("graphite", "Графитовый", Color(0xFF263238)),
    )

    private fun alias(context: Context, icon: String, color: String) = ComponentName(
        context,
        "ru.huegrad.app.Icon" + icon.replaceFirstChar { it.uppercase() } + color.replaceFirstChar { it.uppercase() },
    )

    /** Какой значок включён сейчас (id значка и id цвета). Источник правды: состояние алиасов в системе. */
    fun current(context: Context): Pair<String, String> {
        val pm = context.packageManager
        for (icon in icons) for (color in colors) {
            val isDefault = icon.id == DEFAULT_ICON && color.id == DEFAULT_COLOR
            val state = pm.getComponentEnabledSetting(alias(context, icon.id, color.id))
            // DEFAULT = «как в манифесте»: включён только стандартный алиас
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && isDefault)
            ) return icon.id to color.id
        }
        return DEFAULT_ICON to DEFAULT_COLOR
    }

    /** Включает выбранный алиас и выключает остальные. Новый включаем первым, чтобы ярлык приложения не пропадал. */
    fun apply(context: Context, icon: String, color: String) {
        val pm = context.packageManager
        val target = alias(context, icon, color)
        pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        for (i in icons) for (c in colors) {
            val other = alias(context, i.id, c.id)
            if (other != target) {
                pm.setComponentEnabledSetting(other, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
