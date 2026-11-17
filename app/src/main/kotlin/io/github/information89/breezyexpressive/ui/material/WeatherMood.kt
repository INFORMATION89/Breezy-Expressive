package io.github.information89.breezyexpressive.ui.material

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.information89.breezyexpressive.insight.Sky

/**
 * A full-color sky: [top] to [bottom] gradient, a soft [light] at the top (sun or moon),
 * a color [bloom] rising from the bottom, and the [accent] for highlights.
 * Never black: even nights stay deep blue/violet.
 */
@Immutable
data class MoodColors(
    val top: Color,
    val bottom: Color,
    val light: Color,
    val bloom: Color,
    val accent: Color,
    /** 0 = full color to the edges (day), 1 = edges fade to black (night). */
    val vignette: Float = 0f,
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
        // Rain, storms, snow and fog have one palette; at night it's dimmed toward deep blue, never black.
        val dimAtNight = sky == Sky.RAINY || sky == Sky.STORMY || sky == Sky.SNOWY || sky == Sky.SLEETY ||
            sky == Sky.FOGGY || sky == Sky.HAZY
        val colored = if (!isDay && dimAtNight) mood.dimmed() else mood
        return if (isDay) colored else colored.copy(vignette = 1f)
    }

    private fun MoodColors.dimmed(): MoodColors {
        val night = Color(0xFF141A33)
        return copy(top = lerp(top, night, 0.55f), bottom = lerp(bottom, night, 0.45f), light = lerp(light, night, 0.35f), bloom = lerp(bloom, night, 0.3f))
    }

    private fun mood(top: Long, bottom: Long, light: Long, bloom: Long, accent: Long) =
        MoodColors(Color(top), Color(bottom), Color(light), Color(bloom), Color(accent))

    private val SUNNY = mood(0xFF3C86E0, 0xFF2462C4, 0xFFFFE3A3, 0xFF6FB8FF, 0xFFFFD27A)
    private val CLEAR_NIGHT = mood(0xFF141B40, 0xFF2B2672, 0xFFC9D4FF, 0xFF5B4BD6, 0xFFB9C6FF)
    private val PARTLY_DAY = mood(0xFF5A8FCC, 0xFF3A6AA8, 0xFFFFE7B8, 0xFF8FB8E8, 0xFFFFE0A0)
    private val PARTLY_NIGHT = mood(0xFF1A2244, 0xFF34306C, 0xFFC2CBEA, 0xFF5A55B0, 0xFFC2CBEA)
    private val CLOUDY = mood(0xFF7D8FA6, 0xFF56677E, 0xFFE6ECF4, 0xFF9AA9BE, 0xFFE3E9F2)
    private val CLOUDY_NIGHT = mood(0xFF242B40, 0xFF3A4260, 0xFFA8B2CC, 0xFF56608A, 0xFFC3CCE0)
    private val RAINY = mood(0xFF3E5670, 0xFF24384F, 0xFF9EC2E6, 0xFF3E78B8, 0xFF8EC0F0)
    private val STORMY = mood(0xFF2E2648, 0xFF4A2F72, 0xFFC8B4F0, 0xFF7A4FD0, 0xFFC6A8FF)
    private val SNOWY = mood(0xFF9FB4CC, 0xFF7690AE, 0xFFFFFFFF, 0xFFC4DCF2, 0xFFEAF4FF)
    private val FOGGY = mood(0xFF8E8A82, 0xFF6B675F, 0xFFEAE4D8, 0xFFA8A194, 0xFFEFE7D6)
}
