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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.domain.weather.model.getDescription
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.glass
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

private const val DAY = 24L * 60 * 60 * 1000

/** The moon's phase angle (0/360 new, 180 full) and how lit it is, at [date]. */
private fun moonAt(date: Date): Pair<Int, Float> {
    val phase = MoonIllumination.compute().on(date).execute().phase
    val angle = (phase + 180).roundToInt().mod(360)
    val lit = ((1 - cos(Math.toRadians(angle.toDouble()))) / 2).toFloat()
    return angle to lit
}

/**
 * A moon in its phase: a pale disc with soft grey "seas", and the shadow sliding across it; lit on
 * the right while waxing (angle < 180).
 */
internal fun DrawScope.drawMoon(center: Offset, r: Float, angle: Int, lit: Float, shadow: Color) {
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFF4F1E8), Color(0xFFCFCBC0)),
            center = center - Offset(r / 3, r / 3),
            radius =
            r * 1.6f
        ),
        radius = r,
        center = center
    )
    // A few soft seas, always in the same places.
    val rnd = Random(4)
    repeat(6) {
        val o = Offset((rnd.nextFloat() - 0.5f) * r * 1.2f, (rnd.nextFloat() - 0.5f) * r * 1.2f)
        drawCircle(
            Color(0xFF9E9A90).copy(alpha = 0.28f),
            radius = r * (0.14f + rnd.nextFloat() * 0.18f),
            center =
            center + o
        )
    }
    val waxing = angle < 180
    val shift = 2f * r * lit * if (waxing) -1f else 1f
    clipPath(Path().apply { addOval(Rect(center, r)) }) {
        drawCircle(shadow, radius = r * 1.02f, center = center + Offset(shift, 0f))
    }
}

/** The moon phase button under the sun path: always visible, outlined so it reads as a button. */
@Composable
internal fun MoonButton(location: Location, now: Long, text: Color) {
    val context = LocalContext.current
    val (angle, lit) = moonAt(Date(now))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, text.copy(alpha = 0.35f), CircleShape)
            .clickable { MoonCardState.request = location to now }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            listOfNotNull(
                breezyweather.domain.weather.model.MoonPhase(angle).getDescription(context),
                "${(lit * 100).roundToInt()} %"
            ).joinToString(" · "),
            color = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Which place's moon card is open (the shell draws it over everything, on its glass). */
internal object MoonCardState {
    var request by androidx.compose.runtime.mutableStateOf<Pair<Location, Long>?>(null)
}

/**
 * The moon card over the shell, like the search card: the page is only darkened a little, and the
 * blur is only behind the card (the shell's glass). Size and position to review with the user.
 */
@Composable
internal fun MoonCardOverlay(glass: GlassSource) {
    val (location, now) = MoonCardState.request ?: return
    BackHandler { MoonCardState.request = null }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { MoonCardState.request = null }
        )
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                // Taps on the card stay on the card (they used to fall through and close it).
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            MoonCard(location, now, glass)
        }
    }
}

/**
 * The moon card (user's references, 2026-11-18): a floating dark, starry card. A big moon in its
 * phase, the phase name and the date, a dial of days to scrub through phases (drag), then rows:
 * illumination, moonrise, moonset, next full moon, distance.
 */
@Composable
private fun MoonCard(location: Location, now: Long, glass: GlassSource) {
    val context = LocalContext.current
    // Scrubbing offset in days (fractional while dragging).
    var offset by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    var fling by remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Haptics: a firm tick per day, a lighter one per 6-hour step.
    fun tick(before: Float, after: Float) {
        when {
            kotlin.math.floor(before + 0.5f) != kotlin.math.floor(after + 0.5f) ->
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            kotlin.math.floor(before * 4f + 0.5f) != kotlin.math.floor(after * 4f + 0.5f) ->
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val moon = MoonRenderer.remember()
    val scope = rememberCoroutineScope()
    val at = Date(now + (offset * DAY).toLong())
    val (angle, lit) = moonAt(at)
    val zone = location.timeZone
    val times = MoonTimes.compute().on(
        at
    ).timezone(zone).at(location.latitude, location.longitude).fullCycle().execute()
    val full = MoonPhase.compute().phase(MoonPhase.Phase.FULL_MOON).on(at).execute().time
    val distance = MoonPosition.compute().on(at).at(location.latitude, location.longitude).execute().distance
    val ink = Color(0xFFEDEBF5)
    run {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                // Black glass, the same material as the search card's (darker tint).
                .glass(glass, RoundedCornerShape(32.dp), Color(0xFF06060C).copy(alpha = 0.62f))
                .clip(RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Text(stringResource(R.string.moon_title), color = ink, style = MaterialTheme.typography.titleMedium)
            // The moon on a starry night.
            Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                val rnd = Random(11)
                repeat(70) {
                    drawCircle(
                        Color.White.copy(alpha = 0.25f + rnd.nextFloat() * 0.6f),
                        radius = 0.6f + rnd.nextFloat() * 1.4f,
                        center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                    )
                }
                // A soft glow, then the real moon.
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.6f
                    ),
                    radius = size.minDimension * 0.6f,
                    center = center
                )
                moon.draw(this, center, size.minDimension * 0.36f, angle, Color(0xFF050509))
            }
            Text(
                breezyweather.domain.weather.model.MoonPhase(angle).getDescription(context) ?: "",
                color = ink,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).apply { timeZone = zone }.format(at),
                color = ink.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            // The dial: ticks every 6 hours, a longer one per day; drag to move through the days.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .pointerInput(Unit) {
                        val perDay = size.width / 4f
                        val tracker = VelocityTracker()
                        detectHorizontalDragGestures(
                            onDragStart = {
                                fling?.cancel()
                                tracker.resetTracking()
                            },
                            onDragEnd = {
                                // Inertia: a flick keeps the dial turning, slowing down, then it
                                // settles on the nearest 6-hour tick.
                                val v = -tracker.calculateVelocity().x / perDay
                                fling = scope.launch {
                                    var last = offset
                                    animateDecay(offset, v, FloatExponentialDecaySpec(frictionMultiplier = 1.6f)) {
                                            value,
                                            _,
                                        ->
                                        offset = value.coerceIn(-14f, 14f)
                                        tick(last, offset)
                                        last = offset
                                    }
                                    val snapped = (kotlin.math.round(offset * 4f) / 4f).coerceIn(-14f, 14f)
                                    animate(
                                        offset,
                                        snapped,
                                        animationSpec = spring(
                                            dampingRatio = 0.9f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) {
                                            value,
                                            _,
                                        ->
                                        offset = value
                                    }
                                }
                            }
                        ) { change, dx ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            val before = offset
                            // Drag left to go forward in time (the ticks follow the finger).
                            offset = (offset - dx / perDay).coerceIn(-14f, 14f)
                            tick(before, offset)
                        }
                    }
                    // Double-tap: back to today.
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scope.launch {
                                animate(
                                    offset,
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                                ) {
                                        v,
                                        _,
                                    ->
                                    offset = v
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                        })
                    }
            ) {
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val perDay = size.width / 4f
                    val mid = size.width / 2
                    for (q in -40..40) {
                        val d = q / 4f - (offset - offset.toInt())
                        val x = mid + d * perDay
                        if (x < 0 || x > size.width) continue
                        val major = (q + (offset.toInt() * 4)) % 4 == 0
                        val fade = 1f - (abs(x - mid) / mid).coerceIn(0f, 1f) * 0.8f
                        drawLine(
                            ink.copy(alpha = (if (major) 0.9f else 0.35f) * fade),
                            Offset(x, if (major) 2f else 8f),
                            Offset(x, size.height - 2f),
                            strokeWidth = if (major) 2f else 1.2f
                        )
                    }
                    // Today: a small dot under its tick (moves with the dial); out of view, an arrow
                    // at that edge points the way back.
                    val todayX = mid - offset * perDay
                    if (todayX !in 0f..size.width) {
                        val left = todayX < 0f
                        val ax = if (left) 6f * density else size.width - 6f * density
                        val ay = size.height + 6f * density
                        val dir = if (left) 1f else -1f
                        drawPath(
                            Path().apply {
                                moveTo(ax - dir * 4f * density, ay)
                                lineTo(ax + dir * 3f * density, ay - 4f * density)
                                lineTo(ax + dir * 3f * density, ay + 4f * density)
                                close()
                            },
                            Color(0xFFF6B04A)
                        )
                    }
                    if (todayX in 0f..size.width) {
                        drawCircle(
                            Color(0xFFF6B04A),
                            radius = 3.5f * density,
                            center = Offset(
                                todayX,
                                size.height + 6f * density
                            )
                        )
                    }
                    drawLine(Color(0xFF6FB7F0), Offset(mid, 0f), Offset(mid, size.height), strokeWidth = 3f)
                }
            }
            Text(
                if (abs(offset) <
                    0.5f
                ) {
                    stringResource(R.string.shell_today)
                } else {
                    SimpleDateFormat("EEE", Locale.getDefault()).apply {
                        timeZone =
                            zone
                    }.format(at)
                },
                color = ink.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(10.dp))
            fun hm(d: Date?) =
                d?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = zone }.format(it) } ?: "–"
            MoonRow(stringResource(R.string.moon_illumination), "${(lit * 100).roundToInt()} %", ink)
            MoonRow(stringResource(R.string.moon_rise), hm(times.rise), ink)
            MoonRow(stringResource(R.string.moon_set), hm(times.set), ink)
            MoonRow(
                stringResource(R.string.moon_next_full),
                context.getString(
                    R.string.moon_days,
                    ((full.time - at.time) / DAY.toDouble()).roundToInt().coerceAtLeast(0)
                ),
                ink
            )
            MoonRow(stringResource(R.string.moon_distance), "%,d km".format(distance.roundToInt()), ink, last = true)
        }
    }
}

@Composable
private fun MoonRow(label: String, value: String, ink: Color, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ink, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = ink.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
    }
    if (!last) HorizontalDivider(color = ink.copy(alpha = 0.12f))
}
