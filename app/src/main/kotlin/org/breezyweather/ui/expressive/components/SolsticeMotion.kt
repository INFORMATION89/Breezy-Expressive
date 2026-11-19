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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

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
object SolsticeMotion {
    /** Morphing containers (sheets, menus, pills): start immediately, subtle elasticity. */
    val container = spring<Float>(dampingRatio = 0.78f, stiffness = 380f)

    /** Dismissing a container (a sheet back into its button): no bounce, a touch slower, like iOS. */
    val dismiss = spring<Float>(dampingRatio = 1f, stiffness = 260f)

    /** Responsive controls (buttons, toggles, sliders): no bounce. */
    val control = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Inner content's scale-in. */
    val content = spring<Float>(dampingRatio = 0.85f, stiffness = 400f)

    /** Settling into place after a drag or a fling (critically damped, no bounce). */
    val settle = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

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
    fun rubberBand(over: Float, dimension: Float, coefficient: Float = 0.55f): Float {
        if (over <= 0f || dimension <= 0f) return 0f
        return (1f - 1f / (over * coefficient / dimension + 1f)) * dimension
    }

    /**
     * Where a fling would come to rest, given a [velocity] (units per second), so the release can
     * pick its target from the gesture's momentum and not just its position.
     */
    fun project(value: Float, velocity: Float, decelerationRate: Float = 0.998f): Float =
        value + velocity / 1000f * decelerationRate / (1f - decelerationRate)
}
