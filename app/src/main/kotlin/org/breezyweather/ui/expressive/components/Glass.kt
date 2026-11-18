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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Frosted glass for overlays (toolbar, sheets later): the content behind is blurred, tinted and
 * edged with a fine light line.
 *
 * How: the content area records itself into a [GraphicsLayer] once per frame ([glassSource]); each
 * glass element re-draws just its own rectangle of that layer through a GPU blur
 * ([RenderEffect][BlurEffect]), so only the small glass area is blurred. Android 12+ only; older
 * versions get a more opaque tint instead of blur.
 */
@Stable
class GlassSource internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassSource(): GlassSource {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassSource(layer) }
}

/** Marks the content that glass elements should show, blurred, behind them. */
fun Modifier.glassSource(source: GlassSource): Modifier = this
    .onGloballyPositioned { source.origin = it.positionInRoot() }
    .drawWithContent {
        source.layer.record { this@drawWithContent.drawContent() }
        drawLayer(source.layer)
    }

/** A frosted-glass background in [shape]: blurred backdrop + [tint] + a hairline light edge. */
@Composable
fun Modifier.glass(
    source: GlassSource,
    shape: Shape,
    tint: Color,
    blur: Dp = 28.dp,
    edge: Color = Color.White.copy(alpha = 0.22f),
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this.clip(shape).background(tint.copy(alpha = maxOf(tint.alpha, 0.92f))).border(0.5.dp, edge, shape)
    }
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val radius = blur.toPx()
            blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            blurLayer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
                // Only this element's rectangle of the backdrop goes through the blur.
                translate(source.origin.x - origin.x, source.origin.y - origin.y) {
                    drawLayer(source.layer)
                }
            }
            drawLayer(blurLayer)
            drawRect(tint)
        }
        .border(0.5.dp, edge, shape)
}
