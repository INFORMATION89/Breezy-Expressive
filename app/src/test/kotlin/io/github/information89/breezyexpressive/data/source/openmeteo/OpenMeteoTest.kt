package io.github.information89.breezyexpressive.data.source.openmeteo

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.model.WeatherCode
import io.github.information89.breezyexpressive.data.source.Network
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses real Open-Meteo responses recorded on 2026-11-17 (src/test/resources/openmeteo). */
class OpenMeteoTest {

    private val mexicoCity = Location("mx", "Mexico City", 19.4326, -99.1332, "America/Mexico_City", countryCode = "MX")
    private val berlin = Location("de", "Berlin", 52.52, 13.405, "Europe/Berlin", countryCode = "DE")
    private val source = OpenMeteoSource(OkHttpClient(), Network.json)

    private fun fixture(name: String) =
        requireNotNull(javaClass.classLoader!!.getResource("openmeteo/$name")) { "missing fixture $name" }.readText()

    private fun parse(city: String) = OpenMeteoMapper.map(
        Network.json.decodeFromString<OmForecastResponse>(fixture("forecast_$city.json")),
        Network.json.decodeFromString<OmAirQualityResponse>(fixture("airquality_$city.json")),
    )

    @Test
    fun `parses Mexico City forecast`() {
        val part = parse("mexico_city")
        assertEquals("America/Mexico_City", part.timeZone)

        val current = assertNotNull(part.current).let { part.current!! }
        assertEquals(13.5, current.temperature!!, 0.01)
        assertEquals(WeatherCode.CLOUDY, current.weatherCode) // WMO 3 = overcast
        assertEquals(false, current.isDay)

        assertEquals(8 * 24, part.hourly!!.size) // yesterday + 7 days
        assertEquals(8, part.daily!!.size)
        assertEquals(28, part.minutely!!.size) // 4 past + 24 future steps
        assertTrue(part.daily!!.all { it.sunrise != null && it.sunset != null })
        assertTrue(part.hourly!!.zipWithNext().all { (a, b) -> b.time - a.time == 3600L })
    }

    @Test
    fun `no pollen outside Europe, pollen in Berlin`() {
        val mexico = parse("mexico_city").airQuality!!
        assertTrue(mexico.isNotEmpty())
        assertTrue(mexico.all { it.pollen == null })
        assertNotNull(mexico.first().usAqi)

        val berlinAir = parse("berlin").airQuality!!
        assertTrue(berlinAir.any { it.pollen != null })
    }

    @Test
    fun `maps WMO codes like Breezy Weather`() {
        assertEquals(WeatherCode.CLEAR, OpenMeteoMapper.weatherCode(1))
        assertEquals(WeatherCode.PARTLY_CLOUDY, OpenMeteoMapper.weatherCode(2))
        assertEquals(WeatherCode.FOG, OpenMeteoMapper.weatherCode(48))
        assertEquals(WeatherCode.RAIN, OpenMeteoMapper.weatherCode(81))
        assertEquals(WeatherCode.SNOW, OpenMeteoMapper.weatherCode(86))
        assertEquals(WeatherCode.THUNDERSTORM, OpenMeteoMapper.weatherCode(99))
        assertNull(OpenMeteoMapper.weatherCode(42))
    }

    @Test
    fun `coverage is partial in Mexico City and full in Berlin`() {
        assertEquals(Coverage.ESTIMATED, source.coverage(mexicoCity, Feature.MINUTELY))
        assertEquals(Coverage.NONE, source.coverage(mexicoCity, Feature.POLLEN))
        assertEquals(Coverage.FULL, source.coverage(mexicoCity, Feature.AIR_QUALITY))
        assertEquals(Coverage.FULL, source.coverage(berlin, Feature.MINUTELY))
        assertEquals(Coverage.FULL, source.coverage(berlin, Feature.POLLEN))
        assertEquals(Coverage.NONE, source.coverage(berlin, Feature.ALERTS))
    }

    @Test
    fun `requests only what is needed`() {
        val url = source.forecastUrl(mexicoCity, setOf(Feature.CURRENT, Feature.FORECAST, Feature.MINUTELY))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("1", url.queryParameter("past_days"))
        assertEquals("precipitation", url.queryParameter("minutely_15"))

        val currentOnly = source.forecastUrl(mexicoCity, setOf(Feature.CURRENT))
        assertNull(currentOnly.queryParameter("hourly"))

        val air = source.airQualityUrl(mexicoCity, setOf(Feature.AIR_QUALITY))
        assertTrue("pollen" !in air.queryParameter("hourly")!!)
    }
}
