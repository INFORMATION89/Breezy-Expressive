/*
 * This file is part of Solstice, a fork of Breezy Weather.
 *
 * Solstice is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Solstice is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Solstice. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.expressive.sky

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import breezyweather.domain.weather.reference.WeatherCode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * A sky: a base colour, three soft colour fields (top-left, right, bottom-left), the glow of the
 * sun (or moon) with its strength, the text colour that reads on it, and the film-grain strength.
 * Palettes approved on 2026-11-18 (docs/PLAN.md, "Forecast background").
 */
data class SkyPalette(
    val base: Color,
    val blobs: List<Color>,
    val text: Color,
    val grain: Float,
    val sun: Color,
    val sunStrength: Float,
)

/** Weather families; each has a day and a night sky, and clear skies turn golden at dawn/dusk. */
enum class SkyWeather(val day: SkyPalette, val night: SkyPalette, val golden: Boolean) {
    CLEAR(
        sky(0xBFD3F2, 0x8FB0EE, 0xFFE1A8, 0xF3EBDD, 0x1E2340, sun = 0xFFF0C8, sunStrength = 0.9f),
        sky(0x1D1A3D, 0x8E7CD0, 0x5B5BD6, 0x0F0D22, 0xECE9FF, 0.30f, MOON, 0.4f),
        golden = true
    ),
    PARTLY_CLOUDY(
        sky(0xF2D7C9, 0x9DAEE6, 0xF4A46B, 0xF7E3C8, 0x2A1A1A, sun = 0xFFE7C2, sunStrength = 0.75f),
        sky(0x232043, 0x8E86C9, 0x45417E, 0x14122A, 0xEEEAFB, 0.30f, MOON, 0.3f),
        golden = true
    ),
    OVERCAST(
        sky(0xC9CBD3, 0xA7ABB8, 0xE2DED8, 0xB9B4C4, 0x22252E, sun = 0xFFF6E8, sunStrength = 0.35f),
        sky(0x2B2A3B, 0x4A4860, 0x6B6680, 0x1C1B27, 0xEEEAFB, 0.30f, MOON, 0.2f),
        golden = false
    ),
    RAIN(
        sky(0x7E8FA8, 0x4D6185, 0x9FB7C9, 0x5B6E7E, 0xF2F5FA, 0.28f, sun = 0xE8EEF5, sunStrength = 0.25f),
        sky(0x1E2638, 0x2E3D5C, 0x4F6A86, 0x121826, 0xEEF2FA, 0.30f, MOON, 0.15f),
        golden = false
    ),
    THUNDERSTORM(
        sky(0x3A3550, 0x1E1B33, 0x7A5FB0, 0x4A5570, 0xF1EEFA, 0.30f, sun = 0xD9D2F2, sunStrength = 0.15f),
        sky(0x16131F, 0x0C0A14, 0x5B3F96, 0x2A2F45, 0xF1EEFA, 0.30f, MOON, 0.1f),
        golden = false
    ),
    SNOW(
        sky(0xE7ECF4, 0xC3D2EC, 0xFFFFFF, 0xD8D2EA, 0x1F2633, sun = 0xFFFFFF, sunStrength = 0.5f),
        sky(0x2A3044, 0x6E7FA6, 0xB8C3DE, 0x3A3C5E, 0xF3F5FC, 0.30f, MOON, 0.25f),
        golden = false
    ),
    FOG(
        sky(0xD9D6D1, 0xEDEAE4, 0xC4C0BA, 0xE4E0DA, 0x26241F, 0.18f, sun = 0xFFF6E6, sunStrength = 0.45f),
        sky(0x2E2C33, 0x4A4750, 0x6E6A72, 0x3A383F, 0xEEEAF2, 0.22f, MOON, 0.25f),
        golden = false
    ),
    ;

    companion object {
        fun of(code: WeatherCode?): SkyWeather = when (code) {
            WeatherCode.CLEAR, null -> CLEAR
            WeatherCode.PARTLY_CLOUDY, WeatherCode.WIND -> PARTLY_CLOUDY
            WeatherCode.CLOUDY -> OVERCAST
            WeatherCode.RAIN, WeatherCode.SLEET -> RAIN
            WeatherCode.THUNDER, WeatherCode.THUNDERSTORM, WeatherCode.HAIL -> THUNDERSTORM
            WeatherCode.SNOW -> SNOW
            WeatherCode.FOG, WeatherCode.HAZE -> FOG
        }
    }
}

/** Sunrise/sunset light (clear skies). */
val GoldenSky = sky(0xF6B48A, 0x8C8FD6, 0xFF7B4A, 0xFFC870, 0x2B1408, sun = 0xFFC36B, sunStrength = 1f)

/** Short blue-violet step between golden hour and night (clear skies). */
val TwilightSky = sky(0x5B4F8E, 0x2F2A66, 0xE07A8A, 0xF2A675, 0xFBF4FF, 0.26f, sun = 0xF59A8C, sunStrength = 0.8f)

/**
 * What to draw: the blended [palette] and where the sun/moon glow sits ([sunX], [sunY] as fractions
 * of the width/height). [sunHeight] is the sun's height from -1 (deep night) to 1 (noon).
 */
data class SkyState(val palette: SkyPalette, val sunX: Float, val sunY: Float, val sunHeight: Float)

/**
 * The sky for [weather] at time [now], given that day's [sunrise] and [sunset] (epoch millis).
 * Without sun times, [isDaylight] picks plain day or night with the sun high in the middle.
 */
fun skyAt(weather: SkyWeather, now: Long, sunrise: Long?, sunset: Long?, isDaylight: Boolean = true): SkyState {
    val progress: Float
    val height: Float
    if (sunrise != null && sunset != null && sunset > sunrise) {
        val dayLength = (sunset - sunrise).toFloat()
        val nightLength = DAY_MILLIS - dayLength
        if (now in sunrise..sunset) {
            progress = (now - sunrise) / dayLength
            height = sin(PI * progress).toFloat()
        } else {
            val sinceSunset = ((now - sunset) % DAY_MILLIS + DAY_MILLIS) % DAY_MILLIS
            progress = (sinceSunset / nightLength).coerceIn(0f, 1f)
            height = -sin(PI * progress).toFloat()
        }
    } else {
        progress = 0.5f
        height = if (isDaylight) 0.8f else -0.8f
    }
    return SkyState(
        palette = paletteAt(weather, height),
        sunX = 0.12f + 0.76f * progress,
        sunY = 0.62f - 0.5f * abs(height),
        sunHeight = height
    )
}

/** Blends day → golden → twilight → night by the sun's [height] (-1…1). */
fun paletteAt(weather: SkyWeather, height: Float): SkyPalette = if (weather.golden) {
    when {
        height >= 0.30f -> weather.day
        height >= 0.12f -> lerp(GoldenSky, weather.day, (height - 0.12f) / 0.18f)
        height >= 0f -> GoldenSky
        height >= -0.12f -> lerp(TwilightSky, GoldenSky, (height + 0.12f) / 0.12f)
        height >= -0.30f -> lerp(weather.night, TwilightSky, (height + 0.30f) / 0.18f)
        else -> weather.night
    }
} else {
    when {
        height >= 0.15f -> weather.day
        height >= -0.20f -> lerp(weather.night, weather.day, (height + 0.20f) / 0.35f)
        else -> weather.night
    }
}

/** Colour-by-colour blend; text switches to whichever end's text suits the blended base. */
fun lerp(a: SkyPalette, b: SkyPalette, t: Float): SkyPalette {
    val f = t.coerceIn(0f, 1f)
    val base = lerp(a.base, b.base, f)
    val lighter = if (a.base.luminance() >= b.base.luminance()) a else b
    val darker = if (lighter === a) b else a
    return SkyPalette(
        base = base,
        blobs = a.blobs.indices.map { lerp(a.blobs[it], b.blobs[it], f) },
        text = if (base.luminance() > 0.42f) lighter.text else darker.text,
        grain = a.grain + (b.grain - a.grain) * f,
        sun = lerp(a.sun, b.sun, f),
        sunStrength = a.sunStrength + (b.sunStrength - a.sunStrength) * f
    )
}

private const val DAY_MILLIS = 24f * 60 * 60 * 1000
private const val MOON = 0xE9E4FF

private fun sky(
    base: Int,
    b1: Int,
    b2: Int,
    b3: Int,
    text: Int,
    grain: Float = 0.22f,
    sun: Int,
    sunStrength: Float,
) = SkyPalette(
    base = rgb(base),
    blobs = listOf(rgb(b1), rgb(b2), rgb(b3)),
    text = rgb(text),
    grain = grain,
    sun = rgb(sun),
    sunStrength = sunStrength
)

private fun rgb(value: Int) = Color(0xFF000000.toInt() or value)
