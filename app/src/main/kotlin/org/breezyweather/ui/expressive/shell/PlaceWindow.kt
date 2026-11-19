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

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FlightLand
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.ui.expressive.components.LocalPageGlass
import org.breezyweather.ui.expressive.components.LocalSkyGlass
import org.breezyweather.ui.expressive.components.glassSource
import org.breezyweather.ui.expressive.components.rememberGlassSource
import org.breezyweather.ui.expressive.sky.SkyBackground
import org.breezyweather.ui.expressive.sky.SkySystemBars
import org.breezyweather.ui.expressive.sky.SkyWeather
import org.breezyweather.ui.expressive.theme.Corners
import org.breezyweather.ui.expressive.theme.Sizes
import org.breezyweather.ui.expressive.theme.SolsticePalette
import org.breezyweather.ui.expressive.theme.Space
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import kotlin.math.abs

/**
 * A place opened from its postcard (agreed 2026-11-18, "card becomes the sky"): the postcard grows
 * into a full-screen window with that place's Forecast and, a swipe
 * to the left, its This week. The paper frame and stamp fade away while the sky grows into the
 * window's sky, and the forecast fades in as it grows. Back plays it in reverse and follows the
 * finger (predictive back; cancel midway to stay). The window only shows the place: it doesn't
 * change the selected or main place.
 *
 * [progress] (0 = the postcard, 1 = full screen) is read in draw only. [origin] is the
 * postcard's bounds on screen.
 */
@Composable
internal fun PlaceWindow(
    location: Location,
    origin: Rect?,
    progress: () -> Float,
    now: Long,
    onBackProgress: (Float) -> Unit,
    onBackCancelled: () -> Unit,
    onClose: () -> Unit,
    pinned: Boolean,
    fadeOut: Boolean,
    onPin: () -> Unit,
    onDetails: () -> Unit,
    weather: SkyWeather,
) {
    PredictiveBackHandler { events ->
        try {
            events.collect { onBackProgress(it.progress) }
            onClose()
        } catch (e: CancellationException) {
            onBackCancelled()
            throw e
        }
    }
    val sky = skyFor(location, now)
    // Night there: the window's own interface is dark (the main screen keeps your setting).
    val night = sky.sunHeight < 0f
    BreezyWeatherTheme(darkTheme = night || isSystemInDarkTheme()) {
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val paper = if (dark) SolsticePalette.PaperDim else SolsticePalette.Paper
        val page = if (dark) Color.Black else Color.White
        val onPage = if (dark) Color.White else Color.Black
        // The postcard's paper border, which stretches out and thins away as the window grows.
        val bezel = postcardBezel()
        val pager = rememberPagerState(initialPage = 1) { 2 }
        val days = rememberLazyListState()
        // The status and navigation bar icons follow the window once it's mostly open: the sky's
        // text colour on Forecast, the page's on This week (light icons on a dark night window).
        val covers by remember { derivedStateOf { progress() > 0.5f } }
        if (covers) {
            SkySystemBars(if (pager.currentPage == 1) sky.palette else plainPalette(page, onPage))
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    val from = origin ?: Rect(0f, 0f, size.width, size.height)
                    val rect = Rect(
                        from.left * (1f - p),
                        from.top * (1f - p),
                        from.right + (size.width - from.right) * p,
                        from.bottom + (size.height - from.bottom) * p
                    )
                    // Clipped to the growing card; corners from the postcard's to square.
                    shape = RectShape(rect, Corners.L.toPx() * (1f - p))
                    clip = true
                    // Fades in over the postcard during the first moments (their skies are laid out
                    // differently), and out again at the very end of a close.
                    // A visit you didn't keep fades away all the way down instead.
                    alpha = if (fadeOut) p else (p / 0.12f).coerceIn(0f, 1f)
                }
                .drawWithContent {
                    drawContent()
                    val p = progress().coerceIn(0f, 1f)
                    val from = origin ?: return@drawWithContent
                    val rect = Rect(
                        from.left * (1f - p),
                        from.top * (1f - p),
                        from.right + (size.width - from.right) * p,
                        from.bottom + (size.height - from.bottom) * p
                    )
                    // The paper frame stretches out with it and thins away...
                    val edge = bezel.toPx() * (1f - p)
                    if (edge > 0.3f) {
                        drawRoundRect(
                            paper,
                            topLeft = rect.topLeft + Offset(edge / 2, edge / 2),
                            size = Size(rect.width - edge, rect.height - edge),
                            cornerRadius = CornerRadius(Corners.L.toPx()),
                            style = Stroke(edge)
                        )
                    }
                    // ...and the stamp fades.
                    val stampAlpha = (1f - p / 0.35f).coerceIn(0f, 1f)
                    if (stampAlpha > 0f) {
                        val w = 46.dp.toPx()
                        val h = 52.dp.toPx()
                        drawRect(
                            paper.copy(alpha = stampAlpha),
                            topLeft = Offset(rect.right - w - bezel.toPx(), rect.top + bezel.toPx() + Space.S5.toPx()),
                            size = Size(w, h)
                        )
                    }
                }
        ) {
            // The window's own glass: its toolbar blurs the window's sky and pages, like the home toolbar.
            val windowGlass = rememberGlassSource()
            // The window's sky alone, for its Forecast cards' blur.
            val windowSky = rememberGlassSource()
            Box(Modifier.fillMaxSize().glassSource(windowGlass)) {
                // The postcard's sky is what grows: laid out full screen and scaled down onto the card.
                SkyBackground(
                    state = sky,
                    modifier = Modifier.fillMaxSize().glassSource(windowSky).graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        val from = origin ?: Rect(0f, 0f, size.width, size.height)
                        val left = from.left * (1f - p)
                        val top = from.top * (1f - p)
                        val right = from.right + (size.width - from.right) * p
                        val bottom = from.bottom + (size.height - from.bottom) * p
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = (right - left) / size.width
                        scaleY = (bottom - top) / size.height
                        translationX = left
                        translationY = top
                    }
                ) {}
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = ((progress() - 0.25f) / 0.5f).coerceIn(0f, 1f) }
                        // This week sits on the plain page, like Outlook.
                        .plainPageBackdrop(pager, skyPage = 1, page = page)
                ) {
                    HorizontalPager(
                        state = pager,
                        beyondViewportPageCount = 1,
                        flingBehavior = pageFling(pager),
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        if (index == 0) {
                            DaysPage(location, onPage, page, days) {
                                abs(pager.currentPage + pager.currentPageOffsetFraction - 1f).coerceIn(0f, 1f)
                            }
                        } else {
                            CompositionLocalProvider(
                                LocalSkyGlass provides windowSky,
                                LocalPageGlass provides windowGlass
                            ) {
                                ForecastPage(
                                    location = location,
                                    sky = sky,
                                    now = now,
                                    refreshing = false,
                                    onRefresh = {},
                                    onDetails = onDetails,
                                    onOpenPlaces = onClose,
                                    showLocalTime = false
                                )
                            }
                        }
                    }
                    // "Visiting" until you pin it, and its local time when it differs from yours.
                    val context = LocalContext.current
                    // "[landing plane] Visiting · [clock] 2:33 PM, 14 hours ahead".
                    val localTime = timeDifference(location.timeZone, now)?.let { diff ->
                        time(context, java.util.Date(now), location.timeZone) + ", " + diff
                    }
                    val ink = sky.palette.text.copy(alpha = 0.85f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = Space.S2)
                    ) {
                        Icon(
                            Icons.Outlined.FlightLand,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(Sizes.IconSmall)
                        )
                        Spacer(Modifier.width(Space.S1))
                        Text(
                            stringResource(R.string.shell_visiting),
                            style = MaterialTheme.typography.labelLarge,
                            color = ink
                        )
                        if (localTime != null) {
                            Text(" · ", style = MaterialTheme.typography.labelLarge, color = ink)
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = ink,
                                modifier = Modifier.size(Sizes.IconSmall)
                            )
                            Spacer(Modifier.width(Space.S1))
                            Text(localTime, style = MaterialTheme.typography.labelLarge, color = ink)
                        }
                    }
                }
            }
            // The home toolbar, as is: This week and Forecast, and Pin budding out of Forecast like search
            // does out of Places (pin keeps the place; otherwise it's gone when you go back).
            val scope = rememberCoroutineScope()
            ToolbarDock(alpha = { ((progress() - 0.25f) / 0.5f).coerceIn(0f, 1f) }) {
                ShellToolbar(
                    pager = pager,
                    sky = sky,
                    weather = weather,
                    glass = windowGlass,
                    labelShown = listOf(1f, 1f),
                    onTab = { scope.launch { pager.animateScrollToPage(it) } },
                    searchEnabled = true,
                    onSearch = onPin,
                    onSearchBounds = {},
                    withPlaces = false,
                    // Outline when it's a visit; filled once pinned.
                    budIcon = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    budDescription = stringResource(if (pinned) R.string.shell_pinned else R.string.shell_pin),
                    budChecked = pinned
                )
            }
        }
    }
}

/** A rounded rectangle at [rect] (screen space) with corner radius [radius] px. */
private class RectShape(private val rect: Rect, private val radius: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ) = androidx.compose.ui.graphics.Outline.Rounded(
        androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(radius, radius))
    )
}
