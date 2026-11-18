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

package org.breezyweather.ui.expressive.search

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.breezyweather.R
import org.breezyweather.ui.expressive.shell.timeDifference
import org.breezyweather.ui.search.LoadableLocationStatus
import org.breezyweather.ui.search.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Search as one big card (agreed 2026-11-18). It grows out of the round search bubble into a card
 * docked at the bottom, above the keyboard: a handle on top, results (segmented rows) in the
 * middle, the field at the bottom. The card has a fixed height; drag the handle up to show more,
 * or down to dismiss (it follows the finger; releasing early springs it back). Results settle in
 * place when they change. [progress] (0 = bubble, 1 = open) is one spring for opening and closing;
 * it's read in layout and draw only, so opening never recomposes the screen.
 */
@Composable
fun SearchOverlay(
    open: Boolean,
    progress: () -> Float,
    origin: Rect?,
    searchViewModel: SearchViewModel,
    near: Pair<Double, Double>?,
    countryCode: String?,
    onPickLocation: (Location) -> Unit,
    onPickCity: (City) -> Unit,
    onClose: () -> Unit,
) {
    // Only this check reads the progress in composition, and it flips just twice per open/close.
    val visible by remember { derivedStateOf { progress() > 0f } }
    if (!visible && !open) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val results by searchViewModel.listResource.collectAsState()
    // Handle drag: extra height above the compact size, and how far the card is pulled down.
    var extra by remember { mutableFloatStateOf(0f) }
    var pulled by remember { mutableFloatStateOf(0f) }
    var settle by remember { mutableStateOf<Job?>(null) }
    // The query last sent, so "no results" only shows once that search has answered.
    var requested by remember { mutableStateOf("") }

    BackHandler(enabled = open, onBack = onClose)
    LaunchedEffect(open) {
        if (open) {
            extra = 0f
            pulled = 0f
            delay(120)
            runCatching { focus.requestFocus() }
        } else {
            focusManager.clearFocus()
            query = ""
        }
    }
    LaunchedEffect(query) {
        if (query.trim().length < 2) return@LaunchedEffect
        delay(300)
        searchViewModel.requestLocationList(query)
        requested = query
    }
    val suggestions by produceState(emptyList<City>(), near, countryCode) {
        value = withContext(Dispatchers.IO) { Cities.topPlaces(context, countryCode, near) }
    }
    val recents = remember(open) { RecentSearches.all(context) }

    val view = LocalView.current
    val ime = WindowInsets.ime
    val navigation = WindowInsets.navigationBars
    val status = WindowInsets.statusBars
    // Written by the card's layout each frame; read by the handle's drag (how far it can grow).
    val room = remember { FloatArray(1) }

    // How far the inside is shown, from the open progress (read only in draw/layout lambdas).
    fun inside() = ((progress().coerceIn(0f, 1f) - 0.45f) / 0.55f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        // The page behind goes darker (the shell also blurs it). Pulling down lightens it, so
        // dismissing feels continuous.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pullFade = 1f - (pulled / 220.dp.toPx()).coerceIn(0f, 1f)
                    alpha = progress().coerceIn(0f, 1f) * pullFade
                }
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = open,
                    onClick = onClose
                )
        )

        // The card's size and place follow the spring, keyboard and handle in the layout phase only:
        // the open animation re-lays out the card each frame but recomposes nothing.
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val width = constraints.maxWidth.toFloat()
                    val height = constraints.maxHeight.toFloat()
                    val shrunk = (view.rootView.height - constraints.maxHeight).coerceAtLeast(0)
                    val keyboard = ime.getBottom(this)
                    val bottomInset = if (keyboard > 0) {
                        (keyboard - shrunk).coerceAtLeast(0)
                    } else {
                        navigation.getBottom(this)
                    }
                    val top = status.getTop(this) + 12.dp.toPx()
                    val margin = 12.dp.toPx()
                    val chrome = (24.dp + 64.dp).toPx() // handle + field
                    val compact = 280.dp.toPx()
                    val tall = (height - bottomInset - margin - top - chrome).coerceAtLeast(compact)
                    room[0] = tall - compact
                    val content = (compact + extra).coerceIn(compact, tall)
                    val bottom = height - bottomInset - margin + pulled
                    val target = Rect(margin, bottom - content - chrome, width - margin, bottom)
                    val start = origin ?: target
                    val p = progress().coerceIn(0f, 1f)
                    val left = start.left + (target.left - start.left) * p
                    val topEdge = start.top + (target.top - start.top) * p
                    val w = (start.width + (target.width - start.width) * p).roundToInt().coerceAtLeast(0)
                    val h = (start.height + (target.height - start.height) * p).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(left.roundToInt(), topEdge.roundToInt())
                    }
                }
                // Translucent material, like the toolbar: the blurred, darkened page shows through.
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
        ) {
            // While still small it's the bubble's search icon.
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = 1f - inside() }
            )
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = inside() }) {
                Spacer(Modifier.height(24.dp)) // room for the handle, drawn on top
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // Built once the card is a third of the way open (it's invisible before that), so the
                    // first frame of the animation only has the card and field to build.
                    val ready by remember { derivedStateOf { progress() > 0.3f } }
                    if (ready || !open) {
                        Results(
                            context = context,
                            query = query,
                            results = results.first,
                            loading = results.second == LoadableLocationStatus.LOADING || requested != query,
                            // A place already under Recent isn't repeated under Popular places.
                            suggestions = suggestions.filterNot { c ->
                                recents.any { it.name == c.name && it.countryCode == c.countryCode }
                            },
                            recents = recents,
                            onPickLocation = onPickLocation,
                            onPickCity = onPickCity
                        )
                    }
                }
                SearchField(
                    query = query,
                    onQuery = { query = it },
                    focus = focus,
                    onSearch = { results.first.firstOrNull()?.let(onPickLocation) }
                )
            }
            // The handle's touch area is taller than it looks (it overlaps the top of the results).
            Handle(
                modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = inside() },
                onDelta = { delta ->
                    settle?.cancel()
                    if (delta > 0f) {
                        // Down: shrink back to compact first, then pull the card down.
                        val shrink = minOf(delta, extra)
                        extra -= shrink
                        pulled += delta - shrink
                    } else {
                        // Up: undo any pull first, then grow.
                        val lift = minOf(-delta, pulled)
                        pulled -= lift
                        extra = (extra + (-delta - lift)).coerceAtMost(room[0])
                    }
                },
                onRelease = { velocity ->
                    val far = with(density) { 110.dp.toPx() }
                    val some = with(density) { 24.dp.toPx() }
                    if (pulled > far || (pulled > some && velocity > 900f)) {
                        onClose()
                    } else {
                        val soft = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        val goTall = (extra > room[0] / 2 && velocity < 900f) || velocity < -900f
                        val to = if (goTall) room[0] else 0f
                        settle = scope.launch {
                            launch { animate(pulled, 0f, velocity, soft) { v, _ -> pulled = v } }
                            launch { animate(extra, to, -velocity, soft) { v, _ -> extra = v } }
                        }
                    }
                }
            )
        }
    }
}

/** The grab bar: drag up for more, down to dismiss. */
@Composable
private fun Handle(modifier: Modifier, onDelta: (Float) -> Unit, onRelease: (Float) -> Unit) {
    // Looks like a 24 dp strip with the bar in the middle, but grabs 40 dp.
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState(onDelta),
                onDragStopped = { velocity -> onRelease(velocity) }
            )
    ) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .width(36.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, focus: FocusRequester, onSearch: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    stringResource(R.string.shell_search_elsewhere),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
    }
}

/** Rows keep their place when results change: they slide on springs, new ones fade in, gone ones fade out. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Results(
    context: Context,
    query: String,
    results: List<Location>,
    loading: Boolean,
    suggestions: List<City>,
    recents: List<City>,
    onPickLocation: (Location) -> Unit,
    onPickCity: (City) -> Unit,
) {
    val now = System.currentTimeMillis()
    val searching = query.trim().length >= 2
    val rowSpring =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))
    when {
        searching && loading && results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        searching && results.isEmpty() -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Icon(
                Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.search_no_results, query.trim()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.search_no_results_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        searching -> LazyColumn(
            reverseLayout = true, // the best result sits right above the field
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(results, key = { _, it -> it.formattedId }) { i, location ->
                ResultRow(
                    title = location.city.ifEmpty { location.customName ?: location.country },
                    subtitle = listOfNotNull(location.admin1?.takeIf { it.isNotEmpty() }, location.country)
                        .distinct()
                        .joinToString(", "),
                    zone = location.timeZone,
                    now = now,
                    context = context,
                    highlighted = i == 0,
                    first = i == results.lastIndex,
                    last = i == 0,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(180),
                        placementSpec = rowSpring,
                        fadeOutSpec = tween(120)
                    ),
                    onClick = { onPickLocation(location) }
                )
            }
        }
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (recents.isNotEmpty()) {
                item(key = "recent") { SectionTitle(stringResource(R.string.search_recent), Icons.Outlined.History) }
                itemsIndexed(recents, key = { _, c -> "r" + c.name + c.countryCode }) { i, city ->
                    CityRow(city, now, context, first = i == 0, last = i == recents.lastIndex) { onPickCity(city) }
                }
            }
            if (suggestions.isNotEmpty()) {
                item(key = "top") { SectionTitle(stringResource(R.string.search_top_places), Icons.Outlined.Public) }
                itemsIndexed(suggestions, key = { _, c -> "t" + c.name + c.countryCode }) { i, city ->
                    CityRow(city, now, context, first = i == 0, last = i == suggestions.lastIndex) {
                        onPickCity(city)
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(city: City, now: Long, context: Context, first: Boolean, last: Boolean, onClick: () -> Unit) =
    ResultRow(
        title = city.name,
        subtitle = city.country,
        zone = TimeZone.getTimeZone(city.timeZone),
        now = now,
        context = context,
        highlighted = false,
        first = first,
        last = last,
        onClick = onClick
    )

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * One segment of a group: big outer corners on the group's first/last row, small ones inside.
 * Name, region/country, local time and time difference; the best result is highlighted.
 */
@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    zone: TimeZone,
    now: Long,
    context: Context,
    highlighted: Boolean,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
        .apply { timeZone = zone }
        .format(Date(now))
    val diff = timeDifference(zone, now)
    val big = 20.dp
    val small = 4.dp
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(
                RoundedCornerShape(
                    topStart = if (first) big else small,
                    topEnd = if (first) big else small,
                    bottomStart = if (last) big else small,
                    bottomEnd = if (last) big else small
                )
            )
            .background(
                if (highlighted) colors.secondaryContainer else colors.surfaceContainerHighest.copy(alpha = 0.7f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            if (diff != null) {
                Text(diff, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
