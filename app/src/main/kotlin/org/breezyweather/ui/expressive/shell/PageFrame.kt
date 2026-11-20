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

import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import io.github.information89.motion.Motion
import org.breezyweather.ui.expressive.theme.Space
import kotlin.math.abs

/*
 * The frame both the home shell and a postcard window build their pages in (component catalog,
 * 2026-11-19): the pager's position, the plain page that fades in away from the sky, the pager's
 * fling, and the dock the toolbar sits in.
 */

/** Where the pager is, fractionally (1.5 = half way between pages 1 and 2). Read it in draw/layout. */
internal val PagerState.position: Float get() = currentPage + currentPageOffsetFraction

/**
 * The plain page under the pages: invisible on the sky's page ([skyPage]), fully there one page
 * away, so pages other than Forecast sit on [page] and the sky only shows behind Forecast. Read in
 * draw, so it follows the finger without recomposing.
 */
internal fun Modifier.plainPageBackdrop(pager: PagerState, skyPage: Int, page: Color): Modifier =
    drawBehind { drawRect(page, alpha = abs(pager.position - skyPage).coerceIn(0f, 1f)) }

/** The pages' fling: an unhurried, unbouncy snap to the nearest page (Motion.gentle). */
@Composable
internal fun pageFling(pager: PagerState): TargetedFlingBehavior = PagerDefaults.flingBehavior(
    state = pager,
    snapAnimationSpec = Motion.gentle.spec()
)

/**
 * Where the toolbar (and anything stacked above it, like messages) sits: centred at the bottom,
 * above the navigation bar. [alpha] (read in draw) lets it step aside while something covers it.
 */
@Composable
internal fun ToolbarDock(alpha: () -> Float = { 1f }, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Space.S3)
                .graphicsLayer { this.alpha = alpha() },
            content = content
        )
    }
}
