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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.breezyweather.ui.expressive.theme.CornerShapes
import org.breezyweather.ui.expressive.theme.Space

/** What a [SolsticeCard] is made of. */
enum class CardMaterial {
    /** Frosted glass over the sky (Forecast): the sky behind shows through, blurred. */
    Glass,

    /** Plain paper over a plain page (This week): a faint surface. */
    Paper,
}

/**
 * Solstice's one card (component catalog, 2026-11-19): full width, the large corner, and one of two
 * materials. [ink] is the text colour on it, which picks the glass's strength (dark text means a
 * light sky, so whiter glass) and the paper's tone. [tint] colours the glass (e.g. rain soon).
 */
@Composable
fun SolsticeCard(
    material: CardMaterial,
    ink: Color,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = CornerShapes.L
    val surface = when (material) {
        CardMaterial.Glass -> {
            val light = ink.red + ink.green + ink.blue < 1.5f // dark text → light sky → white glass
            val alpha = when {
                light -> 0.34f
                tint != null -> 0.24f
                else -> 0.10f
            }
            val fill = (tint ?: Color.White).copy(alpha = alpha)
            // Real frosted glass: the sky behind it, blurred, under the tint (plain tint without a sky).
            val sky = LocalSkyGlass.current
            val material = if (sky != null) {
                Modifier.glass(sky, shape, fill, edge = Color.White.copy(alpha = 0.14f))
            } else {
                Modifier.clip(shape).background(fill)
            }
            material.padding(Space.S4)
        }
        CardMaterial.Paper ->
            Modifier
                .clip(shape)
                .background(ink.copy(alpha = 0.09f))
                .border(0.5.dp, ink.copy(alpha = 0.10f), shape)
                .padding(Space.S5)
    }
    Box(modifier.fillMaxWidth().then(surface)) { Column(content = content) }
}
