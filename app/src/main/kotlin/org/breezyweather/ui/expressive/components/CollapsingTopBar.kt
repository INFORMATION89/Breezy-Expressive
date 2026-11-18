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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A large top bar whose single title moves and shrinks with the scroll (agreed 2026-11-18,
 * "follow and settle"), instead of cross-fading a large title into a small one. The title is laid
 * out once at its large size and scaled down as the bar collapses, sliding from below the
 * navigation icon into the bar row: it follows the finger exactly, never shows twice, and the
 * scroll behaviour's snap settles it fully open or closed on release (interruptible).
 *
 * Reads [scrollBehavior]'s state in layout and draw only, so scrolling never recomposes the bar.
 */
@Composable
fun CollapsingTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    containerColor: Color,
    scrolledContainerColor: Color,
    contentColor: Color,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val collapsed = with(density) { 64.dp.toPx() }
    val expanded = with(density) { 152.dp.toPx() }
    val state = scrollBehavior?.state
    LaunchedEffect(state, collapsed, expanded) {
        val limit = collapsed - expanded
        if (state != null && state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
    }
    val large = MaterialTheme.typography.headlineMedium
    val small = MaterialTheme.typography.titleLarge
    val ratio = small.fontSize.value / large.fontSize.value
    fun fraction() = state?.collapsedFraction?.coerceIn(0f, 1f) ?: 0f

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Layout(
            content = {
                Box(Modifier.layoutId("nav")) { navigationIcon() }
                Row(Modifier.layoutId("actions"), verticalAlignment = Alignment.CenterVertically) { actions() }
                Box(Modifier.layoutId("title")) { ProvideTextStyle(large) { title() } }
            },
            modifier = modifier
                .drawBehind {
                    // Takes on the scrolled colour as it collapses.
                    drawRect(lerp(containerColor, scrolledContainerColor, fraction()))
                }
                .windowInsetsPadding(windowInsets)
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val f = fraction()
            val height = (expanded + (state?.heightOffset ?: 0f)).roundToInt().coerceAtLeast(collapsed.roundToInt())
            val loose = Constraints(maxWidth = width, maxHeight = collapsed.roundToInt())
            val nav = measurables.first { it.layoutId == "nav" }.measure(loose)
            val acts = measurables.first { it.layoutId == "actions" }.measure(loose)
            val edge = 16.dp.roundToPx()
            val navEnd = if (nav.width > 0) 4.dp.roundToPx() + nav.width else edge
            // Room for the title when small (between the icons), shown at its large size.
            val smallRoom = (width - navEnd - acts.width - edge).coerceAtLeast(0)
            val largeRoom = width - 2 * edge
            val titleMax = minOf(largeRoom, (smallRoom / ratio).roundToInt()).coerceAtLeast(0)
            val title = measurables.first { it.layoutId == "title" }
                .measure(Constraints(maxWidth = titleMax, maxHeight = Constraints.Infinity))
            layout(width, height) {
                val rowTop = 0
                nav.placeRelative(4.dp.roundToPx(), rowTop + (collapsed.roundToInt() - nav.height) / 2)
                acts.placeRelative(
                    width - acts.width - 4.dp.roundToPx(),
                    rowTop + (collapsed.roundToInt() - acts.height) / 2
                )
                // From its large place (left edge, near the bottom) to the bar row (after the icon).
                val scale = 1f + (ratio - 1f) * f
                val fromX = edge.toFloat()
                val fromY = height - 20.dp.toPx() - title.height
                val toX = navEnd.toFloat()
                val toY = (collapsed - title.height * ratio) / 2f
                // Sideways leads (eased out), so the title clears the back arrow before it reaches the row.
                val fx = 1f - (1f - f) * (1f - f)
                val x = fromX + (toX - fromX) * fx
                val y = fromY + (toY - fromY) * f
                title.placeRelativeWithLayer(x.roundToInt(), max(0f, y).roundToInt()) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                }
            }
        }
    }
}
