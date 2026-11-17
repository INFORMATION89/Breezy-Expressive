/*
 * This file is part of Breezy Expressive. The WMO weather-code mapping is derived from
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

import io.github.information89.breezyexpressive.data.model.AirQualityHour
import io.github.information89.breezyexpressive.data.model.Current
import io.github.information89.breezyexpressive.data.model.Daily
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Minutely
import io.github.information89.breezyexpressive.data.model.Pollen
import io.github.information89.breezyexpressive.data.model.WeatherCode
import io.github.information89.breezyexpressive.data.source.WeatherPart

/** Pure conversion from Open-Meteo JSON to our models. Unit-tested with recorded responses. */
internal object OpenMeteoMapper {

    fun map(forecast: OmForecastResponse?, airQuality: OmAirQualityResponse?): WeatherPart = WeatherPart(
        timeZone = forecast?.timezone,
        current = forecast?.current?.let(::mapCurrent),
        hourly = forecast?.hourly?.let(::mapHourly),
        daily = forecast?.daily?.let(::mapDaily),
        minutely = forecast?.minutely15?.let(::mapMinutely),
        airQuality = airQuality?.hourly?.let(::mapAirQuality),
    )

    private fun mapCurrent(c: OmCurrent) = Current(
        time = c.time,
        temperature = c.temperature,
        apparentTemperature = c.apparentTemperature,
        weatherCode = weatherCode(c.weatherCode),
        isDay = c.isDay != 0,
        precipitation = c.precipitation,
        windSpeed = c.windSpeed,
        windGusts = c.windGusts,
        windDirection = c.windDirection,
        uvIndex = c.uvIndex,
        humidity = c.humidity,
        dewPoint = c.dewPoint,
        pressure = c.pressure,
        cloudCover = c.cloudCover,
        visibility = c.visibility,
    )

    private fun mapHourly(h: OmHourly) = h.time.mapIndexed { i, time ->
        Hourly(
            time = time,
            temperature = h.temperature.at(i),
            apparentTemperature = h.apparentTemperature.at(i),
            weatherCode = weatherCode(h.weatherCode.at(i)),
            isDay = h.isDay.at(i) != 0,
            precipitationProbability = h.precipitationProbability.at(i),
            precipitation = h.precipitation.at(i),
            windSpeed = h.windSpeed.at(i),
            windGusts = h.windGusts.at(i),
            uvIndex = h.uvIndex.at(i),
            humidity = h.humidity.at(i),
            visibility = h.visibility.at(i),
        )
    }

    private fun mapDaily(d: OmDaily) = d.time.mapIndexed { i, date ->
        Daily(
            date = date,
            weatherCode = weatherCode(d.weatherCode.at(i)),
            temperatureMax = d.temperatureMax.at(i),
            temperatureMin = d.temperatureMin.at(i),
            apparentTemperatureMax = d.apparentTemperatureMax.at(i),
            apparentTemperatureMin = d.apparentTemperatureMin.at(i),
            precipitationProbabilityMax = d.precipitationProbabilityMax.at(i),
            precipitationSum = d.precipitationSum.at(i),
            sunrise = d.sunrise.at(i),
            sunset = d.sunset.at(i),
            uvIndexMax = d.uvIndexMax.at(i),
            windSpeedMax = d.windSpeedMax.at(i),
            windGustsMax = d.windGustsMax.at(i),
        )
    }

    private fun mapMinutely(m: OmMinutely) = m.time.mapIndexed { i, time ->
        Minutely(time = time, precipitation = m.precipitation.at(i))
    }

    private fun mapAirQuality(a: OmAirQualityHourly) = a.time.mapIndexed { i, time ->
        val pollen = Pollen(
            alder = a.alder.at(i),
            birch = a.birch.at(i),
            grass = a.grass.at(i),
            mugwort = a.mugwort.at(i),
            olive = a.olive.at(i),
            ragweed = a.ragweed.at(i),
        )
        AirQualityHour(
            time = time,
            usAqi = a.usAqi.at(i),
            pm25 = a.pm25.at(i),
            pm10 = a.pm10.at(i),
            ozone = a.ozone.at(i),
            nitrogenDioxide = a.nitrogenDioxide.at(i),
            // Outside the pollen coverage area every value is null: store no pollen at all.
            pollen = pollen.takeIf { it.max != null },
        )
    }

    /** WMO weather interpretation codes → our codes (mapping from Breezy Weather). */
    fun weatherCode(code: Int?): WeatherCode? = when (code) {
        null -> null
        0, 1 -> WeatherCode.CLEAR // Clear sky or mainly clear
        2 -> WeatherCode.PARTLY_CLOUDY
        3 -> WeatherCode.CLOUDY // Overcast
        45, 48 -> WeatherCode.FOG // Fog and depositing rime fog
        51, 53, 55, // Drizzle
        56, 57, // Freezing drizzle
        61, 63, 65, // Rain
        66, 67, // Freezing rain
        80, 81, 82, // Rain showers
        -> WeatherCode.RAIN
        71, 73, 75, // Snowfall
        85, 86, // Snow showers
        -> WeatherCode.SNOW
        77 -> WeatherCode.SLEET // Snow grains
        95, 96, 99 -> WeatherCode.THUNDERSTORM // Thunderstorm, with or without hail
        else -> null
    }

    private fun <T> List<T?>?.at(index: Int): T? = this?.getOrNull(index)
}
