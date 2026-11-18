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

package org.breezyweather.ui.expressive.reorder

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.breezyweather.R

/**
 * A segmented list whose rows are reordered by dragging the handle (or long-pressing the row).
 * The dragged row follows the finger; the others slide out of the way with springs
 * (`animateItem`), so moves can reverse mid-way. The list scrolls when the row nears an edge.
 * TalkBack gets "move up" / "move down" actions instead of dragging.
 */
@Composable
fun <T : Any> ReorderList(
    items: SnapshotStateList<T>,
    key: (T) -> String,
    label: (T) -> String,
    onRemove: (T) -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable ((T) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
    listState: LazyListState = rememberLazyListState(),
    footer: LazyListScope.() -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val edge = with(LocalDensity.current) { 96.dp.toPx() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun move(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    // Swap with a neighbour once the dragged row's centre passes into it.
    fun swapIfNeeded() {
        val info = listState.layoutInfo.visibleItemsInfo
        val current = info.firstOrNull { it.key == draggingKey } ?: return
        val centre = current.offset + dragOffset + current.size / 2f
        val target = info.firstOrNull {
            it.key != draggingKey &&
                it.index < items.size &&
                centre > it.offset &&
                centre < it.offset + it.size
        } ?: return
        move(current.index, target.index)
        dragOffset += current.offset - target.offset
    }

    fun autoScroll() {
        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == draggingKey } ?: return
        val top = current.offset + dragOffset
        val bottom = top + current.size
        val delta = when {
            bottom > info.viewportEndOffset - edge -> 24f
            top < info.viewportStartOffset + edge -> -24f
            else -> 0f
        }
        if (delta != 0f) {
            scope.launch {
                val consumed = listState.scrollBy(delta)
                dragOffset += consumed
                swapIfNeeded()
            }
        }
    }

    fun endDrag() {
        draggingKey = null
        dragOffset = 0f
    }

    val dragModifier = { item: T ->
        Modifier.pointerInput(key(item)) {
            detectVerticalDragGestures(
                onDragStart = {
                    draggingKey = key(item)
                    dragOffset = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                },
                onDragEnd = { endDrag() },
                onDragCancel = { endDrag() },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    dragOffset += amount
                    swapIfNeeded()
                    autoScroll()
                }
            )
        }
    }
    val longPressDragModifier = { item: T ->
        Modifier.pointerInput(key(item)) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggingKey = key(item)
                    dragOffset = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = { endDrag() },
                onDragCancel = { endDrag() },
                onDrag = { change, amount ->
                    change.consume()
                    dragOffset += amount.y
                    swapIfNeeded()
                    autoScroll()
                }
            )
        }
    }

    val moveUp = stringResource(R.string.action_move_up_expressive)
    val moveDown = stringResource(R.string.action_move_down_expressive)
    val remove = stringResource(R.string.action_remove_expressive)

    LazyColumn(state = listState, contentPadding = contentPadding, modifier = modifier) {
        itemsIndexed(items, key = { _, it -> key(it) }) { index, item ->
            val isDragging = key(item) == draggingKey
            val elevation by animateDpAsState(
                if (isDragging) 6.dp else 0.dp,
                MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "dragElevation"
            )
            val first = index == 0
            val last = index == items.lastIndex
            val big = 24.dp
            val small = if (isDragging) big else 4.dp
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (first) big else small,
                    topEnd = if (first) big else small,
                    bottomStart = if (last) big else small,
                    bottomEnd = if (last) big else small
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = elevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .then(
                        if (isDragging) {
                            Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset }
                        } else {
                            Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                            )
                        }
                    )
                    .then(longPressDragModifier(item))
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(moveUp) {
                                val i = items.indexOf(item)
                                move(i, i - 1)
                                true
                            },
                            CustomAccessibilityAction(moveDown) {
                                val i = items.indexOf(item)
                                move(i, i + 1)
                                true
                            },
                            CustomAccessibilityAction(remove) {
                                onRemove(item)
                                true
                            }
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .then(dragModifier(item))
                            .padding(12.dp)
                    )
                    Text(
                        text = label(item),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    trailing?.invoke(item)
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = remove,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        footer()
    }
}
