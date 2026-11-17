package io.github.information89.breezyexpressive.ui.material

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.ui.theme.LocalAppearance
import kotlin.math.roundToInt

enum class GlassStrength { Clear, Regular }

/**
 * Glass without blur: the panel draws the same small sky bitmap as the background, at its own
 * window position, then a tint, grain, a hairline edge and (optionally) a faint glow of the
 * conditions' accent rising from the bottom edge. Over the smooth sky this looks like frosted
 * glass, and it costs one bitmap draw instead of a blur every frame.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    strength: GlassStrength = GlassStrength.Clear,
    glow: Boolean = false,
): Modifier {
    val sky = LocalSky.current
    val appearance = LocalAppearance.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminanceApprox() < 0.5f
    val blur = appearance.blur.coerceIn(0f, 1f)

    // Same hue as the sky behind: a slightly deeper (dark) or lighter (light) translucent panel.
    val baseAlpha = when (strength) {
        GlassStrength.Clear -> if (dark) 0.22f else 0.30f
        GlassStrength.Regular -> if (dark) 0.22f else 0.45f
    } + (1f - blur) * 0.15f
    val tint = (if (dark) Color.Black else Color.White).copy(alpha = baseAlpha)
    val edge = Color.White.copy(alpha = if (dark) 0.14f else 0.5f)
    val glowColor = if (glow) scheme.primary.copy(alpha = if (dark) 0.16f else 0.12f) else Color.Unspecified

    return this
        .clip(shape)
        .then(GlassElement(sky, tint, Grain.alpha(appearance.grain), glowColor))
        .border(1.dp, edge, shape)
}

private data class GlassElement(
    val sky: SkyState?,
    val tint: Color,
    val grainAlpha: Float,
    val glow: Color,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(sky, tint, grainAlpha, glow)

    override fun update(node: GlassNode) {
        node.sky = sky
        node.tint = tint
        node.grainAlpha = grainAlpha
        node.glow = glow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glass"
    }
}

/**
 * Moves its sky with the panel: when the panel's window position changes (scrolling, paging) only
 * its drawing is invalidated: no recomposition, no relayout.
 */
private class GlassNode(
    var sky: SkyState?,
    var tint: Color,
    var grainAlpha: Float,
    var glow: Color,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var positionInWindow = Offset.Unspecified

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow()
        if (position != positionInWindow) {
            positionInWindow = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val sky = sky
        val image = sky?.image // state read: redraws when the sky changes
        if (sky != null && image != null && positionInWindow.isSpecified) {
            val offset = sky.windowOrigin - positionInWindow
            drawImage(
                image,
                dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
                dstSize = sky.size,
                filterQuality = FilterQuality.Low,
            )
        }
        drawRect(tint)
        if (grainAlpha > 0f) drawRect(Grain.brush, alpha = grainAlpha)
        if (glow.isSpecified) {
            // Light rising from the bottom edge, like the reference: subtle, one color.
            drawRect(
                Brush.radialGradient(
                    listOf(glow, Color.Transparent),
                    center = Offset(size.width / 2, size.height * 1.05f),
                    radius = maxOf(size.width * 0.7f, size.height),
                ),
            )
        }
        drawContent()
    }
}

private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
