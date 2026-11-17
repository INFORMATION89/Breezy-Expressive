package io.github.information89.breezyexpressive.ui.material

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.information89.breezyexpressive.insight.Sky

/**
 * High-contrast sky: a deep base ([top] to [bottom]) with one luminous orb in the condition's
 * color ([light] core, [bloom] halo), like a moon or sun seen through haze. [accent] is used for
 * highlights; [vignette] darkens the edges (strongest at night).
 */
@Immutable
data class MoodColors(
    val top: Color,
    val bottom: Color,
    val light: Color,
    val bloom: Color,
    val accent: Color,
    val vignette: Float = 0.55f,
)

object WeatherMood {

    fun colors(sky: Sky?, isDay: Boolean): MoodColors {
        val mood = when (sky) {
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
        // Rain, storms, snow and fog share one palette; at night the base sinks toward black.
        val dimAtNight = sky == Sky.RAINY || sky == Sky.STORMY || sky == Sky.SNOWY || sky == Sky.SLEETY ||
            sky == Sky.FOGGY || sky == Sky.HAZY
        val colored = if (!isDay && dimAtNight) mood.dimmed() else mood
        return if (isDay) colored else colored.copy(vignette = 1f)
    }

    private fun MoodColors.dimmed(): MoodColors {
        val night = Color(0xFF050609)
        return copy(top = lerp(top, night, 0.7f), bottom = lerp(bottom, night, 0.7f), light = lerp(light, night, 0.15f), bloom = lerp(bloom, night, 0.35f))
    }

    private fun mood(top: Long, bottom: Long, light: Long, bloom: Long, accent: Long) =
        MoodColors(Color(top), Color(bottom), Color(light), Color(bloom), Color(accent))

    //                          base top    base bottom  orb core     orb halo     accent
    private val SUNNY = mood(0xFF0B3BD9, 0xFF031A7A, 0xFFFFF1C9, 0xFF4F8BFF, 0xFFFFD27A)
    private val CLEAR_NIGHT = mood(0xFF05060C, 0xFF0A0D1C, 0xFFDDE5FF, 0xFF3A4FA8, 0xFFC9D4FF)
    private val PARTLY_DAY = mood(0xFF1E4FB0, 0xFF0B2366, 0xFFFFF4DE, 0xFF7FA6E8, 0xFFFFE0A0)
    private val PARTLY_NIGHT = mood(0xFF06070D, 0xFF0D1022, 0xFFC9D2F0, 0xFF3B4680, 0xFFC2CBEA)
    private val CLOUDY = mood(0xFF3B4A62, 0xFF161E2C, 0xFFF0F3F8, 0xFF8C9BB4, 0xFFE3E9F2)
    private val CLOUDY_NIGHT = mood(0xFF07080C, 0xFF10131C, 0xFFB8C2DA, 0xFF39425C, 0xFFC3CCE0)
    private val RAINY = mood(0xFF0A2A44, 0xFF03101C, 0xFFBFE0FF, 0xFF2C79C2, 0xFF8EC0F0)
    private val STORMY = mood(0xFF120A24, 0xFF05030C, 0xFFE2D2FF, 0xFF6B3FD0, 0xFFC6A8FF)
    private val SNOWY = mood(0xFF2C4A6E, 0xFF0E1C2E, 0xFFFFFFFF, 0xFFA9CBEA, 0xFFEAF4FF)
    private val FOGGY = mood(0xFF3A3833, 0xFF151411, 0xFFF2EBDD, 0xFF8E877A, 0xFFEFE7D6)
}
