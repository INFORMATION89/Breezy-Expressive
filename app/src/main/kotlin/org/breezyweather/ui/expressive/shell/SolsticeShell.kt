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
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.breezyweather.R
import org.breezyweather.common.utils.helpers.SnackbarHelper
import org.breezyweather.domain.location.model.applyDefaultPreset
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.sources.SourceManager
import org.breezyweather.ui.common.composables.SecondarySourcesPreference
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.SolsticeIcons
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
import kotlin.math.roundToInt

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
    internal var pending by mutableStateOf<ShellMessage?>(null)

    /** Show [message] in the shell's message pill. */
    fun say(message: ShellMessage) {
        pending = message
    }

    internal var searchOpen by mutableStateOf(false)

    /** Set by the host while upstream's details screen covers the shell (its back closes that). */
    var detailsOpen by mutableStateOf(false)

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
    val messages = remember { ShellMessages() }
    // One star: the main place (agreed 2026-11-18). The app opens on it and "back" returns to it;
    // any other place you look at is a visit.
    var starred by remember { mutableStateOf(loadStarred(context).take(1).toSet()) }
    val main = starred.firstOrNull()?.takeIf { id -> locations.any { it.formattedId == id } }
    val visiting = remember(controller.visitsChanged, locations) { Visits.all(context) }
    // Visits left from the previous run go away unless they were starred; then open on the main place.
    val initCompleted by viewModel.initCompleted.collectAsState()
    LaunchedEffect(initCompleted) {
        if (!initCompleted) return@LaunchedEffect
        Visits.takeStale(context).filter { it !in starred }.forEach { id ->
            val index = viewModel.validLocationList.value.indexOfFirst { it.formattedId == id }
            if (index >= 0) viewModel.deleteLocation(index)
        }
        controller.visitsChanged++
    }
    // There's always one starred place (the current position, else the first), and the main screen
    // shows it.
    LaunchedEffect(locations, starred) {
        if (locations.isEmpty()) return@LaunchedEffect
        if (starred.none { id -> locations.any { it.formattedId == id } }) {
            val pick = (locations.firstOrNull { it.isCurrentPosition } ?: locations.first()).formattedId
            starred = setOf(pick)
            saveStarred(context, starred)
            Visits.unmark(context, pick)
            controller.visitsChanged++
        }
    }
    LaunchedEffect(main, location?.formattedId) {
        if (main != null && location?.formattedId != main) viewModel.setLocation(main)
    }
    LaunchedEffect(controller.message) {
        controller.message?.let {
            controller.message = null
            messages.show(ShellMessage(Icons.Outlined.Explore, it))
        }
    }
    LaunchedEffect(controller.pending) {
        controller.pending?.let {
            controller.pending = null
            messages.show(it)
        }
    }

    val page = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.Black else Color.White
    val onPage = if (page == Color.Black) Color.White else Color.Black
    // System bar icons follow whatever is behind them.

    val glass = rememberGlassSource()
    // Tab jump This week <-> Places: a real crossfade. The page being left is snapshotted and drawn on
    // top fading out, the new page is already underneath; both carry a little blur that clears
    // quickly. The toolbar's highlight glides across on its own.
    val leaving = rememberGraphicsLayer()
    val leavingDraw = rememberGraphicsLayer()
    var capture by remember { mutableStateOf(false) }
    // A frozen image of the page being left (a recorded layer alone would keep showing live content).
    var leavingImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val fadeOut = remember { Animatable(0f) }
    val haze = remember { Animatable(0f) }
    val toolbarPosition = remember { Animatable(-1f) }
    var jumpDir by remember { mutableFloatStateOf(1f) }

    // Navigation (reworked 2026-11-18): one place decides how to get to a page. Every page change
    // (toolbar taps, requests from the host, "open Places", the toolbar slide) goes through here; a
    // new one, or a finger, cancels the transition in progress and resets its state first, so
    // nothing is left half-way (a fast second tap used to fight the first).
    var navJob by remember { mutableStateOf<Job?>(null) }
    suspend fun restJump() {
        fadeOut.snapTo(0f)
        haze.snapTo(0f)
        toolbarPosition.snapTo(-1f)
        leavingImage = null
        capture = false
    }
    fun interruptNavigation() {
        navJob?.cancel()
        navJob = null
    }
    fun navigate(target: Int) {
        navJob?.cancel()
        navJob = scope.launch {
            try {
                restJump()
                val here = (pager.currentPage + pager.currentPageOffsetFraction).roundToInt()
                if (abs(target - here) >= 2 && abs(pager.currentPageOffsetFraction) < 0.01f) {
                    // This week <-> Places (agreed): a slide with a frozen image of the page being left.
                    val from = here.toFloat()
                    capture = true
                    withFrameNanos { }
                    withFrameNanos { }
                    leavingImage = runCatching { leaving.toImageBitmap() }.getOrNull()
                    jumpDir = if (target > from) 1f else -1f
                    fadeOut.snapTo(1f)
                    haze.snapTo(1f)
                    toolbarPosition.snapTo(from)
                    pager.scrollToPage(target)
                    coroutineScope {
                        launch { haze.animateTo(0f, tween(180)) }
                        launch {
                            toolbarPosition.animateTo(
                                target.toFloat(),
                                // About the pace of a neighbouring tab change (user: edge to edge a touch faster).
                                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                        fadeOut.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
                    }
                } else {
                    pager.animateScrollToPage(target)
                }
            } finally {
                // Finished or interrupted: always back at rest (non-cancellable, so this runs even
                // when a new navigation cancelled us).
                withContext(NonCancellable) { restJump() }
            }
        }
    }
    LaunchedEffect(controller.request) {
        controller.request?.let {
            controller.request = null
            navigate(it)
        }
    }
    val searchBlur = rememberGraphicsLayer()
    val searchSharp = rememberGraphicsLayer()
    // Search grows out of the Places button (agreed motion); one spring drives open and close.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    SideEffect { controller.searchOpen = searchOpen }
    var searchOrigin by remember { mutableStateOf<Rect?>(null) }
    // Where the search card is, and whether it has just handed itself over to a postcard window.
    var searchCard by remember { mutableStateOf<Rect?>(null) }
    var searchHandoff by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    // A postcard opened as a window (agreed 2026-11-18): one spring grows it from its card and back.
    var windowId by rememberSaveable { mutableStateOf<String?>(null) }
    var windowOrigin by remember { mutableStateOf<Rect?>(null) }
    val windowProgress = remember { Animatable(0f) }
    // System bar icons follow whatever is behind them; a postcard window sets its own once it's
    // mostly open.
    val windowCovers by remember { derivedStateOf { windowId != null && windowProgress.value > 0.5f } }
    if (!windowCovers) {
        SkySystemBars(if (pager.currentPage == PAGE_FORECAST) sky.palette else plainPalette(page, onPage))
    }
    var windowFade by remember { mutableStateOf(false) }
    val windowSpring = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
    // Grows once the place is in the list (a place just picked in search is added a moment later),
    // so the window never pops in half-way.
    val windowReady = windowId != null && locations.any { it.formattedId == windowId }
    LaunchedEffect(windowId, windowReady) {
        if (windowReady) windowProgress.animateTo(1f, windowSpring)
    }
    fun closeWindow() {
        val leaving = windowId
        // A visit that isn't kept fades away as it shrinks (it has no slot to land in).
        windowFade = leaving != null && leaving in Visits.all(context) && leaving !in starred
        scope.launch {
            windowProgress.animateTo(0f, windowSpring)
            val id = windowId
            windowId = null
            // A visit you didn't pin (or star) is gone once you go back.
            if (id != null && id in Visits.all(context) && id !in starred) {
                val index = viewModel.validLocationList.value.indexOfFirst { it.formattedId == id }
                if (index >= 0) viewModel.deleteLocation(index)
                Visits.unmark(context, id)
                controller.visitsChanged++
            }
        }
    }

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
        val existing = viewModel.validLocationList.value.firstOrNull {
            it.formattedId == place.formattedId ||
                (it.city == place.city && it.countryCode == place.countryCode && it.city.isNotEmpty())
        }
        val id = existing?.formattedId ?: place.formattedId
        if (existing == null) {
            // Added as a visit (gone when you go back, unless you pin it) and fetched in the
            // background; your selected and main place stay as they are.
            viewModel.addLocation(place, null, context, select = false)
            Visits.mark(context, id)
            controller.visitsChanged++
            viewModel.refreshPlace(context, place)
        }
        RecentSearches.add(context, place)
        searchOpen = false
        if (id in starred) {
            // The main place is the Forecast itself.
            controller.request = PAGE_FORECAST
        } else {
            // The search card itself turns into the postcard and opens into the window.
            keyboard?.hide()
            windowOrigin = searchCard ?: searchOrigin
            searchHandoff = true
            windowId = id
        }
    }
    fun pick(place: Location) = searchViewModel.setSelectedLocation(
        place,
        sourceManager.getLocationSearchSourceOrDefault(searchViewModel.locationSearchSource.value)
    )
    // Title hand-off: while a page's big title is on screen its toolbar tab shows only the icon; as
    // the title scrolls away the label unfolds in the toolbar (never the same word twice).
    val daysList = rememberLazyListState()
    // This week starts at the top again whenever you come back to it.
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != PAGE_DAYS &&
            daysList.firstVisibleItemIndex + daysList.firstVisibleItemScrollOffset > 0
        ) {
            daysList.scrollToItem(0)
        }
    }
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
                // All three pages stay built: going Places -> Forecast would otherwise build Outlook (ten
                // sky cards) in the middle of the swipe.
                beyondViewportPageCount = 2,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pager,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (capture) {
                            leaving.record { this@drawWithContent.drawContent() }
                            capture = false
                        }
                        drawContent()
                        // The page being left, on top, fading out (and blurring a little).
                        val f = fadeOut.value
                        val image = leavingImage
                        if (f > 0.01f && image != null) {
                            // A slide: the old page stays opaque and moves out; just a touch of blur.
                            leavingDraw.alpha = 1f
                            val b = 4.dp.toPx() * (1f - f)
                            leavingDraw.renderEffect =
                                if (b > 0.5f &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                ) {
                                    BlurEffect(b, b, TileMode.Decal)
                                } else {
                                    null
                                }
                            leavingDraw.record { drawImage(image) }
                            // Outside the moving layer (its blur would clip it): slides out the other way.
                            translate(left = -jumpDir * size.width * (1f - f)) {
                                drawLayer(leavingDraw)
                            }
                        }
                    }
                    .graphicsLayer {
                        // The page recedes a little while search or the moon card is open.
                        val k = 1f - 0.04f * maxOf(searchProgress.value, MoonCardState.progress)
                        scaleX = k
                        scaleY = k
                        // The tab jump: the arriving page slides in from the tapped side (a quarter of
                        // the width) while the old one slides out under the crossfade.
                        translationX = jumpDir * size.width * fadeOut.value
                        // The tab jump's brief blur on the arriving page.
                        val b = 5.dp.toPx() * haze.value
                        renderEffect = if (b > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BlurEffect(b, b, TileMode.Decal)
                        } else {
                            null
                        }
                    }
            ) { index ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Partly in view: a little blur that fades as the page comes fully in (read in
                            // the draw phase, so it follows the finger and reverses with it).
                            val here = pager.currentPage + pager.currentPageOffsetFraction
                            val d = abs(here - index).coerceIn(0f, 1f)
                            val b = 6.dp.toPx() * d
                            val blurOk = b > 0.5f && d < 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            renderEffect = if (blurOk) {
                                BlurEffect(b, b, TileMode.Decal)
                            } else {
                                null
                            }
                        }
                ) {
                    when (index) {
                        PAGE_DAYS -> DaysPage(location, onPage, page, daysList) {
                            // The edge blur only comes in once the plain page is mostly there (it showed
                            // over the sky as soon as a swipe started).
                            edgeFade(pager.currentPage + pager.currentPageOffsetFraction)
                        }
                        PAGE_FORECAST -> ForecastPage(
                            location = location,
                            sky = sky,
                            now = now,
                            refreshing = loading,
                            onRefresh = {
                                viewModel.updateWithUpdatingChecking(
                                    triggeredByUser = true,
                                    checkPermissions = true
                                )
                            },
                            onDetails = actions::openDetails,
                            onOpenPlaces = { navigate(PAGE_PLACES) }
                        )
                        else -> PlacesPage(
                            // The visit open in a window isn't listed until you pin it.
                            locations = locations.filterNot {
                                it.formattedId == windowId && it.formattedId in visiting
                            },
                            currentId = location?.formattedId,
                            starred = starred,
                            visiting = visiting,
                            now = now,
                            onPage = onPage,
                            // Tapping a postcard opens it as a window (no jump, no change of place).
                            onSelect = { place, bounds ->
                                windowOrigin = bounds
                                windowId = place.formattedId
                            },
                            onStar = {
                                val id = it.formattedId
                                // One star: starring moves it here; the starred place stays starred.
                                if (id in starred) return@PlacesPage
                                starred = setOf(id)
                                saveStarred(context, starred)
                                messages.show(
                                    ShellMessage(Icons.Filled.Star, context.getString(R.string.shell_now_main_place))
                                )
                                // The main place is kept, never a visit.
                                if (id in starred) Visits.unmark(context, id)
                                controller.visitsChanged++
                            },
                            onDelete = { place ->
                                val index = locations.indexOfFirst { it.formattedId == place.formattedId }
                                if (index >= 0) {
                                    viewModel.deleteLocation(index)
                                    messages.show(
                                        ShellMessage(
                                            icon = Icons.Outlined.Delete,
                                            text = context.getString(
                                                R.string.shell_place_removed,
                                                place.getPlace(context)
                                            ),
                                            action = context.getString(R.string.shell_undo),
                                            onAction = { viewModel.addLocation(place, index, context) }
                                        )
                                    )
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
                            onSettings = actions::openSettings,
                            listState = placesList,
                            pageFade = { edgeFade(pager.currentPage + pager.currentPageOffsetFraction) }
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    // The toolbar steps aside while search or a place window is open.
                    .graphicsLayer { alpha = 1f - maxOf(searchProgress.value, windowProgress.value) }
            ) {
                ShellMessageHost(messages, glass)
                ShellToolbar(
                    pager = pager,
                    sky = sky,
                    weather = SkyWeather.of(location?.weather?.current?.weatherCode),
                    glass = glass,
                    labelShown = labelShown,
                    onTab = { navigate(it) },
                    onInterrupt = { interruptNavigation() },
                    position = { toolbarPosition.value.takeIf { it >= 0f } },
                    searchEnabled = !searchOpen,
                    onSearch = {
                        searchHandoff = false
                        searchOpen = true
                    },
                    onSearchBounds = { searchOrigin = it }
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
            progress = { searchProgress.value },
            origin = searchOrigin,
            searchViewModel = searchViewModel,
            near = nearPoint,
            countryCode = (locations.firstOrNull { it.isCurrentPosition } ?: location)?.countryCode,
            onPickLocation = { pick(it) },
            onPickCity = { pick(it.toLocation()) },
            onClose = { searchOpen = false },
            onCardBounds = { searchCard = it },
            glass = glass,
            handedOff = searchHandoff && windowReady
        )
        // Above search: a city picked there grows into its window right where the search card was.
        val windowPlace = locations.firstOrNull { it.formattedId == windowId }
        if (windowPlace != null) {
            PlaceWindow(
                location = windowPlace,
                origin = windowOrigin,
                progress = { windowProgress.value },
                now = now,
                onBackProgress = { e -> scope.launch { windowProgress.snapTo(1f - 0.5f * e) } },
                onBackCancelled = { scope.launch { windowProgress.animateTo(1f, windowSpring) } },
                onClose = { closeWindow() },
                pinned = windowPlace.formattedId !in visiting,
                fadeOut = windowFade,
                weather = SkyWeather.of(windowPlace.weather?.current?.weatherCode),
                onPin = {
                    val id = windowPlace.formattedId
                    if (id in visiting) Visits.unmark(context, id) else Visits.mark(context, id)
                    controller.visitsChanged++
                },
                onDetails = {
                    viewModel.setLocation(windowPlace.formattedId)
                    actions.openDetails()
                }
            )
        }
        // The moon card, over everything (also over a postcard window), on the search card's glass.
        MoonCardOverlay(glass)
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
internal fun plainPalette(page: Color, text: Color) = SkyPalette(
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

internal class Tab(val label: Int, val icon: ImageVector)

/**
 * Floating toolbar. Each tab is an icon; the selected one unfolds to show its title. Widths, label
 * and highlight are all driven by the pager position, so they follow the finger mid-swipe and fold
 * back if the swipe reverses.
 */
@Composable
internal fun ShellToolbar(
    pager: PagerState,
    sky: SkyState,
    weather: SkyWeather,
    glass: GlassSource,
    labelShown: List<Float>,
    onTab: (Int) -> Unit,
    searchEnabled: Boolean,
    onSearch: () -> Unit,
    onSearchBounds: (Rect) -> Unit,
    // The same toolbar in a postcard window: no Places tab, and the bud (Pin) comes out of Forecast.
    withPlaces: Boolean = true,
    budIcon: ImageVector = Icons.Filled.Search,
    budDescription: String = stringResource(R.string.shell_search_elsewhere),
    budChecked: Boolean = false,
    position: () -> Float? = { null },
    // A finger on the toolbar (the slide) stops any page transition in progress.
    onInterrupt: () -> Unit = {},
) {
    val tabs = listOfNotNull(
        Tab(R.string.shell_tab_days, Icons.Outlined.CalendarMonth),
        Tab(R.string.shell_tab_forecast, conditionIcon(weather, night = sky.sunHeight < 0f)),
        if (withPlaces) Tab(R.string.shell_tab_places, Icons.Outlined.Map) else null
    )
    val collapsed = 52.dp
    val expanded = 132.dp
    val position = position() ?: (pager.currentPage + pager.currentPageOffsetFraction)
    val open = tabs.indices.map { (1f - abs(position - it)).coerceIn(0f, 1f) }
    // How far each tab's title is unfolded: selected (follows the swipe) and not already shown as the
    // page's own big title.
    val gliding = position() != null
    val shown = tabs.indices.map { i ->
        // While gliding over tabs (a tab jump), only the end tab unfolds its label.
        val passing = gliding && i != 0 && i != tabs.lastIndex
        if (passing) 0f else open[i] * labelShown[i]
    }
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

    // The search bud (agreed 2026-11-18, "gooey bud"): on Places a round search button swells out of
    // the pill's right end, held by a stretchy glass neck, and snaps free into its slot beside the
    // toolbar. Pill, neck and bubble are one glass shape. The swipe drives it; only the neck's
    // snap (and its regrowth on the way back) is a spring.
    // The bud comes out of the last tab (Places at home, Forecast in a window).
    val bud = open[tabs.lastIndex]
    val height = 52.dp
    val gap = 10.dp
    val pillWidth = starts.last() + 8.dp
    val joined = bud < 0.97f
    val neck = remember { Animatable(1f) }
    // Liquid drop (agreed 2026-11-18): free of the pill, the bubble wobbles (overshoot); taken back in,
    // the pill ripples a little as it absorbs it.
    val wobble = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(joined) {
        if (!joined) {
            launch {
                wobble.snapTo(1f)
                wobble.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow))
            }
        } else if (bud > 0.05f) {
            launch {
                ripple.snapTo(1f)
                ripple.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            }
        }
        neck.animateTo(
            if (joined) 1f else 0f,
            // Letting go: no bounce (overshooting below zero brought a sliver of neck back).
            spring(dampingRatio = if (joined) 0.5f else 1f, stiffness = Spring.StiffnessMediumLow)
        )
    }
    val density = LocalDensity.current
    val shape = with(density) {
        BudShape(budExtra = ((gap + height) * bud).toPx(), neck = neck.value, gap = gap.toPx())
    }
    Box(
        Modifier
            .width(pillWidth + (gap + height) * bud)
            .height(height)
            .graphicsLayer {
                // The ripple when it absorbs the bubble.
                scaleX = 1f + 0.03f * ripple.value
                scaleY = 1f - 0.04f * ripple.value
            }
            .glass(glass, shape, tint)
    ) {
        ToolbarTabs(
            pillWidth, open, shown, widths, starts, highlightX, highlightW, primary, onPrimary, onSurface, tabs,
            pager, onTab, onInterrupt
        )
        // It swells inside the pill first (a little bigger as it comes out), then shows its icon.
        val iconAlpha = ((bud - 0.6f) / 0.4f).coerceIn(0f, 1f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = pillWidth - height / 2 + (gap + height) * bud - height / 2)
                .size(height)
                .onGloballyPositioned { onSearchBounds(it.boundsInRoot()) }
                .clip(CircleShape)
                // Only touchable once it's out: tucked in, it sits over the Places tab, and even a
                // disabled clickable would swallow that tab's taps.
                .then(if (searchEnabled && bud > 0.9f) Modifier.clickable(onClick = onSearch) else Modifier)
        ) {
            // Push in (agreed 2026-11-18): pinning tilts the pin, jabs it down (squash, haptic) and
            // stands it up in the Forecast colour; unpinning pops it back out with a little tilt.
            val jab = remember { Animatable(0f) }
            var wasChecked by remember { mutableStateOf(budChecked) }
            val haptics = LocalHapticFeedback.current
            LaunchedEffect(budChecked) {
                if (budChecked != wasChecked) {
                    jab.snapTo(if (budChecked) 1f else -1f)
                    if (budChecked) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    jab.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                }
                wasChecked = budChecked
            }
            // White in dark mode, black in light mode (the outline or the fill carries the state).
            val tintColor by animateColorAsState(onSurface, label = "bud")
            Icon(
                budIcon,
                contentDescription = if (bud > 0.5f) budDescription else null,
                tint = tintColor,
                modifier = Modifier.graphicsLayer {
                    val j = jab.value
                    alpha = iconAlpha
                    // In: down and squashed; out: up and stretched.
                    translationY = j * 1.5.dp.toPx()
                    scaleY = 1f - 0.07f * j
                    scaleX = 1f + 0.03f * j
                    rotationZ = -8f * kotlin.math.abs(j)
                    // The bubble's own wobble right after it pops free (scale only).
                    val w = wobble.value
                    scaleX *= 1f + 0.1f * w
                    scaleY *= 1f + 0.1f * w
                }
            )
        }
    }
}

@Composable
private fun ToolbarTabs(
    pillWidth: Dp,
    open: List<Float>,
    shown: List<Float>,
    widths: List<Dp>,
    starts: List<Dp>,
    highlightX: Dp,
    highlightW: Dp,
    primary: Color,
    onPrimary: Color,
    onSurface: Color,
    tabs: List<Tab>,
    pager: PagerState,
    onTab: (Int) -> Unit,
    onInterrupt: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Slide to switch pages (like iOS 26/27): hold the pill and the selection pops out (lifts and
    // grows a little); then it follows the finger at the finger's pace, tab by tab, with the page
    // following along. Let go and it settles on the nearest page and sinks back in.
    val pop = remember { Animatable(0f) }
    Box(
        Modifier
            .width(pillWidth)
            .pointerInput(pager, tabs.size) {
                val collapsedPx = 52.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onInterrupt()
                        scope.launch {
                            pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragEnd = {
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        onTab(position.roundToInt().coerceIn(0, tabs.lastIndex))
                        scope.launch {
                            pop.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragCancel = {
                        onTab(pager.currentPage)
                        scope.launch { pop.animateTo(0f) }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val pagePx =
                            pager.layoutInfo.pageSize.toFloat().takeIf { it > 0f }
                                ?: return@detectDragGesturesAfterLongPress
                        // One page per tab's width of finger travel (the unselected tabs' width), so
                        // it moves at the finger's pace rather than racing ahead.
                        pager.dispatchRawDelta(amount.x * pagePx / (collapsedPx * 1.6f))
                    }
                )
            }
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .offset(x = highlightX)
                .width(highlightW)
                .height(44.dp)
                .graphicsLayer {
                    // Popped out while held: a bit bigger, lifted with a soft shadow.
                    val k = 1f + 0.12f * pop.value
                    scaleX = k
                    scaleY = k
                    shadowElevation = 6.dp.toPx() * pop.value
                    shape = CircleShape
                }
                .background(primary, CircleShape)
        )
        Row {
            tabs.forEachIndexed { i, tab ->
                val label = stringResource(tab.label)
                val color = lerp(onSurface, onPrimary, open[i])
                // No square ripple: the tab's content gives a little press (shrinks, springs back).
                val press = remember { MutableInteractionSource() }
                val pressed by press.collectIsPressedAsState()
                val squeeze = remember { Animatable(1f) }
                LaunchedEffect(pressed) {
                    squeeze.animateTo(
                        if (pressed) 0.86f else 1f,
                        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                    )
                }
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
                        .clickable(interactionSource = press, indication = null, role = Role.Tab) { onTab(i) }
                        .graphicsLayer {
                            scaleX = squeeze.value
                            scaleY = squeeze.value
                        }
                        .padding(horizontal = 6.dp),
                    // Icon and label centred in the tab, whatever the label's length.
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp * shown[i]))
                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            // Takes only as much width as it has unfolded, so the icon stays centred
                            // in a folded tab.
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                                )
                                layout((placeable.width * shown[i]).roundToInt(), placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                            .graphicsLayer { alpha = shown[i] }
                    )
                }
            }
        }
    }
}

private operator fun Dp.times(f: Float): Dp = Dp(value * f)

/**
 * Blurs the page behind the search card. The blur runs on a quarter-size copy of the page (16x
 * fewer pixels, and a strong blur hides the lower resolution), drawn scaled up over the sharp
 * page and faded in with [progress]. The radius stays fixed, so the GPU's work doesn't grow as
 * the card opens.
 */
private fun Modifier.searchBlur(layer: GraphicsLayer, sharp: GraphicsLayer, progress: () -> Float): Modifier =
    drawWithContent {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0.01f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawContent()
            return@drawWithContent
        }
        // The page's background is behind the pager, so the sharp page has to fade out as the blurred
        // copy fades in (otherwise the sharp text shows through the blur).
        sharp.record { this@drawWithContent.drawContent() }
        sharp.alpha = 1f - p
        drawLayer(sharp)
        val k = 4f
        val radius = 28.dp.toPx() / k
        if (layer.renderEffect == null) layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
        layer.record(IntSize((size.width / k).toInt().coerceAtLeast(1), (size.height / k).toInt().coerceAtLeast(1))) {
            scale(1f / k, 1f / k, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
        }
        layer.alpha = p
        scale(k, k, pivot = Offset.Zero) { drawLayer(layer) }
    }

/**
 * The toolbar pill plus the search bubble, as one outline. The bubble is a circle as tall as the
 * pill whose centre slides [budExtra] from the pill's right cap (hidden in it) to its
 * slot. Once they separate, a neck (two curves pinching towards the middle) joins them; it thins as
 * they part and vanishes with [neck] = 0.
 */
private class BudShape(
    private val budExtra: Float,
    private val neck: Float,
    private val gap: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = size.height / 2f
        // Measured from the size being drawn, so the outline always fits the box exactly.
        val pillWidth = size.width - budExtra
        val centre = pillWidth - r + budExtra
        // Tucked in (every page but Places): just the pill, no path operations.
        if (budExtra < 0.5f) {
            return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
        }
        val pill = Path().apply { addRoundRect(RoundRect(0f, 0f, pillWidth, size.height, CornerRadius(r, r))) }
        val ball = Path().apply { addOval(Rect(centre - r, 0f, centre + r, size.height)) }
        var path = Path.combine(PathOperation.Union, pill, ball)
        val cap = pillWidth - r
        val apart = (centre - r) - pillWidth // space between pill and bubble
        if (neck > 0.02f && apart > 0f) {
            // Attach where the circles face each other, 50 degrees above and below the axis.
            val dx = r * 0.643f
            val dy = r * 0.766f
            val thinning = (apart / gap).coerceIn(0f, 1f)
            val waist = (neck.coerceIn(0f, 1.3f) * (1.5f - 1.2f * thinning) * r).coerceAtMost(2 * dy)
            val mid = (cap + centre) / 2f
            // Control points chosen so the curve's midpoint is exactly at the waist.
            val top = r + dy - waist
            val bottom = r - dy + waist
            val bridge = Path().apply {
                moveTo(cap + dx, r - dy)
                quadraticTo(mid, top, centre - dx, r - dy)
                lineTo(centre - dx, r + dy)
                quadraticTo(mid, bottom, cap + dx, r + dy)
                close()
            }
            path = Path.combine(PathOperation.Union, path, bridge)
        }
        return Outline.Generic(path)
    }
}

/** The Forecast tab's icon follows the current weather. */
private fun conditionIcon(weather: SkyWeather, night: Boolean): ImageVector = when (weather) {
    SkyWeather.CLEAR -> if (night) Icons.Outlined.Bedtime else Icons.Outlined.WbSunny
    SkyWeather.PARTLY_CLOUDY -> if (night) Icons.Outlined.Bedtime else Icons.Outlined.WbCloudy
    SkyWeather.OVERCAST -> Icons.Outlined.Cloud
    SkyWeather.RAIN -> SolsticeIcons.RainCloud
    SkyWeather.THUNDERSTORM -> Icons.Outlined.Thunderstorm
    SkyWeather.SNOW -> Icons.Outlined.AcUnit
    SkyWeather.FOG -> Icons.Outlined.Dehaze
}

/** How strongly a plain page's edge blur shows: none until the page is ~40 % in, full when it's there. */
internal fun edgeFade(position: Float): Float =
    ((abs(position - PAGE_FORECAST) - 0.4f) / 0.6f).coerceIn(0f, 1f)
