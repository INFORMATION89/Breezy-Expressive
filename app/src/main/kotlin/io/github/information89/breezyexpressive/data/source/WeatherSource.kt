package io.github.information89.breezyexpressive.data.source

import io.github.information89.breezyexpressive.data.model.AirQualityHour
import io.github.information89.breezyexpressive.data.model.Alert
import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Current
import io.github.information89.breezyexpressive.data.model.Daily
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Location
import io.github.information89.breezyexpressive.data.model.Minutely

/** One weather provider. The repository picks a source per feature and merges the parts. */
interface WeatherSource {
    val id: String
    val name: String
    val features: Set<Feature>

    /** How well [feature] is covered at [location]. Shown to the user when choosing a source. */
    fun coverage(location: Location, feature: Feature): Coverage

    /** Fetches [features] for [location]. Throws on network/server errors. */
    suspend fun fetch(location: Location, features: Set<Feature>): WeatherPart
}

/** What one source returned; null means "not requested or not provided". */
data class WeatherPart(
    val timeZone: String? = null,
    val current: Current? = null,
    val hourly: List<Hourly>? = null,
    val daily: List<Daily>? = null,
    val minutely: List<Minutely>? = null,
    val airQuality: List<AirQualityHour>? = null,
    val alerts: List<Alert>? = null,
)

/** Simple bounding box check for coverage areas. */
data class BoundingBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    operator fun contains(location: Location): Boolean =
        location.latitude in south..north && location.longitude in west..east
}
