package io.github.information89.breezyexpressive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

/** A clear glass card with a small, quiet title. */
@Composable
fun WeatherCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.material3.Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** Colors that carry meaning: the data tints its card (PLAN.md 5.2). */
object DataColors {
    val rain = Color(0xFF4A9FE0)
    val storm = Color(0xFF9A7BE0)
    val snow = Color(0xFF8CC8F0)
    val sun = Color(0xFFFFB02E)
    val moon = Color(0xFF9AA8E0)
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

    /** Cold blue → mild gold → hot orange, for temperature curves and ranges. */
    fun temperature(celsius: Double): Color {
        val stops = listOf(0.0 to Color(0xFF8CC8F0), 12.0 to Color(0xFF6FA8DC), 20.0 to Color(0xFFF2C94C), 28.0 to Color(0xFFFFA24C), 36.0 to Color(0xFFFF5A4C))
        if (celsius <= stops.first().first) return stops.first().second
        if (celsius >= stops.last().first) return stops.last().second
        val (a, b) = stops.zipWithNext().first { celsius <= it.second.first }
        val t = ((celsius - a.first) / (b.first - a.first)).toFloat()
        return androidx.compose.ui.graphics.lerp(a.second, b.second, t)
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
