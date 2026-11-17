package io.github.information89.breezyexpressive.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.information89.breezyexpressive.ui.theme.AppType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Layout mock (sample data): compact hero + cards.
 * Words first, numbers second and smaller (see PLAN.md 5.0).
 */
@Composable
fun ForecastPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 112.dp), // room for the floating toolbar
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Hero()
        HourlyCard()
        RainCard()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UvCard(Modifier.weight(1f))
            SunCard(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Hero() {
    val s = ForecastSample
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(
            s.location,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.temperature,
                style = AppType.voices.heroTemperature.copy(fontSize = 64.sp, lineHeight = 64.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(s.condition, style = MaterialTheme.typography.titleLarge)
                Text(
                    s.highLow,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(s.summary, style = AppType.voices.narrativeMedium)
    }
}

@Composable
private fun ForecastCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun HourlyCard() {
    val s = ForecastSample
    ForecastCard("Next hours") {
        Text(s.hourlyHeadline, style = AppType.voices.narrativeSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(s.hours) { hour ->
                Column(
                    Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(hour.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(hour.icon, fontSize = 22.sp)
                    Text("${hour.temp}°", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hour.rainChance >= 10) "${hour.rainChance}%" else " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RainCard() {
    val s = ForecastSample
    val barColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ForecastCard("Precipitation") {
        Text(s.rainHeadline, style = MaterialTheme.typography.headlineSmall)
        Text(s.rainDetail, style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            val chances = s.hours.map { it.rainChance / 100f }
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * (chances.size - 1)) / chances.size
            chances.forEachIndexed { i, chance ->
                val x = i * (barWidth + gap)
                drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                // Proportional height, so 5% never looks like 40%. Tiny chances get a thin sliver.
                val h = (size.height * chance).coerceAtLeast(if (chance > 0f) 4.dp.toPx() else 0f)
                val radius = CornerRadius(minOf(barWidth, h) / 2)
                if (h > 0f) drawRoundRect(barColor, Offset(x, size.height - h), Size(barWidth, h), radius)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("Now", "6 pm", "10 pm").forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (i) { 0 -> TextAlign.Start; 1 -> TextAlign.Center; else -> TextAlign.End },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UvCard(modifier: Modifier = Modifier) {
    val s = ForecastSample
    ForecastCard("UV", modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.uvWord, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            // Expressive shape badge carries the number, secondary to the word.
            Box(
                Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {}
                Text(
                    "${s.uvIndex}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Text(s.uvAdvice, style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SunCard(modifier: Modifier = Modifier) {
    val s = ForecastSample
    val arcColor = MaterialTheme.colorScheme.outlineVariant
    val sunColor = MaterialTheme.colorScheme.primary
    ForecastCard("Sun", modifier) {
        Text("Sunset ${s.sunsetTime}", style = MaterialTheme.typography.titleMedium)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            // Half-circle arc that always fits inside the canvas, dot included.
            val stroke = 3.dp.toPx()
            val dot = 7.dp.toPx()
            val r = minOf(size.width / 2, size.height) - dot
            val center = Offset(size.width / 2, size.height - stroke)
            val sweep = 180f
            val start = 180f
            drawArc(
                arcColor, start, sweep, useCenter = false,
                topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val angle = (start + sweep * s.dayProgress) * PI / 180
            drawCircle(sunColor, dot, Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
        }
        Text(
            "${s.sunDetail}\nSunrise ${s.sunriseTime}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
