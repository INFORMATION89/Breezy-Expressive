package io.github.information89.breezyexpressive.data.model

import kotlinx.serialization.Serializable

/*
 * Weather data shared by all sources. Units are fixed (converted for display later):
 * temperature °C, wind km/h, precipitation mm, pressure hPa, visibility m.
 * Times are epoch seconds (UTC); the location's time zone lives on [Weather.timeZone].
 */

@Serializable
data class Location(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val region: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
)

/** The kinds of data a source can provide. */
@Serializable
enum class Feature { CURRENT, FORECAST, MINUTELY, AIR_QUALITY, POLLEN, ALERTS }

/** How well a feature is covered for a given location. */
@Serializable
enum class Coverage {
    /** Real data at full precision. */
    FULL,

    /** Available, but estimated (e.g. 15-minute rain interpolated from hourly data). */
    ESTIMATED,

    /** Not available here. */
    NONE,
}

@Serializable
data class Weather(
    val updatedAt: Long,
    val timeZone: String,
    val current: Current? = null,
    val hourly: List<Hourly> = emptyList(),
    val daily: List<Daily> = emptyList(),
    /** 15-minute precipitation for the next few hours. */
    val minutely: List<Minutely> = emptyList(),
    val airQuality: List<AirQualityHour> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    /** Which source provided each feature, and how well it covers this location. */
    val coverage: Map<Feature, FeatureCoverage> = emptyMap(),
) {
    /** Full when every feature is fully covered; otherwise partial (the UI lists what's missing). */
    val isFullCoverage: Boolean
        get() = Feature.entries.all { coverage[it]?.coverage == Coverage.FULL }
}

@Serializable
data class FeatureCoverage(val sourceId: String, val coverage: Coverage)

@Serializable
data class Current(
    val time: Long,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val weatherCode: WeatherCode? = null,
    val isDay: Boolean = true,
    val precipitation: Double? = null,
    val windSpeed: Double? = null,
    val windGusts: Double? = null,
    val windDirection: Int? = null,
    val uvIndex: Double? = null,
    val humidity: Int? = null,
    val dewPoint: Double? = null,
    val pressure: Double? = null,
    val cloudCover: Int? = null,
    val visibility: Double? = null,
)

@Serializable
data class Hourly(
    val time: Long,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val weatherCode: WeatherCode? = null,
    val isDay: Boolean = true,
    /** 0–100. */
    val precipitationProbability: Int? = null,
    val precipitation: Double? = null,
    val windSpeed: Double? = null,
    val windGusts: Double? = null,
    val uvIndex: Double? = null,
    val humidity: Int? = null,
    val visibility: Double? = null,
)

@Serializable
data class Daily(
    /** Start of the day (local midnight) as epoch seconds. */
    val date: Long,
    val weatherCode: WeatherCode? = null,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null,
    val apparentTemperatureMax: Double? = null,
    val apparentTemperatureMin: Double? = null,
    val precipitationProbabilityMax: Int? = null,
    val precipitationSum: Double? = null,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val uvIndexMax: Double? = null,
    val windSpeedMax: Double? = null,
    val windGustsMax: Double? = null,
)

@Serializable
data class Minutely(
    val time: Long,
    /** mm during this 15-minute step. */
    val precipitation: Double? = null,
)

@Serializable
data class AirQualityHour(
    val time: Long,
    /** US AQI, 0–500. */
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    val nitrogenDioxide: Double? = null,
    val pollen: Pollen? = null,
)

/** Pollen grains/m³. */
@Serializable
data class Pollen(
    val alder: Double? = null,
    val birch: Double? = null,
    val grass: Double? = null,
    val mugwort: Double? = null,
    val olive: Double? = null,
    val ragweed: Double? = null,
) {
    val max: Double?
        get() = listOfNotNull(alder, birch, grass, mugwort, olive, ragweed).maxOrNull()
}

@Serializable
enum class AlertSeverity { MINOR, MODERATE, SEVERE, EXTREME }

@Serializable
data class Alert(
    val id: String,
    val title: String,
    val description: String? = null,
    val severity: AlertSeverity = AlertSeverity.MODERATE,
    val start: Long? = null,
    val end: Long? = null,
    val source: String? = null,
)
