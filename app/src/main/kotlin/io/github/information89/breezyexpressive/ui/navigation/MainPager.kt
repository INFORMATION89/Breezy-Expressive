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
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.appContainer
import io.github.information89.breezyexpressive.ui.forecast.ForecastPage
import io.github.information89.breezyexpressive.ui.forecast.ForecastViewModel
import io.github.information89.breezyexpressive.ui.locations.LocationsPage
import io.github.information89.breezyexpressive.ui.material.GlassStrength
import io.github.information89.breezyexpressive.ui.material.LocalGlassSource
import io.github.information89.breezyexpressive.ui.material.LocalScrollActivity
import io.github.information89.breezyexpressive.ui.material.ScrollActivity
import io.github.information89.breezyexpressive.ui.material.WeatherBackground
import io.github.information89.breezyexpressive.ui.material.WeatherMood
import io.github.information89.breezyexpressive.ui.material.MoodColors
import io.github.information89.breezyexpressive.ui.material.glass
import io.github.information89.breezyexpressive.ui.theme.LocalAppearance
import io.github.information89.breezyexpressive.ui.theme.isDarkTheme
import io.github.information89.breezyexpressive.ui.yourday.YourDayPage
import kotlinx.coroutines.launch

enum class Page(@param:StringRes val label: Int, val icon: ImageVector) {
    FORECAST(R.string.page_forecast, Icons.Rounded.Home),
    YOUR_DAY(R.string.page_your_day, Icons.Rounded.DateRange),
    LOCATIONS(R.string.page_locations, Icons.Rounded.LocationOn),
}

/**
 * Forecast · Your day · Locations, swipeable, over the weather background,
 * with an Expressive floating toolbar made of glass that hides while scrolling down.
 */
@Composable
fun MainPager(onOpenSettings: () -> Unit) {
    val pages = Page.entries
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val forecastViewModel: ForecastViewModel = viewModel { ForecastViewModel(context.appContainer.weatherRepository) }
    val forecast by forecastViewModel.state.collectAsStateWithLifecycle()

    val appearance = LocalAppearance.current
    val dark = appearance.isDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val mood = if (appearance.weatherTint) {
        WeatherMood.colors(forecast.insights?.now?.sky, forecast.weather?.current?.isDay ?: true, dark)
    } else {
        MoodColors(core = scheme.primaryContainer, mid = scheme.surfaceContainerHigh, edge = scheme.surface)
    }

    val hazeState = rememberHazeState()
    // The toolbar blurs everything under it (background and page content), cards blur only the background.
    val toolbarHaze = rememberHazeState()
    val scrollActivity = remember { ScrollActivity() }
    val toolbarVisibility = remember { HideOnScroll() }
    // Coming to a page always shows the toolbar again.
    LaunchedEffect(pagerState.currentPage) { toolbarVisibility.visible = true }

    // Back from any other page returns to Forecast before leaving the app.
    BackHandler(enabled = pagerState.currentPage != Page.FORECAST.ordinal) {
        scope.launch { pagerState.animateScrollToPage(Page.FORECAST.ordinal) }
    }

    CompositionLocalProvider(
        LocalGlassSource provides hazeState,
        LocalScrollActivity provides scrollActivity,
        LocalContentColor provides scheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize().hazeSource(toolbarHaze)) {
            WeatherBackground(
                colors = mood,
                grain = appearance.grain,
                animated = appearance.animatedBackground,
                scrollActivity = scrollActivity,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
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
                }
            }

            // Soft fade under the status bar so scrolled content never collides with the clock.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 24.dp)))
                    .background(Brush.verticalGradient(listOf(lerp(mood.mid, mood.edge, 0.6f).copy(alpha = 0.95f), lerp(mood.mid, mood.edge, 0.6f).copy(alpha = 0f)))),
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
                CompositionLocalProvider(LocalGlassSource provides toolbarHaze) {
                    PageToolbar(
                        selected = pages[pagerState.currentPage],
                        onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } },
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.glass(CircleShape, GlassStrength.Regular),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageToolbar(
    selected: Page,
    onSelect: (Page) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = Color.Transparent),
        expandedShadowElevation = 0.dp,
        collapsedShadowElevation = 0.dp,
        trailingContent = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        shape = CircleShape,
        content = {
            Page.entries.forEach { page ->
                val isSelected = page == selected
                val label = stringResource(page.label)
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onSelect(page) },
                    colors = ToggleButtonDefaults.colors(containerColor = Color.Transparent),
                ) {
                    // The label is only shown (and announced) on the selected page.
                    Icon(page.icon, contentDescription = if (isSelected) null else label)
                    AnimatedVisibility(visible = isSelected) {
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        },
    )
}

/** Hides on scroll down, shows on scroll up (only vertical scrolling counts). */
private class HideOnScroll : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    var visible by androidx.compose.runtime.mutableStateOf(true)

    override fun onPreScroll(
        available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
    ): androidx.compose.ui.geometry.Offset {
        if (kotlin.math.abs(available.y) > kotlin.math.abs(available.x) && kotlin.math.abs(available.y) > 4f) {
            visible = available.y > 0
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }
}
