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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import io.github.information89.motion.Motion

/**
 * Screen transitions (agreed 2026-11-18, "iOS-style push"): the new screen slides in from the
 * right while the one below shifts a quarter of the way left and dims; back does the reverse.
 * With predictive back, Navigation 3 drives the pop with the finger, and it can be cancelled.
 * Springs without bounce, so a quick back during the push simply reverses it.
 */
object ScreenPush {
    private val slide = Motion.settle.spec(visibilityThreshold = IntOffset(1, 1))
    private val dim = Motion.settle.spec<Float>()
    private const val UNDER = 0.55f

    /** Going forward: the new screen comes in from the right, over the one it covers. */
    fun push(): ContentTransform =
        slideInHorizontally(slide) { it } togetherWith
            (slideOutHorizontally(slide) { -it / 4 } + fadeOut(dim, targetAlpha = UNDER))

    /** Going back: the top screen leaves to the right, above the one coming back. */
    fun pop(): ContentTransform =
        (slideInHorizontally(slide) { -it / 4 } + fadeIn(dim, initialAlpha = UNDER)) togetherWith
            slideOutHorizontally(slide) { it }
}
