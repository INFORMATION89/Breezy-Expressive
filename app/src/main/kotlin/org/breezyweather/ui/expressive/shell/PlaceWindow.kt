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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.ui.expressive.sky.SkyBackground
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
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val paper = if (dark) Color(0xFFE6E0D2) else Color(0xFFFAF7F0)
    val page = if (dark) Color.Black else Color.White
    val onPage = if (dark) Color.White else Color.Black
    val pager = rememberPagerState(initialPage = 1) { 2 }
    val days = rememberLazyListState()

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
                shape = RectShape(rect, 6.dp.toPx() * (1f - p))
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
                val edge = 7.dp.toPx() * (1f - p)
                if (edge > 0.3f) {
                    drawRoundRect(
                        paper,
                        topLeft = rect.topLeft + Offset(edge / 2, edge / 2),
                        size = Size(rect.width - edge, rect.height - edge),
                        cornerRadius = CornerRadius(6.dp.toPx()),
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
                        topLeft = Offset(rect.right - w - 11.dp.toPx(), rect.top + 11.dp.toPx()),
                        size = Size(w, h)
                    )
                }
            }
    ) {
        // The postcard's sky is what grows: laid out full screen and scaled down onto the card.
        SkyBackground(
            state = sky,
            modifier = Modifier.fillMaxSize().graphicsLayer {
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
                .drawBehind {
                    // This week sits on the plain page, like Outlook.
                    val position = pager.currentPage + pager.currentPageOffsetFraction
                    drawRect(page, alpha = abs(position - 1f).coerceIn(0f, 1f))
                }
        ) {
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pager,
                    snapAnimationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
                ),
                modifier = Modifier.fillMaxSize()
            ) { index ->
                if (index == 0) {
                    DaysPage(location, onPage, page, days) {
                        abs(pager.currentPage + pager.currentPageOffsetFraction - 1f).coerceIn(0f, 1f)
                    }
                } else {
                    ForecastPage(
                        location = location,
                        sky = sky,
                        now = now,
                        refreshing = false,
                        onRefresh = {},
                        onDetails = onDetails,
                        onOpenPlaces = onClose
                    )
                }
            }
            // "Visiting" until you pin it.
            if (!pinned) {
                Text(
                    stringResource(R.string.shell_visiting),
                    style = MaterialTheme.typography.labelLarge,
                    color = sky.palette.text,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            // The window's toolbar: This week and Forecast, with Pin in line (keeps it in Places;
            // otherwise it's gone when you go back).
            val scope = rememberCoroutineScope()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                        .padding(4.dp)
                ) {
                    val onDays = pager.currentPage == 0
                    KeepButton(
                        checked = onDays,
                        icon = Icons.Outlined.CalendarMonth,
                        label = if (onDays) stringResource(R.string.shell_tab_days) else null,
                        onClick = { scope.launch { pager.animateScrollToPage(0) } }
                    )
                    KeepButton(
                        checked = !onDays,
                        icon = Icons.Outlined.WbSunny,
                        label = if (!onDays) stringResource(R.string.shell_tab_forecast) else null,
                        onClick = { scope.launch { pager.animateScrollToPage(1) } }
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                        .padding(4.dp)
                ) {
                    KeepButton(
                        checked = pinned,
                        icon = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        label = stringResource(if (pinned) R.string.shell_pinned else R.string.shell_pin),
                        onClick = onPin
                    )
                }
            }
        }
    }
}

@Composable
private fun KeepButton(
    checked: Boolean,
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (checked) colors.primary else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (checked) colors.onPrimary else colors.onSurface)
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (checked) colors.onPrimary else colors.onSurface
            )
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
