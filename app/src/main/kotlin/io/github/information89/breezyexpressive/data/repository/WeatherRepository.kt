package io.github.information89.breezyexpressive.data.repository

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.FeatureCoverage
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.data.source.HttpException
import io.github.information89.breezyexpressive.data.source.WeatherPart
import io.github.information89.breezyexpressive.data.source.WeatherSource
import io.github.information89.breezyexpressive.data.store.WeatherCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import java.io.IOException

enum class RefreshError { OFFLINE, SERVER, BAD_DATA, UNKNOWN }

data class WeatherState(
    /** Last known weather, possibly from cache. Null only before the first successful load. */
    val weather: Weather?,
    val isRefreshing: Boolean,
    val error: RefreshError? = null,
)

/**
 * Cache-first weather: emits the cached weather immediately, then refreshes from the network
 * when it's older than [maxAgeSeconds] (or when forced). Each feature comes from its chosen source.
 */
class WeatherRepository(
    private val sources: List<WeatherSource>,
    private val cache: WeatherCache,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    /** The user's preferred source id per feature (Settings → Sources). Null = automatic. */
    private val preferredSource: (Feature) -> String? = { null },
    private val maxAgeSeconds: Long = 15 * 60,
) {

    fun weather(location: Location, force: Boolean = false): Flow<WeatherState> = flow {
        val cached = cache.read(location.id)
        val isFresh = cached != null && now() - cached.updatedAt < maxAgeSeconds
        val needsRefresh = force || !isFresh
        emit(WeatherState(cached, isRefreshing = needsRefresh))
        if (!needsRefresh) return@flow

        try {
            emit(WeatherState(refresh(location), isRefreshing = false))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(WeatherState(cached, isRefreshing = false, error = e.toRefreshError()))
        }
    }

    /** Fetches every feature from its source, merges the parts, caches and returns the result. */
    suspend fun refresh(location: Location): Weather {
        val plan: Map<WeatherSource, Set<Feature>> = Feature.entries
            .mapNotNull { feature -> sourceFor(location, feature)?.let { it to feature } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

        val results = coroutineScope {
            plan.map { (source, features) ->
                async { Triple(source, features, runCatchingNonCancel { source.fetch(location, features) }) }
            }.awaitAll()
        }

        // The forecast is essential; anything else (air quality, alerts) may fail without blocking.
        val forecastResult = results.firstOrNull { Feature.FORECAST in it.second }?.third
        forecastResult?.exceptionOrNull()?.let { throw it }

        val succeeded = results.filter { it.third.isSuccess }
        val parts = succeeded.map { it.third.getOrThrow() }
        val coverage = Feature.entries.associateWith { feature ->
            val source = succeeded.firstOrNull { feature in it.second }?.first
            if (source == null) {
                FeatureCoverage(sourceId = NO_SOURCE, coverage = Coverage.NONE)
            } else {
                FeatureCoverage(sourceId = source.id, coverage = source.coverage(location, feature))
            }
        }

        val weather = merge(parts, location, coverage)
        cache.write(location.id, weather)
        return weather
    }

    /** The preferred source if it supports [feature], else the first source that covers it here. */
    internal fun sourceFor(location: Location, feature: Feature): WeatherSource? {
        val candidates = sources.filter { feature in it.features }
        val preferred = preferredSource(feature)?.let { id -> candidates.firstOrNull { it.id == id } }
        return preferred
            ?: candidates.firstOrNull { it.coverage(location, feature) != Coverage.NONE }
    }

    private fun merge(parts: List<WeatherPart>, location: Location, coverage: Map<Feature, FeatureCoverage>) = Weather(
        updatedAt = now(),
        timeZone = parts.firstNotNullOfOrNull { it.timeZone } ?: location.timeZone,
        current = parts.firstNotNullOfOrNull { it.current },
        hourly = parts.firstNotNullOfOrNull { it.hourly }.orEmpty(),
        daily = parts.firstNotNullOfOrNull { it.daily }.orEmpty(),
        minutely = parts.firstNotNullOfOrNull { it.minutely }.orEmpty(),
        airQuality = parts.firstNotNullOfOrNull { it.airQuality }.orEmpty(),
        alerts = parts.flatMap { it.alerts.orEmpty() },
        coverage = coverage,
    )

    private inline fun <T> runCatchingNonCancel(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun Throwable.toRefreshError() = when (this) {
        is HttpException -> RefreshError.SERVER
        is SerializationException, is IllegalArgumentException -> RefreshError.BAD_DATA
        is IOException -> RefreshError.OFFLINE
        else -> RefreshError.UNKNOWN
    }

    companion object {
        const val NO_SOURCE = "none"
    }
}
