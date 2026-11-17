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
 *  - Data voice (sans, Roboto Flex): headings in a heavy, tightly spaced grotesk; body and labels regular.
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

private fun mono(weight: Int) = Font(
    resId = R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val MonoFamily = FontFamily(mono(400), mono(500), mono(600))

private val Base = Typography()

val DataTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    displayMedium = Base.displayMedium.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    displaySmall = Base.displaySmall.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    headlineLarge = Base.headlineLarge.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    headlineMedium = Base.headlineMedium.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    headlineSmall = Base.headlineSmall.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.03).em),
    titleLarge = Base.titleLarge.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.01).em),
    titleMedium = Base.titleMedium.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.01).em),
    titleSmall = Base.titleSmall.copy(fontFamily = DataFamily, fontWeight = FontWeight(700), letterSpacing = (-0.01).em),
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
    /** Heavy grotesk for big statements (condition, page titles). */
    val statement: TextStyle,
    /** Monospace: small uppercase labels and section indexes. */
    val monoLabel: TextStyle,
    /** Monospace: data (times, percentages, coordinates). */
    val monoData: TextStyle,
)

val DefaultTypeVoices = TypeVoices(
    heroTemperature = TextStyle(
        fontFamily = DataFamily,
        fontWeight = FontWeight(600),
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-0.05).em,
    ),
    narrativeLarge = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 30.sp),
    narrativeMedium = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 24.sp),
    narrativeSmall = TextStyle(fontFamily = NarrativeFamily, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 20.sp),
    statement = TextStyle(fontFamily = DataFamily, fontWeight = FontWeight(700), fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.03).em),
    monoLabel = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight(500), fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.08.em),
    monoData = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp),
)

val LocalTypeVoices = staticCompositionLocalOf { DefaultTypeVoices }

/** Access as `AppType.voices.narrativeLarge`, next to `MaterialTheme.typography`. */
object AppType {
    val voices: TypeVoices
        @Composable @ReadOnlyComposable get() = LocalTypeVoices.current
}
