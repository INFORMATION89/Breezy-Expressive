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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.breezyweather.R
import org.breezyweather.ui.expressive.shell.timeDifference
import org.breezyweather.ui.search.LoadableLocationStatus
import org.breezyweather.ui.search.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Search, morphing out of the "Search somewhere else" button (agreed motion, 2026-11-18): the
 * button's rectangle grows into a search bar at the bottom of the screen, above the keyboard; the
 * page behind dims; results rise from the bar, nearest result closest to the thumb and the first
 * one highlighted (Enter picks it). [progress] (0 = the button, 1 = open) drives everything, so
 * opening and closing are the same spring and can reverse mid-way. Back, a tap on the dimmed page
 * or swiping the bar down closes it back into the button.
 */
@Composable
fun SearchOverlay(
    open: Boolean,
    progress: Float,
    origin: Rect?,
    searchViewModel: SearchViewModel,
    near: Pair<Double, Double>?,
    onPickLocation: (Location) -> Unit,
    onPickCity: (City) -> Unit,
    onClose: () -> Unit,
) {
    if (progress <= 0f && !open) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val results by searchViewModel.listResource.collectAsState()

    BackHandler(enabled = open, onBack = onClose)
    LaunchedEffect(open) {
        if (open) {
            delay(120)
            runCatching { focus.requestFocus() }
        } else {
            focusManager.clearFocus()
            query = ""
        }
    }
    // Results as you type, after a short pause.
    LaunchedEffect(query) {
        if (query.trim().length < 2) return@LaunchedEffect
        delay(300)
        searchViewModel.requestLocationList(query)
    }
    val nearby by produceState(emptyList<City>(), near) {
        value = near?.let { (lat, lon) -> withContext(Dispatchers.IO) { Cities.nearby(context, lat, lon) } }.orEmpty()
    }
    val recents = remember(open) { RecentSearches.all(context) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val margin = with(density) { 16.dp.toPx() }
        val barHeight = with(density) { 56.dp.toPx() }
        // The window may already shrink for the keyboard (then our height is smaller than the root's):
        // only add the part of the keyboard that the resize hasn't taken away.
        val view = LocalView.current
        // constraints (not view.height) so a resize recomposes this.
        val shrunk = (view.rootView.height - constraints.maxHeight).coerceAtLeast(0)
        val ime = WindowInsets.ime.getBottom(density)
        val bottomInset = if (ime >
            0
        ) {
            (ime - shrunk).coerceAtLeast(0)
        } else {
            WindowInsets.navigationBars.getBottom(density)
        }
        val target = Rect(
            left = margin,
            top = height - bottomInset - margin - barHeight,
            right = width - margin,
            bottom = height - bottomInset - margin
        )
        val start = origin ?: target
        val p = progress.coerceIn(0f, 1f)
        val rect = Rect(
            left = start.left + (target.left - start.left) * p,
            top = start.top + (target.top - start.top) * p,
            right = start.right + (target.right - start.right) * p,
            bottom = start.bottom + (target.bottom - start.bottom) * p
        )

        // The page behind dims; a tap on it closes search.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * p))
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    enabled = open
                ) {
                    onClose()
                }
        )

        // Results rise from the bar.
        val listBottom = with(density) { (height - rect.top + 8.dp.toPx()).toDp() }
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, bottom = listBottom)
                .graphicsLayer {
                    alpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    translationY = (1f - p) * 48.dp.toPx()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            ResultList(
                context = context,
                query = query,
                results = results.first,
                loading = results.second == LoadableLocationStatus.LOADING,
                nearby = nearby,
                recents = recents,
                onPickLocation = onPickLocation,
                onPickCity = onPickCity,
                maxHeight = with(density) { (rect.top * 0.85f).toDp() }
            )
        }

        // The morphing container: the button's rectangle becoming the bar.
        val corner = 24.dp + 4.dp * p
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { },
                    onDragStopped = { velocity -> if (velocity > 300f) onClose() }
                )
                .padding(horizontal = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
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
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { results.first.firstOrNull()?.let(onPickLocation) }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultList(
    context: Context,
    query: String,
    results: List<Location>,
    loading: Boolean,
    nearby: List<City>,
    recents: List<City>,
    onPickLocation: (Location) -> Unit,
    onPickCity: (City) -> Unit,
    maxHeight: androidx.compose.ui.unit.Dp,
) {
    val now = System.currentTimeMillis()
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        when {
            query.trim().length >= 2 && loading && results.isEmpty() -> Box(
                Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) { LoadingIndicator() }
            query.trim().length >= 2 -> LazyColumn(
                reverseLayout = true, // the first (best) result sits right above the bar
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(results, key = { _, it -> it.formattedId }) { i, location ->
                    ResultRow(
                        title = location.city.ifEmpty { location.customName ?: location.country },
                        subtitle = listOfNotNull(location.admin1?.takeIf { it.isNotEmpty() }, location.country)
                            .distinct().joinToString(", "),
                        zone = location.timeZone,
                        now = now,
                        context = context,
                        highlighted = i == 0,
                        onClick = { onPickLocation(location) }
                    )
                }
            }
            // Before typing: recent first, then cities nearby right above the bar (top-down list,
            // so each heading stays above its rows).
            else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
                if (recents.isNotEmpty()) {
                    item(key = "recent") {
                        SectionTitle(stringResource(R.string.search_recent), Icons.Outlined.History)
                    }
                    itemsIndexed(recents, key = { _, c -> "r" + c.name + c.countryCode }) { _, city ->
                        ResultRow(city.name, city.country, TimeZone.getTimeZone(city.timeZone), now, context, false) {
                            onPickCity(city)
                        }
                    }
                }
                if (nearby.isNotEmpty()) {
                    item(key = "nearby") { SectionTitle(stringResource(R.string.search_nearby), Icons.Outlined.NearMe) }
                    itemsIndexed(nearby, key = { _, c -> "n" + c.name + c.countryCode }) { _, city ->
                        ResultRow(city.name, city.country, TimeZone.getTimeZone(city.timeZone), now, context, false) {
                            onPickCity(city)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

/** Name, region/country, local time and time difference; the first result is highlighted. */
@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    zone: TimeZone,
    now: Long,
    context: Context,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", java.util.Locale.getDefault())
        .apply { timeZone = zone }.format(Date(now))
    val diff = timeDifference(zone, now)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (diff != null) {
                Text(
                    diff,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
