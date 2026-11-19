/*
 * This file is part of Solstice, a fork of Breezy Weather.
 *
 * Solstice is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Solstice is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Solstice. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.expressive.components

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.hlcaptain.symbols.material.Add
import io.github.hlcaptain.symbols.material.AddLocationAlt
import io.github.hlcaptain.symbols.material.CalendarMonth
import io.github.hlcaptain.symbols.material.Check
import io.github.hlcaptain.symbols.material.ClearDay
import io.github.hlcaptain.symbols.material.ClearNight
import io.github.hlcaptain.symbols.material.Close
import io.github.hlcaptain.symbols.material.Cloud
import io.github.hlcaptain.symbols.material.CloudOff
import io.github.hlcaptain.symbols.material.Delete
import io.github.hlcaptain.symbols.material.DragIndicator
import io.github.hlcaptain.symbols.material.Explore
import io.github.hlcaptain.symbols.material.FlightLand
import io.github.hlcaptain.symbols.material.Foggy
import io.github.hlcaptain.symbols.material.History
import io.github.hlcaptain.symbols.material.HourglassEmpty
import io.github.hlcaptain.symbols.material.Info
import io.github.hlcaptain.symbols.material.Map
import io.github.hlcaptain.symbols.material.MaterialSymbol
import io.github.hlcaptain.symbols.material.MaterialSymbols
import io.github.hlcaptain.symbols.material.MyLocation
import io.github.hlcaptain.symbols.material.NearMe
import io.github.hlcaptain.symbols.material.PartlyCloudyDay
import io.github.hlcaptain.symbols.material.PartlyCloudyNight
import io.github.hlcaptain.symbols.material.Percent
import io.github.hlcaptain.symbols.material.Public
import io.github.hlcaptain.symbols.material.PushPin
import io.github.hlcaptain.symbols.material.Rainy
import io.github.hlcaptain.symbols.material.Schedule
import io.github.hlcaptain.symbols.material.Search
import io.github.hlcaptain.symbols.material.SearchOff
import io.github.hlcaptain.symbols.material.Settings
import io.github.hlcaptain.symbols.material.Star
import io.github.hlcaptain.symbols.material.Thunderstorm
import io.github.hlcaptain.symbols.material.WaterDrop
import io.github.hlcaptain.symbols.material.WeatherSnowy
import io.github.hlcaptain.symbols.material.rounded.vectors.asRoundedImageVector
import org.breezyweather.ui.expressive.sky.SkyWeather

/**
 * Every icon Solstice's own screens use, from one set (decided 2026-11-19): Google's Material Symbols,
 * **Rounded**, through the Symbols library (io.github.hlcaptain, Apache-2.0). Call sites never pick a
 * source or a style; they ask for a meaning here.
 *
 * Fill rule: icons are unfilled; the `…On` ones are filled and only for an on/selected state
 * (starred, pinned, the current tab, the location in use).
 */
object SolsticeIcons {
    // Unfilled icons from the Symbols library; filled ("on") ones from FilledSymbols (the library's
    // filled vectors draw some glyphs hollow).
    private fun icon(symbol: MaterialSymbol): ImageVector = symbol.asRoundedImageVector(false)

    // Actions
    val Search by lazy { icon(MaterialSymbols.Search) }
    val SearchOff by lazy { icon(MaterialSymbols.SearchOff) }
    val Settings by lazy { icon(MaterialSymbols.Settings) }
    val Delete by lazy { icon(MaterialSymbols.Delete) }
    val Add by lazy { icon(MaterialSymbols.Add) }
    val Close by lazy { icon(MaterialSymbols.Close) }
    val Check by lazy { icon(MaterialSymbols.Check) }
    val Drag by lazy { icon(MaterialSymbols.DragIndicator) }
    val AddPlace by lazy { icon(MaterialSymbols.AddLocationAlt) }
    val MyLocation by lazy { icon(MaterialSymbols.MyLocation) }

    // Information
    val Info by lazy { icon(MaterialSymbols.Info) }
    val History by lazy { icon(MaterialSymbols.History) }
    val World by lazy { icon(MaterialSymbols.Public) }
    val Explore by lazy { icon(MaterialSymbols.Explore) }
    val Time by lazy { icon(MaterialSymbols.Schedule) }
    val Arrival by lazy { icon(MaterialSymbols.FlightLand) }
    val Offline by lazy { icon(MaterialSymbols.CloudOff) }
    val Waiting by lazy { icon(MaterialSymbols.HourglassEmpty) }
    val Percent by lazy { icon(MaterialSymbols.Percent) }
    val Drop by lazy { icon(MaterialSymbols.WaterDrop) }

    // Pages (the toolbar fills the current one)
    val Days by lazy { icon(MaterialSymbols.CalendarMonth) }
    val DaysOn by lazy { FilledSymbols.CalendarMonth }
    val Places by lazy { icon(MaterialSymbols.Map) }
    val PlacesOn by lazy { FilledSymbols.Map }

    // States
    val Star by lazy { icon(MaterialSymbols.Star) }

    val StarOn by lazy { FilledSymbols.Star }
    val Pin by lazy { icon(MaterialSymbols.PushPin) }
    val PinOn by lazy { FilledSymbols.PushPin }

    /** The location in use (always "on": it marks where you are). */
    val HereOn by lazy { FilledSymbols.NearMe }

    fun star(on: Boolean) = if (on) StarOn else Star
    fun pin(on: Boolean) = if (on) PinOn else Pin

    // Weather: Material Symbols' own weather set
    private val ClearDay by lazy { icon(MaterialSymbols.ClearDay) }
    private val ClearDayOn by lazy { FilledSymbols.ClearDay }
    private val ClearNight by lazy { icon(MaterialSymbols.ClearNight) }
    private val ClearNightOn by lazy { FilledSymbols.ClearNight }
    private val PartlyDay by lazy { icon(MaterialSymbols.PartlyCloudyDay) }
    private val PartlyDayOn by lazy { FilledSymbols.PartlyCloudyDay }
    private val PartlyNight by lazy { icon(MaterialSymbols.PartlyCloudyNight) }
    private val PartlyNightOn by lazy { FilledSymbols.PartlyCloudyNight }
    private val Overcast by lazy { icon(MaterialSymbols.Cloud) }
    private val OvercastOn by lazy { FilledSymbols.Cloud }
    private val Rain by lazy { icon(MaterialSymbols.Rainy) }
    private val RainOn by lazy { FilledSymbols.Rainy }
    private val Storm by lazy { icon(MaterialSymbols.Thunderstorm) }
    private val StormOn by lazy { FilledSymbols.Thunderstorm }
    private val Snow by lazy { icon(MaterialSymbols.WeatherSnowy) }
    private val SnowOn by lazy { FilledSymbols.WeatherSnowy }
    private val Fog by lazy { icon(MaterialSymbols.Foggy) }
    private val FogOn by lazy { FilledSymbols.Foggy }

    /** The rain cloud on its own (e.g. "no rain expected"). */
    val RainCloud: ImageVector get() = Rain

    /** A weather condition's icon, day or night; [on] fills it (the Forecast tab when it's current). */
    fun condition(weather: SkyWeather, night: Boolean, on: Boolean = false): ImageVector = when (weather) {
        SkyWeather.CLEAR -> if (night) (if (on) ClearNightOn else ClearNight) else (if (on) ClearDayOn else ClearDay)
        SkyWeather.PARTLY_CLOUDY ->
            if (night) (if (on) PartlyNightOn else PartlyNight) else (if (on) PartlyDayOn else PartlyDay)
        SkyWeather.OVERCAST -> if (on) OvercastOn else Overcast
        SkyWeather.RAIN -> if (on) RainOn else Rain
        SkyWeather.THUNDERSTORM -> if (on) StormOn else Storm
        SkyWeather.SNOW -> if (on) SnowOn else Snow
        SkyWeather.FOG -> if (on) FogOn else Fog
    }
}
