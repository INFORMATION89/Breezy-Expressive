package io.github.information89.breezyexpressive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.data.model.WeatherCode
import io.github.information89.breezyexpressive.insight.AirLevel
import io.github.information89.breezyexpressive.insight.PrecipKind
import io.github.information89.breezyexpressive.insight.TempFeel
import io.github.information89.breezyexpressive.insight.UvLevel
import io.github.information89.breezyexpressive.ui.material.glass
import io.github.information89.breezyexpressive.ui.theme.AppType
import io.github.information89.breezyexpressive.ui.material.GlassTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/** Soft, generous corners (like the Seoul reference). */
val CardShape = RoundedCornerShape(24.dp)

/**
 * A glass panel with a plain header: a bold title and, optionally, a short muted summary on the
 * right ("Today ... clear all day").
 */
@Composable
fun WeatherCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassTheme {
        Column(
            modifier
                .fillMaxWidth()
                .glass(CardShape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                    maxLines = 1,
                )
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
            }
            content()
        }
    }
}

/** "01  NEXT HOURS" in monospace, quiet. */
@Composable
fun SectionLabel(title: String, index: String? = null, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
        if (index != null) {
            Text(index, style = AppType.voices.monoLabel, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
        }
        Text(title.uppercase(), style = AppType.voices.monoLabel, color = color)
    }
}

/** A thin horizontal rule, like a printed layout line. */
@Composable
fun Hairline(modifier: Modifier = Modifier, alpha: Float = 0.25f) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}

/** Colors that carry meaning (kept for later use: alerts, air quality). */
object DataColors {
    val rain = Color(0xFF4A9FE0)
    val storm = Color(0xFF9A7BE0)
    val snow = Color(0xFF8CC8F0)
    val sun = Color(0xFFFFB02E)
    val moon = Color(0xFFB8C4F2)
    val dry = Color(0xFF7FC4B8)

    fun uv(level: UvLevel): Color = when (level) {
        UvLevel.LOW -> Color(0xFF5CC97B)
        UvLevel.MODERATE -> Color(0xFFF2C94C)
        UvLevel.HIGH -> Color(0xFFF2994A)
        UvLevel.VERY_HIGH -> Color(0xFFEB5757)
        UvLevel.EXTREME -> Color(0xFFB36BF0)
    }

    fun precip(kind: PrecipKind?): Color = when (kind) {
        null -> dry
        PrecipKind.RAIN -> rain
        PrecipKind.STORM -> storm
        PrecipKind.SNOW, PrecipKind.SLEET, PrecipKind.HAIL -> snow
    }

    fun feel(feel: TempFeel): Color = when (feel) {
        TempFeel.FREEZING, TempFeel.COLD -> Color(0xFF8CC8F0)
        TempFeel.CHILLY, TempFeel.COOL -> Color(0xFF6FA8DC)
        TempFeel.MILD -> Color(0xFFF2C94C)
        TempFeel.WARM -> Color(0xFFFFA24C)
        TempFeel.HOT, TempFeel.VERY_HOT -> Color(0xFFFF6A4C)
    }

    fun air(level: AirLevel): Color = when (level) {
        AirLevel.GOOD -> Color(0xFF5CC97B)
        AirLevel.MODERATE -> Color(0xFFF2C94C)
        AirLevel.SENSITIVE -> Color(0xFFF2994A)
        AirLevel.UNHEALTHY -> Color(0xFFEB5757)
        AirLevel.VERY_UNHEALTHY, AirLevel.HAZARDOUS -> Color(0xFFB36BF0)
    }

    fun icon(code: WeatherCode?, isDay: Boolean, neutral: Color, night: Color): Color = when (code) {
        null, WeatherCode.CLEAR, WeatherCode.PARTLY_CLOUDY -> if (isDay) sun else night
        WeatherCode.RAIN -> rain
        WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> storm
        WeatherCode.SNOW, WeatherCode.SLEET, WeatherCode.HAIL -> snow
        else -> neutral
    }
}
