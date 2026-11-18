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

package org.breezyweather.ui.expressive.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.breezyweather.R

/**
 * Google Sans Flex (SIL OFL 1.1), trimmed to the weight (300–900) and optical size (14–72) axes.
 * Each family pins one optical size, so big text uses the display cut and small text the text cut.
 * Below Android 8 variable axes are ignored and the default instance is used.
 */
private val weights = listOf(300, 400, 500, 600, 700, 800, 900)

private fun googleSansFlex(opticalSize: Float) = FontFamily(
    weights.map { w ->
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.Setting("opsz", opticalSize)
            )
        )
    }
)

private val Display = googleSansFlex(48f)
private val Headline = googleSansFlex(28f)
private val Title = googleSansFlex(20f)
private val Text = googleSansFlex(14f)

private fun TextStyle.with(family: FontFamily) = copy(fontFamily = family)

/** Material 3 Expressive type scale (sizes and weights unchanged) set in Google Sans Flex. */
val ExpressiveTypography: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.with(Display),
        displayMedium = t.displayMedium.with(Display),
        displaySmall = t.displaySmall.with(Display),
        headlineLarge = t.headlineLarge.with(Headline),
        headlineMedium = t.headlineMedium.with(Headline),
        headlineSmall = t.headlineSmall.with(Headline),
        titleLarge = t.titleLarge.with(Title),
        titleMedium = t.titleMedium.with(Text),
        titleSmall = t.titleSmall.with(Text),
        bodyLarge = t.bodyLarge.with(Text),
        bodyMedium = t.bodyMedium.with(Text),
        bodySmall = t.bodySmall.with(Text),
        labelLarge = t.labelLarge.with(Text),
        labelMedium = t.labelMedium.with(Text),
        labelSmall = t.labelSmall.with(Text),
        displayLargeEmphasized = t.displayLargeEmphasized.with(Display),
        displayMediumEmphasized = t.displayMediumEmphasized.with(Display),
        displaySmallEmphasized = t.displaySmallEmphasized.with(Display),
        headlineLargeEmphasized = t.headlineLargeEmphasized.with(Headline),
        headlineMediumEmphasized = t.headlineMediumEmphasized.with(Headline),
        headlineSmallEmphasized = t.headlineSmallEmphasized.with(Headline),
        titleLargeEmphasized = t.titleLargeEmphasized.with(Title),
        titleMediumEmphasized = t.titleMediumEmphasized.with(Text),
        titleSmallEmphasized = t.titleSmallEmphasized.with(Text),
        bodyLargeEmphasized = t.bodyLargeEmphasized.with(Text),
        bodyMediumEmphasized = t.bodyMediumEmphasized.with(Text),
        bodySmallEmphasized = t.bodySmallEmphasized.with(Text),
        labelLargeEmphasized = t.labelLargeEmphasized.with(Text),
        labelMediumEmphasized = t.labelMediumEmphasized.with(Text),
        labelSmallEmphasized = t.labelSmallEmphasized.with(Text)
    )
}
