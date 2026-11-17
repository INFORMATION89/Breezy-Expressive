package io.github.information89.breezyexpressive.data.source.openmeteo

import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.source.Network
import io.github.information89.breezyexpressive.insight.InsightEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Hits the real Open-Meteo API. Skipped by default; run with:
 *   LIVE_TESTS=1 ./gradlew :app:testDebugUnitTest --tests '*OpenMeteoLiveTest*'
 */
class OpenMeteoLiveTest {

    @Test
    fun `fetches real weather for Mexico City`() = runTest {
        assumeTrue(System.getenv("LIVE_TESTS") == "1")
        val source = OpenMeteoSource(Network.client(), Network.json)
        val location = Location("mx", "Mexico City", 19.4326, -99.1332, "America/Mexico_City")

        val part = source.fetch(location, source.features)

        assertTrue(part.hourly!!.size >= 7 * 24)
        assertTrue(part.daily!!.size >= 7)
        assertTrue(part.airQuality!!.isNotEmpty())
        println("Live current: ${part.current}")
        println("Coverage: " + Feature.entries.associateWith { source.coverage(location, it) })
        val weather = io.github.information89.breezyexpressive.data.model.Weather(
            updatedAt = part.current!!.time,
            timeZone = part.timeZone!!,
            current = part.current,
            hourly = part.hourly!!,
            daily = part.daily!!,
            minutely = part.minutely!!,
            airQuality = part.airQuality!!,
        )
        println("Live summary: ${InsightEngine.analyze(weather, System.currentTimeMillis() / 1000).summary}")
    }
}
