package io.github.information89.breezyexpressive.ui.material

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
 * A full-color sky in the weather's mood: top-to-bottom gradient, a soft sun/moon light at the top,
 * a slowly drifting color bloom rising from the bottom, grain, and black edges at night.
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
    val top by animateColorAsState(colors.top, tween(1200), label = "top")
    val bottom by animateColorAsState(colors.bottom, tween(1200), label = "bottom")
    val light by animateColorAsState(colors.light, tween(1200), label = "light")
    val bloom by animateColorAsState(colors.bloom, tween(1200), label = "bloom")
    val vignette by animateFloatAsState(colors.vignette, tween(1200), label = "vignette")
    val grainAlpha = grain.coerceIn(0f, 1f) * 0.12f

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
        val shader = remember { RuntimeShader(BACKGROUND_SHADER) }
        Box(
            modifier.drawWithCache {
                val brush = ShaderBrush(shader)
                onDrawBehind {
                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("time", time.floatValue) // read in draw: no recomposition
                    shader.setColorUniform("top", top.toArgb())
                    shader.setColorUniform("bottom", bottom.toArgb())
                    shader.setColorUniform("light", light.toArgb())
                    shader.setColorUniform("bloom", bloom.toArgb())
                    shader.setFloatUniform("vignette", vignette)
                    shader.setFloatUniform("grain", grainAlpha)
                    drawRect(brush)
                }
            },
        )
    } else {
        val noise = remember { noiseTile() }
        Box(
            modifier.drawWithCache {
                val gradient = Brush.verticalGradient(listOf(top, bottom))
                val bloomBrush = Brush.radialGradient(listOf(bloom, Color.Transparent), center = Offset(size.width / 2, size.height * 1.05f), radius = size.width)
                val vignetteBrush = Brush.radialGradient(listOf(Color.Transparent, Color.Black), center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * 0.75f)
                val grainBrush = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
                onDrawBehind {
                    drawRect(gradient)
                    drawRect(bloomBrush, alpha = 0.7f)
                    if (vignette > 0f) drawRect(vignetteBrush, alpha = vignette)
                    if (grainAlpha > 0f) drawRect(grainBrush, alpha = grainAlpha * 2f)
                }
            },
        )
    }
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
layout(color) uniform half4 top;
layout(color) uniform half4 bottom;
layout(color) uniform half4 light;
layout(color) uniform half4 bloom;
uniform float vignette;
uniform float grain;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float aspect = resolution.x / resolution.y;

    // Sky: top color into bottom color.
    half3 color = mix(top.rgb, bottom.rgb, smoothstep(0.0, 1.0, uv.y));

    // Soft sun / moon light at the top, drifting a little.
    float2 g = float2((uv.x - 0.5 - 0.05 * sin(time * 0.15)) * aspect, uv.y + 0.04);
    color = mix(color, light.rgb, 0.5 * exp(-dot(g, g) / 0.07));

    // Color bloom rising from the bottom, breathing and swaying slowly.
    float2 b = float2((uv.x - 0.5 - 0.18 * sin(time * 0.23)) * aspect * 0.8, uv.y - 1.08 - 0.03 * sin(time * 0.4));
    color = mix(color, bloom.rgb, 0.8 * exp(-dot(b, b) / (0.20 + 0.03 * sin(time * 0.5))));

    // Night only: edges fade to black.
    float2 v = float2((uv.x - 0.5) * aspect, uv.y - 0.45);
    color *= 1.0 - vignette * smoothstep(0.18, 0.62, length(v) * 1.1);

    // Static film grain.
    color += (hash(floor(coord)) - 0.5) * grain;
    return half4(color, 1.0);
}
"""
