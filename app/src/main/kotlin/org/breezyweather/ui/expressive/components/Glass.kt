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
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.drawscope.scale
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
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Which glass renderer draws [glass] (prototype, 2026-11-19): our own backdrop recorder, or Haze.
 * Off by default; switched in Settings › Debug to compare looks and frame times on a release build
 * before deciding (docs/UI_SYSTEMS.md). Not saved: resets when the app restarts.
 */
object GlassBackend {
    var useHaze by mutableStateOf(false)
}

/**
 * Frosted glass for overlays (toolbar, sheets later): the content behind is blurred, tinted and
 * edged with a fine light line.
 *
 * How: the content area records itself into a [GraphicsLayer] once per frame ([glassSource]); each
 * glass element re-draws just its own rectangle of that layer through a GPU blur
 * ([RenderEffect][BlurEffect]), so only the small glass area is blurred. Android 12+ only; older
 * versions get a more opaque tint instead of blur. With [GlassBackend.useHaze], Haze does both.
 */
@Stable
class GlassSource internal constructor(internal val layer: GraphicsLayer, internal val haze: HazeState) {
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
    val haze = rememberHazeState()
    return remember(layer, haze) { GlassSource(layer, haze) }
}

/**
 * The sky alone (no pages), for cards that sit on it: they blur the sky behind them, like real
 * frosted glass. Provided by the sky that draws it (the home sky, or a postcard window's own).
 */
val LocalSkyGlass = compositionLocalOf<GlassSource?> { null }

/** The whole screen a page is on (sky and pages), for sheets opened from it, like the moon card. */
val LocalPageGlass = compositionLocalOf<GlassSource?> { null }

/** Marks the content that glass elements should show, blurred, behind them. */
fun Modifier.glassSource(source: GlassSource): Modifier = if (GlassBackend.useHaze) {
    this.hazeSource(source.haze)
} else {
    glassSourceRecorder(source)
}

private fun Modifier.glassSourceRecorder(source: GlassSource): Modifier = this
    .onGloballyPositioned {
        source.coordinates = it
        source.moved = it.positionInRoot()
    }
    .drawWithContent {
        source.layer.record { this@drawWithContent.drawContent() }
        drawLayer(source.layer)
    }

/** How much smaller the backdrop is blurred than it's shown. */
private const val GLASS_DOWNSCALE = 4f

/** A frosted-glass background in [shape]: blurred backdrop + [tint] + a hairline light edge. */
@Composable
fun Modifier.glass(
    source: GlassSource?,
    shape: Shape,
    tint: Color,
    blur: Dp = 28.dp,
    edge: Color = Color.White.copy(alpha = 0.22f),
    // How much of the glass shows (read while drawing), e.g. while a button's look becomes a sheet's.
    alpha: () -> Float = { 1f },
): Modifier {
    if (source == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this.clip(shape).background(tint.copy(alpha = maxOf(tint.alpha, 0.92f))).border(0.5.dp, edge, shape)
    }
    if (GlassBackend.useHaze) return hazeGlass(source, shape, tint, blur, edge, alpha)
    val blurLayer = rememberGraphicsLayer()
    val placed = remember { Placed() }
    val radius = with(LocalDensity.current) { blur.toPx() }
    // The backdrop is blurred at a quarter of its size, then scaled back up (2026-11-19): 1/16 of
    // the pixels for the GPU, and the same look, since a blur throws that detail away anyway. At
    // full size every glass re-rendered the whole animated sky each frame and frames were missed.
    // Set once per radius: a new RenderEffect every frame makes the GPU rebuild the blur each time.
    remember(blurLayer, radius) {
        val r = radius / GLASS_DOWNSCALE
        blurLayer.renderEffect = BlurEffect(r, r, TileMode.Clamp)
    }
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
            val small = IntSize(
                (size.width / GLASS_DOWNSCALE).toInt().coerceAtLeast(1),
                (size.height / GLASS_DOWNSCALE).toInt().coerceAtLeast(1)
            )
            blurLayer.record(size = small) {
                // Only this element's rectangle of the backdrop goes through the blur. Positions are
                // read now, not from last frame's layout, so a resize never shows a shifted backdrop.
                source.moved // subscribe: redraw when either side moves
                placed.moved
                val from = source.coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                val to = placed.coordinates?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                scale(1f / GLASS_DOWNSCALE, pivot = Offset.Zero) {
                    translate(from.x - to.x, from.y - to.y) {
                        drawLayer(source.layer)
                    }
                }
            }
            val a = alpha().coerceIn(0f, 1f)
            if (a > 0f) {
                blurLayer.alpha = a
                clipPath(outline) {
                    scale(GLASS_DOWNSCALE, pivot = Offset.Zero) { drawLayer(blurLayer) }
                    drawRect(tint.copy(alpha = tint.alpha * a))
                }
                drawPath(outline, edge.copy(alpha = edge.alpha * a), style = Stroke(0.5.dp.toPx()))
            }
        }
}

/**
 * The Haze version of [glass] (prototype). Same look: blurred backdrop at [blur], [tint], hairline
 * [edge]. Known gap: [alpha] is read when composed, not every frame, so a morph that fades the glass
 * (the moon card) won't fade it smoothly yet.
 */
@Composable
private fun Modifier.hazeGlass(
    source: GlassSource,
    shape: Shape,
    tint: Color,
    blur: Dp,
    edge: Color,
    alpha: () -> Float,
): Modifier {
    val a = alpha().coerceIn(0f, 1f)
    val style = remember(tint, blur, a) {
        HazeBlurStyle {
            blurRadius(blur)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = maxOf(tint.alpha, 0.92f))))
            alpha(a)
        }
    }
    return this
        .clip(shape)
        .hazeBlur(HazeInput.Sources(source.haze), style, performanceMode = HazePerformanceMode.Balanced)
        .border(0.5.dp, edge.copy(alpha = edge.alpha * a), shape)
}
