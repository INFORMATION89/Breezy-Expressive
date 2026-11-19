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

package org.breezyweather.ui.expressive.shell

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import org.breezyweather.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * A realistic moon (2026-11-19): NASA's CGI Moon Kit colour map (LRO data, credit NASA's Scientific
 * Visualization Studio; see third_party/nasa-moon) wrapped onto a sphere by a small AGSL shader and
 * lit by the phase: a soft terminator, a little limb darkening, and faint earthshine on the dark
 * side. One renderer for the moon card, the moon button and the night sun path. Below Android 13
 * it falls back to the drawn moon.
 */
internal class MoonRenderer private constructor(private val shader: Any?) {

    /**
     * Draws the moon centred at [center] with radius [r], in its phase [angle] (0/360 new, 180 full).
     * [limbAngle] is the bright limb's position angle (degrees, eastward from north; negative while
     * waxing) and [parallactic] the parallactic angle at the observer: together they turn the moon
     * the way it actually looks in the sky from that place at that time (it rotates through the
     * night, and is "upside down" in the southern hemisphere).
     */
    fun draw(
        scope: DrawScope,
        center: Offset,
        r: Float,
        angle: Int,
        fallbackShadow: Color,
        limbAngle: Double? = null,
        parallactic: Double = 0.0,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader is MoonShader) {
            shader.draw(scope, center, r, angle, limbAngle, parallactic)
        } else {
            val lit = ((1 - cos(Math.toRadians(angle.toDouble()))) / 2).toFloat()
            scope.drawMoon(center, r, angle, lit, fallbackShadow)
        }
    }

    companion object {
        @Composable
        fun remember(): MoonRenderer {
            val context = LocalContext.current
            return remember {
                val shader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.moon_map)
                        MoonShader(bitmap)
                    }.getOrNull()
                } else {
                    null
                }
                MoonRenderer(shader)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class MoonShader(bitmap: android.graphics.Bitmap) {
    private val shader = RuntimeShader(MOON_AGSL)
    private val brush = ShaderBrush(shader)

    init {
        shader.setInputShader("map", BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP))
        shader.setFloatUniform("mapSize", bitmap.width.toFloat(), bitmap.height.toFloat())
    }

    fun draw(scope: DrawScope, center: Offset, r: Float, angle: Int, limbAngle: Double?, parallactic: Double) {
        val a = Math.toRadians(angle.toDouble())
        // The bright limb's direction on the disc (north up, east to the left as we see it); without
        // a position angle: lit from the right while waxing.
        val chi = Math.toRadians(limbAngle ?: if (angle < 180) -90.0 else 90.0)
        val bx = -sin(chi)
        val by = cos(chi)
        // The sun's direction seen from the moon: behind it at new moon, behind us at full.
        val k = sin(a)
        shader.setFloatUniform("sunDir", (bx * k).toFloat(), (by * k).toFloat(), -cos(a).toFloat())
        // The whole disc turns by the parallactic angle (north of the sky vs. "up" for the observer).
        shader.setFloatUniform("rot", Math.toRadians(parallactic).toFloat())
        shader.setFloatUniform("center", center.x, center.y)
        shader.setFloatUniform("radius", r)
        scope.drawRect(brush, topLeft = Rect(center, r + 2f).topLeft, size = Rect(center, r + 2f).size)
    }
}

private const val MOON_AGSL = """
uniform shader map;
uniform float2 mapSize;
uniform float3 sunDir;
uniform float2 center;
uniform float radius;
uniform float rot;

half4 main(float2 p) {
    float2 s = (p - center) / radius;
    // Screen to the moon's own frame (y up, then turned by the parallactic angle).
    float2 q = float2(s.x, -s.y);
    float cr = cos(rot);
    float sr = sin(rot);
    float2 d = float2(q.x * cr - q.y * sr, q.x * sr + q.y * cr);
    float r2 = dot(d, d);
    if (r2 > 1.0) return half4(0.0);
    float z = sqrt(1.0 - r2);
    float3 n = float3(d.x, d.y, z);
    // The near side faces us: longitude 0 at the centre of the map.
    float lat = asin(clamp(n.y, -1.0, 1.0));
    float lon = atan(n.x, n.z);
    float2 uv = float2((lon / 6.2831853 + 0.5) * mapSize.x, (0.5 - lat / 3.1415927) * mapSize.y);
    half3 c = map.eval(uv).rgb;
    float l = dot(n, normalize(sunDir));
    float lit = smoothstep(-0.04, 0.10, l);
    float limb = 0.72 + 0.28 * z;
    // The shadow is 80 % dark: the night side stays faintly visible (earthshine).
    half3 col = c * limb * (lit * 1.05 + 0.2 * (1.0 - lit));
    float edge = smoothstep(1.0, 0.982, sqrt(r2));
    return half4(col * edge, edge);
}
"""
