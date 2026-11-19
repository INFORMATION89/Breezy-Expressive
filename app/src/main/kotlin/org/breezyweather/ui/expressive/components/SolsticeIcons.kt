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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icons Material doesn't have (until the icon direction is decided). */
object SolsticeIcons {
    /** A rain cloud: the outlined cloud, a little smaller and higher, with three slanted drops. */
    val RainCloud: ImageVector by lazy {
        ImageVector.Builder("RainCloud", 24.dp, 24.dp, 24f, 24f).apply {
            group(scaleX = 0.82f, scaleY = 0.82f, translationX = 2.2f, translationY = -1.6f) {
                addPath(
                    PathParser().parsePathString(
                        "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14" +
                            "c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96z" +
                            "M19,18H6c-2.21,0 -4,-1.79 -4,-4 0,-2.05 1.53,-3.76 3.56,-3.97l1.07,-0.11 0.5,-0.95" +
                            "C8.08,7.14 9.94,6 12,6c2.62,0 4.88,1.86 5.39,4.43l0.3,1.5 1.53,0.11" +
                            "c1.56,0.1 2.78,1.41 2.78,2.96 0,1.65 -1.35,3 -3,3z"
                    ).toNodes(),
                    fill = SolidColor(Color.Black)
                )
            }
            // Three drops under the cloud.
            for (x in listOf(7.5f, 12f, 16.5f)) {
                path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
                    moveTo(x, 18.2f)
                    lineTo(x - 1.1f, 21.6f)
                }
            }
        }.build()
    }
}
