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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
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
    // Plain field, read at draw time: a state read here would lag a frame behind layout changes.
    internal var coordinates: LayoutCoordinates? = null

    // Bumped when the source moves, so glass redraws even if nothing else asks it to.
    internal var moved by mutableStateOf(Offset.Zero)
}

/** Holds a glass element's own coordinates, read at draw time like [GlassSource.coordinates]. */
private class Placed {
    var coordinates: LayoutCoordinates? = null
    var moved by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassSource(): GlassSource {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassSource(layer) }
}

/** Marks the content that glass elements should show, blurred, behind them. */
fun Modifier.glassSource(source: GlassSource): Modifier = this
    .onGloballyPositioned {
        source.coordinates = it
        source.moved = it.positionInRoot()
    }
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
    val placed = remember { Placed() }
    val radius = with(LocalDensity.current) { blur.toPx() }
    // Set once per radius: a new RenderEffect every frame makes the GPU rebuild the blur each time.
    remember(blurLayer, radius) { blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp) }
    val outline = remember { Path() }
    return this
        .onGloballyPositioned {
            placed.coordinates = it
            placed.moved = it.positionInRoot()
        }
        // Clip in the draw pass, with the outline built for this frame's size: a layer clip
        // (Modifier.clip) can lag a frame behind a resize and cut the toolbar off square.
        .drawBehind {
            outline.reset()
            outline.addOutline(shape.createOutline(size, layoutDirection, this@drawBehind))
            blurLayer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
                // Only this element's rectangle of the backdrop goes through the blur. Positions are
                // read now, not from last frame's layout, so a resize never shows a shifted backdrop.
                source.moved // subscribe: redraw when either side moves
                placed.moved
                val from = source.coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                val to = placed.coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                translate(from.x - to.x, from.y - to.y) {
                    drawLayer(source.layer)
                }
            }
            clipPath(outline) {
                drawLayer(blurLayer)
                drawRect(tint)
            }
            drawPath(outline, edge, style = Stroke(0.5.dp.toPx()))
        }
}
