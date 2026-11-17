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
    val top by animateColorAsState(colors.top, tween(1200), label = "top")
    val accent by animateColorAsState(colors.accent, tween(1200), label = "accent")
    val bottom by animateColorAsState(colors.bottom, tween(1200), label = "bottom")
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
                    if (!scrollActivity.isScrollingRecently()) time.floatValue += 0.066f
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= 33) {
        ShaderBackground(top, accent, bottom, grainAlpha, { time.floatValue }, modifier)
    } else {
        FallbackBackground(top, accent, bottom, grainAlpha, modifier)
    }
}

@RequiresApi(33)
@Composable
private fun ShaderBackground(
    top: Color,
    accent: Color,
    bottom: Color,
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
                shader.setColorUniform("top", top.toArgb())
                shader.setColorUniform("accent", accent.toArgb())
                shader.setColorUniform("bottom", bottom.toArgb())
                shader.setFloatUniform("grain", grainAlpha)
                drawRect(brush)
            }
        },
    )
}

@Composable
private fun FallbackBackground(top: Color, accent: Color, bottom: Color, grainAlpha: Float, modifier: Modifier) {
    val noise = remember { noiseTile() }
    Box(
        modifier.drawWithCache {
            val gradient = Brush.verticalGradient(listOf(top, accent, bottom))
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
layout(color) uniform half4 top;
layout(color) uniform half4 accent;
layout(color) uniform half4 bottom;
uniform float grain;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float aspect = resolution.x / resolution.y;
    float t = time * 0.12;

    // Vertical base: top color into bottom color.
    half3 color = mix(top.rgb, bottom.rgb, smoothstep(0.0, 1.0, uv.y));

    // Two soft blobs drifting slowly.
    float2 p = float2(uv.x * aspect, uv.y);
    float2 blob1 = float2((0.25 + 0.18 * sin(t * 1.3)) * aspect, 0.22 + 0.10 * cos(t));
    float2 blob2 = float2((0.85 + 0.12 * cos(t * 0.8)) * aspect, 0.60 + 0.14 * sin(t * 1.1));
    color = mix(color, accent.rgb, 0.85 * smoothstep(0.75, 0.0, distance(p, blob1)));
    color = mix(color, top.rgb, 0.55 * smoothstep(0.6, 0.0, distance(p, blob2)));

    // Static film grain.
    color += (hash(floor(coord)) - 0.5) * grain;
    return half4(color, 1.0);
}
"""
