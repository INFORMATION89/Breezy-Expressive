/*
 * This file is part of Breezy Weather.
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

package org.breezyweather.ui.theme.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import org.breezyweather.ui.expressive.theme.ExpressiveTheme

// Breezy Expressive: the whole Compose UI is themed by ExpressiveTheme (ui/expressive/theme).
@Composable
fun BreezyWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) = ExpressiveTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

@Composable
fun themeRipple(
    bounded: Boolean = true,
) = ripple(
    color = MaterialTheme.colorScheme.primary,
    bounded = bounded
)
