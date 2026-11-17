package io.github.information89.breezyexpressive.data.source.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* Raw Open-Meteo responses (timeformat=unixtime). Only the fields we request. */

@Serializable
internal data class OmForecastResponse(
    val timezone: String? = null,
    val current: OmCurrent? = null,
    val hourly: OmHourly? = null,
    val daily: OmDaily? = null,
    @SerialName("minutely_15") val minutely15: OmMinutely? = null,
)

@Serializable
internal data class OmCurrent(
    val time: Long,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("dew_point_2m") val dewPoint: Double? = null,
    @SerialName("pressure_msl") val pressure: Double? = null,
    @SerialName("cloud_cover") val cloudCover: Int? = null,
    val visibility: Double? = null,
)

@Serializable
internal data class OmHourly(
    val time: List<Long> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>? = null,
    val precipitation: List<Double?>? = null,
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("is_day") val isDay: List<Int?>? = null,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>? = null,
    @SerialName("wind_gusts_10m") val windGusts: List<Double?>? = null,
    @SerialName("uv_index") val uvIndex: List<Double?>? = null,
    @SerialName("relative_humidity_2m") val humidity: List<Int?>? = null,
    val visibility: List<Double?>? = null,
)

@Serializable
internal data class OmDaily(
    val time: List<Long> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?>? = null,
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double?>? = null,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @SerialName("precipitation_sum") val precipitationSum: List<Double?>? = null,
    val sunrise: List<Long?>? = null,
    val sunset: List<Long?>? = null,
    @SerialName("uv_index_max") val uvIndexMax: List<Double?>? = null,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?>? = null,
    @SerialName("wind_gusts_10m_max") val windGustsMax: List<Double?>? = null,
)

@Serializable
internal data class OmMinutely(
    val time: List<Long> = emptyList(),
    val precipitation: List<Double?>? = null,
)

@Serializable
internal data class OmAirQualityResponse(
    val hourly: OmAirQualityHourly? = null,
)

@Serializable
internal data class OmAirQualityHourly(
    val time: List<Long> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?>? = null,
    @SerialName("pm2_5") val pm25: List<Double?>? = null,
    val pm10: List<Double?>? = null,
    val ozone: List<Double?>? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: List<Double?>? = null,
    @SerialName("alder_pollen") val alder: List<Double?>? = null,
    @SerialName("birch_pollen") val birch: List<Double?>? = null,
    @SerialName("grass_pollen") val grass: List<Double?>? = null,
    @SerialName("mugwort_pollen") val mugwort: List<Double?>? = null,
    @SerialName("olive_pollen") val olive: List<Double?>? = null,
    @SerialName("ragweed_pollen") val ragweed: List<Double?>? = null,
)
