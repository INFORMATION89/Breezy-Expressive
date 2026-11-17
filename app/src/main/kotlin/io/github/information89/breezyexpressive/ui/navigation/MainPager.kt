package io.github.information89.breezyexpressive.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import io.github.information89.breezyexpressive.ui.theme.conditionColorScheme
import androidx.compose.material3.Text
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.appContainer
import io.github.information89.breezyexpressive.ui.forecast.ForecastPage
import io.github.information89.breezyexpressive.ui.forecast.ForecastViewModel
import io.github.information89.breezyexpressive.ui.locations.LocationsPage
import io.github.information89.breezyexpressive.ui.material.GlassStrength
import io.github.information89.breezyexpressive.ui.material.LocalSky
import io.github.information89.breezyexpressive.ui.material.LocalDayGlass
import io.github.information89.breezyexpressive.ui.material.GlassTheme
import io.github.information89.breezyexpressive.ui.material.SkyState
import io.github.information89.breezyexpressive.ui.material.LocalScrollActivity
import io.github.information89.breezyexpressive.ui.material.ScrollActivity
import io.github.information89.breezyexpressive.ui.material.WeatherBackground
import io.github.information89.breezyexpressive.ui.material.WeatherMood
import io.github.information89.breezyexpressive.ui.material.SkyPreview
import io.github.information89.breezyexpressive.ui.material.MoodColors
import io.github.information89.breezyexpressive.ui.material.glass
import io.github.information89.breezyexpressive.ui.theme.LocalAppearance
import io.github.information89.breezyexpressive.ui.theme.isDarkTheme
import io.github.information89.breezyexpressive.ui.yourday.YourDayPage
import io.github.information89.breezyexpressive.ui.settings.SettingsScreen
import io.github.information89.breezyexpressive.data.store.Appearance
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Page(@param:StringRes val label: Int, val icon: ImageVector) {
    FORECAST(R.string.page_forecast, Icons.Rounded.Home),
    YOUR_DAY(R.string.page_your_day, Icons.Rounded.DateRange),
    LOCATIONS(R.string.page_locations, Icons.Rounded.LocationOn),
    SETTINGS(R.string.settings, Icons.Rounded.Settings),
}

/**
 * Forecast · Your day · Locations, swipeable, over the weather background,
 * with an Expressive floating toolbar made of glass that hides while scrolling down.
 */
@Composable
fun MainPager(
    appearance: Appearance,
    onAppearanceChange: ((Appearance) -> Appearance) -> Unit,
) {
    val pages = Page.entries
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val forecastViewModel: ForecastViewModel = viewModel { ForecastViewModel(context.appContainer.weatherRepository) }
    val forecast by forecastViewModel.state.collectAsStateWithLifecycle()

    val dark = appearance.isDarkTheme()
    val scheme = MaterialTheme.colorScheme
    // Debug previews (Settings > Debug) override the live sky and/or time of day.
    val isDay = SkyPreview.isDay.value ?: forecast.weather?.current?.isDay ?: true
    val mood = if (appearance.weatherTint) {
        WeatherMood.colors(SkyPreview.sky.value ?: forecast.insights?.now?.sky, isDay)
    } else {
        MoodColors(top = scheme.surfaceContainerHigh, bottom = scheme.surface, light = scheme.surfaceContainerHighest, bloom = scheme.primaryContainer, accent = scheme.primary)
    }

    val sky = remember { SkyState() }
    // White glass with dark text by day (only while the sky follows the weather).
    val dayGlass = appearance.weatherTint && isDay
    val scrollActivity = remember { ScrollActivity() }
    val toolbarVisibility = remember { HideOnScroll() }
    // Coming to a page always shows the toolbar again.
    LaunchedEffect(pagerState.currentPage) { toolbarVisibility.visible = true }

    // Back from any other page returns to Forecast before leaving the app.
    BackHandler(enabled = pagerState.currentPage != Page.FORECAST.ordinal) {
        scope.launch { pagerState.animateScrollToPage(Page.FORECAST.ordinal) }
    }

    // For now the whole main screen takes its colors from the conditions, not the wallpaper.
    val accent by animateColorAsState(mood.accent, tween(1200), label = "accent")
    // Vivid skies always carry white text, in light and dark theme alike (like the system weather apps).
    val conditionScheme = if (appearance.weatherTint) conditionColorScheme(accent, dark = true) else scheme
    MaterialExpressiveTheme(colorScheme = conditionScheme, motionScheme = MotionScheme.expressive(), typography = MaterialTheme.typography) {
    CompositionLocalProvider(
        LocalSky provides sky,
        LocalDayGlass provides dayGlass,
        LocalScrollActivity provides scrollActivity,
        LocalContentColor provides conditionScheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize()) {
            WeatherBackground(
                colors = mood,
                grain = appearance.grain,
                animated = appearance.animatedBackground,
                scrollActivity = scrollActivity,
                sky = sky,
                modifier = Modifier.fillMaxSize(),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollActivity)
                    .nestedScroll(toolbarVisibility),
                beyondViewportPageCount = 1,
                key = { pages[it].name },
            ) { index ->
                when (pages[index]) {
                    Page.FORECAST -> ForecastPage(forecast, onRefresh = forecastViewModel::refresh)
                    Page.YOUR_DAY -> YourDayPage(forecast)
                    Page.LOCATIONS -> LocationsPage()
                    Page.SETTINGS -> SettingsScreen(appearance, onAppearanceChange)
                }
            }

            // Soft fade under the status bar so scrolled content never collides with the clock.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 24.dp)))
                    .background(Brush.verticalGradient(listOf(mood.top.copy(alpha = 0.9f), mood.top.copy(alpha = 0f)))),
            )
          }

            // The whole toolbar (glass included) slides away while scrolling down.
            AnimatedVisibility(
                visible = toolbarVisibility.visible,
                enter = slideInVertically { it * 2 } + fadeIn(),
                exit = slideOutVertically { it * 2 } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                GlassTheme {
                PageToolbar(
                    // Live pager position (e.g. 0.4 = 40% of the way from Forecast to Your day).
                    position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                    onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } },
                    modifier = Modifier.glass(CircleShape, GlassStrength.Regular),
                )
                }
            }
        }
    }
    }
}

@Composable
private fun PageToolbar(
    position: () -> Float,
    onSelect: (Page) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = Color.Transparent),
        expandedShadowElevation = 0.dp,
        collapsedShadowElevation = 0.dp,
        shape = CircleShape,
        content = {
            Page.entries.forEach { page ->
                if (page.ordinal > 0) Spacer(Modifier.width(4.dp))
                PageTab(page, position, onClick = { onSelect(page) })
            }
        },
    )
}

/**
 * One page in the toolbar. Follows the finger: while swiping between pages the current pill
 * shrinks and fades as the next one grows, and goes back if the swipe is let go.
 * [position] is read here, so only these small items recompose while the pager moves.
 */
@Composable
private fun PageTab(page: Page, position: () -> Float, onClick: () -> Unit) {
    val selection = (1f - abs(position() - page.ordinal)).coerceIn(0f, 1f) // 1 = fully selected
    val isSelected = selection > 0.5f
    val scheme = MaterialTheme.colorScheme
    val container = scheme.primary.copy(alpha = selection)
    val content = lerp(scheme.onSurfaceVariant, scheme.onPrimary, selection)
    val label = stringResource(page.label)

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .drawBehind { drawRect(container) }
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The label is only announced on the selected page; the icon names the others.
        Icon(page.icon, contentDescription = if (isSelected) null else label, tint = content)
        if (selection > 0f) {
            Text(
                label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        // Full-size label, revealed by the selection amount (the pill grows with it).
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                        val width = (placeable.width * selection).roundToInt()
                        layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .graphicsLayer { alpha = selection }
                    .then(if (isSelected) Modifier else Modifier.clearAndSetSemantics {}),
            )
        }
    }
}

/** Hides when content scrolls down, shows when it scrolls up (only real vertical movement counts). */
private class HideOnScroll : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    var visible by androidx.compose.runtime.mutableStateOf(true)

    override fun onPostScroll(
        consumed: androidx.compose.ui.geometry.Offset,
        available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
    ): androidx.compose.ui.geometry.Offset {
        if (kotlin.math.abs(consumed.y) > kotlin.math.abs(consumed.x) && kotlin.math.abs(consumed.y) > 1f) {
            visible = consumed.y > 0
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }
}
