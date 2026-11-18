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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape of one row in a segmented group that morphs while pressed: the small inner corners
 * grow towards the outer ones, so the pressed row reads as lifted out of the group.
 * Spring-driven by the theme's motion scheme, so a quick tap or a scroll that cancels the
 * press simply retargets the animation (interruptible).
 *
 * Returns the [Modifier] to put on the row (it only observes touches, never consumes them)
 * and the current shape.
 */
@Composable
fun rememberPressMorphShape(
    isFirst: Boolean,
    isLast: Boolean,
    outer: Dp,
    inner: Dp = 2.dp,
    pressedInner: Dp = 16.dp,
): Pair<Modifier, RoundedCornerShape> {
    var pressed by remember { mutableStateOf(false) }
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val corner by animateDpAsState(if (pressed) pressedInner else inner, spec, label = "pressMorph")
    val observe = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            pressed = true
            waitForUpOrCancellation(pass = PointerEventPass.Initial)
            pressed = false
        }
    }
    val top = if (isFirst) outer else corner
    val bottom = if (isLast) outer else corner
    return observe to RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
