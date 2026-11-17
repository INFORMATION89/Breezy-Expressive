package io.github.information89.breezyexpressive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.information89.breezyexpressive.R

/*
 * Two type voices:
 *  - Data voice (sans, Roboto Flex): numbers, labels, UI. Display/headline styles use its expanded width.
 *  - Narrative voice (serif, Roboto Serif): sentences that explain the weather.
 * To try another font, change only the families below.
 */

private const val EXPANDED_WIDTH = 151f // Roboto Flex's widest setting

private fun flex(weight: Int, width: Float = 100f) = Font(
    resId = R.font.roboto_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
)

private fun serif(weight: Int) = Font(
    resId = R.font.roboto_serif,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val DataFamily = FontFamily(flex(300), flex(400), flex(500), flex(600), flex(700))
private val ExpandedFamily = FontFamily(
    flex(300, EXPANDED_WIDTH), flex(400, EXPANDED_WIDTH), flex(500, EXPANDED_WIDTH),
    flex(600, EXPANDED_WIDTH), flex(700, EXPANDED_WIDTH),
)
private val NarrativeFamily = FontFamily(serif(400), serif(500), serif(600))

private val Base = Typography()

val DataTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = ExpandedFamily),
    displayMedium = Base.displayMedium.copy(fontFamily = ExpandedFamily),
    displaySmall = Base.displaySmall.copy(fontFamily = ExpandedFamily),
    headlineLarge = Base.headlineLarge.copy(fontFamily = ExpandedFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = ExpandedFamily),
    headlineSmall = Base.headlineSmall.copy(fontFamily = ExpandedFamily),
    titleLarge = Base.titleLarge.copy(fontFamily = DataFamily),
    titleMedium = Base.titleMedium.copy(fontFamily = DataFamily),
    titleSmall = Base.titleSmall.copy(fontFamily = DataFamily),
    bodyLarge = Base.bodyLarge.copy(fontFamily = DataFamily),
    bodyMedium = Base.bodyMedium.copy(fontFamily = DataFamily),
    bodySmall = Base.bodySmall.copy(fontFamily = DataFamily),
    labelLarge = Base.labelLarge.copy(fontFamily = DataFamily),
    labelMedium = Base.labelMedium.copy(fontFamily = DataFamily),
    labelSmall = Base.labelSmall.copy(fontFamily = DataFamily),
)

/** Styles outside the Material type scale: the narrative voice and the hero temperature. */
@Immutable
data class TypeVoices(
    val heroTemperature: TextStyle,
    val narrativeLarge: TextStyle,
    val narrativeMedium: TextStyle,
    val narrativeSmall: TextStyle,
)

val DefaultTypeVoices = TypeVoices(
    heroTemperature = TextStyle(
        fontFamily = ExpandedFamily,
        fontWeight = FontWeight(400),
        fontSize = 88.sp,
        lineHeight = 88.sp,
        letterSpacing = (-0.02).em,
    ),
    narrativeLarge = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 30.sp),
    narrativeMedium = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 24.sp),
    narrativeSmall = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 20.sp),
)

val LocalTypeVoices = staticCompositionLocalOf { DefaultTypeVoices }

/** Access as `AppType.voices.narrativeLarge`, next to `MaterialTheme.typography`. */
object AppType {
    val voices: TypeVoices
        @Composable @ReadOnlyComposable get() = LocalTypeVoices.current
}
