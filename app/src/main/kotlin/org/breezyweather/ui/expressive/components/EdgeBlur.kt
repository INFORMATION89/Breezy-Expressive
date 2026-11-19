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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Progressive blur at the top and bottom edges of scrolling content: near an edge the content gets
 * more blurred and fades into [page], so it doesn't end in a hard cut and it hints that there is
 * more to scroll. Only the two edge bands are blurred (GPU, Android 12+); older versions get the
 * colour fade only.
 */
@Composable
fun Modifier.edgeBlur(
    page: Color,
    top: Dp = 96.dp,
    bottom: Dp = 140.dp,
    blur: Dp = 18.dp,
    strength: () -> Float = { 1f },
): Modifier {
    val content = rememberGraphicsLayer()
    val topBand = rememberGraphicsLayer()
    val bottomBand = rememberGraphicsLayer()
    return this.drawWithContent {
        val topPx = top.toPx()
        val bottomPx = bottom.toPx()
        // Read in the draw phase, e.g. how far the page behind has faded in.
        val k = strength().coerceIn(0f, 1f)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawContent()
        } else {
            content.record { this@drawWithContent.drawContent() }
            drawLayer(content)
            topBand.alpha = k
            bottomBand.alpha = k
            band(topBand, content, offsetY = 0f, height = topPx, blur.toPx(), fadeDown = true)
            band(
                bottomBand,
                content,
                offsetY = size.height - bottomPx,
                height = bottomPx,
                blur.toPx(),
                fadeDown = false
            )
        }
        // A soft fade into the page colour on top, so text near the edges never fights the system bars.
        drawRect(
            Brush.verticalGradient(0f to page.copy(alpha = 0.85f * k), 1f to page.copy(alpha = 0f), endY = topPx * 0.8f)
        )
        drawRect(
            Brush.verticalGradient(
                0f to page.copy(alpha = 0f),
                1f to page.copy(alpha = 0.85f * k),
                startY = size.height - bottomPx * 0.7f,
                endY = size.height
            )
        )
    }
}

/** Draws a blurred copy of [content]'s band, fading from full blur at the edge to none inward. */
private fun DrawScope.band(
    layer: GraphicsLayer,
    content: GraphicsLayer,
    offsetY: Float,
    height: Float,
    blurPx: Float,
    fadeDown: Boolean,
) {
    // Set once (2026-11-19): a new RenderEffect every frame made the GPU rebuild the blur each time.
    if (layer.renderEffect == null) layer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
    layer.record(IntSize(size.width.toInt(), height.toInt())) {
        translate(top = -offsetY) { drawLayer(content) }
        val mask = if (fadeDown) {
            Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent)
        } else {
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black)
        }
        drawRect(mask, blendMode = BlendMode.DstIn)
    }
    translate(top = offsetY) { drawLayer(layer) }
}
