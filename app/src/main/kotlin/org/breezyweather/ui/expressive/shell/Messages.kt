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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.ui.expressive.components.GlassSource
import org.breezyweather.ui.expressive.components.glass
import kotlin.math.abs

/** A short, icon-led message: "[compass] Visiting Tokyo", "[bin] Guadalajara removed · Undo". */
class ShellMessage(
    val icon: ImageVector,
    val text: String,
    val action: String? = null,
    val onAction: () -> Unit = {},
) {
    /** How long it stays: a bit longer when there's something to undo. */
    internal val millis: Long get() = if (action != null) 5_000L else 3_000L
}

/** Holds the message on screen. A new message replaces the current one. */
@Stable
class ShellMessages {
    internal var current by mutableStateOf<ShellMessage?>(null)

    fun show(message: ShellMessage) {
        current = message
    }
}

/**
 * The message, agreed 2026-11-18 ("rise and swipe"): it rises from behind the toolbar on a soft
 * spring and, after a few seconds, sinks back by itself. Swipe it sideways to dismiss: it follows
 * the finger with a slight tilt and flies off past a point, or springs back. No close button; Undo
 * is a compact action where it applies. Every motion can be caught mid-way.
 */
@Composable
fun ShellMessageHost(messages: ShellMessages, glass: GlassSource, modifier: Modifier = Modifier) {
    val message = messages.current ?: return
    val scope = rememberCoroutineScope()
    val rise = remember(message) { Animatable(0f) } // 0 = tucked behind the toolbar, 1 = shown
    val slide = remember(message) { Animatable(0f) } // sideways, in px
    var width by remember { mutableStateOf(1) }
    var holding by remember(message) { mutableStateOf(false) }
    val soft = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

    suspend fun sinkAndClear() {
        rise.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
        if (messages.current === message) messages.current = null
    }

    // Holding it (mid-swipe) pauses the timer; letting go restarts it.
    LaunchedEffect(message, holding) {
        rise.animateTo(1f, soft)
        if (holding) return@LaunchedEffect
        delay(message.millis)
        sinkAndClear()
    }

    // Cut along the toolbar's top edge (this box's bottom), so a sinking message slips behind the
    // toolbar instead of showing beside or under it. Only the bottom is cut: a sideways swipe can
    // still fly the message off past the sides.
    Box(
        modifier.drawWithContent {
            val big = size.width * 4f
            clipRect(left = -big, top = -big, right = size.width + big, bottom = size.height) {
                this@drawWithContent.drawContent()
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 10.dp)
                .widthIn(max = 420.dp)
                .heightIn(min = 48.dp)
                .onSizeChanged { width = it.width.coerceAtLeast(1) }
                .graphicsLayer {
                    val r = rise.value
                    // Rises out from behind the toolbar (it sits below; the toolbar draws on top).
                    translationY = (1f - r) * 56.dp.toPx()
                    val x = slide.value
                    translationX = x
                    rotationZ = x / width * 8f
                    alpha = (r * (1f - abs(x) / width)).coerceIn(0f, 1f)
                    scaleX = 0.92f + 0.08f * r
                    scaleY = 0.92f + 0.08f * r
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> scope.launch { slide.snapTo(slide.value + delta) } },
                    onDragStarted = { holding = true },
                    onDragStopped = { velocity ->
                        holding = false
                        val far = abs(slide.value) > width * 0.35f || abs(velocity) > 1200f
                        if (far) {
                            val dir = if (slide.value + velocity * 0.1f >= 0f) 1f else -1f
                            slide.animateTo(dir * width * 1.3f, spring(stiffness = Spring.StiffnessMediumLow), velocity)
                            if (messages.current === message) messages.current = null
                        } else {
                            slide.animateTo(0f, soft, velocity)
                        }
                    }
                )
                .glass(glass, CircleShape, MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(start = 16.dp, end = if (message.action != null) 6.dp else 20.dp)
        ) {
            Icon(
                message.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(vertical = 12.dp)
            )
            message.action?.let { label ->
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    message.onAction()
                    scope.launch { sinkAndClear() }
                }) { Text(label) }
            }
        }
    }
}

/** An icon for upstream's plain-text messages, picked from what they say. */
fun messageIcon(context: Context, text: String): ImageVector = when (text) {
    context.getString(R.string.location_message_added) -> Icons.Outlined.AddLocationAlt
    context.getString(R.string.location_message_updated) -> Icons.Outlined.Check
    context.getString(R.string.location_message_deleted) -> Icons.Outlined.Delete
    context.getString(R.string.message_please_wait_refresh) -> Icons.Outlined.HourglassEmpty
    context.getString(R.string.message_multiple_refresh_errors) -> Icons.Outlined.CloudOff
    else -> Icons.Outlined.Info
}
