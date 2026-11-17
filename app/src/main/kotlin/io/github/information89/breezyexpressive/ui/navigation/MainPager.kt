package io.github.information89.breezyexpressive.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.ui.forecast.ForecastPage
import io.github.information89.breezyexpressive.ui.locations.LocationsPage
import io.github.information89.breezyexpressive.ui.yourday.YourDayPage
import kotlinx.coroutines.launch

enum class Page(@param:StringRes val label: Int, val icon: ImageVector) {
    FORECAST(R.string.page_forecast, Icons.Rounded.Home),
    YOUR_DAY(R.string.page_your_day, Icons.Rounded.DateRange),
    LOCATIONS(R.string.page_locations, Icons.Rounded.LocationOn),
}

/** Forecast · Your day · Locations, swipeable, with an Expressive floating toolbar. */
@Composable
fun MainPager(onOpenSettings: () -> Unit) {
    val pages = Page.entries
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    // Back from any other page returns to Forecast before leaving the app.
    BackHandler(enabled = pagerState.currentPage != Page.FORECAST.ordinal) {
        scope.launch { pagerState.animateScrollToPage(Page.FORECAST.ordinal) }
    }

    // Surface (not a plain background) so text gets the theme's content color.
    // In M2 this becomes the weather gradient + grain background.
    Surface(color = MaterialTheme.colorScheme.surface) {
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { pages[it].name },
        ) { index ->
            when (pages[index]) {
                Page.FORECAST -> ForecastPage()
                Page.YOUR_DAY -> YourDayPage()
                Page.LOCATIONS -> LocationsPage()
            }
        }

        PageToolbar(
            selected = pages[pagerState.currentPage],
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } },
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
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
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        trailingContent = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
            }
        },
    ) {
        Page.entries.forEach { page ->
            val isSelected = page == selected
            val label = stringResource(page.label)
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelect(page) },
            ) {
                // The label is only shown (and announced) on the selected page.
                Icon(page.icon, contentDescription = if (isSelected) null else label)
                AnimatedVisibility(visible = isSelected) {
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
