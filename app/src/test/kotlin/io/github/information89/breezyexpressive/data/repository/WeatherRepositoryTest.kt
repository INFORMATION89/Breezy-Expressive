package io.github.information89.breezyexpressive.data.repository

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Current
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.model.Alert
import io.github.information89.breezyexpressive.data.source.Network
import io.github.information89.breezyexpressive.data.source.WeatherPart
import io.github.information89.breezyexpressive.data.source.WeatherSource
import io.github.information89.breezyexpressive.data.store.WeatherCache
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class WeatherRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val location = Location("loc", "Somewhere", 19.4, -99.1, "America/Mexico_City")

    private class FakeSource(
        override val id: String,
        override val features: Set<Feature>,
        var failWith: Exception? = null,
        val part: WeatherPart = WeatherPart(),
    ) : WeatherSource {
        override val name = id
        var calls = 0
        override fun coverage(location: Location, feature: Feature) =
            if (feature == Feature.POLLEN) Coverage.NONE else Coverage.FULL

        override suspend fun fetch(location: Location, features: Set<Feature>): WeatherPart {
            calls++
            failWith?.let { throw it }
            return part
        }
    }

    private var clock = 1_000_000L

    private fun repo(vararg sources: WeatherSource, preferred: (Feature) -> String? = { null }) = WeatherRepository(
        sources = sources.toList(),
        cache = WeatherCache(tmp.root, Network.json),
        now = { clock },
        preferredSource = preferred,
    )

    private val forecastPart = WeatherPart(
        timeZone = "America/Mexico_City",
        current = Current(time = 1_000_000L, temperature = 20.0),
        hourly = listOf(Hourly(time = 1_000_000L, temperature = 20.0)),
    )

    @Test
    fun `first load fetches, then cache is served while fresh`() = runTest {
        val source = FakeSource("om", setOf(Feature.CURRENT, Feature.FORECAST, Feature.POLLEN), part = forecastPart)
        val repo = repo(source)

        val first = repo.weather(location).toList()
        assertEquals(2, first.size)
        assertNull(first[0].weather)
        assertTrue(first[0].isRefreshing)
        assertEquals(20.0, first[1].weather!!.current!!.temperature!!, 0.0)

        clock += 60 // a minute later: fresh, no network
        val second = repo.weather(location).toList()
        assertEquals(1, second.size)
        assertFalse(second[0].isRefreshing)
        assertEquals(1, source.calls)
    }

    @Test
    fun `offline keeps showing the cached weather`() = runTest {
        val source = FakeSource("om", setOf(Feature.CURRENT, Feature.FORECAST), part = forecastPart)
        val repo = repo(source)
        repo.weather(location).toList()

        clock += 3600
        source.failWith = IOException("no network")
        val states = repo.weather(location).toList()

        val last = states.last()
        assertEquals(RefreshError.OFFLINE, last.error)
        assertEquals(20.0, last.weather!!.current!!.temperature!!, 0.0)
    }

    @Test
    fun `features come from their sources and coverage is recorded`() = runTest {
        val forecast = FakeSource("om", setOf(Feature.CURRENT, Feature.FORECAST, Feature.POLLEN), part = forecastPart)
        val alerts = FakeSource("accu", setOf(Feature.ALERTS), part = WeatherPart(alerts = listOf(Alert("1", "Storm warning"))))

        val weather = repo(forecast, alerts).refresh(location)

        assertEquals("Storm warning", weather.alerts.single().title)
        assertEquals("om", weather.coverage[Feature.FORECAST]!!.sourceId)
        assertEquals("accu", weather.coverage[Feature.ALERTS]!!.sourceId)
        // No source covers pollen here, so it's marked as missing: partial coverage.
        assertEquals(Coverage.NONE, weather.coverage[Feature.POLLEN]!!.coverage)
        assertFalse(weather.isFullCoverage)
    }

    @Test
    fun `a failing alerts source does not block the forecast`() = runTest {
        val forecast = FakeSource("om", setOf(Feature.CURRENT, Feature.FORECAST), part = forecastPart)
        val alerts = FakeSource("accu", setOf(Feature.ALERTS), failWith = IOException("quota"))

        val weather = repo(forecast, alerts).refresh(location)

        assertEquals(20.0, weather.current!!.temperature!!, 0.0)
        assertEquals(Coverage.NONE, weather.coverage[Feature.ALERTS]!!.coverage)
    }

    @Test
    fun `preferred source wins when it supports the feature`() {
        val a = FakeSource("a", setOf(Feature.FORECAST))
        val b = FakeSource("b", setOf(Feature.FORECAST))
        val repo = repo(a, b, preferred = { "b" })

        assertEquals("b", repo.sourceFor(location, Feature.FORECAST)!!.id)
    }
}
