package io.github.information89.breezyexpressive.ui.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import io.github.information89.breezyexpressive.ui.theme.LocalAppearance

/** The background that glass surfaces blur. Provided by the page shell. */
val LocalGlassSource = staticCompositionLocalOf<HazeState?> { null }

enum class GlassStrength { Clear, Regular }

/**
 * Frosted glass: light blur of what's behind + a thin translucent tint + grain + a highlight edge.
 * [dataTint] colors the glass with the card's data (UV level, rain, sun…) as a soft glow from the top corner.
 * With blur at 0 in Settings it becomes tint only.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    strength: GlassStrength = GlassStrength.Clear,
    dataTint: Color = Color.Unspecified,
): Modifier {
    val source = LocalGlassSource.current
    val appearance = LocalAppearance.current
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminanceApprox() < 0.5f
    val blur = appearance.blur.coerceIn(0f, 1f)

    // Mostly see-through: the background should stay visible behind cards.
    val baseAlpha = when (strength) {
        GlassStrength.Clear -> if (dark) 0.18f else 0.30f
        GlassStrength.Regular -> if (dark) 0.40f else 0.55f
    } + (1f - blur) * 0.25f
    val tint = surface.copy(alpha = baseAlpha.coerceAtMost(0.9f))

    val style = remember(tint, blur, appearance.grain) {
        HazeBlurStyle {
            blurEnabled(blur > 0f)
            blurRadius((20 * blur).dp)
            noiseFactor(appearance.grain * 0.15f)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 0.8f)))
        }
    }

    val edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.22f else 0.7f), Color.White.copy(alpha = if (dark) 0.04f else 0.2f)),
    )

    val blurred = this
        .clip(shape)
        .let { if (source != null) it.hazeBlur(HazeInput.Sources(source), style) else it }
    val tinted = if (dataTint.isSpecified) {
        blurred.drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(dataTint.copy(alpha = if (dark) 0.42f else 0.32f), dataTint.copy(alpha = 0f)),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.maxDimension * 1.1f,
                ),
            )
        }
    } else {
        blurred
    }
    return tinted.border(1.dp, edge, shape)
}

/** A glass container whose content uses the theme's normal text color. */
@Composable
fun GlassBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    strength: GlassStrength = GlassStrength.Clear,
    dataTint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(modifier.glass(shape, strength, dataTint), content = content)
    }
}

private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
