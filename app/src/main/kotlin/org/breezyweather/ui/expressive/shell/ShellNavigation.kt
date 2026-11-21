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

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.information89.motion.Motion
import io.github.information89.motion.PageTransition
import io.github.information89.motion.pageSettleFling
import io.github.information89.motion.rememberPageTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How the pages change, shared by home and the postcard window (2026-11-21, tonight's item 3), so both
 * behave the same by construction and only their configuration differs.
 *
 * - Tab taps go through [navigate]: the pager switches at once and `:motion`'s [PageTransition]
 *   draws the slide (taps are never swallowed).
 * - A swipe let go settles the same way ([fling]: the pager jumps, the rest is drawn).
 * - [visualPosition] is where the pages *look* to be, for anything that follows the page position
 *   (the sky's fade, edge blur, swipe blur, the toolbar).
 */
@Stable
internal class ShellNavigation(
    val pager: PagerState,
    val pageChange: PageTransition,
    private val scope: CoroutineScope,
) {
    private var jumpFrom by mutableFloatStateOf(0f)
    private var jumpTo by mutableFloatStateOf(0f)

    /** The toolbar's highlight while a tap glides it across (-1 at rest). */
    val toolbarGlide = Animatable(-1f)
    private var job: Job? = null

    /** Where the pages look to be: between the page left and the one arrived at while a change is drawn. */
    fun visualPosition(): Float {
        val r = pageChange.remaining
        return if (r > 0f) jumpTo + (jumpFrom - jumpTo) * r else pager.currentPage + pager.currentPageOffsetFraction
    }

    /**
     * Whether tab [i] is only passed over by a change in progress (a jump from This week to Places
     * passes Forecast): its label stays folded. The tabs a change starts and ends on unfold normally
     * (2026-11-21: with every tab tap gliding, the middle tab's label stayed folded and popped in).
     */
    fun isPassing(i: Int): Boolean {
        if (toolbarGlide.value < 0f && pageChange.remaining <= 0f) return false
        val a = jumpFrom.roundToInt()
        val b = jumpTo.roundToInt()
        return i > minOf(a, b) && i < maxOf(a, b)
    }

    /** Where the toolbar should show the selection, or null to follow the pager. */
    fun toolbarPosition(): Float? =
        toolbarGlide.value.takeIf { it >= 0f } ?: visualPosition().takeIf { pageChange.remaining > 0f }

    /** The pager's fling: let go, it jumps to the page and the rest of the way is drawn. */
    val fling: TargetedFlingBehavior = pageSettleFling(pager, pageChange, scope) { from, to ->
        jumpFrom = from
        jumpTo = to.toFloat()
    }

    /** A finger on the toolbar (its slide) stops any page change in progress. */
    fun interrupt() {
        job?.cancel()
        job = null
    }

    /**
     * Goes to page [target]. A new navigation, or a finger, cancels the one in progress and resets
     * its state first, so nothing is left half-way (a fast second tap used to fight the first).
     */
    fun navigate(target: Int) {
        job?.cancel()
        job = scope.launch {
            try {
                rest()
                val here = (pager.currentPage + pager.currentPageOffsetFraction).roundToInt()
                if (target != here && abs(pager.currentPageOffsetFraction) < 0.01f) {
                    // The pager switches at once; the change is drawn on top (agreed 2026-11-21).
                    val from = here.toFloat()
                    jumpFrom = from
                    jumpTo = target.toFloat()
                    pageChange.go(
                        forward = target > here,
                        switch = {
                            toolbarGlide.snapTo(from)
                            pager.scrollToPage(target)
                        },
                        alongside = {
                            // About the pace of a neighbouring tab change (user: edge to edge a touch faster).
                            toolbarGlide.animateTo(target.toFloat(), Motion.settle.spec())
                        }
                    )
                } else {
                    pager.animateScrollToPage(target)
                }
            } finally {
                // Finished or interrupted: always back at rest.
                withContext(NonCancellable) { rest() }
            }
        }
    }

    private suspend fun rest() {
        pageChange.rest()
        toolbarGlide.snapTo(-1f)
    }
}

@Composable
internal fun rememberShellNavigation(pager: PagerState): ShellNavigation {
    val scope = rememberCoroutineScope()
    val pageChange = rememberPageTransition()
    return remember(pager, pageChange, scope) { ShellNavigation(pager, pageChange, scope) }
}

/**
 * Title hand-off, for each tab: while its page's big title is on screen the tab shows only the icon;
 * as the title scrolls away the label unfolds in the toolbar (never the same word twice). One list
 * per tab, or null for a page with no big title (its label always shows).
 */
@Composable
internal fun rememberTitleHandOff(lists: List<LazyListState?>): State<List<Float>> {
    val titleHeight = with(LocalDensity.current) { 72.dp.toPx() }
    return remember(lists, titleHeight) {
        derivedStateOf { lists.map { it?.let { list -> 1f - titleVisible(list, titleHeight) } ?: 1f } }
    }
}

/** 1 while the list's big title (its first item) is fully on screen, 0 once it has scrolled away. */
private fun titleVisible(state: LazyListState, titleHeight: Float): Float =
    if (state.firstVisibleItemIndex > 0) {
        0f
    } else {
        (1f - state.firstVisibleItemScrollOffset / titleHeight).coerceIn(0f, 1f)
    }

/**
 * A page partly in view gets a little blur that clears as it comes fully in. Read in the draw phase
 * from [position] (the pages' visual position), so it follows the finger and reverses with it.
 */
internal fun Modifier.pageSwipeBlur(position: () -> Float, index: Int): Modifier = graphicsLayer {
    val d = abs(position() - index).coerceIn(0f, 1f)
    val b = 6.dp.toPx() * d
    renderEffect = if (b > 0.5f && d < 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BlurEffect(b, b, TileMode.Decal)
    } else {
        null
    }
}
