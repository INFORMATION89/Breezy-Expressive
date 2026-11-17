/*
 * This file is part of Breezy Expressive and is derived from Breezy Weather
 * (breezyweather/domain/weather/reference/WeatherCode.kt), modified for this app.
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

package io.github.information89.breezyexpressive.data.model

import kotlinx.serialization.Serializable

/** Broad sky/precipitation condition, shared by all sources. */
@Serializable
enum class WeatherCode {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    WIND,
    FOG,
    HAZE,
    SLEET,
    HAIL,
    THUNDER,
    THUNDERSTORM,
    ;

    val isPrecipitation: Boolean
        get() = this == RAIN || this == SNOW || this == SLEET || this == HAIL || this == THUNDERSTORM
}
