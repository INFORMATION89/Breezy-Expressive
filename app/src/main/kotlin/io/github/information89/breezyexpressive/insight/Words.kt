package io.github.information89.breezyexpressive.insight

import io.github.information89.breezyexpressive.data.model.WeatherCode

/*
 * Numbers → words. Every value the app shows has a word level first (PLAN.md 5.0).
 * These are plain levels; the UI turns them into localized text.
 */

/** How the temperature feels, from the apparent ("feels like") temperature in °C. */
enum class TempFeel { FREEZING, COLD, CHILLY, COOL, MILD, WARM, HOT, VERY_HOT }

fun tempFeel(apparentC: Double): TempFeel = when {
    apparentC < 0 -> TempFeel.FREEZING
    apparentC < 8 -> TempFeel.COLD
    apparentC < 14 -> TempFeel.CHILLY
    apparentC < 18 -> TempFeel.COOL
    apparentC < 22 -> TempFeel.MILD
    apparentC < 27 -> TempFeel.WARM
    apparentC < 33 -> TempFeel.HOT
    else -> TempFeel.VERY_HOT
}

enum class Sky { SUNNY, CLEAR_NIGHT, PARTLY_CLOUDY, CLOUDY, FOGGY, HAZY, WINDY, RAINY, SNOWY, SLEETY, STORMY }

fun sky(code: WeatherCode?, isDay: Boolean): Sky = when (code) {
    null, WeatherCode.CLEAR -> if (isDay) Sky.SUNNY else Sky.CLEAR_NIGHT
    WeatherCode.PARTLY_CLOUDY -> Sky.PARTLY_CLOUDY
    WeatherCode.CLOUDY -> Sky.CLOUDY
    WeatherCode.FOG -> Sky.FOGGY
    WeatherCode.HAZE -> Sky.HAZY
    WeatherCode.WIND -> Sky.WINDY
    WeatherCode.RAIN -> Sky.RAINY
    WeatherCode.SNOW -> Sky.SNOWY
    WeatherCode.SLEET, WeatherCode.HAIL -> Sky.SLEETY
    WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> Sky.STORMY
}

/** 0 = clear … 3 = precipitation. Used to describe trends ("clouding over", "clearing up"). */
internal fun cloudiness(code: WeatherCode?): Int = when (code) {
    null, WeatherCode.CLEAR, WeatherCode.WIND -> 0
    WeatherCode.PARTLY_CLOUDY, WeatherCode.HAZE -> 1
    WeatherCode.CLOUDY, WeatherCode.FOG -> 2
    else -> 3
}

enum class WindLevel { CALM, LIGHT, BREEZY, WINDY, STRONG }

/** From sustained wind and gusts in km/h. */
fun windLevel(speedKmh: Double?, gustsKmh: Double? = null): WindLevel {
    val speed = speedKmh ?: 0.0
    if ((gustsKmh ?: 0.0) >= 60) return WindLevel.STRONG
    return when {
        speed < 6 -> WindLevel.CALM
        speed < 20 -> WindLevel.LIGHT
        speed < 30 -> WindLevel.BREEZY
        speed < 50 -> WindLevel.WINDY
        else -> WindLevel.STRONG
    }
}

enum class UvLevel { LOW, MODERATE, HIGH, VERY_HIGH, EXTREME }

fun uvLevel(index: Double): UvLevel = when {
    index < 3 -> UvLevel.LOW
    index < 6 -> UvLevel.MODERATE
    index < 8 -> UvLevel.HIGH
    index < 11 -> UvLevel.VERY_HIGH
    else -> UvLevel.EXTREME
}

/** US AQI categories. */
enum class AirLevel { GOOD, MODERATE, SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }

fun airLevel(usAqi: Int): AirLevel = when {
    usAqi <= 50 -> AirLevel.GOOD
    usAqi <= 100 -> AirLevel.MODERATE
    usAqi <= 150 -> AirLevel.SENSITIVE
    usAqi <= 200 -> AirLevel.UNHEALTHY
    usAqi <= 300 -> AirLevel.VERY_UNHEALTHY
    else -> AirLevel.HAZARDOUS
}

enum class PollenLevel { NONE, LOW, MODERATE, HIGH, VERY_HIGH }

/** From the highest pollen count in grains/m³ (rough, cross-species thresholds). */
fun pollenLevel(grains: Double): PollenLevel = when {
    grains < 1 -> PollenLevel.NONE
    grains < 20 -> PollenLevel.LOW
    grains < 80 -> PollenLevel.MODERATE
    grains < 300 -> PollenLevel.HIGH
    else -> PollenLevel.VERY_HIGH
}

/** Ordered by how much it matters, so the "worst" kind of an episode wins. */
enum class PrecipKind { RAIN, SNOW, SLEET, HAIL, STORM }

fun precipKind(code: WeatherCode?): PrecipKind = when (code) {
    WeatherCode.SNOW -> PrecipKind.SNOW
    WeatherCode.SLEET -> PrecipKind.SLEET
    WeatherCode.HAIL -> PrecipKind.HAIL
    WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> PrecipKind.STORM
    else -> PrecipKind.RAIN
}

enum class Intensity { LIGHT, MODERATE, HEAVY }

/** From mm per hour. */
fun intensity(mmPerHour: Double): Intensity = when {
    mmPerHour < 2.5 -> Intensity.LIGHT
    mmPerHour < 7.6 -> Intensity.MODERATE
    else -> Intensity.HEAVY
}
