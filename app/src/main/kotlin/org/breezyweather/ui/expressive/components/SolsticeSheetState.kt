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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.information89.motion.MotionPhysics
import io.github.information89.motion.MotionValue
import io.github.information89.motion.rememberMotionValue

/**
 * The motion of a Solstice sheet: a card that grows out of a source (a button), docks at the bottom
 * and can be dragged taller, shorter or away, like an iOS sheet (agreed 2026-11-19, option B).
 *
 * Two [MotionValue]s, read only in layout and draw:
 * - [presence]: 0 = folded into its source, 1 = open. Opens on [io.github.information89.motion.Motion.container],
 *   closes on `dismiss` (no bounce). A touch catches it mid-open or mid-close and the finger drives it.
 * - [detent]: how the open card is sized, in px: 0 = compact, [room] = tall, below 0 = pulled down;
 *   rest points `-dismissDistance`, 0 and [room], with the UIKit release rule and iOS rubber-banding
 *   past tall. A release heading for `-dismissDistance` means "dismiss": the card holds where it was
 *   let go and the fling's speed carries into [presence] closing ([dismiss]).
 *
 * Search is the first user; the moon card moves onto it later (M4).
 */
@Stable
class SolsticeSheetState internal constructor(
    val presence: MotionValue,
    val detent: MotionValue,
    private val slowPx: Float,
    private val fastPx: Float,
) {
    /** Extra height above compact the layout allows (written by the layout each frame). */
    var room = 0f
        private set

    /** How far down a pull must come to rest to dismiss (px). */
    var dismissDistance = 1f
        private set

    /** How far the card's top moves between folded and open (px; written by the layout). */
    var travel = 1f

    /** Extra height above compact (0 to [room]). */
    val extra: Float get() = detent.value.coerceIn(0f, room + OVER)

    /** How far the card is pulled down below compact (px). */
    val pulled: Float get() = (-detent.value).coerceAtLeast(0f)

    /**
     * Closing, or held or opening part way: a drag then drives [presence], not [detent]. (The last
     * touch of an opening bounce counts as open.)
     */
    val unsettled: Boolean get() = presence.value < 0.999f || (presence.isRunning && presence.target < 0.5f)

    /** How much the sheet covers the page behind: open, less as it's pulled down (for scrims, scale). */
    fun backdrop(): Float = presence.value.coerceIn(0f, 1f) * (1f - (pulled / dismissDistance).coerceIn(0f, 1f))

    /** Called by the layout with the current sizes (they follow the keyboard). */
    fun measure(room: Float, dismissDistance: Float) {
        if (room != this.room || dismissDistance != this.dismissDistance) {
            this.room = room
            this.dismissDistance = dismissDistance
            detent.restPoints = listOf(-dismissDistance, 0f, room)
        }
        detent.slowFlick = slowPx
        detent.fastFlick = fastPx
        presence.slowFlick = slowPx / travel.coerceAtLeast(1f)
        presence.fastFlick = fastPx / travel.coerceAtLeast(1f)
    }

    /** Opens tall (as tall as there is room for, whatever the keyboard does meanwhile). */
    fun showTall() {
        detent.snapTo(TALL)
        presence.animateTo(1f)
    }

    /** Back to open from part way (caught while closing): from a pull, back to compact. */
    fun reopen() {
        presence.animateTo(1f)
        if (detent.value < 0f) detent.animateTo(0f)
    }

    /** Folds back into the source, carrying [velocityDown] (px/s, + = down) from a drag. */
    fun dismiss(velocityDown: Float = Float.NaN) {
        if (velocityDown.isNaN()) {
            presence.animateTo(0f)
        } else {
            presence.animateTo(0f, -velocityDown / travel.coerceAtLeast(1f))
        }
    }

    /** A touch: stops whatever is moving, under the finger. */
    fun catch() {
        if (unsettled) presence.stop()
        detent.stop()
        // Opened tall before the size was known: start from the real tall size.
        if (detent.value > room + OVER) detent.snapTo(room)
    }

    /** Springs to tall (e.g. the keyboard was hidden). */
    fun expand() {
        catch()
        detent.animateTo(room)
    }

    /** Finger moved by [deltaDown] px (+ = down). */
    fun drag(deltaDown: Float) {
        if (unsettled) {
            presence.drag(-deltaDown / travel.coerceAtLeast(1f))
        } else {
            if (detent.value > room + OVER) detent.snapTo(room)
            detent.drag(-deltaDown)
        }
    }

    /**
     * Finger lifted with [velocityDown] (px/s, + = down). Returns true when the sheet should close
     * (the caller updates its own open state), false when it should be (or stay) open.
     */
    fun release(velocityDown: Float): Boolean = if (unsettled) {
        presence.release(-velocityDown / travel.coerceAtLeast(1f)) < 0.5f
    } else {
        if (detent.targetFor(-velocityDown) <= -dismissDistance + 0.5f) {
            // Dismissing: the card stays where it was let go and folds into its source from there,
            // carrying the fling, so it stays on screen (and can be caught) all the way.
            detent.stop()
            dismiss(velocityDown)
            true
        } else {
            detent.release(-velocityDown)
            false
        }
    }

    private companion object {
        /** Past tall this far, the value is the "open tall" placeholder rather than a rubber-band. */
        const val OVER = 100_000f
        const val TALL = 1_000_000f
    }
}

@Composable
fun rememberSolsticeSheetState(): SolsticeSheetState {
    val density = LocalDensity.current
    val overPx = with(density) { 80.dp.toPx() }
    val presence = rememberMotionValue(initial = 0f, restPoints = listOf(0f, 1f), overMin = 0.05f, overMax = 0.05f)
    val detent = rememberMotionValue(
        initial = 0f,
        restPoints = listOf(-1f, 0f, 1f),
        overMin = overPx * 2f,
        overMax = overPx
    )
    return remember(presence, detent) {
        // UIKit's release rule: slower than 250 dp/s is no flick, faster than 1000 dp/s moves a step.
        SolsticeSheetState(
            presence,
            detent,
            with(density) { MotionPhysics.SlowFlick.toPx() },
            with(density) { MotionPhysics.FastFlick.toPx() }
        )
    }
}
