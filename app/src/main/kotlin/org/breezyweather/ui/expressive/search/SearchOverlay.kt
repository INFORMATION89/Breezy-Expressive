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
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.breezyweather.R
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.glass
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
@OptIn(ExperimentalLayoutApi::class)
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
    // A pick hands the card over to the postcard window: it reports where it is and then vanishes.
    onCardBounds: (Rect) -> Unit = {},
    glass: GlassSource? = null,
    handedOff: Boolean = false,
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
    // Written by the card's layout each frame; read by the drags (how far it can grow).
    val room = remember { FloatArray(1) }
    // The query last sent, so "no results" only shows once that search has answered.
    var requested by remember { mutableStateOf("") }

    BackHandler(enabled = open, onBack = onClose)
    // Hiding the keyboard (its dismiss key) while search is open expands the card to full height.
    val imeShown = WindowInsets.isImeVisible
    val imeNow = rememberUpdatedState(imeShown)
    val keyboardController = LocalSoftwareKeyboardController.current
    var hadIme by remember { mutableStateOf(false) }
    LaunchedEffect(imeShown) {
        if (open && hadIme && !imeShown) {
            settle?.cancel()
            settle = scope.launch {
                animate(
                    extra,
                    room[0],
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                ) {
                        v,
                        _,
                    ->
                    extra = v
                }
            }
        }
        hadIme = imeShown
    }
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
        // Build the typing index now, so the first letters already find cities.
        withContext(Dispatchers.Default) { Cities.search(context, "a") }
    }
    var recentsVersion by remember { mutableStateOf(0) }
    val recents = remember(open, recentsVersion) { RecentSearches.all(context) }

    // Never looks like it's loading (agreed 2026-11-18): matches from the bundled city list show
    // instantly as you type; when the online answer for this query arrives, rows settle into it
    // (same city keeps its row). While waiting with no local match, the last rows stay.
    val local by produceState(emptyList<City>(), query) {
        val q = query.trim()
        value = if (q.length < 2) emptyList() else withContext(Dispatchers.Default) { Cities.search(context, q) }
    }
    val answered = requested == query && results.second != LoadableLocationStatus.LOADING
    val online = if (answered) results.first else emptyList()
    var lastHits by remember { mutableStateOf(emptyList<Hit>()) }
    val hits = remember(online, local, answered) {
        val fresh = when {
            online.isNotEmpty() -> online.map { it.toHit(onPickLocation) }
            local.isNotEmpty() -> local.map { it.toHit(onPickCity) }
            else -> emptyList()
        }
        uniqueKeys(fresh)
    }
    val shownHits = if (hits.isNotEmpty() || answered) hits else lastHits
    SideEffect { if (hits.isNotEmpty()) lastHits = hits }

    val view = LocalView.current
    val ime = WindowInsets.ime
    val navigation = WindowInsets.navigationBars
    val status = WindowInsets.statusBars
    // Round as the bubble; docked, only the top corners stay round (the bottom sits on the keyboard).
    val cardShape = remember {
        GenericShape { size, _ ->
            val top = minOf(32.dp.value * density.density, size.minDimension / 2)
            val bottom = top * (1f - progress().coerceIn(0f, 1f))
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
    // Written by the card's layout each frame; read by the handle's drag (how far it can grow).

    // How far the inside is shown, from the open progress (read only in draw/layout lambdas).
    fun inside() = ((progress().coerceIn(0f, 1f) - 0.45f) / 0.55f).coerceIn(0f, 1f)

    // The card's height and pull, from the handle or the list (deltas: + is the finger going down).
    fun drag(delta: Float) {
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
            val before = extra
            extra = (extra + (-delta - lift)).coerceAtMost(room[0])
            // With the keyboard hidden, swiping the card up until it hits the top brings the
            // keyboard back.
            if (!imeNow.value && before < room[0] - 0.5f && extra >= room[0] - 0.5f) {
                runCatching { focus.requestFocus() }
                keyboardController?.show()
            }
        }
    }

    // Let go: far enough down (or flung down) dismisses; otherwise it settles compact or tall.
    fun release(velocity: Float) {
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

    // The list drives the card like a bottom sheet: scrolling the results up grows the card first;
    // with the list at its edge, pulling down shrinks it and then dismisses it. Flings settle it.
    val sheet = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                val space = pulled + (room[0] - extra).coerceAtLeast(0f)
                val take = minOf(-dy, space)
                if (take <= 0f) return Offset.Zero
                drag(-take)
                return Offset(0f, -take)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                drag(dy)
                return Offset(0f, dy)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val between = pulled > 0f || (extra > 0.5f && extra < room[0] - 0.5f)
                if (!between) return Velocity.Zero
                release(available.y)
                return available
            }
        }
    }

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
                .background(Color.Black.copy(alpha = 0.25f))
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
                    // On the keyboard; with the keyboard hidden, down to the screen's edge (behind the
                    // navigation bar; the field keeps clear of it with padding).
                    val bottomInset = if (keyboard > 0) (keyboard - shrunk).coerceAtLeast(0) else 0
                    val top = status.getTop(this) + 12.dp.toPx()
                    // Full width, sitting right on the keyboard (no gaps).
                    val margin = 0f
                    val chrome = (24.dp + 64.dp).toPx() // handle + field
                    val compact = 280.dp.toPx()
                    val tall = (height - bottomInset - top - chrome).coerceAtLeast(compact)
                    room[0] = tall - compact
                    val content = (compact + extra).coerceIn(compact, tall)
                    val bottom = height - bottomInset + pulled
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
                .onGloballyPositioned { onCardBounds(it.boundsInRoot()) }
                .graphicsLayer { alpha = if (handedOff) 0f else 1f }
                // The toolbar's glass: the page behind, blurred, under a light tint, with a fine edge.
                .glass(glass, cardShape, MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .clip(cardShape)
        ) {
            // While still small it's the bubble's search icon.
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = 1f - inside() }
            )
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = inside() }.nestedScroll(sheet)) {
                Spacer(Modifier.height(24.dp)) // room for the handle, drawn on top
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // Built once the card is a third of the way open (it's invisible before that), so the
                    // first frame of the animation only has the card and field to build.
                    val ready by remember { derivedStateOf { progress() > 0.3f } }
                    if (ready || !open) {
                        Results(
                            context = context,
                            query = query,
                            hits = shownHits,
                            answered = answered,
                            // A place already under Recent isn't repeated under Popular places.
                            suggestions = suggestions.filterNot { c ->
                                recents.any { it.name == c.name && it.countryCode == c.countryCode }
                            },
                            recents = recents,
                            onPickCity = onPickCity,
                            onClearRecents = {
                                RecentSearches.clear(context)
                                recentsVersion++
                            }
                        )
                    }
                }
                SearchField(
                    query = query,
                    onQuery = { query = it },
                    focus = focus,
                    onSelect = { shownHits.firstOrNull()?.pick?.invoke() }
                )
                // Behind the navigation bar when the keyboard is hidden.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars.exclude(WindowInsets.ime)))
            }
            // The handle's touch area is taller than it looks (it overlaps the top of the results).
            Handle(
                modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = inside() },
                onDelta = { drag(it) },
                onRelease = { release(it) }
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
private fun SearchField(query: String, onQuery: (String) -> Unit, focus: FocusRequester, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
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
                // Results already follow the typing, so the key picks the highlighted match.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSelect() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
    }
}

private enum class ResultsMode { NONE, FOUND, SUGGEST }

/** One row of results, from the online lookup or the bundled list; the same city keeps its key. */
private class Hit(
    val key: String,
    val title: String,
    val subtitle: String,
    val zone: TimeZone,
    val pick: () -> Unit,
)

private fun cityKey(name: String, countryCode: String?) = (name + "|" + countryCode.orEmpty()).lowercase()

private fun Location.toHit(onPick: (Location) -> Unit): Hit {
    val name = city.ifEmpty { customName ?: country }
    return Hit(
        key = cityKey(name, countryCode),
        title = name,
        subtitle = listOfNotNull(admin1?.takeIf { it.isNotEmpty() }, country).distinct().joinToString(", "),
        zone = timeZone,
        pick = { onPick(this) }
    )
}

private fun City.toHit(onPick: (City) -> Unit) = Hit(
    key = cityKey(name, countryCode),
    title = name,
    subtitle = country,
    zone = TimeZone.getTimeZone(timeZone),
    pick = { onPick(this) }
)

/** Same name in the same country (several Springfields) gets "#2", "#3"… so list keys stay unique. */
private fun uniqueKeys(hits: List<Hit>): List<Hit> {
    val seen = HashMap<String, Int>()
    return hits.map { h ->
        val n = (seen[h.key] ?: 0) + 1
        seen[h.key] = n
        if (n == 1) h else Hit(h.key + "#" + n, h.title, h.subtitle, h.zone, h.pick)
    }
}

/**
 * The list inside the card (agreed 2026-11-18). It fades softly into the field and under the
 * handle instead of being cut by a hard line. Rows rise in one after another when they first
 * appear, starting next to the field. When results change, rows that stay slide to their new
 * place, new ones rise in and gone ones fade out. Switching between loading, no results and a
 * list crossfades.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Results(
    context: Context,
    query: String,
    hits: List<Hit>,
    answered: Boolean,
    suggestions: List<City>,
    recents: List<City>,
    onPickCity: (City) -> Unit,
    onClearRecents: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val searching = query.trim().length >= 2
    val mode = when {
        searching && hits.isEmpty() && answered -> ResultsMode.NONE
        searching -> ResultsMode.FOUND
        else -> ResultsMode.SUGGEST
    }
    val rowSpring =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(90)) },
        label = "results",
        modifier = Modifier.fillMaxSize().softEdges()
    ) { shown ->
        when (shown) {
            ResultsMode.NONE -> Column(
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
            ResultsMode.FOUND -> {
                // Rows composed within a moment of a new set of hits rise in (new cities); rows that
                // stay keep their place and slide; rows scrolled into view later just appear.
                val changedAt = remember(hits.map { it.key }) { SystemClock.uptimeMillis() }
                LazyColumn(
                    reverseLayout = true, // the best match sits right above the field
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(hits, key = { _, it -> it.key }) { i, hit ->
                        ResultRow(
                            title = hit.title,
                            subtitle = hit.subtitle,
                            zone = hit.zone,
                            now = now,
                            context = context,
                            highlighted = i == 0,
                            first = i == hits.lastIndex,
                            last = i == 0,
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, placementSpec = rowSpring, fadeOutSpec = tween(120))
                                .riseIn(order = i, shownAt = changedAt),
                            onClick = hit.pick
                        )
                    }
                }
            }
            ResultsMode.SUGGEST -> {
                val shownAt = remember { SystemClock.uptimeMillis() }
                // Top-down list: the rows nearest the field (about the fifth on screen) go first.
                val rows = (if (recents.isEmpty()) 0 else recents.size + 1) + suggestions.size + 1
                fun order(index: Int) = (minOf(rows, 6) - 1 - index).coerceAtLeast(0)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    var at = 0
                    if (recents.isNotEmpty()) {
                        val o = order(at++)
                        item(key = "recent") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionTitle(
                                    stringResource(R.string.search_recent),
                                    Icons.Outlined.History,
                                    Modifier.riseIn(o, shownAt).weight(1f)
                                )
                                TextButton(onClick = onClearRecents, modifier = Modifier.riseIn(o, shownAt)) {
                                    Text(stringResource(R.string.search_clear_recents))
                                }
                            }
                        }
                        val base = at
                        itemsIndexed(recents, key = { _, c -> "r" + c.name + c.countryCode }) { i, city ->
                            CityRow(
                                city,
                                now,
                                context,
                                first = i == 0,
                                last = i == recents.lastIndex,
                                Modifier.riseIn(order(base + i), shownAt)
                            ) { onPickCity(city) }
                        }
                        at += recents.size
                    }
                    if (suggestions.isNotEmpty()) {
                        val o = order(at++)
                        item(key = "top") {
                            SectionTitle(
                                stringResource(R.string.search_top_places),
                                Icons.Outlined.Public,
                                Modifier.riseIn(o, shownAt)
                            )
                        }
                        val base = at
                        itemsIndexed(suggestions, key = { _, c -> "t" + c.name + c.countryCode }) { i, city ->
                            CityRow(
                                city,
                                now,
                                context,
                                first = i == 0,
                                last = i == suggestions.lastIndex,
                                Modifier.riseIn(order(base + i), shownAt)
                            ) { onPickCity(city) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A row's entrance: it rises a few dp and fades in, [order] x 25 ms after the others (capped), on
 * a soft spring. Only for rows composed within a moment of [shownAt]; a row scrolled into view
 * later (or one that stays when results change) is simply there.
 */
@Composable
private fun Modifier.riseIn(order: Int, shownAt: Long): Modifier {
    val fresh = remember { SystemClock.uptimeMillis() - shownAt < 350 }
    val enter = remember { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            delay(order.coerceIn(0, 6) * 25L)
            enter.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    return graphicsLayer {
        alpha = enter.value
        translationY = (1f - enter.value) * 10.dp.toPx()
    }
}

/** Fades the list out over a short band at the top (under the handle) and bottom (above the field). */
private fun Modifier.softEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val top = 14.dp.toPx()
        val bottom = 18.dp.toPx()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = top),
            size = Size(size.width, top),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                startY = size.height - bottom,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - bottom),
            size = Size(size.width, bottom),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
private fun CityRow(
    city: City,
    now: Long,
    context: Context,
    first: Boolean,
    last: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) =
    ResultRow(
        title = city.name,
        subtitle = city.country,
        zone = TimeZone.getTimeZone(city.timeZone),
        now = now,
        context = context,
        highlighted = false,
        first = first,
        last = last,
        modifier = modifier,
        onClick = onClick
    )

@Composable
private fun SectionTitle(text: String, icon: ImageVector, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
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
    // The best-match highlight eases on and off instead of jumping between rows.
    val fill by animateColorAsState(
        if (highlighted) colors.secondaryContainer else colors.surfaceContainerHighest.copy(alpha = 0.7f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "row"
    )
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
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // The keyboard's key picks this one.
                if (highlighted) {
                    Text(
                        " · " + stringResource(R.string.search_select),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        maxLines = 1
                    )
                }
            }
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
