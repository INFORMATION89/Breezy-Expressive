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

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.expressive.components.edgeBlur
import org.breezyweather.ui.expressive.sky.SkyBackground
import org.breezyweather.ui.expressive.sky.SkyState
import org.breezyweather.ui.expressive.sky.SkyWeather
import org.breezyweather.ui.expressive.sky.paletteAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Forecast

@Composable
internal fun ForecastPage(
    location: Location?,
    sky: SkyState,
    now: Long,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onDetails: () -> Unit,
    onOpenPlaces: () -> Unit,
) {
    val text = sky.palette.text
    if (location == null) {
        EmptyForecast(text, onOpenPlaces)
        return
    }
    val context = LocalContext.current
    val weather = location.weather
    val current = weather?.current
    // Develop like a photo (agreed 2026-11-18): while there's no data the values are faint; when it
    // arrives they come up from soft to sharp (a slight blur clearing) instead of popping in.
    val hasData = current != null
    val develop = remember { Animatable(if (hasData) 1f else 0f) }
    LaunchedEffect(hasData) {
        if (hasData) develop.animateTo(1f, tween(700)) else develop.snapTo(0f)
    }
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val d = develop.value
                    alpha = 0.35f + 0.65f * d
                    val blur = 10.dp.toPx() * (1f - d)
                    renderEffect = if (blur > 0.5f && android.os.Build.VERSION.SDK_INT >= 31) {
                        BlurEffect(blur, blur, TileMode.Decal)
                    } else {
                        null
                    }
                }
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 120.dp)
        ) {
            // Only the location's title at the top (plus an icon for the current position).
            val (title, city) = placeName(context, location)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (location.isCurrentPosition) {
                    Icon(
                        Icons.Filled.NearMe,
                        contentDescription = stringResource(R.string.location_current),
                        tint = text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                // Long names shrink to fit, then ellipsize.
                Text(
                    title,
                    color = text,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    autoSize = TextAutoSize.StepBased(16.sp, MaterialTheme.typography.titleLarge.fontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            city?.let {
                Text(
                    it,
                    color = text.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                current?.temperature?.temperature?.let { degrees(context, it) } ?: "–",
                color = text,
                fontSize = 112.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-4).sp
            )
            current?.weatherText?.let {
                Text(it, color = text, style = MaterialTheme.typography.headlineSmallEmphasized)
            }
            feelsLikeLine(context, location)?.let {
                Text(it, color = text.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(28.dp))
            SunPath(location, now, text)
            Spacer(Modifier.height(20.dp))
            TodayCard(location, now, text)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDetails) {
                Text(stringResource(R.string.shell_more_details), color = text)
            }
        }
    }
}

@Composable
private fun EmptyForecast(text: Color, onOpenPlaces: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(
            stringResource(R.string.shell_no_location),
            color = text,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onOpenPlaces) { Text(stringResource(R.string.shell_tab_places)) }
    }
}

/** Sunrise left, sunset right, the sun's arc between with a dot where it is now. */
@Composable
private fun SunPath(location: Location, now: Long, text: Color) {
    val context = LocalContext.current
    val sun = location.weather?.today?.sun ?: return
    val rise = sun.riseDate ?: return
    val set = sun.setDate ?: return
    val progress = ((now - rise.time).toFloat() / (set.time - rise.time)).coerceIn(0f, 1f)
    val daytime = now in rise.time..set.time
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val w = size.width
            val h = size.height
            val base = h - 6.dp.toPx()
            fun point(p: Float) = Offset(p * w, base - sin(PI * p).toFloat() * (base - 6.dp.toPx()))
            val path = Path().apply {
                moveTo(0f, base)
                for (i in 1..48) point(i / 48f).let { lineTo(it.x, it.y) }
            }
            drawLine(text.copy(alpha = 0.3f), Offset(0f, base), Offset(w, base), strokeWidth = 1.dp.toPx())
            drawPath(path, text.copy(alpha = 0.55f), style = Stroke(1.5.dp.toPx()))
            if (daytime) {
                val done = Path().apply {
                    moveTo(0f, base)
                    val steps = (48 * progress).toInt().coerceAtLeast(1)
                    for (i in 1..steps) point(progress * i / steps).let { lineTo(it.x, it.y) }
                }
                drawPath(done, Color(0xFFE58A3C), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(Color(0xFFF6B04A), radius = 7.dp.toPx(), center = point(progress))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TimeLabel(
                time(context, rise, location.timeZone, noPeriod = true),
                stringResource(R.string.shell_sunrise),
                text,
                Alignment.Start
            )
            Spacer(Modifier.weight(1f))
            TimeLabel(
                time(context, set, location.timeZone, noPeriod = true),
                stringResource(R.string.shell_sunset),
                text,
                Alignment.End
            )
        }
    }
}

@Composable
private fun TimeLabel(time: String, label: String, text: Color, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(time, color = text, style = MaterialTheme.typography.titleLargeEmphasized)
        Text(label, color = text.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
    }
}

/** The next hours, "Now" first, on a translucent card. */
@Composable
private fun TodayCard(location: Location, now: Long, text: Color) {
    val context = LocalContext.current
    val hours = location.weather?.nextHourlyForecast?.take(6).orEmpty()
    if (hours.isEmpty()) return
    GlassCard(text) {
        Text(
            stringResource(R.string.shell_today),
            color = text,
            style = MaterialTheme.typography.titleMediumEmphasized
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hours.forEachIndexed { i, hour ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        hour.temperature?.temperature?.let { degrees(context, it) } ?: "–",
                        color = text,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (i == 0 && hour.date.time <= now) {
                            stringResource(R.string.shell_now)
                        } else {
                            time(context, hour.date, location.timeZone, hourOnly = true)
                        },
                        color = text.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Outlook (next days)

/** Plain page (white/black); each day's card wears its own weather, as a daytime sky. */
@Composable
internal fun DaysPage(
    location: Location?,
    onPage: Color,
    page: Color,
    listState: LazyListState,
    pageFade: () -> Float,
) {
    val context = LocalContext.current
    val days = location?.weather?.dailyForecastStartingToday.orEmpty().take(10)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Edge to edge: the list scrolls under the status bar and the toolbar, with a progressive blur
    // at both edges (also a hint that there's more to scroll).
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 40.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().edgeBlur(page, top = top + 56.dp, strength = pageFade)
    ) {
        item {
            Text(
                stringResource(R.string.shell_tab_days),
                color = onPage,
                style = MaterialTheme.typography.displaySmallEmphasized.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
        }
        items(days) { day ->
            val sky = daySky(SkyWeather.of(day.day?.weatherCode))
            val text = sky.palette.text
            SkyBackground(
                state = sky,
                modifier = Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                ) {
                    Text(
                        dayName(context, day.date, location!!.timeZone),
                        color = text,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.width(88.dp)
                    )
                    Text(
                        day.day?.weatherText ?: "",
                        color = text.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        listOfNotNull(
                            day.night?.temperature?.temperature?.let { degrees(context, it) },
                            day.day?.temperature?.temperature?.let { degrees(context, it) }
                        ).joinToString("  "),
                        color = text,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/** A day's weather as a mid-morning sky (sun high on the right), for cards. */
private fun daySky(weather: SkyWeather) =
    SkyState(paletteAt(weather, 0.8f), sunX = 0.82f, sunY = 0.3f, sunHeight = 0.8f)

// ---------------------------------------------------------------------------------------------
// Places

@Composable
internal fun PlacesPage(
    locations: List<Location>,
    currentId: String?,
    starred: Set<String>,
    visiting: Set<String>,
    now: Long,
    onPage: Color,
    onSelect: (Location, Rect?) -> Unit,
    onStar: (Location) -> Unit,
    onDelete: (Location) -> Unit,
    onAddCurrent: () -> Unit,
    onEdit: (Location) -> Unit,
    onMove: (id: String, beforeId: String?) -> Unit,
    onSettings: () -> Unit,
    listState: LazyListState,
    pageFade: () -> Float = { 1f },
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val byId = locations.associateBy { it.formattedId }
    // The starred (main) place always first, then the current position; both stay where they are.
    val sortedIds = locations
        .sortedWith(
            compareByDescending<Location> { it.formattedId in starred }.thenByDescending { it.isCurrentPosition }
        )
        .map { it.formattedId }
    fun isPinned(id: String) = id in starred || byId[id]?.isCurrentPosition == true

    // Where each postcard is on screen, so a tapped one can grow into its window from there.
    val cardBounds = remember { HashMap<String, Rect>() }

    // Reorder state: while a card is carried we show our own copy of the order and save on drop.
    val order = remember { mutableStateListOf<String>() }
    var carried by remember { mutableStateOf<String?>(null) }
    var carryY by remember { mutableFloatStateOf(0f) }
    var tilt by remember { mutableFloatStateOf(0f) }
    val lift = remember { Animatable(1f) }
    var landed by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val shown = if (carried == null) sortedIds else order.toList()
    val nudgeMax = with(density) { 7.dp.toPx() }
    val bumpMax = with(density) { 5.dp.toPx() }

    fun swapIfNeeded() {
        val info = listState.layoutInfo.visibleItemsInfo
        val cur = info.firstOrNull { it.key == carried } ?: return
        val centre = cur.offset + carryY + cur.size / 2f
        val target = info.firstOrNull {
            it.key != carried && it.key in order && (it.key as? String)?.let { k -> !isPinned(k) } == true &&
                centre > it.offset && centre < it.offset + it.size
        } ?: return
        val from = order.indexOf(carried)
        val to = order.indexOf(target.key)
        order.add(to, order.removeAt(from))
        carryY += cur.offset - target.offset
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    // Neighbours lean out of the carried card's way as it approaches: the push rises and falls back
    // to zero by the time the cards swap, so there is never a jump. Read in the draw phase only.
    fun nudge(id: String): Float {
        val c = carried ?: return 0f
        if (c == id) return 0f
        val info = listState.layoutInfo.visibleItemsInfo
        val cur = info.firstOrNull { it.key == c } ?: return 0f
        val me = info.firstOrNull { it.key == id } ?: return 0f
        val dist = (me.offset + me.size / 2f) - (cur.offset + carryY + cur.size / 2f)
        val near = me.size / 2f
        val reach = me.size * 1.3f
        val d = abs(dist)
        if (d <= near || d >= reach) return 0f
        return sign(dist) * nudgeMax * sin(PI * (reach - d) / (reach - near)).toFloat()
    }

    LazyColumn(
        state = listState,
        // Top padding chosen so the heading sits at the same height as This week's.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().grainyPaper(onPage)
            // The same progressive edge blur as This week.
            .edgeBlur(
                if (onPage.luminance() < 0.5f) Color(0xFFF7F3EA) else Color(0xFF1D1C19),
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
                strength = pageFade
            )
            .statusBarsPadding()
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.shell_tab_places),
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(fontWeight = FontWeight.SemiBold),
                    color = onPage,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.action_settings),
                        tint = onPage.copy(alpha = 0.7f)
                    )
                }
            }
        }
        items(shown, key = { it }) { id ->
            val location = byId[id] ?: return@items
            val isCarried = carried == id
            val pinned = isPinned(id)
            // Neighbours' landing bump when a carried card is dropped near them.
            val bump = remember { Animatable(0f) }
            LaunchedEffect(landed) {
                val l = landed ?: return@LaunchedEffect
                if (l.first == id) return@LaunchedEffect
                val diff = shown.indexOf(id) - shown.indexOf(l.first)
                if (abs(diff) in 1..2) {
                    bump.animateTo(sign(diff.toFloat()) * bumpMax / abs(diff), spring(stiffness = Spring.StiffnessHigh))
                    bump.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
                }
            }
            // Starred just now: it lifts, glides to the top over the others, and sets down.
            val starredNow = id in starred
            var wasStarred by remember { mutableStateOf(starredNow) }
            val glide = remember { Animatable(0f) }
            LaunchedEffect(starredNow) {
                if (starredNow && !wasStarred) {
                    glide.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
                    delay(380)
                    glide.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
                }
                wasStarred = starredNow
            }
            val lifted = glide.value > 0.01f
            Box(
                modifier = Modifier
                    .zIndex(if (isCarried || lifted) 1f else 0f)
                    .then(
                        if (isCarried) {
                            Modifier
                        } else {
                            Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset(1, 1)
                                )
                            )
                        }
                    )
                    .graphicsLayer {
                        if (!isCarried && glide.value > 0f) {
                            val k = 1f + 0.035f * glide.value
                            scaleX = k
                            scaleY = k
                            shadowElevation = glide.value * 10.dp.toPx()
                            ambientShadowColor = Color.Black.copy(alpha = 0.10f)
                            spotShadowColor = Color.Black.copy(alpha = 0.16f)
                            shape = RoundedCornerShape(4.dp)
                        }
                        if (isCarried) {
                            translationY = carryY
                            scaleX = lift.value
                            scaleY = lift.value
                            rotationZ = tilt
                            shadowElevation = ((lift.value - 1f) / 0.03f).coerceIn(0f, 1f) * 10.dp.toPx()
                            ambientShadowColor = Color.Black.copy(alpha = 0.10f)
                            spotShadowColor = Color.Black.copy(alpha = 0.16f)
                            shape = RoundedCornerShape(4.dp)
                        } else {
                            translationY = nudge(id) + bump.value
                        }
                    }
            ) {
                Column {
                    SwipeablePlace(
                        starred = id in starred,
                        enabled = carried == null,
                        onStar = { onStar(location) },
                        onDelete = { onDelete(location) }
                    ) {
                        PlaceCard(
                            location = location,
                            selected = id == currentId,
                            starred = id in starred,
                            visiting = id in visiting,
                            now = now,
                            context = context,
                            // The starred place is the main screen itself: it doesn't open as a postcard.
                            onClick = { if (id !in starred) onSelect(location, cardBounds[id]) },
                            // Long-press lifts the card; drag to move it, or release without moving to edit.
                            modifier = Modifier.onGloballyPositioned {
                                cardBounds[id] = it.boundsInRoot()
                            }.pointerInput(id) {
                                var moved = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        moved = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        order.clear()
                                        order.addAll(sortedIds)
                                        carryY = 0f
                                        tilt = 0f
                                        carried = id
                                        scope.launch {
                                            lift.animateTo(1.03f, spring(stiffness = Spring.StiffnessMedium))
                                        }
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        moved += abs(amount.y) + abs(amount.x)
                                        if (!pinned) {
                                            carryY += amount.y
                                            val target = (amount.y * 0.35f).coerceIn(-3f, 3f)
                                            tilt += (target - tilt) * 0.3f
                                            swapIfNeeded()
                                        }
                                    },
                                    onDragEnd = {
                                        val wasMoved = moved > viewConfiguration.touchSlop && !pinned
                                        val index = order.indexOf(id)
                                        val before = order.getOrNull(index + 1)
                                        scope.launch {
                                            // Soft, non-bouncy settle into the slot.
                                            val soft =
                                                spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
                                            launch { lift.animateTo(1f, soft) }
                                            launch { animate(tilt, 0f, animationSpec = soft) { v, _ -> tilt = v } }
                                            if (wasMoved) landed = id to System.nanoTime()
                                            animate(carryY, 0f, animationSpec = soft) { v, _ -> carryY = v }
                                            carried = null
                                            // (Release without moving used to open the place's settings;
                                            // removed, it clashed with reordering. To revisit.)
                                            if (wasMoved) onMove(id, before)
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            lift.animateTo(1f)
                                            carryY = 0f
                                            tilt = 0f
                                            carried = null
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        if (locations.none { it.isCurrentPosition }) {
            item(key = "current") {
                OutlinedButton(onClick = onAddCurrent, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.shell_use_current_location))
                }
            }
        }
    }
}

/**
 * Swipe a place from right to left: past the first stage (about a quarter of the width) it stars
 * the place, past the second (60 %) it deletes it. The card follows the finger, a haptic tick marks
 * each stage, and on release it springs back (or slides out to delete). A new touch interrupts the
 * spring. TalkBack gets "star" and "delete" actions instead. Off while a card is being carried.
 */
@Composable
private fun SwipeablePlace(
    starred: Boolean,
    enabled: Boolean,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    // The drag moves the card directly (no queued coroutines); only the release animates, and a
    // new touch cancels that animation, so the card can never get stuck mid-way.
    var x by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val starLabel = stringResource(if (starred) R.string.shell_unstar else R.string.shell_star)
    val deleteLabel = stringResource(R.string.shell_delete)
    val settle = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(starLabel) {
                        onStar()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    }
                )
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val starAt = width * 0.22f
        val deleteAt = width * 0.6f
        fun stageOf(pulled: Float) = when {
            pulled >= deleteAt -> 2
            pulled >= starAt -> 1
            else -> 0
        }
        val pulled = -x
        val stage = stageOf(pulled)
        LaunchedEffect(stage) {
            if (stage > 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        // What the swipe reveals behind the card.
        val behind = if (stage ==
            2
        ) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
        val onBehind = if (stage == 2) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }
        val icon = when {
            stage == 2 -> Icons.Outlined.Delete
            starred -> Icons.Filled.Star
            else -> Icons.Outlined.StarOutline
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = (pulled / starAt).coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(24.dp))
                .background(behind)
                .padding(end = 28.dp)
        ) {
            Icon(icon, contentDescription = null, tint = onBehind, modifier = Modifier.size(28.dp))
        }
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                // Only a right-to-left swipe moves the card; a left-to-right one is left to the pager
                // (it just goes to the page on the left).
                .pointerInput(enabled, width) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var leftward = false
                        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            if (over < 0f || x < 0f) {
                                leftward = true
                                change.consume()
                            }
                        }
                        if (slop == null || !leftward) return@awaitEachGesture
                        settleJob?.cancel()
                        val tracker = VelocityTracker()
                        tracker.addPosition(slop.uptimeMillis, slop.position)
                        horizontalDrag(slop.id) { change ->
                            val delta = change.positionChange().x
                            x = (x + delta).coerceIn(-width, 0f)
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        }
                        val velocity = tracker.calculateVelocity().x
                        // Decide from where the card is right now, not from the last recomposition.
                        val released = stageOf(-x)
                        val out = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                        settleJob = scope.launch {
                            when (released) {
                                2 -> {
                                    animate(x, -width, velocity, out) { v, _ -> x = v }
                                    onDelete()
                                    x = 0f
                                }
                                1 -> {
                                    onStar()
                                    animate(x, 0f, velocity, settle) { v, _ -> x = v }
                                }
                                else -> animate(x, 0f, velocity, settle) { v, _ -> x = v }
                            }
                        }
                    }
                }
        ) { content() }
    }
}

/**
 * A place as the front of a vintage postcard (user's references, 2026-11-18): an aged cream card
 * with a deckled edge and fine paper grain, captions in small capitals above and below the picture
 * (place and weather on top, region and local time below), the place's sky as the picture with a
 * faint linen texture and the temperature on it, a postmark over the corner, and a stamp. The
 * main place wears a star sticker on its stamp; a visit has a dashed inner frame. Postcard
 * proportions (about 3:2). The sky is baked and static, so scrolling stays cheap. Sinks a little
 * while pressed.
 */
@Composable
private fun PlaceCard(
    location: Location,
    selected: Boolean,
    starred: Boolean,
    visiting: Boolean,
    now: Long,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sky = skyFor(location, now)
    val current = location.weather?.current
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val sink = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        sink.animateTo(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    val card = Color(0xFFF8F2E4)
    val ink = Color(0xFF2E2A24)
    // Sticker peel (agreed 2026-11-18): when it gets the star, a gold star sticker drops onto the
    // stamp slightly too big and tilted, presses down (squash, haptic), settles, and a shine sweeps
    // across it as it flattens. Waits for the card's glide to the top.
    var hadStar by remember { mutableStateOf(starred) }
    val peel = remember { Animatable(1f) }
    val shine = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(starred) {
        if (starred && !hadStar) {
            peel.snapTo(0f)
            shine.snapTo(0f)
            delay(520)
            launch {
                delay(330)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            peel.animateTo(1f, tween(560, easing = LinearEasing))
        }
        hadStar = starred
    }
    val grain = remember { paperGrain(light = true) }
    val grainBrush = remember(grain) { ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)) }
    val linen = remember { ShaderBrush(ImageShader(linenTexture(), TileMode.Repeated, TileMode.Repeated)) }
    val caption = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
    )
    val (title, city) = placeName(context, location)
    val showTime = !location.isCurrentPosition && timeDifference(location.timeZone, now) != null
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
                // A visit is lighter until you keep it.
                alpha = if (visiting) 0.92f else 1f
            }
            // Taps are taken on the whole card (outside the deckled clip, which can swallow them).
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            // A soft, low shadow, like card lying on paper.
            .shadow(
                elevation = if (selected) 8.dp else 6.dp,
                shape = Deckled,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(Deckled)
            .drawBehind {
                drawRect(card)
                drawRect(grainBrush)
                // Aged edges: a faint warm darkening towards the border.
                drawRect(
                    Brush.radialGradient(
                        0.6f to Color.Transparent,
                        1f to Color(0xFF8A6A3A).copy(alpha = 0.16f),
                        center = center,
                        radius = size.maxDimension * 0.62f
                    )
                )
            }
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top caption, on the picture: the place, and its weather.
            val topCaption: @Composable (Modifier) -> Unit = { captionModifier ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = captionModifier) {
                    if (location.isCurrentPosition) {
                        Icon(
                            Icons.Filled.NearMe,
                            contentDescription = stringResource(R.string.location_current),
                            tint = sky.palette.text,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        title.uppercase(),
                        style = caption,
                        color = sky.palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        listOfNotNull(
                            if (visiting && selected) context.getString(R.string.shell_visiting) else null,
                            current?.weatherText
                        ).joinToString(" · ").uppercase(),
                        style = caption,
                        color = sky.palette.text,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            // The picture: the place's sky, linen-textured, with the temperature.
            SkyBackground(
                state = sky,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawRect(linen, blendMode = BlendMode.Multiply)
                        if (visiting) {
                            drawRect(
                                ink.copy(alpha = 0.5f),
                                style = Stroke(
                                    width = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
                                )
                            )
                        }
                    }
            ) {
                Box(Modifier.fillMaxSize()) {
                    topCaption(Modifier.align(Alignment.TopStart).padding(start = 10.dp, end = 10.dp, top = 8.dp))
                    Text(
                        current?.temperature?.temperature?.let { degrees(context, it) } ?: "–",
                        color = sky.palette.text,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp)
                    )
                }
            }
            // Bottom caption: the region, and the local time when it differs from yours.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                Text(
                    location.country.uppercase(),
                    style = caption,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showTime) {
                    Text(
                        listOfNotNull(
                            time(context, Date(now), location.timeZone),
                            timeDifference(location.timeZone, now)
                        ).joinToString(" · ").uppercase(),
                        style = caption,
                        color = ink,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        // The stamp sits on the picture's top right corner, the postmark half over it.
        // The star stamp marks the main place only. Placed and pressed (agreed 2026-11-18): it
        // lowers onto the card from a little above (bigger, soft shadow), touches down tilted, gets
        // pressed flat (shadow gone, slight squash, haptic) and rests at a small natural angle.
        if (starred) {
            Stamp(
                time = null,
                starred = true,
                paper = Color(0xFFF6EBD6),
                ink = ink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 16.dp)
                    .graphicsLayer {
                        val t = peel.value
                        val lower = (t / 0.6f).coerceIn(0f, 1f)
                        val press = ((t - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        // Lowering: 1.35 to 1, then a small squash that releases.
                        val squash = if (press <
                            0.5f
                        ) {
                            1f - 0.05f * (press / 0.5f)
                        } else {
                            0.95f + 0.05f * ((press - 0.5f) / 0.5f)
                        }
                        val k = (1.35f - 0.35f * (1f - (1f - lower) * (1f - lower))) * squash
                        scaleX = k
                        scaleY = k
                        rotationZ = -9f + 6f * lower
                        shadowElevation = 10.dp.toPx() * (1f - lower)
                        ambientShadowColor = Color.Black.copy(alpha = 0.12f)
                        spotShadowColor = Color.Black.copy(alpha = 0.18f)
                        alpha = (t * 6f).coerceIn(0f, 1f)
                    }
            )
        }
        // The postmark is inked over the picture, so it takes the picture's text colour.
        Postmark(
            country = location.countryCode,
            ink = sky.palette.text,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 26.dp, end = 12.dp)
        )
    }
}

/** A deckled (finely scalloped) card edge, like an old postcard's. */
private val Deckled = GenericShape { size, _ ->
    val step = 5f
    val bite = 1.6f
    var x = 0f
    moveTo(0f, bite)
    while (x < size.width) {
        lineTo(x + step / 2, 0f)
        lineTo(x + step, bite)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        lineTo(size.width, y + step / 2)
        lineTo(size.width - bite, y + step)
        y += step
    }
    x = size.width
    while (x > 0f) {
        lineTo(x - step / 2, size.height)
        lineTo(x - step, size.height - bite)
        x -= step
    }
    y = size.height
    while (y > 0f) {
        lineTo(0f, y - step / 2)
        lineTo(bite, y - step)
        y -= step
    }
    close()
}

/** A small tile of fine horizontal and vertical threads, like linen-finish card stock. */
private fun linenTexture(): ImageBitmap {
    val size = 64
    val pixels = IntArray(size * size)
    val random = java.util.Random(3)
    for (yy in 0 until size) {
        val row = random.nextFloat()
        for (xx in 0 until size) {
            val thread = if (yy % 3 == 0) 0.10f else 0f
            val warp = if (xx % 4 == 0) 0.05f else 0f
            val dark = (thread + warp + row * 0.03f).coerceIn(0f, 0.2f)
            val v = (255 * (1f - dark)).toInt()
            pixels[yy * size + xx] = (255 shl 24) or (v shl 16) or (v shl 8) or v
        }
    }
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/** A faint round postmark with the country code, partly over the stamp. */
@Composable
private fun Postmark(country: String?, ink: Color, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .graphicsLayer { rotationZ = -12f }
            .drawBehind {
                drawCircle(ink.copy(alpha = 0.32f), style = Stroke(1.2.dp.toPx()))
                drawCircle(ink.copy(alpha = 0.22f), radius = size.minDimension * 0.36f, style = Stroke(0.8.dp.toPx()))
            }
    ) {
        Text(
            country.orEmpty().uppercase(),
            color = ink.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

/** A small postage stamp with a perforated edge: the local time (when it differs), or a star sticker. */
@Composable
private fun Stamp(
    time: String?,
    starred: Boolean,
    paper: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    peel: () -> Float = { 1f },
    shine: () -> Float = { 1f },
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .drawBehind {
                drawRect(paper)
                val dot = 2.dp.toPx()
                drawRect(
                    color = ink.copy(alpha = 0.3f),
                    topLeft = Offset(dot, dot),
                    size = Size(size.width - 2 * dot, size.height - 2 * dot),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 3.dp.toPx())),
                        cap = StrokeCap.Round
                    )
                )
            }
            .padding(3.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (starred) {
                val gold = Color(0xFFD69A2E)
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.shell_starred),
                    tint = gold,
                    modifier = Modifier
                        .size(if (time != null) 14.dp else 22.dp)
                        .graphicsLayer {
                            val t = 1f
                            val sc = when {
                                t < 0.45f -> 1.55f - 0.69f * (t / 0.45f)
                                t < 0.7f -> 0.86f + 0.18f * ((t - 0.45f) / 0.25f)
                                else -> 1.04f - 0.04f * ((t - 0.7f) / 0.3f)
                            }
                            scaleX = sc
                            scaleY = sc * (if (t in 0.4f..0.55f) 0.94f else 1f)
                            rotationZ = -14f * (1f - (t / 0.45f).coerceIn(0f, 1f))
                            alpha = (t * 5f).coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            // The shine sweeping across as the sticker flattens.
                            val k = shine()
                            if (k in 0.001f..0.999f) {
                                val x = -size.width + 3f * size.width * k
                                drawRect(
                                    Brush.linearGradient(
                                        0f to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.85f),
                                        1f to Color.Transparent,
                                        start = Offset(x, 0f),
                                        end = Offset(x + size.width * 0.7f, size.height)
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                )
            }
            if (time != null) {
                Text(
                    time,
                    color = ink,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/** Grainy paper for the Places page: a warm off-white (or deep warm grey) with fine fibre grain. */
@Composable
private fun Modifier.grainyPaper(onPage: Color): Modifier {
    val light = onPage.luminance() < 0.5f
    val grain = remember(light) { paperGrain(light) }
    val brush = remember(grain) { ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)) }
    return drawBehind {
        drawRect(if (light) Color(0xFFF7F3EA) else Color(0xFF1D1C19))
        drawRect(brush)
    }
}

/** A small tile of soft noise (seeded, so it's the same every time), drawn repeated. */
private fun paperGrain(light: Boolean): ImageBitmap {
    val size = 128
    val pixels = IntArray(size * size)
    val random = java.util.Random(7)
    for (i in pixels.indices) {
        val n = random.nextGaussian().toFloat()
        val alpha = (kotlin.math.abs(n) * if (light) 16f else 12f).toInt().coerceIn(0, 48)
        val shade = if (n > 0f) 255 else 0
        pixels[i] = (alpha shl 24) or (shade shl 16) or (shade shl 8) or shade
    }
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/**
 * The current position's name: neighbourhood first, its city second (when the address has both),
 * e.g. "Providencia" over "Guadalajara". Other places keep their usual name.
 */
internal fun placeName(context: Context, location: Location): Pair<String, String?> {
    // For now the borough (e.g. "Miguel Hidalgo") rather than the neighbourhood: shorter.
    val borough = location.admin2?.takeIf { it.isNotEmpty() && it != location.city && it != location.admin1 }
    val district = location.district?.takeIf { it.isNotEmpty() && it != location.city }
    val local = borough ?: district
    return if (location.isCurrentPosition && location.customName.isNullOrEmpty() && local != null) {
        local to location.city.takeIf { it.isNotEmpty() }
    } else {
        location.getPlace(context) to null
    }
}

/**
 * Under the place name: its weather, plus its local time and time difference when it's in another
 * time zone (always for a visit), e.g. "Visiting · Overcast · 14:32, +1 h".
 */
private fun placeSubtitle(
    context: Context,
    location: Location,
    visiting: Boolean,
    now: Long,
    withTime: Boolean = true,
): String {
    val diff = timeDifference(location.timeZone, now)
    val local = if (diff != null || visiting) {
        listOfNotNull(if (withTime) time(context, Date(now), location.timeZone) else null, diff)
            .joinToString(", ")
            .ifEmpty { null }
    } else {
        null
    }
    return listOfNotNull(
        if (visiting) context.getString(R.string.shell_visiting) else null,
        location.weather?.current?.weatherText,
        local
    ).joinToString(" · ")
}

/** A dashed outline marking a place you're only visiting. */
private fun Modifier.dashedEdge(color: Color, corner: androidx.compose.ui.unit.Dp) = drawWithContent {
    drawContent()
    val r = corner.toPx()
    drawRoundRect(
        color = color.copy(alpha = 0.55f),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    )
}

// ---------------------------------------------------------------------------------------------
// Shared bits

/** A translucent card that lets the sky show through (blur comes later). */
@Composable
private fun GlassCard(text: Color, content: @Composable () -> Unit) {
    val light = text.red + text.green + text.blue < 1.5f // dark text → light sky → white glass
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (light) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.10f))
            .padding(16.dp)
    ) { Column { content() } }
}

private fun degrees(context: Context, t: org.breezyweather.unit.temperature.Temperature): String {
    val unit = SettingsManager.getInstance(context).getTemperatureUnit(context)
    return "${t.toDouble(unit).roundToInt()}°"
}

private fun feelsLikeLine(context: Context, location: Location): String? {
    val current = location.weather?.current ?: return null
    val feels = current.temperature?.feelsLikeTemperature ?: return null
    val actual = current.temperature?.temperature ?: return null
    if (degrees(context, feels) == degrees(context, actual)) return null
    return context.getString(R.string.shell_feels_like, degrees(context, feels))
}

internal fun time(
    context: Context,
    date: Date,
    zone: TimeZone,
    hourOnly: Boolean = false,
    noPeriod: Boolean = false,
): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = when {
        hourOnly && is24 -> "HH"
        hourOnly -> "h a"
        is24 -> "HH:mm"
        noPeriod -> "h:mm"
        else -> "h:mm a"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = zone }.format(date)
}

private fun dayName(context: Context, date: Date, zone: TimeZone): String =
    SimpleDateFormat("EEE d", context.resources.configuration.locales[0]).apply { timeZone = zone }.format(date)
