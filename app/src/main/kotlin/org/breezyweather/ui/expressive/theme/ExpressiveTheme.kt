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

package org.breezyweather.ui.expressive.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import org.breezyweather.R

/**
 * Solstice's Compose theme: Material 3 Expressive with wallpaper (dynamic) color,
 * spring-based expressive motion, larger corners and Google Sans Flex.
 *
 * Motion rule for everything built on this theme: animate with the theme's springs
 * (`MaterialTheme.motionScheme`), so animations can be interrupted and retargeted mid-flight.
 */
@Composable
fun ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = expressiveColorScheme(darkTheme, dynamicColor),
        motionScheme = SolsticeMotionScheme,
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content
    )
}

/** Larger, softer corners than baseline Material 3. Buttons are pills by default in Expressive. */
val ExpressiveShapes = Shapes(
    extraSmall = CornerShapes.XS,
    small = CornerShapes.S,
    medium = CornerShapes.M,
    large = CornerShapes.L,
    extraLarge = CornerShapes.XL,
    largeIncreased = CornerShapes.LIncreased,
    extraLargeIncreased = CornerShapes.XLIncreased,
    extraExtraLarge = CornerShapes.XXL
)

@Composable
private fun expressiveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return fallbackColorScheme(darkTheme)
}

/**
 * Sky-blue fallback (no dynamic color, or Android < 12). Uses the same palette as the View theme
 * (`md_theme_*` in values/ and values-night/), so Compose and View screens match; the surface
 * container roles, which that palette lacks, are blended from surface and primary.
 */
@Composable
private fun fallbackColorScheme(darkTheme: Boolean): ColorScheme {
    val primary = colorResource(R.color.md_theme_primary)
    val surface = colorResource(R.color.md_theme_surface)
    val onSurface = colorResource(R.color.md_theme_onSurface)
    val onSurfaceVariant = colorResource(R.color.md_theme_onSurfaceVariant)
    fun tint(f: Float) = lerp(surface, primary, f)
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = colorResource(R.color.md_theme_onPrimary),
        primaryContainer = colorResource(R.color.md_theme_primaryContainer),
        onPrimaryContainer = colorResource(R.color.md_theme_onPrimaryContainer),
        inversePrimary = colorResource(R.color.md_theme_inversePrimary),
        secondary = colorResource(R.color.md_theme_secondary),
        onSecondary = colorResource(R.color.md_theme_onSecondary),
        secondaryContainer = colorResource(R.color.md_theme_secondaryContainer),
        onSecondaryContainer = colorResource(R.color.md_theme_onSecondaryContainer),
        tertiary = colorResource(R.color.md_theme_tertiary),
        onTertiary = colorResource(R.color.md_theme_onTertiary),
        tertiaryContainer = colorResource(R.color.md_theme_tertiaryContainer),
        onTertiaryContainer = colorResource(R.color.md_theme_onTertiaryContainer),
        error = colorResource(R.color.md_theme_error),
        onError = colorResource(R.color.md_theme_onError),
        errorContainer = colorResource(R.color.md_theme_errorContainer),
        onErrorContainer = colorResource(R.color.md_theme_onErrorContainer),
        background = colorResource(R.color.md_theme_background),
        onBackground = colorResource(R.color.md_theme_onBackground),
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = colorResource(R.color.md_theme_surfaceVariant),
        onSurfaceVariant = onSurfaceVariant,
        inverseSurface = colorResource(R.color.md_theme_inverseSurface),
        inverseOnSurface = colorResource(R.color.md_theme_inverseOnSurface),
        surfaceTint = primary,
        outline = lerp(onSurfaceVariant, surface, 0.35f),
        outlineVariant = lerp(onSurfaceVariant, surface, 0.7f),
        surfaceContainerLowest = if (darkTheme) lerp(surface, Color.Black, 0.3f) else Color.White,
        surfaceContainerLow = tint(0.04f),
        surfaceContainer = tint(0.07f),
        surfaceContainerHigh = tint(0.10f),
        surfaceContainerHighest = tint(0.13f),
        surfaceBright = if (darkTheme) tint(0.16f) else surface,
        surfaceDim = if (darkTheme) lerp(surface, Color.Black, 0.2f) else lerp(surface, onSurface, 0.1f)
    )
}
