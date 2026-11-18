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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
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
                    style = MaterialTheme.typography.titleLarge,
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
                time(context, rise, location.timeZone),
                stringResource(R.string.shell_sunrise),
                text,
                Alignment.Start
            )
            Spacer(Modifier.weight(1f))
            TimeLabel(time(context, set, location.timeZone), stringResource(R.string.shell_sunset), text, Alignment.End)
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
                style = MaterialTheme.typography.displaySmallEmphasized,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.shell_tab_places),
                    style = MaterialTheme.typography.displaySmallEmphasized,
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
                            shadowElevation = glide.value * 16.dp.toPx()
                            shape = RoundedCornerShape(6.dp)
                        }
                        if (isCarried) {
                            translationY = carryY
                            scaleX = lift.value
                            scaleY = lift.value
                            rotationZ = tilt
                            shadowElevation = ((lift.value - 1f) / 0.03f).coerceIn(0f, 1f) * 18.dp.toPx()
                            shape = RoundedCornerShape(24.dp)
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
                    // The main place: a bigger postcard with a "Home" caption, a little apart from the rest.
                    if (id in starred) {
                        Text(
                            stringResource(R.string.shell_home),
                            style = MaterialTheme.typography.labelLarge,
                            color = onPage.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 10.dp)
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
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta -> x = (x + delta).coerceIn(-width, 0f) },
                    onDragStarted = { settleJob?.cancel() },
                    onDragStopped = { velocity ->
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
                )
        ) { content() }
    }
}

/**
 * A place as a postcard (agreed 2026-11-18, style B "framed photo"): its sky is the photo inside a
 * cream paper frame, a perforated stamp shows the local time, and
 * the temperature sits on the picture. The main place wears a star; a visit has a dashed frame.
 * The sky is baked and static, so scrolling stays cheap. Sinks a little while pressed.
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
    val text = sky.palette.text
    val current = location.weather?.current
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val sink = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        sink.animateTo(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val paper = if (dark) Color(0xFFE6E0D2) else Color(0xFFFAF7F0)
    val ink = Color(0xFF3A3731)
    val frame = RoundedCornerShape(6.dp)
    // The main place is a bigger "hero" postcard; the height eases when the star moves.
    val height by animateDpAsState(
        if (starred) 184.dp else 132.dp,
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        label = "postcard"
    )
    // Stamp thump (agreed 2026-11-18): when it gets the star, the star lands hard on the stamp with
    // a little overshoot and an ink ring spreads, like a postmark. Waits for the glide to the top.
    var hadStar by remember { mutableStateOf(starred) }
    val thump = remember { Animatable(1f) }
    val ring = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(starred) {
        if (starred && !hadStar) {
            thump.snapTo(0f)
            ring.snapTo(0f)
            delay(520)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            launch { ring.animateTo(1f, tween(480)) }
            thump.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
        }
        hadStar = starred
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
                // A visit is lighter until you keep it.
                alpha = if (visiting) 0.9f else 1f
            }
            .shadow(if (selected) 6.dp else 2.dp, frame)
            .clip(frame)
            .background(paper)
            .then(if (visiting) Modifier.dashedEdge(ink, 6.dp) else Modifier)
            .clickable(interactionSource = press, indication = ripple(), onClick = onClick)
            .padding(7.dp)
    ) {
        SkyBackground(state = sky, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp))) {
            Box(Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)) {
                // Top left: the weather (and local time when it differs), with the current-position icon.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopStart).padding(end = 56.dp)
                ) {
                    if (location.isCurrentPosition) {
                        Icon(
                            Icons.Filled.NearMe,
                            contentDescription = stringResource(R.string.location_current),
                            tint = text.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        // "Visiting" only shows on the selected place. The stamp has the local time.
                        placeSubtitle(context, location, visiting && selected, now, withTime = false),
                        color = text.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Bottom left: the name; the city above it for a neighbourhood.
                val (title, city) = placeName(context, location)
                Column(Modifier.align(Alignment.BottomStart).padding(end = 84.dp)) {
                    city?.let {
                        Text(
                            it,
                            color = text.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = text,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            autoSize = TextAutoSize.StepBased(
                                16.sp,
                                MaterialTheme.typography.titleLargeEmphasized.fontSize
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                // Bottom right: the temperature, on the picture.
                Text(
                    current?.temperature?.temperature?.let { degrees(context, it) } ?: "–",
                    color = text,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        // The stamp: the local time, perforated edge, overlapping the picture's corner.
        // The time only when it differs from yours (never on the current position); the stamp
        // stays for the star.
        val showTime = !location.isCurrentPosition && timeDifference(location.timeZone, now) != null
        if (showTime || starred) {
            Stamp(
                time = if (showTime) time(context, Date(now), location.timeZone) else null,
                starred = starred,
                thump = { thump.value },
                ring = { ring.value },
                paper = paper,
                ink = ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}

/** A small postage stamp: paper with a dotted (perforated) edge and the local time; the main place's has a star. */
@Composable
private fun Stamp(
    time: String?,
    starred: Boolean,
    paper: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    thump: () -> Float = { 1f },
    ring: () -> Float = { 1f },
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 46.dp, height = 52.dp)
            .drawBehind {
                drawRect(paper)
                val dot = 2.dp.toPx()
                drawRect(
                    color = ink.copy(alpha = 0.35f),
                    topLeft = Offset(dot, dot),
                    size = Size(size.width - 2 * dot, size.height - 2 * dot),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 3.dp.toPx())),
                        cap = StrokeCap.Round
                    )
                )
            }
            .padding(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (starred) {
                val gold = Color(0xFFC9892B)
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.shell_starred),
                    tint = gold,
                    modifier = Modifier
                        .size(16.dp)
                        .drawBehind {
                            // The ink ring of the thump, spreading and fading.
                            val t = ring()
                            if (t < 1f) {
                                drawCircle(
                                    gold.copy(alpha = 0.55f * (1f - t)),
                                    radius = (6.dp.toPx() + 18.dp.toPx() * t),
                                    style = Stroke(width = 2.dp.toPx() * (1f - t) + 0.5f)
                                )
                            }
                        }
                        .graphicsLayer {
                            // Lands from big and tilted, overshoots, settles.
                            val k = thump()
                            val s = 1.9f - 0.9f * k
                            scaleX = s
                            scaleY = s
                            rotationZ = -18f * (1f - k)
                            alpha = (k * 3f).coerceIn(0f, 1f)
                        }
                )
            }
            if (time != null) {
                Text(
                    time,
                    color = ink,
                    style = if (starred) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * The current position's name: neighbourhood first, its city second (when the address has both),
 * e.g. "Providencia" over "Guadalajara". Other places keep their usual name.
 */
internal fun placeName(context: Context, location: Location): Pair<String, String?> {
    val district = location.district?.takeIf { it.isNotEmpty() && it != location.city }
    return if (location.isCurrentPosition && location.customName.isNullOrEmpty() && district != null) {
        district to location.city.takeIf { it.isNotEmpty() }
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

private fun time(context: Context, date: Date, zone: TimeZone, hourOnly: Boolean = false): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = when {
        hourOnly && is24 -> "HH"
        hourOnly -> "h a"
        is24 -> "HH:mm"
        else -> "h:mm a"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = zone }.format(date)
}

private fun dayName(context: Context, date: Date, zone: TimeZone): String =
    SimpleDateFormat("EEE d", context.resources.configuration.locales[0]).apply { timeZone = zone }.format(date)
