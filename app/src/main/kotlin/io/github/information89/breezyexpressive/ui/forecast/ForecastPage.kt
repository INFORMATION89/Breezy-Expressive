package io.github.information89.breezyexpressive.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.data.model.Daily
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.insight.Insights
import io.github.information89.breezyexpressive.insight.uvLevel
import io.github.information89.breezyexpressive.ui.components.WeatherIcon
import io.github.information89.breezyexpressive.ui.material.glass
import io.github.information89.breezyexpressive.ui.text.WeatherText
import io.github.information89.breezyexpressive.ui.text.rememberWeatherText
import io.github.information89.breezyexpressive.ui.theme.AppType
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Forecast: compact hero + cards. Words first, numbers second and smaller (PLAN.md 5.0).
 * Everything important fits on the first screen; extras (7 days, data source) are below.
 */
@Composable
fun ForecastPage(
    state: ForecastUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weather = state.weather
    val insights = state.insights
    when {
        weather == null || insights == null ->
            if (state.isRefreshing) LoadingState(modifier) else ErrorState(onRefresh, modifier)
        else -> {
            val pullState = rememberPullToRefreshState()
            val text = rememberWeatherText(ZoneId.of(weather.timeZone), state.now)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                state = pullState,
                modifier = modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding(),
                    )
                },
            ) {
                ForecastContent(state, weather, insights, text)
            }
        }
    }
}

@Composable
private fun ForecastContent(state: ForecastUiState, weather: Weather, insights: Insights, text: WeatherText) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 112.dp), // room for the floating toolbar
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Hero(state, weather, insights, text)
        HourlyCard(weather, insights, text, state.now)
        RainCard(insights, text)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            insights.uv?.let { UvCard(it, weather, text, Modifier.weight(1f)) }
            insights.sun?.let { SunCard(it, text, Modifier.weight(1f)) }
        }
        DaysCard(weather, text, state.now)
        Footer(weather, text)
    }
}

// region Hero

@Composable
private fun Hero(state: ForecastUiState, weather: Weather, insights: Insights, text: WeatherText) {
    val current = weather.current
    val today = weather.daily.lastOrNull { it.date <= state.now }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(
            state.location.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text.temperature(current?.temperature),
                style = AppType.voices.heroTemperature.copy(fontSize = 64.sp, lineHeight = 64.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text.skyTitle(insights.now.sky), style = MaterialTheme.typography.titleLarge)
                if (today != null) {
                    Text(
                        stringResource(R.string.high_low, text.temperature(today.temperatureMax), text.temperature(today.temperatureMin)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text.summary(insights.summary),
            style = AppType.voices.narrativeMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (state.error != null) {
            Text(
                stringResource(R.string.stale_weather, text.time(weather.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// endregion

// region Cards

@Composable
private fun ForecastCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier
            .fillMaxWidth()
            .glass(shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun HourlyCard(weather: Weather, insights: Insights, text: WeatherText, now: Long) {
    val hours = remember(weather, now) { weather.hourly.filter { it.time + 3600 > now }.take(24) }
    ForecastCard(stringResource(R.string.card_next_hours)) {
        Text(text.hourlyHeadline(insights.hourly), style = AppType.voices.narrativeSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(hours, key = { it.time }) { hour ->
                HourItem(hour, if (hour === hours.first()) stringResource(R.string.time_now) else text.hourLabel(hour.time), text)
            }
        }
    }
}

@Composable
private fun HourItem(hour: Hourly, label: String, text: WeatherText) {
    Column(
        Modifier.width(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        WeatherIcon(hour.weatherCode, hour.isDay)
        Text(text.temperature(hour.temperature), style = MaterialTheme.typography.titleMedium)
        val chance = hour.precipitationProbability ?: 0
        Text(
            if (chance >= 20) "$chance%" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4A9FE0),
        )
    }
}

@Composable
private fun RainCard(insights: Insights, text: WeatherText) {
    val rain = insights.rain
    val barColor = Color(0xFF4A9FE0)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    ForecastCard(stringResource(R.string.card_precipitation)) {
        Text(text.rainHeadline(rain), style = MaterialTheme.typography.headlineSmall)
        Text(text.rainDetail(rain), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rain.bars.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                val bars = rain.bars
                val gap = 6.dp.toPx()
                val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
                bars.forEachIndexed { i, bar ->
                    val x = i * (barWidth + gap)
                    drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                    // Proportional height, so 5% never looks like 40%; tiny chances get a thin sliver.
                    val chance = bar.probability / 100f
                    val h = (size.height * chance).coerceAtLeast(if (chance > 0f) 4.dp.toPx() else 0f)
                    if (h > 0f) {
                        drawRoundRect(barColor, Offset(x, size.height - h), Size(barWidth, h), CornerRadius(minOf(barWidth, h) / 2))
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                val labels = listOf(
                    stringResource(R.string.time_now),
                    text.hourLabel(rain.bars[rain.bars.size / 2].time),
                    text.hourLabel(rain.bars.last().time),
                )
                labels.forEachIndexed { i, label ->
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
}

@Composable
private fun UvCard(uv: io.github.information89.breezyexpressive.insight.UvOutlook, weather: Weather, text: WeatherText, modifier: Modifier) {
    val isDay = weather.current?.isDay ?: true
    val index = weather.current?.uvIndex ?: 0.0
    ForecastCard(stringResource(R.string.card_uv), modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text.uv(uvLevel(index)),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Expressive shape badge carries the number, secondary to the word.
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {}
                Text(
                    "${index.roundToInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Text(text.uvAdvice(uv, isDay), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SunCard(sun: io.github.information89.breezyexpressive.insight.SunInfo, text: WeatherText, modifier: Modifier) {
    val arcColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val sunColor = Color(0xFFFFB02E)
    ForecastCard(stringResource(R.string.card_sun), modifier) {
        Text(text.sunTitle(sun), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            // Half-circle arc that always fits inside the canvas, dot included.
            val stroke = 3.dp.toPx()
            val dot = 7.dp.toPx()
            val r = minOf(size.width / 2, size.height) - dot
            val center = Offset(size.width / 2, size.height - stroke)
            drawArc(
                arcColor, 180f, 180f, useCenter = false,
                topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            sun.dayProgress?.let { progress ->
                val angle = (180f + 180f * progress) * PI / 180
                drawCircle(sunColor, dot, Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
            }
        }
        Text(text.sunDetail(sun), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DaysCard(weather: Weather, text: WeatherText, now: Long) {
    val days = remember(weather, now) { weather.daily.filter { it.date + 86_400 > now }.take(7) }
    if (days.isEmpty()) return
    val weekMin = days.mapNotNull { it.temperatureMin }.minOrNull() ?: return
    val weekMax = days.mapNotNull { it.temperatureMax }.maxOrNull() ?: return
    ForecastCard(stringResource(R.string.card_days)) {
        days.forEach { day -> DayRow(day, text, weekMin, weekMax) }
    }
}

@Composable
private fun DayRow(day: Daily, text: WeatherText, weekMin: Double, weekMax: Double) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text.dayName(day.date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(56.dp))
        WeatherIcon(day.weatherCode, isDay = true, size = 22.dp)
        val chance = day.precipitationProbabilityMax ?: 0
        Text(
            if (chance >= 30) "$chance%" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4A9FE0),
            modifier = Modifier.width(40.dp).padding(start = 4.dp),
        )
        Text(
            text.temperature(day.temperatureMin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
        Canvas(
            Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp),
        ) {
            val span = (weekMax - weekMin).coerceAtLeast(1.0)
            val start = (((day.temperatureMin ?: weekMin) - weekMin) / span).toFloat() * size.width
            val end = (((day.temperatureMax ?: weekMax) - weekMin) / span).toFloat() * size.width
            drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2))
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF6FB6E8), Color(0xFFFFB02E), Color(0xFFFF7A59)), startX = 0f, endX = size.width),
                topLeft = Offset(start, 0f),
                size = Size((end - start).coerceAtLeast(size.height), size.height),
                cornerRadius = CornerRadius(size.height / 2),
            )
        }
        Text(text.temperature(day.temperatureMax), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun Footer(weather: Weather, text: WeatherText) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.data_source, "Open-Meteo"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text.coverage(weather),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

// region States

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.loading_weather), style = AppType.voices.narrativeMedium)
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.error_no_weather), style = AppType.voices.narrativeMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

// endregion
