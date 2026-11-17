package io.github.information89.breezyexpressive.ui.material

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The sky, shared by the background and every glass surface: a small bitmap (1/[SKY_SCALE] of the
 * screen) that is redrawn only when the sky changes, then scaled up. Glass panels draw the same
 * bitmap at their own window position, so they look like frosted glass over the sky without
 * blurring anything.
 */
@Stable
class SkyState {
    /** Latest rendered sky; reading it in draw makes the reader redraw when the sky changes. */
    var image by mutableStateOf<ImageBitmap?>(null)
        internal set

    /** Full-size bounds of the background in the window (set during layout, not state). */
    var windowOrigin: Offset = Offset.Zero
        internal set
    var size by mutableStateOf(IntSize.Zero)
        internal set
}

val LocalSky = staticCompositionLocalOf<SkyState?> { null }

internal const val SKY_SCALE = 8

/** Values the sky is drawn from. Colors are sRGB 0..1, like the old shader's working space. */
internal data class SkyParams(
    val top: Color,
    val bottom: Color,
    val light: Color,
    val bloom: Color,
    val vignette: Float,
    val time: Float,
)

/** Renders [SkyParams] into small rotating bitmaps (CPU, safe off the main thread). */
internal class SkyRenderer(val width: Int, val height: Int) {
    private val pixels = IntArray(width * height)
    // Three buffers: one on screen, one possibly still being uploaded, one being written.
    private val buffers = Array(3) { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }
    private var next = 0

    fun render(p: SkyParams): ImageBitmap {
        val aspect = width.toFloat() / height
        // One luminous orb in the upper middle of the screen (a sun or moon), breathing slowly.
        val cx = 0.5f + 0.03f * sin(p.time * 0.17f)
        val cy = 0.36f + 0.02f * cos(p.time * 0.13f)
        val r = 0.27f + 0.015f * sin(p.time * 0.5f)
        val haloDiv = r * r * 4f
        val coreDiv = r * r * 0.55f

        for (y in 0 until height) {
            val v = (y + 0.5f) / height
            val t = smoothstep(0f, 1f, v)
            val baseR = mix(p.top.red, p.bottom.red, t)
            val baseG = mix(p.top.green, p.bottom.green, t)
            val baseB = mix(p.top.blue, p.bottom.blue, t)
            val py = v - cy
            val vy = v - 0.6f
            val row = y * width
            for (x in 0 until width) {
                val u = (x + 0.5f) / width
                // Wide halo, then bright core.
                val px = (u - cx) * aspect
                val d2 = px * px + py * py
                val halo = 0.85f * exp(-d2 / haloDiv)
                val core = 0.85f * exp(-d2 / coreDiv)
                var cr = mix(mix(baseR, p.bloom.red, halo), p.light.red, core)
                var cg = mix(mix(baseG, p.bloom.green, halo), p.light.green, core)
                var cb = mix(mix(baseB, p.bloom.blue, halo), p.light.blue, core)
                // Darker edges (strongest at night).
                val vx = (u - 0.5f) * aspect
                val shade = 1f - p.vignette * smoothstep(0.22f, 0.85f, sqrt(vx * vx + vy * vy) * 1.1f)
                cr *= shade; cg *= shade; cb *= shade
                pixels[row + x] = argb(cr, cg, cb)
            }
        }
        val bitmap = buffers[next]
        next = (next + 1) % buffers.size
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }

    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    private fun argb(r: Float, g: Float, b: Float): Int =
        (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    private fun channel(c: Float) = (c.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
}

/**
 * Film grain as one repeated tile: each pixel is white or black with a random alpha, so drawing it
 * over any color nudges it lighter or darker (like the old shader's +/- noise).
 */
object Grain {
    val brush: ShaderBrush by lazy { ShaderBrush(ImageShader(tile(), TileMode.Repeated, TileMode.Repeated)) }

    /** Draw alpha for a grain setting of 0..1 (matches the old shader's strength). */
    fun alpha(grain: Float) = grain.coerceIn(0f, 1f) * 0.06f

    private fun tile(size: Int = 128): ImageBitmap {
        val random = Random(7)
        val pixels = IntArray(size * size) {
            val n = random.nextFloat() - 0.5f
            val a = (kotlin.math.abs(n) * 2f * 255f).toInt()
            if (n >= 0f) android.graphics.Color.argb(a, 255, 255, 255) else android.graphics.Color.argb(a, 0, 0, 0)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
