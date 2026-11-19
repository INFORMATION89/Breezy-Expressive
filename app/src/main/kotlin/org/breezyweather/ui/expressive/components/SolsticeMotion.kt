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

import io.github.information89.motion.Motion
import io.github.information89.motion.MotionPhysics

/**
 * Solstice's motion system (agreed 2026-11-19, the user's reference): Apple-like physics.
 *
 * - Gestures drive an Animatable directly, and the finger's release velocity feeds the spring.
 *   Over-dragging rubber-bands (never a hard stop).
 * - Containers morph (pill into sheet) rather than crossfade: bounds start at once, lightly
 *   underdamped.
 * - Inner content is choreographed: it fades and scales in (0.92 to 1) as the container reaches
 *   about 70 % of its size, and leaves first when it closes.
 */
// The values now come from the :motion module (io.github.information89.motion); this object keeps the
// existing call sites working until they move to the framework (M3/M4).
object SolsticeMotion {
    /** Morphing containers (sheets, menus, pills): start immediately, subtle elasticity. */
    val container = Motion.container.spec<Float>()

    /** Dismissing a container (a sheet back into its button): no bounce, a touch slower, like iOS. */
    val dismiss = Motion.dismiss.spec<Float>()

    /** Responsive controls (buttons, toggles, sliders): no bounce. */
    val control = Motion.control.spec<Float>()

    /** Inner content's scale-in. */
    val content = Motion.content.spec<Float>()

    /** Settling into place after a drag or a fling (critically damped, no bounce). */
    val settle = Motion.settle.spec<Float>()

    /** Inner content starts scaled to this, then grows to full size. */
    const val CONTENT_START_SCALE = 0.92f

    /** Inner content appears between these fractions of the container's growth (~70 % in). */
    const val CONTENT_FROM = 0.35f
    const val CONTENT_TO = 0.70f

    /**
     * Rubber-banding, like iOS: how far something moves when dragged [over] pixels past its limit,
     * over a [dimension] (e.g. the sheet's height). Close to the finger at first, then ever more
     * resistant, never passing [dimension].
     */
    fun rubberBand(over: Float, dimension: Float, coefficient: Float = MotionPhysics.RUBBER_BAND): Float =
        MotionPhysics.rubberBand(over, dimension, coefficient)

    /**
     * Where a fling would come to rest, given a [velocity] (units per second), so the release can
     * pick its target from the gesture's momentum and not just its position.
     */
    fun project(value: Float, velocity: Float, decelerationRate: Float = MotionPhysics.DECELERATION_NORMAL): Float =
        MotionPhysics.project(value, velocity, decelerationRate)
}
