package io.github.information89.breezyexpressive.ui.material

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Tracks scrolling anywhere below it, so the animated background can pause while content moves
 * (blurred glass over a moving background is the most expensive thing we draw).
 */
class ScrollActivity : NestedScrollConnection {
    internal val lastScrollNanos: MutableLongState = mutableLongStateOf(0L)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        lastScrollNanos.longValue = System.nanoTime()
        return Offset.Zero
    }

    fun isScrollingRecently(now: Long = System.nanoTime()) = now - lastScrollNanos.longValue < 400_000_000L
}

val LocalScrollActivity = staticCompositionLocalOf { ScrollActivity() }

/**
 * Abstract, slowly drifting gradient in the weather's mood, with film grain.
 * Android 13+: one AGSL shader pass. Android 12: static gradient + tiled noise.
 */
@Composable
fun WeatherBackground(
    colors: MoodColors,
    grain: Float,
    animated: Boolean,
    scrollActivity: ScrollActivity,
    modifier: Modifier = Modifier,
) {
    val core by animateColorAsState(colors.core, tween(1200), label = "core")
    val mid by animateColorAsState(colors.mid, tween(1200), label = "mid")
    val edge by animateColorAsState(colors.edge, tween(1200), label = "edge")
    val grainAlpha = grain.coerceIn(0f, 1f) * 0.14f

    val context = LocalContext.current
    val systemAnimationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val shouldAnimate = animated && !systemAnimationsOff && Build.VERSION.SDK_INT >= 33

    if (shouldAnimate) {
        LaunchedEffect(lifecycle) {
            // ~15 fps is plenty for a slow drift; only while visible, paused during scrolling.
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(66)
                    if (!scrollActivity.isScrollingRecently()) time.floatValue += 0.066f // seconds
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= 33) {
        ShaderBackground(core, mid, edge, grainAlpha, { time.floatValue }, modifier)
    } else {
        FallbackBackground(core, mid, edge, grainAlpha, modifier)
    }
}

@RequiresApi(33)
@Composable
private fun ShaderBackground(
    core: Color,
    mid: Color,
    edge: Color,
    grainAlpha: Float,
    time: () -> Float,
    modifier: Modifier,
) {
    val shader = remember { RuntimeShader(BACKGROUND_SHADER) }
    Box(
        modifier.drawWithCache {
            val brush = ShaderBrush(shader)
            onDrawBehind {
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("time", time()) // read in draw: redraws without recomposition
                shader.setColorUniform("core", core.toArgb())
                shader.setColorUniform("mid", mid.toArgb())
                shader.setColorUniform("edge", edge.toArgb())
                shader.setFloatUniform("grain", grainAlpha)
                drawRect(brush)
            }
        },
    )
}

@Composable
private fun FallbackBackground(core: Color, mid: Color, edge: Color, grainAlpha: Float, modifier: Modifier) {
    val noise = remember { noiseTile() }
    Box(
        modifier.drawWithCache {
            val gradient = Brush.radialGradient(listOf(core, mid, edge), center = Offset(size.width / 2, size.height * 0.42f), radius = size.maxDimension * 0.75f)
            val grainBrush = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
            onDrawBehind {
                drawRect(gradient)
                if (grainAlpha > 0f) drawRect(grainBrush, alpha = grainAlpha * 2f)
            }
        },
    )
}

private fun noiseTile(size: Int = 128): ImageBitmap {
    val random = Random(7)
    val pixels = IntArray(size * size) {
        val v = random.nextInt(256)
        android.graphics.Color.argb(255, v, v, v)
    }
    return android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}

private const val BACKGROUND_SHADER = """
uniform float2 resolution;
uniform float time;
layout(color) uniform half4 core;
layout(color) uniform half4 mid;
layout(color) uniform half4 edge;
uniform float grain;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float aspect = resolution.x / resolution.y;

    // A glowing point in the middle (sun, moon...), breathing slowly and drifting a little.
    float2 center = float2(0.5 + 0.03 * sin(time * 0.21), 0.42 + 0.025 * cos(time * 0.17));
    float2 p = float2((uv.x - center.x) * aspect, uv.y - center.y);
    float d = length(p);
    float radius = 0.55 + 0.04 * sin(time * 0.6);

    // Sky-like base: mid color fading to the darker edge color, deeper toward the bottom.
    half3 color = mix(mid.rgb, edge.rgb, smoothstep(0.15, 1.05, d + 0.25 * max(uv.y - 0.5, 0.0)));

    // Core glow.
    float glow = exp(-(d * d) / (radius * radius * 0.45));
    color = mix(color, core.rgb, glow * 0.85);

    // Soft concentric halos moving outward, like light through haze.
    float rings = 0.5 + 0.5 * sin(d * 16.0 - time * 0.7);
    color += core.rgb * rings * 0.07 * exp(-d * 2.2);

    // Darker edges.
    color = mix(color, edge.rgb, 0.55 * smoothstep(0.55, 1.25, d));

    // Static film grain.
    color += (hash(floor(coord)) - 0.5) * grain;
    return half4(color, 1.0);
}
"""
