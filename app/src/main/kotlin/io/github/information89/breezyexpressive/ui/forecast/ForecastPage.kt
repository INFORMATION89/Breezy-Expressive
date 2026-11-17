package io.github.information89.breezyexpressive.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.insight.Insights
import io.github.information89.breezyexpressive.insight.RainOutlook
import io.github.information89.breezyexpressive.insight.SunInfo
import io.github.information89.breezyexpressive.insight.UvOutlook
import io.github.information89.breezyexpressive.insight.uvLevel
import io.github.information89.breezyexpressive.ui.components.DataColors
import io.github.information89.breezyexpressive.ui.components.WeatherCard
import io.github.information89.breezyexpressive.ui.components.weatherIconRes
import io.github.information89.breezyexpressive.ui.components.weatherIconTint
import io.github.information89.breezyexpressive.ui.text.WeatherText
import io.github.information89.breezyexpressive.ui.text.rememberWeatherText
import io.github.information89.breezyexpressive.ui.theme.AppType
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Forecast: compact hero + four cards, all on the first screen.
 * Words first, numbers second and smaller (PLAN.md 5.0); each card glows in its data's color.
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp)
                        .padding(top = 12.dp, bottom = 104.dp), // room for the floating toolbar
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Hero(state, weather, insights, text)
                    TimelineCard(weather, insights, text, state.now)
                    RainCard(insights, text)
                    // Same height for both small cards.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        insights.uv?.let { UvCard(it, weather, text, Modifier.weight(1f).fillMaxHeight()) }
                        insights.sun?.let { SunCard(it, text, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
        }
    }
}

// region Hero

/** Words on the left (where, what, the sentence), the big temperature on the right. */
@Composable
private fun Hero(state: ForecastUiState, weather: Weather, insights: Insights, text: WeatherText) {
    val current = weather.current
    val today = weather.daily.lastOrNull { it.date <= state.now }
    Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                state.location.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text.skyTitle(insights.now.sky), style = MaterialTheme.typography.titleLarge)
            if (today != null) {
                Text(
                    stringResource(R.string.high_low, text.temperature(today.temperatureMax), text.temperature(today.temperatureMin)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
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
        Spacer(Modifier.width(12.dp))
        Text(
            text.temperature(current?.temperature),
            style = AppType.voices.heroTemperature,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// endregion

// region Next hours timeline

/**
 * The next 12 hours as a timeline: a temperature line colored from cool to warm,
 * rain drawn as a band underneath, sunrise/sunset marked, icons and times every 3 hours.
 */
@Composable
private fun TimelineCard(weather: Weather, insights: Insights, text: WeatherText, now: Long) {
    val hours = remember(weather, now) { weather.hourly.filter { it.time + 3600 > now }.take(13) }
    if (hours.size < 2) return
    val labeled = hours.indices.filter { it % 3 == 0 }

    val measurer = rememberTextMeasurer()
    val timeStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val tempStyle = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface)
    val nowLabel = stringResource(R.string.time_now)
    val icons = labeled.map { painterResource(weatherIconRes(hours[it].weatherCode, hours[it].isDay)) }
    val iconTints = labeled.map { weatherIconTint(hours[it].weatherCode, hours[it].isDay) }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val marker = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val sunEvents = remember(weather, now) {
        weather.daily.flatMap { listOfNotNull(it.sunrise, it.sunset) }
            .filter { it > hours.first().time && it < hours.last().time }
    }
    val description = text.hourlyHeadline(insights.hourly)

    val line = MaterialTheme.colorScheme.primary
    WeatherCard(stringResource(R.string.card_next_hours)) {
        Text(description, style = AppType.voices.narrativeSmall)
        Spacer(Modifier.height(2.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(126.dp)
                .semantics { contentDescription = description },
        ) {
            val pad = 16.dp.toPx()
            val usable = size.width - 2 * pad
            fun x(index: Float) = pad + usable * index / (hours.size - 1)

            val iconPx = 22.dp.toPx()
            val tempRow = 26.dp.toPx()
            val curveTop = 50.dp.toPx()
            val curveBottom = 78.dp.toPx()
            val rainTop = 84.dp.toPx()
            val rainBottom = 104.dp.toPx()
            val timeRow = 110.dp.toPx()

            val temps = hours.map { it.temperature ?: 0.0 }
            val minT = temps.min()
            val maxT = temps.max().coerceAtLeast(minT + 1)
            fun y(t: Double) = (curveBottom - (t - minT) / (maxT - minT) * (curveBottom - curveTop)).toFloat()

            // Rain band: one bar per hour, height = chance.
            val barWidth = (usable / (hours.size - 1)) * 0.55f
            hours.forEachIndexed { i, hour ->
                val chance = (hour.precipitationProbability ?: 0) / 100f
                val cx = x(i.toFloat())
                drawRoundRect(track, Offset(cx - barWidth / 2, rainTop), Size(barWidth, rainBottom - rainTop), CornerRadius(barWidth / 2))
                if (chance > 0.05f) {
                    val h = (rainBottom - rainTop) * chance
                    drawRoundRect(line.copy(alpha = 0.8f), Offset(cx - barWidth / 2, rainBottom - h), Size(barWidth, h), CornerRadius(minOf(barWidth, h) / 2))
                }
            }

            // Sunrise / sunset markers.
            sunEvents.forEach { event ->
                val ex = x(((event - hours.first().time) / 3600f))
                drawLine(marker, Offset(ex, curveTop - 6.dp.toPx()), Offset(ex, rainBottom), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }

            // Temperature line, colored by temperature.
            val path = Path()
            hours.forEachIndexed { i, hour ->
                val px = x(i.toFloat())
                val py = y(hour.temperature ?: minT)
                if (i == 0) {
                    path.moveTo(px, py)
                } else {
                    val prevX = x((i - 1).toFloat())
                    val prevY = y(hours[i - 1].temperature ?: minT)
                    val midX = (prevX + px) / 2
                    path.cubicTo(midX, prevY, midX, py, px, py)
                }
            }
            drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

            // Labeled points: icon, temperature, dot, time.
            labeled.forEachIndexed { n, i ->
                val px = x(i.toFloat())
                val hour = hours[i]
                with(icons[n]) {
                    translate(px - iconPx / 2, 0f) {
                        draw(Size(iconPx, iconPx), colorFilter = ColorFilter.tint(iconTints[n]))
                    }
                }
                val temp = measurer.measure(text.temperature(hour.temperature), tempStyle)
                drawText(temp, topLeft = Offset(px - temp.size.width / 2f, tempRow))
                drawCircle(line, if (i == 0) 4.5.dp.toPx() else 3.dp.toPx(), Offset(px, y(hour.temperature ?: minT)))
                val time = measurer.measure(if (i == 0) nowLabel else text.hourLabel(hour.time), timeStyle)
                drawText(time, topLeft = Offset(px - time.size.width / 2f, timeRow))
            }
        }
    }
}

// endregion

// region Other cards

@Composable
private fun RainCard(insights: Insights, text: WeatherText) {
    val rain = insights.rain
    val kind = when (rain) {
        is RainOutlook.Dry -> null
        is RainOutlook.Now -> rain.episode.kind
        is RainOutlook.Later -> rain.episode.kind
    }
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    WeatherCard(stringResource(R.string.card_precipitation)) {
        Text(text.rainHeadline(rain), style = MaterialTheme.typography.titleLarge)
        Text(text.rainDetail(rain), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rain.bars.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp),
            ) {
                val bars = rain.bars
                val gap = 5.dp.toPx()
                val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
                bars.forEachIndexed { i, bar ->
                    val x = i * (barWidth + gap)
                    drawRoundRect(trackColor, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                    // Proportional height, so 5% never looks like 40%; tiny chances get a thin sliver.
                    val chance = bar.probability / 100f
                    val h = (size.height * chance).coerceAtLeast(if (chance > 0f) 3.dp.toPx() else 0f)
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
private fun UvCard(uv: UvOutlook, weather: Weather, text: WeatherText, modifier: Modifier) {
    val isDay = weather.current?.isDay ?: true
    val index = weather.current?.uvIndex ?: 0.0
    val level = uvLevel(index)
    WeatherCard(stringResource(R.string.card_uv), modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text.uv(level),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The number stays secondary to the word.
            Text("${index.roundToInt()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text.uvAdvice(uv, isDay), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SunCard(sun: SunInfo, text: WeatherText, modifier: Modifier) {
    val glow = MaterialTheme.colorScheme.primary
    val arcColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    WeatherCard(stringResource(R.string.card_sun), modifier) {
        Text(text.sunTitle(sun), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp),
        ) {
            // Half-circle arc that always fits inside the canvas, dot included.
            val stroke = 3.dp.toPx()
            val dot = 6.dp.toPx()
            val r = minOf(size.width / 2, size.height) - dot
            val center = Offset(size.width / 2, size.height - stroke)
            drawArc(
                arcColor, 180f, 180f, useCenter = false,
                topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            sun.dayProgress?.let { progress ->
                val angle = (180f + 180f * progress) * PI / 180
                drawCircle(glow, dot, Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
            }
        }
        Text(text.sunDetail(sun), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
