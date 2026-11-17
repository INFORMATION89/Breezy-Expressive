package io.github.information89.breezyexpressive.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.data.repository.RefreshError
import io.github.information89.breezyexpressive.data.repository.WeatherRepository
import io.github.information89.breezyexpressive.insight.InsightEngine
import io.github.information89.breezyexpressive.insight.Insights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

data class ForecastUiState(
    val location: Location,
    val weather: Weather? = null,
    val insights: Insights? = null,
    /** The moment the insights were computed for (epoch seconds). */
    val now: Long = System.currentTimeMillis() / 1000,
    val isRefreshing: Boolean = true,
    val error: RefreshError? = null,
)

/** Placeholder until M3 adds GPS, search and saved locations. */
object DevDefaults {
    val location = Location(
        id = "dev-mexico-city",
        name = "Mexico City",
        latitude = 19.4326,
        longitude = -99.1332,
        timeZone = "America/Mexico_City",
        countryCode = "MX",
    )
}

class ForecastViewModel(
    private val repository: WeatherRepository,
    private val location: Location = DevDefaults.location,
) : ViewModel() {

    private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Re-evaluates wording ("in 20 min") every minute without refetching. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis() / 1000)
            delay(60_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ForecastUiState> =
        // Normal load on (re)subscribe; forced reload on pull-to-refresh.
        merge(flowOf(false), manualRefresh.map { true })
            .flatMapLatest { force -> repository.weather(location, force) }
            .combine(clock) { weatherState, now ->
                ForecastUiState(
                    location = location,
                    weather = weatherState.weather,
                    insights = weatherState.weather?.let { InsightEngine.analyze(it, now) },
                    now = now,
                    isRefreshing = weatherState.isRefreshing,
                    error = weatherState.error,
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState(location))

    fun refresh() {
        manualRefresh.tryEmit(Unit)
    }
}
