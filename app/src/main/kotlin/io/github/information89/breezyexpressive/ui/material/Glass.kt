package io.github.information89.breezyexpressive.ui.material

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * Clear glass: a light blur of what's behind with only a whisper of tint, a hairline edge,
 * and (optionally) a faint glow of the conditions' accent rising from the bottom edge.
 * With blur at 0 in Settings it becomes a thin tint only.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    strength: GlassStrength = GlassStrength.Clear,
    glow: Boolean = true,
): Modifier {
    val source = LocalGlassSource.current
    val appearance = LocalAppearance.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminanceApprox() < 0.5f
    val blur = appearance.blur.coerceIn(0f, 1f)

    val baseAlpha = when (strength) {
        GlassStrength.Clear -> if (dark) 0.06f else 0.16f
        GlassStrength.Regular -> if (dark) 0.30f else 0.45f
    } + (1f - blur) * 0.2f
    val tint = scheme.surface.copy(alpha = baseAlpha)

    val style = remember(tint, blur, appearance.grain) {
        HazeBlurStyle {
            blurEnabled(blur > 0f)
            blurRadius((14 * blur).dp)
            noiseFactor(appearance.grain * 0.12f)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 0.7f)))
        }
    }
    val edge = Color.White.copy(alpha = if (dark) 0.07f else 0.45f)
    val glowColor = scheme.primary.copy(alpha = if (dark) 0.16f else 0.12f)

    return this
        .clip(shape)
        .let { if (source != null) it.hazeBlur(HazeInput.Sources(source), style) else it }
        .let {
            if (!glow) {
                it
            } else {
                it.drawBehind {
                    // Light rising from the bottom edge, like the reference: subtle, one color.
                    drawRect(
                        Brush.radialGradient(
                            listOf(glowColor, Color.Transparent),
                            center = Offset(size.width / 2, size.height * 1.05f),
                            radius = maxOf(size.width * 0.7f, size.height),
                        ),
                    )
                }
            }
        }
        .border(1.dp, edge, shape)
}

private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
