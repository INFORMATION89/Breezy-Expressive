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

package org.breezyweather.ui.expressive.sky

import android.animation.ValueAnimator
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.LocalPageGlass
import org.breezyweather.ui.expressive.components.LocalSkyGlass
import org.breezyweather.ui.expressive.components.glassSource

/**
 * The sky with a calm drift: the colour fields wander slowly (loops of about 45–85 s,
 * out of step with each other) and the sun/moon glow sways a little around its real position.
 *
 * Runs as one small AGSL shader on Android 13+, updated at most [FPS] times a second and only
 * while the screen is resumed. Falls back to the static, baked [SkyBackground] on older Android,
 * in battery saver, and when the system "Remove animations" setting is on.
 */
@Composable
fun AnimatedSkyBackground(
    state: SkyState,
    modifier: Modifier = Modifier,
    // When set, the sky alone is recorded here (and provided to the content as [LocalSkyGlass]),
    // so cards on it can blur it.
    skyGlass: GlassSource? = null,
    // The glass the whole screen is recorded in, provided to the content as [LocalPageGlass].
    pageGlass: GlassSource? = null,
    content: @Composable () -> Unit = {},
) {
    val inner: @Composable () -> Unit = if (skyGlass == null) {
        content
    } else {
        {
            CompositionLocalProvider(LocalSkyGlass provides skyGlass, LocalPageGlass provides pageGlass) {
                content()
            }
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && rememberSkyMotionAllowed()) {
        ShaderSky(state, modifier, skyGlass, inner)
    } else if (skyGlass != null) {
        Box(modifier) {
            SkyBackground(state, Modifier.fillMaxSize().glassSource(skyGlass))
            inner()
        }
    } else {
        SkyBackground(state, modifier, content)
    }
}

/** Motion is off in battery saver and when the system animation scale is zero. */
@Composable
private fun rememberSkyMotionAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        ValueAnimator.areAnimatorsEnabled() && !powerSave
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderSky(state: SkyState, modifier: Modifier, skyGlass: GlassSource?, content: @Composable () -> Unit) {
    val shader = remember { RuntimeShader(SKY_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    // Seconds on a wall-clock base, so the drift continues where it was when coming back.
    var seconds by remember { mutableFloatStateOf(clockSeconds()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (now - last >= FRAME_NANOS) {
                        last = now
                        seconds = clockSeconds()
                    }
                }
            }
        }
    }
    Box(modifier) {
        // The sky in its own offscreen layer: the GPU runs the shader only when the sky changes
        // (30 fps clock, palette, size). Frames in between, e.g. a 90 fps page swipe, reuse it.
        Box(
            Modifier
                .fillMaxSize()
                .then(if (skyGlass != null) Modifier.glassSource(skyGlass) else Modifier)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind {
                    val p = state.palette
                    shader.setFloatUniform("uSize", size.width, size.height)
                    shader.setFloatUniform("uTime", seconds) // reading it here only redraws, no recomposition
                    shader.color("uBase", p.base)
                    shader.color("uC1", p.blobs[0])
                    shader.color("uC2", p.blobs[1])
                    shader.color("uC3", p.blobs[2])
                    shader.color("uSun", p.sun)
                    shader.setFloatUniform("uSunPos", state.sunX, state.sunY, SUN_RADIUS)
                    shader.setFloatUniform("uSunStrength", p.sunStrength)
                    shader.setFloatUniform("uGrain", p.grain)
                    drawRect(brush)
                }
        )
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.color(name: String, color: Color) = setColorUniform(name, color.toArgb())

private fun clockSeconds() = (System.currentTimeMillis() % 3_600_000L) / 1000f

private const val FPS = 30
private const val FRAME_NANOS = 1_000_000_000L / FPS
private const val SUN_RADIUS = 0.42f

/**
 * Same layout as the static sky (fields at top-left, right and bottom-left, radii as fractions of
 * the longer side, same gradient falloff), plus slow drift and overlay grain.
 */
private const val SKY_AGSL = """
uniform float2 uSize;
uniform float uTime;
layout(color) uniform half4 uBase;
layout(color) uniform half4 uC1;
layout(color) uniform half4 uC2;
layout(color) uniform half4 uC3;
layout(color) uniform half4 uSun;
uniform float3 uSunPos;
uniform float uSunStrength;
uniform float uGrain;

float field(float2 p, float2 centre, float radius, float unit) {
    float d = distance(p, centre * uSize) / (radius * unit);
    return d < 0.45 ? mix(1.0, 0.75, d / 0.45) : mix(0.75, 0.0, clamp((d - 0.45) / 0.55, 0.0, 1.0));
}

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 p) {
    float unit = max(uSize.x, uSize.y);
    float t = uTime * 6.2831853;
    float2 d1 = float2(sin(t / 61.0), cos(t / 71.0)) * 0.085;
    float2 d2 = float2(cos(t / 83.0 + 1.3), sin(t / 53.0 + 0.4)) * 0.10;
    float2 d3 = float2(sin(t / 67.0 + 2.1), cos(t / 47.0 + 0.7)) * 0.085;
    float2 ds = float2(sin(t / 97.0), cos(t / 79.0)) * 0.025;

    half3 col = uBase.rgb;
    col = mix(col, uC1.rgb, half(field(p, float2(0.08, 0.06) + d1, 0.62, unit)));
    col = mix(col, uC2.rgb, half(field(p, float2(0.98, 0.46) + d2, 0.55, unit)));
    col = mix(col, uC3.rgb, half(field(p, float2(0.18, 0.98) + d3, 0.60, unit)));
    float s = field(p, uSunPos.xy + ds, uSunPos.z, unit);
    col = mix(col, uSun.rgb, half(s * uSunStrength));

    half3 g = half3(hash(floor(p * 0.5)));
    half3 overlay = mix(2.0 * col * g, 1.0 - 2.0 * (1.0 - col) * (1.0 - g), step(half3(0.5), col));
    col = mix(col, overlay, half(uGrain));
    return half4(col, 1.0);
}
"""
