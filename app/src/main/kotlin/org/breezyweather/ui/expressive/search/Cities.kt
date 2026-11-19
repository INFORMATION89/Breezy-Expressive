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

package org.breezyweather.ui.expressive.search

import android.content.Context
import androidx.core.content.edit
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import java.text.Normalizer
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A city from the bundled list (GeoNames cities15000, CC BY 4.0) or from recent searches. */
data class City(
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val population: Long = 0,
) {
    val country: String get() = Locale("", countryCode).displayCountry

    fun toLocation() = Location(
        latitude = latitude,
        longitude = longitude,
        timeZone = TimeZone.getTimeZone(timeZone),
        country = country,
        countryCode = countryCode,
        city = name,
        // Keep the name that was searched (an address lookup renamed "Puebla" to "Heroica Puebla
        // de Zaragoza, Centro…").
        needsGeocodeRefresh = false
    )

    fun encode() = listOf(name, countryCode, latitude, longitude, timeZone).joinToString("\t")

    companion object {
        fun decode(line: String): City? = line.split("\t").takeIf { it.size >= 5 }?.let {
            City(it[0], it[1], it[2].toDoubleOrNull() ?: return null, it[3].toDoubleOrNull() ?: return null, it[4])
        }
    }
}

/** Offline list of world cities, loaded once on first use (about 34 000 rows). */
object Cities {
    @Volatile
    private var all: List<City>? = null

    private fun load(context: Context): List<City> = all ?: synchronized(this) {
        all ?: context.resources.openRawResource(R.raw.solstice_cities).bufferedReader().useLines { lines ->
            lines.filter { it.isNotEmpty() && !it.startsWith("#") }.mapNotNull { line ->
                val c = line.split("\t")
                if (c.size < 6) {
                    null
                } else {
                    City(c[0], c[1], c[2].toDouble(), c[3].toDouble(), c[5], c[4].toLong())
                }
            }.toList()
        }.also { all = it }
    }

    /** Popular places around the world, in order; the first ones outside your country are shown. */
    private val WORLD = listOf(
        "Tokyo" to "JP",
        "New York City" to "US",
        "London" to "GB",
        "Paris" to "FR",
        "Seoul" to "KR",
        "Sydney" to "AU",
        "Barcelona" to "ES",
        "Mexico City" to "MX",
        "Rome" to "IT",
        "Dubai" to "AE"
    )

    /**
     * Shown before you type: the 5 biggest cities in your country that aren't where you are
     * (or right next to it, or to each other: no suburbs), then 5 popular world cities.
     */
    fun topPlaces(context: Context, countryCode: String?, near: Pair<Double, Double>?): List<City> {
        val all = load(context)
        val country = countryCode?.uppercase()?.takeIf { it.length == 2 }
            ?: near?.let { (lat, lon) -> all.minByOrNull { distanceKm(lat, lon, it.latitude, it.longitude) } }
                ?.countryCode
        val home = mutableListOf<City>()
        if (country != null) {
            for (city in all) { // sorted by population
                if (home.size == 5) break
                if (city.countryCode != country) continue
                val tooClose =
                    (near != null && distanceKm(near.first, near.second, city.latitude, city.longitude) < 50) ||
                        home.any { distanceKm(it.latitude, it.longitude, city.latitude, city.longitude) < 50 }
                if (!tooClose) home += city
            }
        }
        val world = WORLD.filter { it.second != country }.take(5).mapNotNull { (name, cc) ->
            all.firstOrNull { it.name == name && it.countryCode == cc }
        }
        return home + world
    }

    @Volatile
    private var folded: Array<String>? = null

    /** Lower case without accents, so "sao" finds São Paulo and "zurich" finds Zürich. */
    private fun fold(text: String): String {
        // Most names are plain ASCII: just lower-case them (fast path for the 34 000-name index).
        if (text.all { it.code < 128 }) return text.lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        for (c in decomposed) {
            if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) out.append(c)
        }
        return out.toString().lowercase(Locale.ROOT)
    }

    /**
     * Instant matches while typing: cities whose name starts with [query] (accents and case
     * ignored), biggest first (the list is sorted by population).
     */
    fun search(context: Context, query: String, count: Int = 8): List<City> {
        val all = load(context)
        val names = folded ?: synchronized(this) {
            folded ?: Array(all.size) { fold(all[it].name) }.also { folded = it }
        }
        val q = fold(query.trim())
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<City>(count)
        for (i in names.indices) {
            if (names[i].startsWith(q)) {
                out += all[i]
                if (out.size == count) break
            }
        }
        return out
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * asin(sqrt(a))
    }
}

/** The last places picked in search, newest first. */
object RecentSearches {
    private const val PREFS = "solstice_places"
    private const val KEY = "recent_searches"
    private const val MAX = 8

    fun all(context: Context): List<City> = prefs(context).getString(KEY, null).orEmpty()
        .lines().mapNotNull { City.decode(it) }

    fun add(context: Context, location: Location) {
        val city = City(
            name = location.city.ifEmpty { location.customName ?: return },
            countryCode = location.countryCode ?: return,
            latitude = location.latitude,
            longitude = location.longitude,
            timeZone = location.timeZone.id
        )
        val list = (
            listOf(city) + all(context).filterNot {
                it.name == city.name && it.countryCode == city.countryCode
            }
            )
            .take(MAX)
        prefs(context).edit { putString(KEY, list.joinToString("\n") { it.encode() }) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
