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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.common.utils.helpers.SnackbarHelper
import org.breezyweather.domain.location.model.applyDefaultPreset
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.sources.SourceManager
import org.breezyweather.ui.common.composables.SecondarySourcesPreference
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.glass
import org.breezyweather.ui.expressive.components.glassSource
import org.breezyweather.ui.expressive.components.rememberGlassSource
import org.breezyweather.ui.expressive.search.RecentSearches
import org.breezyweather.ui.expressive.search.SearchOverlay
import org.breezyweather.ui.expressive.sky.AnimatedSkyBackground
import org.breezyweather.ui.expressive.sky.SkyPalette
import org.breezyweather.ui.expressive.sky.SkyState
import org.breezyweather.ui.expressive.sky.SkySystemBars
import org.breezyweather.ui.expressive.sky.SkyWeather
import org.breezyweather.ui.expressive.sky.skyAt
import org.breezyweather.ui.main.MainActivityViewModel
import org.breezyweather.ui.search.SearchViewModel
import kotlin.math.abs

/** What the shell asks its host (MainActivity) to do. */
interface ShellActions {
    fun openSearch()
    fun openDetails()
    fun openSettings()
    fun openLocationSettings()

    /** Show [page] and [message] now, and again on the next resume if the activity is recreated. */
    fun afterPick(page: Int?, message: String?)
}

/** Lets the host move the shell between pages (e.g. show Places when there are no locations). */
class ShellController {
    companion object {
        const val FORECAST = PAGE_FORECAST
    }

    internal var request by mutableStateOf<Int?>(null)
    internal var visitsChanged by mutableStateOf(0)

    internal var message by mutableStateOf<String?>(null)

    /** A place picked from search: mark it as a visit; the host then shows its Forecast. */
    fun visit(context: Context, id: String) {
        Visits.mark(context, id)
        visitsChanged++
    }

    /** Called by the host when it's resumed: go to [page] and show [text] in the shell's message bar. */
    fun show(page: Int?, text: String?) {
        if (page != null) request = page
        if (text != null) message = text
    }
    fun showPlaces(visible: Boolean) {
        request = if (visible) PAGE_PLACES else PAGE_FORECAST
    }
}

internal const val PAGE_DAYS = 0
internal const val PAGE_FORECAST = 1
internal const val PAGE_PLACES = 2

/**
 * Solstice's main screen: three pages side by side, Outlook ← Forecast → Places, over the current
 * location's sky. Outlook and Places sit on a plain page colour (white in light mode, black in dark)
 * that fades in exactly with the swipe away from Forecast, so it follows the finger and reverses.
 */
@Composable
fun SolsticeShell(
    viewModel: MainActivityViewModel,
    actions: ShellActions,
    controller: ShellController,
    sourceManager: SourceManager,
    searchViewModel: SearchViewModel,
) {
    val context = LocalContext.current
    val current by viewModel.currentLocation.collectAsState()
    val locations by viewModel.validLocationList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val location = current?.location
    val now by rememberMinuteClock()
    val sky = location?.let { skyFor(it, now) } ?: skyAt(SkyWeather.CLEAR, now, null, null, isDaylight = true)

    val pager = rememberPagerState(initialPage = if (locations.isEmpty()) PAGE_PLACES else PAGE_FORECAST) { 3 }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var starred by remember { mutableStateOf(loadStarred(context)) }
    val visiting = remember(controller.visitsChanged, locations) { Visits.all(context) }
    // Visits left from the previous run go away unless they were starred.
    val initCompleted by viewModel.initCompleted.collectAsState()
    LaunchedEffect(initCompleted) {
        if (!initCompleted) return@LaunchedEffect
        Visits.takeStale(context).filter { it !in starred }.forEach { id ->
            val index = viewModel.validLocationList.value.indexOfFirst { it.formattedId == id }
            if (index >= 0) viewModel.deleteLocation(index)
        }
        controller.visitsChanged++
    }
    LaunchedEffect(controller.message) {
        controller.message?.let {
            controller.message = null
            snackbar.showSnackbar(it, withDismissAction = true)
        }
    }
    LaunchedEffect(controller.request) {
        controller.request?.let {
            pager.animateScrollToPage(it)
            controller.request = null
        }
    }

    val page = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.Black else Color.White
    val onPage = if (page == Color.Black) Color.White else Color.Black
    // System bar icons follow whatever is behind them.
    SkySystemBars(if (pager.currentPage == PAGE_FORECAST) sky.palette else plainPalette(page, onPage))

    val glass = rememberGlassSource()
    // Search grows out of the Places button (agreed motion); one spring drives open and close.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchOrigin by remember { mutableStateOf<Rect?>(null) }
    val searchProgress = remember { Animatable(0f) }
    LaunchedEffect(searchOpen) {
        searchProgress.animateTo(
            if (searchOpen) 1f else 0f,
            spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
        )
    }
    // A picked place becomes a visit straight away: no weather-sources panel (that's in the place's
    // settings as an advanced option).
    val pickReady by searchViewModel.dialogLocationSourcesOpenState.collectAsState()
    val picked by searchViewModel.selectedLocation.collectAsState()
    LaunchedEffect(pickReady, picked) {
        val place = picked ?: return@LaunchedEffect
        if (!pickReady) return@LaunchedEffect
        searchViewModel.closeDialogLocationSources()
        val existing = locations.firstOrNull {
            it.formattedId == place.formattedId ||
                (it.city == place.city && it.countryCode == place.countryCode && it.city.isNotEmpty())
        }
        val id = existing?.formattedId ?: place.formattedId
        if (existing == null) {
            viewModel.addLocation(place, null, context)
            Visits.mark(context, id)
            controller.visitsChanged++
        }
        RecentSearches.add(context, place)
        viewModel.setLocation(id)
        searchOpen = false
        actions.afterPick(
            PAGE_FORECAST,
            if (existing == null) context.getString(R.string.shell_visiting_hint, place.getPlace(context)) else null
        )
    }
    fun pick(place: Location) = searchViewModel.setSelectedLocation(
        place,
        sourceManager.getLocationSearchSourceOrDefault(searchViewModel.locationSearchSource.value)
    )
    // Title hand-off: while a page's big title is on screen its toolbar tab shows only the icon; as
    // the title scrolls away the label unfolds in the toolbar (never the same word twice).
    val daysList = rememberLazyListState()
    val placesList = rememberLazyListState()
    val titleHeight = with(LocalDensity.current) { 72.dp.toPx() }
    val labelShown by remember {
        derivedStateOf {
            listOf(1f - titleVisible(daysList, titleHeight), 1f, 1f - titleVisible(placesList, titleHeight))
        }
    }
    Box(Modifier.fillMaxSize()) {
        // Everything the toolbar's glass shows, blurred, behind it.
        AnimatedSkyBackground(sky, Modifier.fillMaxSize().glassSource(glass)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Read in the draw phase only: the fade redraws with the swipe, no recomposition.
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        drawRect(page, alpha = abs(position - PAGE_FORECAST).coerceIn(0f, 1f))
                    }
            )
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pager,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    // The page recedes a little while search is open.
                    val k = 1f - 0.04f * searchProgress.value
                    scaleX = k
                    scaleY = k
                }
            ) { index ->
                when (index) {
                    PAGE_DAYS -> DaysPage(location, onPage, page, daysList) {
                        // The edge blur fades in with the plain page, not before it.
                        abs(pager.currentPage + pager.currentPageOffsetFraction - PAGE_FORECAST).coerceIn(0f, 1f)
                    }
                    PAGE_FORECAST -> ForecastPage(
                        location = location,
                        sky = sky,
                        now = now,
                        refreshing = loading,
                        onRefresh = {
                            viewModel.updateWithUpdatingChecking(triggeredByUser = true, checkPermissions = true)
                        },
                        onDetails = actions::openDetails,
                        onOpenPlaces = { scope.launch { pager.animateScrollToPage(PAGE_PLACES) } }
                    )
                    else -> PlacesPage(
                        locations = locations,
                        currentId = location?.formattedId,
                        starred = starred,
                        visiting = visiting,
                        now = now,
                        onPage = onPage,
                        onSelect = {
                            viewModel.setLocation(it.formattedId)
                            scope.launch { pager.animateScrollToPage(PAGE_FORECAST) }
                        },
                        onStar = {
                            val id = it.formattedId
                            starred = if (id in starred) starred - id else starred + id
                            saveStarred(context, starred)
                            // Starring a visit keeps it.
                            if (id in starred) Visits.unmark(context, id)
                            controller.visitsChanged++
                        },
                        onDelete = { place ->
                            val index = locations.indexOfFirst { it.formattedId == place.formattedId }
                            if (index >= 0) {
                                viewModel.deleteLocation(index)
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        message = context.getString(
                                            R.string.shell_place_removed,
                                            place.getPlace(context)
                                        ),
                                        actionLabel = context.getString(R.string.shell_undo),
                                        withDismissAction = true
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addLocation(place, index, context)
                                    }
                                }
                            }
                        },
                        onAddCurrent = {
                            viewModel.openChooseWeatherSourcesDialog(
                                Location(isCurrentPosition = true).applyDefaultPreset(sourceManager)
                            )
                        },
                        onEdit = {
                            viewModel.setLocation(it.formattedId)
                            actions.openLocationSettings()
                        },
                        onMove = { id, beforeId ->
                            // Same move in the saved list (where the current position may sit anywhere).
                            val from = locations.indexOfFirst { it.formattedId == id }
                            val rest = locations.filter { it.formattedId != id }
                            val to = beforeId?.let { b -> rest.indexOfFirst { it.formattedId == b } }
                                ?.takeIf { it >= 0 } ?: rest.size
                            if (from >= 0) viewModel.swapLocations(from, to)
                        },
                        onSearch = { searchOpen = true },
                        onSearchBounds = { searchOrigin = it },
                        searchButtonAlpha = { if (searchProgress.value > 0f) 0f else 1f },
                        onSettings = actions::openSettings,
                        listState = placesList
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    // The toolbar steps aside while search is open.
                    .graphicsLayer { alpha = 1f - searchProgress.value }
            ) {
                SnackbarHost(snackbar)
                ShellToolbar(
                    pager = pager,
                    sky = sky,
                    weather = SkyWeather.of(location?.weather?.current?.weatherCode),
                    glass = glass,
                    labelShown = labelShown,
                    onTab = { scope.launch { pager.animateScrollToPage(it) } }
                )
            }
        }
        ChooseSourcesDialog(viewModel, sourceManager)
        val nearPoint = (
            locations.firstOrNull {
                it.isCurrentPosition
            } ?: location
            )?.let { it.latitude to it.longitude }
        SearchOverlay(
            open = searchOpen,
            progress = searchProgress.value,
            origin = searchOrigin,
            searchViewModel = searchViewModel,
            near = nearPoint,
            onPickLocation = { pick(it) },
            onPickCity = { pick(it.toLocation()) },
            onClose = { searchOpen = false }
        )
    }
}

/** Upstream's weather-source chooser shown when adding a place (it used to live in its Locations page). */
@Composable
private fun ChooseSourcesDialog(viewModel: MainActivityViewModel, sourceManager: SourceManager) {
    val context = LocalContext.current
    val open by viewModel.dialogChooseWeatherSourcesOpen.collectAsState()
    val selected by viewModel.selectedLocation.collectAsState()
    if (!open) return
    val location = selected ?: return
    SecondarySourcesPreference(
        sourceManager = sourceManager,
        location = location,
        onClose = { newLocation: Location? ->
            viewModel.closeChooseWeatherSourcesDialog()
            if (newLocation != null) {
                if (viewModel.locationExists(location)) {
                    if (location.forecastSource != newLocation.forecastSource &&
                        viewModel.locationExists(newLocation)
                    ) {
                        SnackbarHelper.showSnackbar(context.getString(R.string.location_message_already_exists))
                    } else {
                        viewModel.updateLocation(newLocation, location)
                        SnackbarHelper.showSnackbar(context.getString(R.string.location_message_updated))
                    }
                } else if (viewModel.locationExists(newLocation)) {
                    SnackbarHelper.showSnackbar(context.getString(R.string.location_message_already_exists))
                } else {
                    viewModel.addLocation(newLocation, null, context)
                    SnackbarHelper.showSnackbar(context.getString(R.string.location_message_added))
                }
            }
        },
        locationExists = { viewModel.locationExists(it) }
    )
}

/** 1 while the list's big title (its first item) is fully on screen, 0 once it has scrolled away. */
private fun titleVisible(state: LazyListState, titleHeight: Float): Float =
    if (state.firstVisibleItemIndex >
        0
    ) {
        0f
    } else {
        (1f - state.firstVisibleItemScrollOffset / titleHeight).coerceIn(0f, 1f)
    }

/** The sky for a location at [now], from its current weather and today's sunrise/sunset. */
internal fun skyFor(location: Location, now: Long): SkyState {
    val weather = location.weather
    val sun = weather?.today?.sun
    return skyAt(
        weather = SkyWeather.of(weather?.current?.weatherCode),
        now = now,
        sunrise = sun?.riseDate?.time,
        sunset = sun?.setDate?.time,
        isDaylight = location.isDaylight
    )
}

/** Current time, ticking once a minute (enough for the sun's position and the hour list). */
@Composable
internal fun rememberMinuteClock() = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(60_000L - value % 60_000L)
    }
}

/** A palette standing for a plain page colour, so system bar icons can follow it. */
private fun plainPalette(page: Color, text: Color) = SkyPalette(
    base = page,
    blobs = listOf(page, page, page),
    text = text,
    grain = 0f,
    sun = page,
    sunStrength = 0f
)

// Starred places (first form of "pin"; see docs/PLAN.md, Places: visit vs pin).
private const val PREFS = "solstice_places"
private const val KEY_STARRED = "starred"

private fun loadStarred(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_STARRED, emptySet()).orEmpty()

private fun saveStarred(context: Context, starred: Set<String>) =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putStringSet(KEY_STARRED, starred) }

private class Tab(val label: Int, val icon: ImageVector)

/**
 * Floating toolbar. Each tab is an icon; the selected one unfolds to show its title. Widths, label
 * and highlight are all driven by the pager position, so they follow the finger mid-swipe and fold
 * back if the swipe reverses.
 */
@Composable
private fun ShellToolbar(
    pager: PagerState,
    sky: SkyState,
    weather: SkyWeather,
    glass: GlassSource,
    labelShown: List<Float>,
    onTab: (Int) -> Unit,
) {
    val tabs = listOf(
        Tab(R.string.shell_tab_days, Icons.Outlined.CalendarMonth),
        Tab(R.string.shell_tab_forecast, conditionIcon(weather, night = sky.sunHeight < 0f)),
        Tab(R.string.shell_tab_places, Icons.Outlined.Map)
    )
    val collapsed = 52.dp
    val expanded = 132.dp
    val position = pager.currentPage + pager.currentPageOffsetFraction
    val open = tabs.indices.map { (1f - abs(position - it)).coerceIn(0f, 1f) }
    // How far each tab's title is unfolded: selected (follows the swipe) and not already shown as the
    // page's own big title.
    val shown = tabs.indices.map { open[it] * labelShown[it] }
    val widths = shown.map { collapsed + (expanded - collapsed) * it }
    val starts = widths.runningFold(0.dp) { acc, w -> acc + w }
    val highlightX = tabs.indices.fold(0.dp) { acc, i -> acc + starts[i] * open[i] }
    val highlightW = tabs.indices.fold(0.dp) { acc, i -> acc + widths[i] * open[i] }
    // Frosted glass: the page behind, blurred, under a light tint of the surface colour.
    val tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f)
    // On Forecast the highlight wears the weather (a colour from the sky); swiping to Outlook or
    // Places morphs it into the Material You colour, in step with the swipe.
    val skyAccent = sky.palette.blobs[1]
    val onSkyAccent = if (skyAccent.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White
    val primary = lerp(MaterialTheme.colorScheme.primary, skyAccent, open[PAGE_FORECAST])
    val onPrimary = lerp(MaterialTheme.colorScheme.onPrimary, onSkyAccent, open[PAGE_FORECAST])
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(Modifier.glass(glass, CircleShape, tint).padding(4.dp)) {
        Box(
            Modifier
                .offset(x = highlightX)
                .width(highlightW)
                .height(44.dp)
                .background(primary, CircleShape)
        )
        Row {
            tabs.forEachIndexed { i, tab ->
                val label = stringResource(tab.label)
                val color = lerp(onSurface, onPrimary, open[i])
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(widths[i])
                        .height(44.dp)
                        .clipToBounds()
                        .semantics(mergeDescendants = true) {
                            selected = pager.currentPage == i
                            contentDescription = label
                        }
                        .clickable(role = Role.Tab) { onTab(i) }
                        .padding(start = 14.dp)
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp * shown[i]))
                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer { alpha = shown[i] }
                    )
                }
            }
        }
    }
}

private operator fun Dp.times(f: Float): Dp = Dp(value * f)

/** The Forecast tab's icon follows the current weather. */
private fun conditionIcon(weather: SkyWeather, night: Boolean): ImageVector = when (weather) {
    SkyWeather.CLEAR -> if (night) Icons.Outlined.Bedtime else Icons.Outlined.WbSunny
    SkyWeather.PARTLY_CLOUDY -> if (night) Icons.Outlined.Bedtime else Icons.Outlined.WbCloudy
    SkyWeather.OVERCAST -> Icons.Outlined.Cloud
    SkyWeather.RAIN -> Icons.Outlined.Umbrella
    SkyWeather.THUNDERSTORM -> Icons.Outlined.Thunderstorm
    SkyWeather.SNOW -> Icons.Outlined.AcUnit
    SkyWeather.FOG -> Icons.Outlined.Dehaze
}
