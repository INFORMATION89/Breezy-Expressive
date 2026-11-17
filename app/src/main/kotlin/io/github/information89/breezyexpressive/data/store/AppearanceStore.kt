package io.github.information89.breezyexpressive.data.store

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Every "look" setting in one place. The theme reads this; screens never read individual settings.
 * New customization options (fonts, blur, tint…) are added here without touching screens.
 */
data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val animatedBackground: Boolean = true,
    /** 0 = no grain, 1 = strong. Noticeable by default. */
    val grain: Float = 0.6f,
    /** 0 = tint only (no blur), 1 = full blur. */
    val blur: Float = 1f,
    val weatherTint: Boolean = true,
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppearanceStore(context: Context) {
    private val store = context.settingsDataStore

    val appearance: Flow<Appearance> = store.data
        .map { it.toAppearance() }
        .distinctUntilChanged()

    suspend fun update(transform: (Appearance) -> Appearance) {
        store.edit { prefs -> prefs.write(transform(prefs.toAppearance())) }
    }

    private fun Preferences.toAppearance(): Appearance {
        val defaults = Appearance()
        return Appearance(
            themeMode = this[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: defaults.themeMode,
            dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
            animatedBackground = this[ANIMATED_BACKGROUND] ?: defaults.animatedBackground,
            grain = (this[GRAIN] ?: defaults.grain).coerceIn(0f, 1f),
            blur = (this[BLUR] ?: defaults.blur).coerceIn(0f, 1f),
            weatherTint = this[WEATHER_TINT] ?: defaults.weatherTint,
        )
    }

    private fun MutablePreferences.write(appearance: Appearance) {
        this[THEME_MODE] = appearance.themeMode.name
        this[DYNAMIC_COLOR] = appearance.dynamicColor
        this[ANIMATED_BACKGROUND] = appearance.animatedBackground
        this[GRAIN] = appearance.grain
        this[BLUR] = appearance.blur
        this[WEATHER_TINT] = appearance.weatherTint
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("appearance_theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("appearance_dynamic_color")
        val ANIMATED_BACKGROUND = booleanPreferencesKey("appearance_animated_background")
        val GRAIN = floatPreferencesKey("appearance_grain")
        val BLUR = floatPreferencesKey("appearance_blur")
        val WEATHER_TINT = booleanPreferencesKey("appearance_weather_tint")
    }
}
