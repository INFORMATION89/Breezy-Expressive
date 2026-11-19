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

package org.breezyweather.ui.expressive.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MotionScheme
import io.github.information89.motion.Motion

/**
 * Material components (switches, buttons, sheets, top bars) move on Solstice's springs, so Settings
 * moves like the rest of the app (agreed 2026-11-19). Spatial = position and size; effects = alpha
 * and colour.
 */
object SolsticeMotionScheme : MotionScheme {
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = Motion.snappy.spec()
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = Motion.container.spec()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = Motion.dismiss.spec()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = Motion.snappy.spec()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = Motion.settle.spec()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = Motion.dismiss.spec()
}
