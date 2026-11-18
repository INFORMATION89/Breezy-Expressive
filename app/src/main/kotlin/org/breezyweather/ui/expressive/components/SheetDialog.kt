/*
 * This file is part of Breezy Expressive, a fork of Breezy Weather.
 *
 * Breezy Expressive is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Expressive is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Expressive. If not, see <https://www.gnu.org/licenses/>.
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
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmallEmphasized) { title() }
                }
            }
            if (text != null) {
                Box(modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)) { text() }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

private typealias Alignment = androidx.compose.ui.Alignment
