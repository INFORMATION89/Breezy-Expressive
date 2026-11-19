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

package org.breezyweather.ui.settings.compose

import android.content.Context
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.breezyweather.R
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.ui.expressive.components.SolsticeIcons

@Composable
fun AboutActivityIconButton(
    context: Context,
    modifier: Modifier = Modifier,
) = IconButton(
    modifier = modifier,
    onClick = { IntentHelper.startAboutActivity(context) }
) {
    Icon(
        imageVector = SolsticeIcons.Info,
        contentDescription = stringResource(R.string.action_about),
        tint = MaterialTheme.colorScheme.onSurface
    )
}
