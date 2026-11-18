/*
 * This file is part of Breezy Expressive, a fork of Breezy Weather.
 *
 * Breezy Expressive is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Expressive is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Expressive. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.expressive.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import org.breezyweather.R

/**
 * A settings icon inside its own colored Expressive shape, like Android 16 Settings.
 * Each known category gets a fixed hue, rendered at the tone of the current (wallpaper)
 * scheme's primary container so it fits light/dark and any palette; others use primary.
 */
@Composable
fun IconBadge(@DrawableRes iconId: Int, modifier: Modifier = Modifier) {
    val style = badgeStyles[iconId]
    val scheme = MaterialTheme.colorScheme
    val (container, content) = remember(iconId, scheme.primaryContainer, scheme.onPrimaryContainer) {
        if (style == null) {
            scheme.primaryContainer to scheme.onPrimaryContainer
        } else {
            withHue(scheme.primaryContainer, style.hue) to withHue(scheme.onPrimaryContainer, style.hue)
        }
    }
    val shape = (style?.shape ?: MaterialShapes.Circle).toShape()
    Box(
        modifier = modifier.size(40.dp).background(container, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
    }
}

private class BadgeStyle(val hue: Float, val shape: androidx.graphics.shapes.RoundedPolygon)

private val badgeStyles = mapOf(
    R.drawable.ic_sync to BadgeStyle(210f, MaterialShapes.Circle),
    R.drawable.ic_palette to BadgeStyle(320f, MaterialShapes.Cookie4Sided),
    R.drawable.ic_home to BadgeStyle(110f, MaterialShapes.Square),
    R.drawable.ic_notifications to BadgeStyle(35f, MaterialShapes.Sunny),
    R.drawable.ic_widgets to BadgeStyle(265f, MaterialShapes.Clover4Leaf),
    R.drawable.ic_location to BadgeStyle(170f, MaterialShapes.Circle),
    R.drawable.ic_factory to BadgeStyle(15f, MaterialShapes.Cookie6Sided),
    R.drawable.ic_bug_report to BadgeStyle(0f, MaterialShapes.Square)
)

/** Same saturation and lightness as [color], with the given hue. */
private fun withHue(color: Color, hue: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[0] = hue
    hsl[1] = hsl[1].coerceAtLeast(0.35f)
    return Color(ColorUtils.HSLToColor(hsl))
}
