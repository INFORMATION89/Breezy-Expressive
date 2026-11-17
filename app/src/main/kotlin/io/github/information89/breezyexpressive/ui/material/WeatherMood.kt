package io.github.information89.breezyexpressive.ui.material

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.information89.breezyexpressive.insight.Sky

/** Three colors for the abstract background: top, accent blob, bottom. */
@Immutable
data class MoodColors(val top: Color, val accent: Color, val bottom: Color)

/**
 * The background's mood for each kind of sky, in light and dark versions.
 * Warm for sun, cool for rain, deep for night.
 */
object WeatherMood {

    fun colors(sky: Sky?, isDay: Boolean, dark: Boolean): MoodColors {
        val pair = when (sky) {
            Sky.SUNNY -> SUNNY
            Sky.CLEAR_NIGHT -> CLEAR_NIGHT
            Sky.PARTLY_CLOUDY -> if (isDay) PARTLY_DAY else PARTLY_NIGHT
            Sky.CLOUDY, Sky.WINDY -> CLOUDY
            Sky.RAINY -> RAINY
            Sky.STORMY -> STORMY
            Sky.SNOWY, Sky.SLEETY -> SNOWY
            Sky.FOGGY, Sky.HAZY -> FOGGY
            null -> if (isDay) PARTLY_DAY else PARTLY_NIGHT
        }
        return if (dark) pair.second else pair.first
    }

    private fun mood(top: Long, accent: Long, bottom: Long) = MoodColors(Color(top), Color(accent), Color(bottom))

    // (light, dark)
    private val SUNNY = mood(0xFFFFD9A0, 0xFFFFB59A, 0xFFFFEEDD) to mood(0xFF5A3418, 0xFF6E2E2A, 0xFF22150F)
    private val CLEAR_NIGHT = mood(0xFFC9C8F2, 0xFFB9A9E8, 0xFFE6E4F7) to mood(0xFF1B1D4A, 0xFF35215C, 0xFF0C0E22)
    private val PARTLY_DAY = mood(0xFFFFE1B8, 0xFFC9D6E8, 0xFFEEF1F6) to mood(0xFF4A3826, 0xFF2F3A4C, 0xFF171B22)
    private val PARTLY_NIGHT = mood(0xFFD0D2E6, 0xFFBCC3DA, 0xFFE8E9F1) to mood(0xFF22263F, 0xFF332C4A, 0xFF101220)
    private val CLOUDY = mood(0xFFD5DCE4, 0xFFC2CBD6, 0xFFECEFF3) to mood(0xFF2E3640, 0xFF3A4250, 0xFF15181E)
    private val RAINY = mood(0xFFBFD3DD, 0xFFA3C3CF, 0xFFE3ECF0) to mood(0xFF1F3540, 0xFF1F4A55, 0xFF0E171C)
    private val STORMY = mood(0xFFC7C0D8, 0xFFA9A0C4, 0xFFE4E1EC) to mood(0xFF2B2340, 0xFF43305A, 0xFF120F18)
    private val SNOWY = mood(0xFFE3EEF7, 0xFFCFE0EF, 0xFFF7FAFC) to mood(0xFF2A3A4A, 0xFF3B4F63, 0xFF141B23)
    private val FOGGY = mood(0xFFE6E1D8, 0xFFD6D2CA, 0xFFF2F0EC) to mood(0xFF3A3833, 0xFF34322E, 0xFF181715)
}
