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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.breezyweather.ui.expressive.theme.Space

/**
 * A bottom sheet with the same slots as upstream's `AlertDialogNoPadding`, so a dialog call site
 * can switch to a sheet by changing only the function name. Dragging the sheet down counts as
 * dismissing (same as tapping outside a dialog).
 */
@Composable
fun SheetDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            if (title != null) {
                Box(modifier = Modifier.padding(start = Space.S6, end = Space.S6, bottom = Space.S3)) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmallEmphasized) { title() }
                }
            }
            if (text != null) {
                Box(modifier = Modifier.weight(1f, fill = false).padding(horizontal = Space.S2)) { text() }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.S4, vertical = Space.S3),
                horizontalArrangement = Arrangement.spacedBy(Space.S2, Alignment.End)
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

private typealias Alignment = androidx.compose.ui.Alignment
