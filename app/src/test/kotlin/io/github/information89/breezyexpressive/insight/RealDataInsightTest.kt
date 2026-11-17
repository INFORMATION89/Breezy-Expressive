package io.github.information89.breezyexpressive.insight

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.FeatureCoverage
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.data.source.Network
import io.github.information89.breezyexpressive.data.source.openmeteo.OmAirQualityResponse
import io.github.information89.breezyexpressive.data.source.openmeteo.OmForecastResponse
import io.github.information89.breezyexpressive.data.source.openmeteo.OpenMeteoMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** End-to-end on recorded real responses: parse → insights, and print them for a human check. */
class RealDataInsightTest {

    private fun fixture(name: String) = javaClass.classLoader!!.getResource("openmeteo/$name")!!.readText()

    private fun load(city: String, minutely: Coverage): Weather {
        val part = OpenMeteoMapper.map(
            Network.json.decodeFromString<OmForecastResponse>(fixture("forecast_$city.json")),
            Network.json.decodeFromString<OmAirQualityResponse>(fixture("airquality_$city.json")),
        )
        return Weather(
            updatedAt = part.current!!.time,
            timeZone = part.timeZone!!,
            current = part.current,
            hourly = part.hourly!!,
            daily = part.daily!!,
            minutely = part.minutely!!,
            airQuality = part.airQuality!!,
            coverage = mapOf(Feature.MINUTELY to FeatureCoverage("openmeteo", minutely)),
        )
    }

    @Test
    fun `Mexico City at recording time`() {
        val weather = load("mexico_city", Coverage.ESTIMATED)
        val insights = InsightEngine.analyze(weather, weather.current!!.time)
        println("Mexico City: $insights")

        // Recorded at night, 13.5 °C feeling 12.7 °C, overcast.
        assertEquals(TempFeel.CHILLY, insights.now.feel)
        assertEquals(Sky.CLOUDY, insights.now.sky)
        assertNotNull(insights.sun)
        assertEquals(12, insights.rain.bars.size)
    }

    @Test
    fun `Berlin at recording time`() {
        val weather = load("berlin", Coverage.FULL)
        val insights = InsightEngine.analyze(weather, weather.current!!.time)
        println("Berlin: $insights")
        assertNotNull(insights.summary)
    }
}
