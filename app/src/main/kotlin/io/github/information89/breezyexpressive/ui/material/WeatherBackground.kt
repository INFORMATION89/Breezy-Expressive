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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
            // Battery: drift at ~12 fps only while visible, pause during scrolling, and come to rest
            // 20 s after the last interaction (every redraw also re-blurs the glass on top).
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var awakeSince = System.nanoTime()
                while (true) {
                    delay(FRAME_MS)
                    val lastScroll = scrollActivity.lastScrollNanos.longValue
                    if (lastScroll > awakeSince) awakeSince = lastScroll
                    if (System.nanoTime() - awakeSince > ACTIVE_NANOS) {
                        // Resting: wait for the next interaction, then wake up again.
                        snapshotFlow { scrollActivity.lastScrollNanos.longValue }.first { it > awakeSince }
                        awakeSince = System.nanoTime()
                    }
                    if (!scrollActivity.isScrollingRecently()) time.floatValue += FRAME_MS / 1000f
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
                val bloomBrush = Brush.radialGradient(listOf(light, bloom, Color.Transparent), center = Offset(size.width / 2, size.height * 0.36f), radius = size.width * 0.8f)
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

private const val FRAME_MS = 83L
private const val ACTIVE_NANOS = 20_000_000_000L

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

    // Deep base.
    half3 color = mix(top.rgb, bottom.rgb, smoothstep(0.0, 1.0, uv.y));

    // One luminous orb, breathing and drifting very slowly.
    float2 center = float2(0.5 + 0.03 * sin(time * 0.17), 0.36 + 0.02 * cos(time * 0.13));
    float2 p = float2((uv.x - center.x) * aspect, uv.y - center.y);
    float d = length(p);
    float r = 0.21 + 0.012 * sin(time * 0.5);
    color = mix(color, bloom.rgb, 0.85 * exp(-(d * d) / (r * r * 4.0)));   // wide halo
    color = mix(color, light.rgb, 0.72 * exp(-(d * d) / (r * r * 0.55)));  // bright core

    // Darker edges (strongest at night).
    float2 v = float2((uv.x - 0.5) * aspect, uv.y - 0.42);
    color *= 1.0 - vignette * smoothstep(0.22, 0.85, length(v) * 1.1);

    // Film grain, a little stronger on the light (printed, analog feel).
    float n = hash(floor(coord)) - 0.5;
    color += n * grain * (1.0 + 0.8 * exp(-(d * d) / (r * r)));
    return half4(color, 1.0);
}
"""
