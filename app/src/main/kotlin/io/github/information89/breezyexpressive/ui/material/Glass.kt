package io.github.information89.breezyexpressive.ui.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Frosted glass: blur of whatever is behind + a translucent tint + grain + a faint highlight edge.
 * With blur turned down to 0 in Settings it becomes a plain translucent tint.
 */
@Composable
fun Modifier.glass(shape: Shape, strength: GlassStrength = GlassStrength.Regular): Modifier {
    val source = LocalGlassSource.current
    val appearance = LocalAppearance.current
    val surface = MaterialTheme.colorScheme.surface
    val blur = appearance.blur.coerceIn(0f, 1f)
    val tintAlpha = when (strength) {
        GlassStrength.Regular -> 0.42f
        GlassStrength.Strong -> 0.6f
    } + (1f - blur) * 0.3f
    val tint = surface.copy(alpha = tintAlpha.coerceAtMost(0.92f))
    val edge = Color.White.copy(alpha = if (surface.luminance() < 0.5f) 0.10f else 0.45f)

    val style = remember(tint, blur, appearance.grain) {
        HazeBlurStyle {
            blurEnabled(blur > 0f)
            blurRadius((32 * blur).dp)
            noiseFactor(appearance.grain * 0.18f)
            colorEffects(listOf(HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = 0.85f)))
        }
    }

    val base = this.clip(shape)
    val blurred = if (source != null) base.hazeBlur(HazeInput.Sources(source), style) else base.then(Modifier)
    return blurred.border(1.dp, edge, shape)
}

enum class GlassStrength { Regular, Strong }

/** A glass container whose content uses the theme's normal text color. */
@Composable
fun GlassBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    strength: GlassStrength = GlassStrength.Regular,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(modifier.glass(shape, strength), content = content)
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
