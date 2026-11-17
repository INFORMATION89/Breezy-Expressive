package io.github.information89.breezyexpressive.ui.material

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.information89.breezyexpressive.insight.Sky

/**
 * Colors for the background: a glowing [core] in the middle (sun, moon, rain light…),
 * fading through [mid] into darker [edge]s.
 */
@Immutable
data class MoodColors(val core: Color, val mid: Color, val edge: Color, val accent: Color = core)

/**
 * The background's mood for each kind of sky, in light and dark versions.
 * Night glows like the moon, sun glows gold, rain glows steel-blue, storms violet.
 * Dark-theme cores stay mid-bright so white text on glass keeps its contrast.
 */
object WeatherMood {

    fun colors(sky: Sky?, isDay: Boolean, dark: Boolean): MoodColors {
        val pair = when (sky) {
            Sky.SUNNY -> SUNNY
            Sky.CLEAR_NIGHT -> CLEAR_NIGHT
            Sky.PARTLY_CLOUDY -> if (isDay) PARTLY_DAY else PARTLY_NIGHT
            Sky.CLOUDY, Sky.WINDY -> if (isDay) CLOUDY else CLOUDY_NIGHT
            Sky.RAINY -> RAINY
            Sky.STORMY -> STORMY
            Sky.SNOWY, Sky.SLEETY -> SNOWY
            Sky.FOGGY, Sky.HAZY -> FOGGY
            null -> if (isDay) PARTLY_DAY else PARTLY_NIGHT
        }
        return if (dark) pair.second else pair.first
    }

    private fun mood(core: Long, mid: Long, edge: Long, accent: Long) = MoodColors(Color(core), Color(mid), Color(edge), Color(accent))

    // (light, dark)
    private val SUNNY = mood(0xFFFFF4DC, 0xFFFFC98E, 0xFFF0A070, 0xFF9A5B12) to mood(0xFFD08A42, 0xFF5E2E16, 0xFF0F0805, 0xFFF2B65A)
    private val CLEAR_NIGHT = mood(0xFFF2F4FF, 0xFFB9C2EA, 0xFF8E99C9, 0xFF4A5A94) to mood(0xFFA4B0E0, 0xFF1E2550, 0xFF04050C, 0xFFB4C4F2)
    private val PARTLY_DAY = mood(0xFFFFF6E8, 0xFFC3D4EA, 0xFF9DB3CF, 0xFF6E5530) to mood(0xFFBE9E70, 0xFF2C3D55, 0xFF080B10, 0xFFE0C08A)
    private val PARTLY_NIGHT = mood(0xFFF0F2FA, 0xFFC3C8DE, 0xFF9CA3C2, 0xFF4E5A82) to mood(0xFF95A0C8, 0xFF1E2440, 0xFF05060C, 0xFFAEB9DC)
    private val CLOUDY = mood(0xFFF7F9FB, 0xFFCBD3DC, 0xFFA7B2BE, 0xFF4E5C6E) to mood(0xFF8E98A6, 0xFF27303C, 0xFF07090C, 0xFFB9C4D2)
    private val CLOUDY_NIGHT = mood(0xFFEFF1F6, 0xFFC2C8D3, 0xFF9AA3B3, 0xFF4B5876) to mood(0xFF8592B4, 0xFF1D2433, 0xFF05060A, 0xFFAAB6D2)
    private val RAINY = mood(0xFFEEF5FA, 0xFFA9C7DA, 0xFF7FA3BA, 0xFF2F5F86) to mood(0xFF6494B8, 0xFF16334A, 0xFF03070B, 0xFF7FB2E6)
    private val STORMY = mood(0xFFF3EFFA, 0xFFC4B6E0, 0xFF9886BD, 0xFF5A4398) to mood(0xFF9A80D0, 0xFF2C1E4A, 0xFF050308, 0xFFB29AEC)
    private val SNOWY = mood(0xFFFFFFFF, 0xFFD6E6F3, 0xFFAFC7DA, 0xFF3E6488) to mood(0xFFA8BED6, 0xFF2E4258, 0xFF080B10, 0xFFD2E6F6)
    private val FOGGY = mood(0xFFFBFAF7, 0xFFDCD7CD, 0xFFB8B2A6, 0xFF6A604E) to mood(0xFFA49E90, 0xFF35322D, 0xFF0B0A09, 0xFFD4CAB6)
}
