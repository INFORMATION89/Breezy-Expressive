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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.information89.motion.Motion
import io.github.information89.motion.MotionSpring

/**
 * Every animation of the toolbar in one place (PLAN.md "Morph refining", point 5, 2026-11-21): which
 * `:motion` spring each one uses and its amounts, instead of values typed inline in `Toolbar.kt`.
 * The motion lab's toolbar demo edits these live, and the app reads the same values, so what's tuned
 * in the lab is what the app does. The defaults are exactly what the toolbar did before (agreed
 * values: the gooey bud and the pin's jab 2026-11-18, slide to switch 2026-11-19); nothing is saved,
 * so a restart brings them back.
 */
internal object ToolbarMotion {
    // The search bud ("gooey bud"): the neck joining the pill and the bubble.
    /** The neck growing back as the bubble rejoins the pill. */
    var neckJoin by mutableStateOf(Motion.playful)

    /** The neck letting go as the bubble pops free (no bounce: a sliver of neck came back). */
    var neckFree by mutableStateOf(Motion.settle)

    /** The bubble's wobble after popping free, and the pill's ripple as it absorbs it. */
    var wobble by mutableStateOf(Motion.wobble)

    /** How much the free bubble swells in its wobble (scale). */
    var wobbleAmount by mutableFloatStateOf(0.1f)

    /** The pill's ripple when it takes the bubble back: wider by this, shorter by [rippleSquash]. */
    var rippleStretch by mutableFloatStateOf(0.03f)
    var rippleSquash by mutableFloatStateOf(0.04f)

    // The pin's jab when pinning (and its little pop back when unpinning).
    var jab by mutableStateOf(Motion.container)

    /** How far down the pin is pushed, in dp. */
    var jabDrop by mutableFloatStateOf(1.5f)

    /** Squashed by this (height) and stretched by [jabStretch] (width) at the bottom of the jab. */
    var jabSquash by mutableFloatStateOf(0.07f)
    var jabStretch by mutableFloatStateOf(0.03f)

    /** Its tilt, in degrees. */
    var jabTilt by mutableFloatStateOf(8f)

    // Slide to switch pages: hold the pill and the selection pops out, then follows the finger.
    var popOut by mutableStateOf(Motion.playful)
    var popIn by mutableStateOf(Motion.container)

    /** Sinking back when the slide is cancelled. */
    var popCancel by mutableStateOf(Motion.dismiss)

    /** How much bigger the selection grows while held. */
    var popScale by mutableFloatStateOf(0.12f)

    /** Finger travel per page, in collapsed tab widths (1.6: it moves at the finger's pace). */
    var slideTravel by mutableFloatStateOf(1.6f)

    // Each tab's press.
    var pressDown by mutableStateOf(Motion.snappy)
    var pressUp by mutableStateOf(Motion.playful)

    /** How far a pressed tab shrinks (scale). */
    var pressScale by mutableFloatStateOf(0.86f)

    /** The bud icon's colour change. */
    var budTint by mutableStateOf(Motion.control)

    /** The tokens a toolbar spring can be set to in the lab, by name. */
    val tokens: List<Pair<String, MotionSpring>> = listOf(
        "snappy" to Motion.snappy,
        "control" to Motion.control,
        "settle" to Motion.settle,
        "content" to Motion.content,
        "container" to Motion.container,
        "dismiss" to Motion.dismiss,
        "gentle" to Motion.gentle,
        "playful" to Motion.playful,
        "wobble" to Motion.wobble
    )

    /** Back to the agreed values. */
    fun reset() {
        neckJoin = Motion.playful
        neckFree = Motion.settle
        wobble = Motion.wobble
        wobbleAmount = 0.1f
        rippleStretch = 0.03f
        rippleSquash = 0.04f
        jab = Motion.container
        jabDrop = 1.5f
        jabSquash = 0.07f
        jabStretch = 0.03f
        jabTilt = 8f
        popOut = Motion.playful
        popIn = Motion.container
        popCancel = Motion.dismiss
        popScale = 0.12f
        slideTravel = 1.6f
        pressDown = Motion.snappy
        pressUp = Motion.playful
        pressScale = 0.86f
        budTint = Motion.control
    }
}
