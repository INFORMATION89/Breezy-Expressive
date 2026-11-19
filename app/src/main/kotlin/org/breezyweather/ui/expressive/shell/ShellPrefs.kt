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

package org.breezyweather.ui.expressive.shell

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

/** Solstice's own display options. */
object ShellPrefs {
    private const val PREFS = "solstice_places"
    private const val KEY_FEELS_LIKE_MAIN = "feels_like_main"

    private val feelsLike = mutableStateOf<Boolean?>(null)

    /** Show the feels-like temperature as the main (big) temperature. */
    fun feelsLikeMain(context: Context): Boolean = feelsLike.value
        ?: prefs(context).getBoolean(KEY_FEELS_LIKE_MAIN, false).also { feelsLike.value = it }

    fun setFeelsLikeMain(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_FEELS_LIKE_MAIN, value) }
        feelsLike.value = value
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
