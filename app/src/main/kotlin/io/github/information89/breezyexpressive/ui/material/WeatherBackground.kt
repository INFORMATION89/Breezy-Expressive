package io.github.information89.breezyexpressive.ui.material

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize

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
 * A full-color sky in the weather's mood: top-to-bottom gradient, a soft light rising from the
 * bottom, grain, and darker edges at night. Drawn into [sky]'s small bitmap (redrawn only when the
 * sky changes) and scaled up; glass surfaces reuse the same bitmap. One code path for Android 12+.
 */
@Composable
fun WeatherBackground(
    colors: MoodColors,
    grain: Float,
    animated: Boolean,
    scrollActivity: ScrollActivity,
    sky: SkyState,
    modifier: Modifier = Modifier,
) {
    val top = animateColorAsState(colors.top, tween(1200), label = "top")
    val bottom = animateColorAsState(colors.bottom, tween(1200), label = "bottom")
    val light = animateColorAsState(colors.light, tween(1200), label = "light")
    val bloom = animateColorAsState(colors.bloom, tween(1200), label = "bloom")
    val vignette = animateFloatAsState(colors.vignette, tween(1200), label = "vignette")
    val grainAlpha = Grain.alpha(grain)

    val context = LocalContext.current
    val systemAnimationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val shouldAnimate = animated && !systemAnimationsOff

    if (shouldAnimate) {
        LaunchedEffect(lifecycle) {
            // Battery: drift at ~12 fps only while visible, pause during scrolling, and come to rest
            // 20 s after the last interaction.
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

    // Redraw the small sky whenever its inputs change (color transitions, drift, size).
    LaunchedEffect(sky) {
        var renderer: SkyRenderer? = null
        snapshotFlow {
            sky.size to SkyParams(top.value, bottom.value, light.value, bloom.value, vignette.value, time.floatValue)
        }.collectLatest { (size, params) ->
            if (size.width <= 0 || size.height <= 0) return@collectLatest
            val w = (size.width / SKY_SCALE).coerceAtLeast(1)
            val h = (size.height / SKY_SCALE).coerceAtLeast(1)
            val r = renderer?.takeIf { it.width == w && it.height == h } ?: SkyRenderer(w, h).also { renderer = it }
            sky.image = withContext(Dispatchers.Default) { r.render(params) }
        }
    }

    Box(
        modifier
            .onGloballyPositioned {
                sky.windowOrigin = it.positionInWindow()
                sky.size = it.size
            }
            .drawWithCache {
                val fallback = Brush.verticalGradient(listOf(top.value, bottom.value))
                onDrawBehind {
                    val image = sky.image
                    if (image == null) {
                        drawRect(fallback)
                    } else {
                        drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low)
                    }
                    if (grainAlpha > 0f) drawRect(Grain.brush, alpha = grainAlpha)
                }
            },
    )
}

private const val FRAME_MS = 83L
private const val ACTIVE_NANOS = 20_000_000_000L
