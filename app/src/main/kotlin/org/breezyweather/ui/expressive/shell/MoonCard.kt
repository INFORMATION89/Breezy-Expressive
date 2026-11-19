@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.domain.weather.model.getDescription
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.SolsticeMotion
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
internal fun DrawScope.drawMoon(center: Offset, r: Float, angle: Int, lit: Float, shadow: Color, alpha: Float = 1f) {
    if (alpha < 0.999f) {
        // Faded as a whole (e.g. towards a dial's edges).
        val paint = androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha }
        drawContext.canvas.saveLayer(Rect(center, r * 1.1f), paint)
        drawMoon(center, r, angle, lit, shadow, 1f)
        drawContext.canvas.restore()
        return
    }
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

/** The moon card's shared transition: Compose's container transform between the button and the sheet. */
internal class MoonShared(val scope: SharedTransitionScope, val transition: Transition<Boolean>)

internal val LocalMoonShared = staticCompositionLocalOf<MoonShared?> { null }

/** The dial's settle spring. */
private val moonSettle = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

private fun hhmm(zone: java.util.TimeZone) = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = zone }

/**
 * The moon morph is always seeked: a gesture-driven spring (SolsticeMotion.container, with the
 * finger's velocity) sets the transition's fraction, and every shared part follows that fraction
 * linearly, so there's one physical curve, not two stacked.
 */
private const val MOON_SPAN = 1000
private val moonBounds = BoundsTransform { _, _ -> tween(MOON_SPAN, easing = LinearEasing) }

/** The pill's own border and "· 71 %" go in the first tenth of the way out, and come back last. */
private val pillExit = fadeOut(tween(MOON_SPAN / 10, easing = LinearEasing))
private val pillEnter = fadeIn(tween(MOON_SPAN / 10, delayMillis = MOON_SPAN * 9 / 10, easing = LinearEasing))

/**
 * Wraps the shell so the moon button (deep in a page) and the moon sheet (over everything) share
 * one transition (agreed 2026-11-19: Compose's shared bounds, seekable so a pull scrubs it).
 */
@Composable
internal fun MoonSharedLayout(content: @Composable () -> Unit) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        val transition = rememberTransition(MoonCardState.seek, label = "moon")
        CompositionLocalProvider(LocalMoonShared provides MoonShared(this, transition)) { content() }
    }
}

/** The moon phase button under the sun path: always visible, outlined so it reads as a button. */
@Composable
internal fun MoonButton(
    location: Location,
    now: Long,
    text: Color,
    palette: org.breezyweather.ui.expressive.sky.SkyPalette,
    night: Boolean,
) {
    val context = LocalContext.current
    val (angle, lit) = moonAt(Date(now))
    val phase = breezyweather.domain.weather.model.MoonPhase(angle).getDescription(context) ?: ""
    val suffix = " · ${(lit * 100).roundToInt()} %"
    // The screen it's on (home, or a postcard window): the card blurs that.
    val pageGlass = org.breezyweather.ui.expressive.components.LocalPageGlass.current
    val shared = LocalMoonShared.current
    val key = "moon-${location.formattedId}"

    @Composable
    fun Pill(modifier: Modifier, label: Modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(CircleShape)
                .border(1.dp, text.copy(alpha = 0.35f), CircleShape)
                .clickable {
                    MoonCardState.glass = pageGlass
                    MoonCardState.buttonInk = text
                    MoonCardState.show(location, now, null, phase + suffix, palette, night)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(phase, color = text, style = MaterialTheme.typography.labelLarge, modifier = label)
            Text(suffix, color = text, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (shared == null) {
        Pill(Modifier, Modifier)
        return
    }
    // A placeholder keeps the button's space while the button itself is the card.
    Box {
        Pill(Modifier.graphicsLayer { alpha = 0f }, Modifier)
        shared.transition.AnimatedVisibility(
            visible = { open -> !(open && MoonCardState.request?.first?.formattedId == location.formattedId) },
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            with(shared.scope) {
                Pill(
                    // The pill's shape morphs into the sheet (no crossfade of the containers).
                    Modifier.sharedBounds(
                        rememberSharedContentState(key),
                        this@AnimatedVisibility,
                        enter = pillEnter,
                        exit = pillExit,
                        boundsTransform = moonBounds,
                        clipInOverlayDuringTransition = OverlayClip(CircleShape)
                    ),
                    Modifier.sharedBounds(
                        rememberSharedContentState("$key-label"),
                        this@AnimatedVisibility,
                        boundsTransform = moonBounds,
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                )
            }
        }
    }
}

/** Which place's moon card is open (the shell draws it over everything, on its glass). */
internal object MoonCardState {
    var request by androidx.compose.runtime.mutableStateOf<Pair<Location, Long>?>(null)

    /** Closed (false) or open (true); seekable, so pulling the sheet scrubs it. */
    val seek = SeekableTransitionState(false)

    /** The button's label. */
    var label: String = ""

    /** The forecast's sky the card is tinted with, and whether it's night at the place (dark card). */
    var palette: org.breezyweather.ui.expressive.sky.SkyPalette? = null
    var night: Boolean = true

    /** The button's text colour. */
    var buttonInk: Color = Color.White

    /** The glass of the screen the card was opened from (null: the shell's). */
    var glass: GlassSource? = null

    /** 0 closed … 1 open; the shell reads it to recede the page, like search. */
    var progress by mutableFloatStateOf(0f)

    fun show(
        location: Location,
        now: Long,
        @Suppress("UNUSED_PARAMETER") from: Rect?,
        text: String,
        sky: org.breezyweather.ui.expressive.sky.SkyPalette,
        isNight: Boolean,
    ) {
        label = text
        palette = sky
        night = isNight
        request = location to now
    }
}

/**
 * The moon card as a sheet, like the search card (agreed 2026-11-19): the same glass, docked at the
 * bottom at full screen height, a handle on top. It's the button grown (Compose's shared bounds):
 * the button's shape and its phase name turn into the sheet and its heading, and back. Pull it
 * down and the morph runs backwards under the finger; let go past a quarter (or flick) to close.
 */
@Composable
internal fun MoonCardOverlay(glass: GlassSource) {
    val request = MoonCardState.request ?: return
    val (location, now) = request
    val shared = LocalMoonShared.current ?: return
    val seek = MoonCardState.seek
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The morph's position: 0 closed … 1 open, above 1 while rubber-banding or overshooting. It's an
    // Animatable driven by the gesture (agreed 2026-11-19), and the shared transition is seeked to
    // it, so the finger's velocity carries into the springs.
    val drive = remember(request) { Animatable(0f) }
    val progress = remember(drive) { androidx.compose.runtime.derivedStateOf { drive.value.coerceIn(0f, 1f) } }
    val range = with(density) { 420.dp.toPx() }
    // Keep the transition on the drive's position, in order (snapshotFlow skips stale values).
    LaunchedEffect(request) {
        snapshotFlow { drive.value.coerceIn(0f, 1f) }.collect { f ->
            MoonCardState.progress = f
            when {
                seek.currentState && seek.targetState -> if (f < 1f) seek.seekTo(1f - f, targetState = false)
                seek.currentState -> if (f <=
                    0f
                ) {
                    seek.snapTo(false)
                } else if (f >= 1f) {
                    seek.snapTo(true)
                } else {
                    seek.seekTo(1f - f, false)
                }
                else -> if (f >= 1f) {
                    seek.snapTo(true)
                } else if (f > 0f) {
                    seek.seekTo(f, targetState = true)
                }
            }
        }
    }
    // Opening: the container's spring from the button.
    LaunchedEffect(request) {
        drive.animateTo(1f, SolsticeMotion.container)
        seek.snapTo(true)
    }
    fun finish(open: Boolean, velocity: Float = 0f) {
        scope.launch {
            drive.animateTo(if (open) 1f else 0f, SolsticeMotion.container, initialVelocity = velocity)
            if (open) {
                seek.snapTo(true)
            } else {
                seek.snapTo(false)
                MoonCardState.progress = 0f
                MoonCardState.request = null
            }
        }
    }
    fun close() = finish(open = false)
    BackHandler(enabled = MoonCardState.request != null) { close() }
    // The drag: down scrubs the morph back towards the pill; up past open rubber-bands.
    var raw by remember(request) { mutableFloatStateOf(0f) }
    fun pull(d: Float) {
        raw += d
        val to = if (raw >= 0f) {
            1f - raw / range
        } else {
            1f + SolsticeMotion.rubberBand(-raw, range) / range * 0.25f
        }
        scope.launch { drive.snapTo(to.coerceAtLeast(0f)) }
    }
    fun release(velocity: Float) {
        // Down is positive for the finger, negative for the morph's position.
        val v = -velocity / range
        val p = drive.value
        val rest = SolsticeMotion.project(p.coerceAtMost(1f), v, decelerationRate = 0.99f)
        raw = 0f
        finish(open = rest > 0.6f && p > 0.08f, velocity = v)
    }
    // Round as the button; docked, only the top corners stay round.
    val cardShape = remember {
        GenericShape { size, _ ->
            val top = minOf(32.dp.value * density.density, size.minDimension / 2)
            val bottom = top * (1f - progress.value.coerceIn(0f, 1f))
            addRoundRect(
                RoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    topLeftCornerRadius = CornerRadius(top),
                    topRightCornerRadius = CornerRadius(top),
                    bottomRightCornerRadius = CornerRadius(bottom),
                    bottomLeftCornerRadius = CornerRadius(bottom)
                )
            )
        }
    }
    // The card takes the forecast's tint: the sky's base colour, darkened at night (the card goes
    // dark mode when it's night at the place) and lightened by day.
    val sky = MoonCardState.palette
    val night = MoonCardState.night
    val tint = when {
        sky == null -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        night -> lerp(sky.base, Color(0xFF05050A), 0.55f).copy(alpha = 0.62f)
        else -> lerp(sky.base, Color.White, 0.55f).copy(alpha = 0.55f)
    }
    val ink = when {
        sky == null -> MaterialTheme.colorScheme.onSurface
        night -> Color(0xFFEDEBF5)
        else -> sky.text
    }
    val status = WindowInsets.statusBars
    val key = "moon-${location.formattedId}"
    // Gestures on the whole screen (the sheet itself shrinks under the finger while a pull scrubs
    // it, so a drag measured on it would run ahead): vertical pulls scrub the morph back, and a tap
    // above the sheet closes it.
    Box(
        Modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { d -> pull(d) },
                onDragStarted = {
                    // Catch it mid-flight: stop the spring where it is, and carry on from there.
                    drive.stop()
                    val p = drive.value
                    raw = if (p >= 1f) 0f else (1f - p) * range
                },
                onDragStopped = { v -> release(v) }
            )
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    if (tap.y < status.getTop(this) + 12.dp.toPx()) close()
                }
            }
    ) {
        // The page behind goes a little darker.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.25f))
        )
        shared.transition.AnimatedVisibility(
            visible = { it },
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            with(shared.scope) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            // Full screen height (agreed 2026-11-19), like search's tall state.
                            .layout { measurable, constraints ->
                                val h = (constraints.maxHeight - status.getTop(this) - 12.dp.toPx()).roundToInt()
                                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, h))
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                            .sharedBounds(
                                rememberSharedContentState(key),
                                this@AnimatedVisibility,
                                // The glass is the pill grown: no fade, from the first frame.
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                                boundsTransform = moonBounds,
                                clipInOverlayDuringTransition = OverlayClip(cardShape)
                            )
                            // Past open (the spring's overshoot, or pulling up), the sheet stretches
                            // up a little from the bottom.
                            .graphicsLayer {
                                val over = (drive.value - 1f).coerceAtLeast(0f)
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                                scaleY = 1f + over * 0.5f
                            }
                            // The search card's glass, in the forecast's tint.
                            .glass(MoonCardState.glass ?: glass, cardShape, tint)
                            .clip(cardShape)
                    ) {
                        Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)) {
                            Spacer(Modifier.height(24.dp)) // room for the handle, drawn on top
                            MoonCard(
                                location,
                                now,
                                ink,
                                headingModifier = Modifier.sharedBounds(
                                    rememberSharedContentState("$key-label"),
                                    this@AnimatedVisibility,
                                    boundsTransform = moonBounds,
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                ),
                                restModifier = Modifier.graphicsLayer {
                                    // Choreographed (agreed): the content fades and scales in from
                                    // 92 % as the glass reaches ~70 % of its size, and leaves first
                                    // when it closes. Tied to the morph, so a pull scrubs it too.
                                    val t = (
                                        (progress.value - SolsticeMotion.CONTENT_FROM) /
                                            (SolsticeMotion.CONTENT_TO - SolsticeMotion.CONTENT_FROM)
                                        ).coerceIn(0f, 1f)
                                    val e = t * t * (3 - 2 * t)
                                    alpha = e
                                    val k =
                                        SolsticeMotion.CONTENT_START_SCALE +
                                            (1f - SolsticeMotion.CONTENT_START_SCALE) * e
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                                    scaleX = k
                                    scaleY = k
                                }
                            )
                        }
                        // The handle, like search's.
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .background(ink.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The moon card's content (redesigned 2026-11-19, design A): the phase as a big heading with the
 * date, the moon in its phase, the dial of days to scrub through phases (drag), and the details in
 * a sentence (values bold and underlined, like This week).
 */
@Composable
private fun MoonCard(
    location: Location,
    now: Long,
    ink: Color,
    headingModifier: Modifier = Modifier,
    restModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Scrubbing offset in days (fractional while dragging).
    var offset by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val phaseIcons = rememberMoonPhaseIcons()
    // The principal phases within the dial's range, and their angles.
    val phaseMarks = remember(now) {
        val start = Date(now - 15 * DAY)
        listOf(
            MoonPhase.Phase.NEW_MOON to 0,
            MoonPhase.Phase.FIRST_QUARTER to 90,
            MoonPhase.Phase.FULL_MOON to 180,
            MoonPhase.Phase.LAST_QUARTER to 270
        ).flatMap { (phase, a) ->
            buildList {
                var from = start
                repeat(2) {
                    val t = MoonPhase.compute().phase(phase).on(from).execute().time
                    add(t.time to a)
                    from = Date(t.time + DAY)
                }
            }
        }.filter { abs(it.first - now) <= 15 * DAY }
    }
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
    val position = MoonPosition.compute().on(at).at(location.latitude, location.longitude).execute()
    val distance = position.distance
    // How the moon is turned in this place's sky at the shown time: the real parallactic angle while
    // the moon is up, easing to upright as it nears and drops below the horizon (where its true
    // tilt would look wild). To keep it calm, the tilt is averaged over the hours around the shown
    // time (so each 6-hour tick doesn't rock it), and a heavy, slow spring follows that: a flick of
    // the dial mostly passes the moon by, and it turns gently into place once the dial settles.
    val limbAngle = MoonIllumination.compute().on(at).execute().angle
    val rest = if (location.latitude < 0) 180.0 else 0.0
    fun tiltAt(t: Long): Double {
        val p = MoonPosition.compute().on(Date(t)).at(location.latitude, location.longitude).execute()
        val up = ((p.altitude + 5.0) / 20.0).coerceIn(0.0, 1.0)
        var q = p.parallacticAngle
        if (q - rest > 180) q -= 360
        if (q - rest < -180) q += 360
        return (q - rest) * up * up * (3 - 2 * up)
    }
    // Snapped to the hour so the average only changes once an hour of the dial has gone by.
    val hour = 3_600_000L
    val centre = ((now + (offset * DAY).toLong()) / hour) * hour
    val targetParallactic = remember(centre, location.latitude, location.longitude) {
        rest + (-4..4).map { tiltAt(centre + it * hour) }.average()
    }
    val parallactic by animateFloatAsState(
        targetParallactic.toFloat(),
        spring(dampingRatio = 1f, stiffness = 12f),
        label = "moonTilt"
    )
    run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
        ) {
            // A big heading, like This week: the phase, and the date under it. It's the button's label
            // grown (shared bounds), so it morphs from and back into the button.
            val heading = breezyweather.domain.weather.model.MoonPhase(angle).getDescription(context) ?: ""
            Text(
                heading,
                color = ink,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = headingModifier
            )
            Column(restModifier) {
                Text(
                    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).apply { timeZone = zone }.format(at),
                    color = ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge
                )
                // The moon (on a starry night when the glass is dark).
                val dark = ink.luminance() > 0.5f
                Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                    if (dark) {
                        val rnd = Random(11)
                        repeat(70) {
                            drawCircle(
                                Color.White.copy(alpha = 0.2f + rnd.nextFloat() * 0.5f),
                                radius = 0.6f + rnd.nextFloat() * 1.4f,
                                center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                            )
                        }
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
                    moon.draw(
                        this,
                        center,
                        size.minDimension * 0.36f,
                        angle,
                        Color(0xFF050509),
                        limbAngle = limbAngle,
                        parallactic = parallactic.toDouble()
                    )
                }
                Spacer(Modifier.height(4.dp))
                // The dial: ticks every 6 hours, a longer one per day; drag to move through the days.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
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
                                        animationSpec = moonSettle
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
                    Canvas(Modifier.fillMaxWidth().padding(top = 16.dp).height(28.dp)) {
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
                        // Where a new phase begins (new, first quarter, full, last quarter): its little moon
                        // icon above the tick.
                        for ((t, a) in phaseMarks) {
                            val x = mid + ((t - now).toFloat() / DAY - offset) * perDay
                            if (x < 0f || x > size.width) continue
                            val markFade = 1f - (abs(x - mid) / mid).coerceIn(0f, 1f) * 0.8f
                            drawMoonPhaseIcon(phaseIcons, Offset(x, -9f * density), 7f * density, a, ink, markFade)
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
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))
                fun hm(d: Date?) =
                    d?.let { hhmm(zone).format(it) } ?: "–"
                // The details in a sentence (design A), values bold and underlined like This week.
                val days = ((full.time - at.time) / DAY.toDouble()).roundToInt().coerceAtLeast(0)
                val km = "%,d km".format(distance.roundToInt())
                val growing = angle < 180
                val sentences = listOf(
                    stringResource(
                        if (growing) R.string.moon_lit_growing else R.string.moon_lit_shrinking
                    ) to listOf("${(lit * 100).roundToInt()} %"),
                    if (times.rise != null && times.set != null && times.set!!.before(times.rise)) {
                        stringResource(R.string.moon_sets_then_rises) to listOf(hm(times.set), hm(times.rise))
                    } else {
                        stringResource(R.string.moon_rises_then_sets) to listOf(hm(times.rise), hm(times.set))
                    },
                    if (days == 0) {
                        stringResource(R.string.moon_full_today) to listOf(km)
                    } else {
                        stringResource(R.string.moon_full_in) to listOf(context.getString(R.string.moon_days, days), km)
                    }
                )
                val bold = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                val body = buildAnnotatedString {
                    sentences.forEachIndexed { i, (template, args) ->
                        if (i > 0) append(" ")
                        var last = 0
                        Regex("%(\\d)\\\$s").findAll(template).forEach { m ->
                            append(template.substring(last, m.range.first))
                            withStyle(bold) { append(args[m.groupValues[1].toInt() - 1]) }
                            last = m.range.last + 1
                        }
                        append(template.substring(last))
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ink.copy(alpha = 0.09f))
                        .border(0.5.dp, ink.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Text(body, color = ink, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
