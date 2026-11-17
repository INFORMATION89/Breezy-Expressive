/*
 * This file is part of Breezy Expressive. Requested fields and coverage areas are derived from
 * Breezy Weather (sources/openmeteo/OpenMeteoService.kt), modified for this app.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.information89.breezyexpressive.data.source.openmeteo

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.source.BoundingBox
import io.github.information89.breezyexpressive.data.source.WeatherPart
import io.github.information89.breezyexpressive.data.source.WeatherSource
import io.github.information89.breezyexpressive.data.source.getJson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class OpenMeteoSource(
    private val client: OkHttpClient,
    private val json: Json,
) : WeatherSource {

    override val id = "openmeteo"
    override val name = "Open-Meteo"
    override val features = setOf(Feature.CURRENT, Feature.FORECAST, Feature.MINUTELY, Feature.AIR_QUALITY, Feature.POLLEN)

    override fun coverage(location: Location, feature: Feature): Coverage = when (feature) {
        Feature.CURRENT, Feature.FORECAST, Feature.AIR_QUALITY -> Coverage.FULL
        Feature.MINUTELY -> if (PRECISE_MINUTELY_AREAS.any { location in it }) Coverage.FULL else Coverage.ESTIMATED
        Feature.POLLEN -> if (location in COPERNICUS_POLLEN_BBOX) Coverage.FULL else Coverage.NONE
        Feature.ALERTS -> Coverage.NONE
    }

    override suspend fun fetch(location: Location, features: Set<Feature>): WeatherPart = coroutineScope {
        val wantsForecast = features.any { it == Feature.CURRENT || it == Feature.FORECAST || it == Feature.MINUTELY }
        val wantsAir = Feature.AIR_QUALITY in features ||
            (Feature.POLLEN in features && coverage(location, Feature.POLLEN) != Coverage.NONE)

        val forecast = if (wantsForecast) {
            async { client.getJson<OmForecastResponse>(json, forecastUrl(location, features)) }
        } else {
            null
        }
        val air = if (wantsAir) {
            async { client.getJson<OmAirQualityResponse>(json, airQualityUrl(location, features)) }
        } else {
            null
        }
        OpenMeteoMapper.map(forecast?.await(), air?.await())
    }

    internal fun forecastUrl(location: Location, features: Set<Feature>): HttpUrl =
        FORECAST_URL.toHttpUrl().newBuilder()
            .addCoordinates(location)
            .apply {
                if (Feature.CURRENT in features) addQueryParameter("current", CURRENT_FIELDS)
                if (Feature.FORECAST in features) {
                    addQueryParameter("hourly", HOURLY_FIELDS)
                    addQueryParameter("daily", DAILY_FIELDS)
                    addQueryParameter("forecast_days", "7")
                    // Yesterday is used for "colder/warmer than yesterday".
                    addQueryParameter("past_days", "1")
                }
                if (Feature.MINUTELY in features) {
                    addQueryParameter("minutely_15", "precipitation")
                    addQueryParameter("forecast_minutely_15", "24") // next 6 hours
                    addQueryParameter("past_minutely_15", "4")
                }
            }
            .addCommonParameters()
            .build()

    internal fun airQualityUrl(location: Location, features: Set<Feature>): HttpUrl {
        val fields = buildList {
            if (Feature.AIR_QUALITY in features) addAll(AIR_QUALITY_FIELDS)
            if (Feature.POLLEN in features) addAll(POLLEN_FIELDS)
        }
        return AIR_QUALITY_URL.toHttpUrl().newBuilder()
            .addCoordinates(location)
            .addQueryParameter("hourly", fields.joinToString(","))
            .addQueryParameter("forecast_days", "3")
            .addCommonParameters()
            .build()
    }

    private fun HttpUrl.Builder.addCoordinates(location: Location) = this
        .addQueryParameter("latitude", location.latitude.toString())
        .addQueryParameter("longitude", location.longitude.toString())

    private fun HttpUrl.Builder.addCommonParameters() = this
        .addQueryParameter("timezone", "auto")
        .addQueryParameter("timeformat", "unixtime")

    internal companion object {
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"

        const val CURRENT_FIELDS = "temperature_2m,apparent_temperature,weather_code,is_day,precipitation," +
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,relative_humidity_2m,dew_point_2m," +
            "pressure_msl,cloud_cover,visibility"
        const val HOURLY_FIELDS = "temperature_2m,apparent_temperature,precipitation_probability,precipitation," +
            "weather_code,is_day,wind_speed_10m,wind_gusts_10m,uv_index,relative_humidity_2m,visibility"
        const val DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max," +
            "apparent_temperature_min,precipitation_probability_max,precipitation_sum,sunrise,sunset," +
            "uv_index_max,wind_speed_10m_max,wind_gusts_10m_max"
        val AIR_QUALITY_FIELDS = listOf("us_aqi", "pm2_5", "pm10", "ozone", "nitrogen_dioxide")
        val POLLEN_FIELDS = listOf(
            "alder_pollen", "birch_pollen", "grass_pollen", "mugwort_pollen", "olive_pollen", "ragweed_pollen",
        )

        /**
         * CAMS European air quality forecasts (pollen): 25° W–45° E, 30° N–72° N.
         * Source: https://ads.atmosphere.copernicus.eu/datasets/cams-europe-air-quality-forecasts
         */
        val COPERNICUS_POLLEN_BBOX = BoundingBox(south = 30.0, west = -25.0, north = 72.0, east = 45.0)

        /**
         * Where Open-Meteo has native 15-minute models (European regional models, US/Canada HRRR-class models).
         * Elsewhere 15-minute data is interpolated from hourly data, so we call it "estimated".
         */
        val PRECISE_MINUTELY_AREAS = listOf(
            BoundingBox(south = 35.0, west = -12.0, north = 72.0, east = 32.0), // Europe
            BoundingBox(south = 21.0, west = -135.0, north = 56.0, east = -60.0), // Contiguous US + southern Canada
        )
    }
}
