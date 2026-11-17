package io.github.information89.breezyexpressive.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import io.github.information89.breezyexpressive.insight.SunInfo
import io.github.information89.breezyexpressive.insight.UvOutlook
import io.github.information89.breezyexpressive.insight.uvLevel
import io.github.information89.breezyexpressive.ui.components.Hairline
import io.github.information89.breezyexpressive.ui.components.WeatherCard
import io.github.information89.breezyexpressive.ui.components.weatherIconRes
import io.github.information89.breezyexpressive.ui.text.WeatherText
import io.github.information89.breezyexpressive.ui.text.rememberWeatherText
import io.github.information89.breezyexpressive.ui.theme.AppType
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Forecast, editorial: a technical header line, a big statement + temperature, then numbered
 * glass sections. Words first, numbers second (PLAN.md 5.0); data set in monospace.
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
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 104.dp), // room for the floating toolbar
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Header(state, text)
                    Hero(state, weather, insights, text)
                    TimelineCard(weather, insights, text, state.now)
                    RainCard(insights, text)
                    // Same height for both small sections.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        insights.uv?.let { UvCard(it, weather, text, Modifier.weight(1f).fillMaxHeight()) }
                        insights.sun?.let { SunCard(it, text, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
        }
    }
}

// region Header + hero

/** "MEXICO CITY — TUE 17 NOV, 3:05 AM ......... 19.43°N 99.13°W" over a hairline. */
@Composable
private fun Header(state: ForecastUiState, text: WeatherText) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.location.name.uppercase()} — ${text.dateLine(state.now).uppercase()}, ${text.clock(state.now)}",
                style = AppType.voices.monoLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text.coordinates(state.location.latitude, state.location.longitude),
                style = AppType.voices.monoLabel,
                color = muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        Hairline()
    }
}

/** Statement and sentence on the left, the big temperature on the right. */
@Composable
private fun Hero(state: ForecastUiState, weather: Weather, insights: Insights, text: WeatherText) {
    val today = weather.daily.lastOrNull { it.date <= state.now }
    Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(top = 10.dp)) {
            Text(text.skyTitle(insights.now.sky), style = AppType.voices.statement)
            if (today != null) {
                Text(
                    "H ${text.temperature(today.temperatureMax)}  /  L ${text.temperature(today.temperatureMin)}",
                    style = AppType.voices.monoData,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text.summary(insights.summary),
                style = AppType.voices.narrativeMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (state.error != null) {
                Text(
                    stringResource(R.string.stale_weather, text.time(weather.updatedAt)),
                    style = AppType.voices.monoLabel,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text.temperature(weather.current?.temperature),
            style = AppType.voices.heroTemperature,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// endregion

// region 01 Next hours: blueprint timeline

/**
 * The next 12 hours drawn like a technical diagram: a thin temperature line over a dotted baseline,
 * rain as fine vertical ticks, sunrise/sunset marked, icons and data every 3 hours in monospace.
 */
@Composable
private fun TimelineCard(weather: Weather, insights: Insights, text: WeatherText, now: Long) {
    val hours = remember(weather, now) { weather.hourly.filter { it.time + 3600 > now }.take(13) }
    if (hours.size < 2) return
    val labeled = hours.indices.filter { it % 3 == 0 }

    val measurer = rememberTextMeasurer()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val dataStyle = AppType.voices.monoData.copy(color = ink)
    val labelStyle = AppType.voices.monoLabel.copy(color = muted)
    val nowLabel = stringResource(R.string.time_now).uppercase()
    val icons = labeled.map { painterResource(weatherIconRes(hours[it].weatherCode, hours[it].isDay)) }
    val sunEvents = remember(weather, now) {
        weather.daily.flatMap { d -> listOfNotNull(d.sunrise?.let { it to true }, d.sunset?.let { it to false }) }
            .filter { it.first > hours.first().time && it.first < hours.last().time }
    }
    val description = text.hourlyHeadline(insights.hourly)

    WeatherCard(stringResource(R.string.card_next_hours), index = "01") {
        Text(description, style = AppType.voices.narrativeSmall)
        Spacer(Modifier.height(2.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .semantics { contentDescription = description },
        ) {
            val pad = 14.dp.toPx()
            val usable = size.width - 2 * pad
            fun x(index: Float) = pad + usable * index / (hours.size - 1)

            val iconPx = 18.dp.toPx()
            val tempRow = 22.dp.toPx()
            val curveTop = 46.dp.toPx()
            val curveBottom = 74.dp.toPx()
            val baseline = 100.dp.toPx()
            val rainMax = 18.dp.toPx()
            val timeRow = 108.dp.toPx()
            val hair = 1.dp.toPx()

            val temps = hours.map { it.temperature ?: 0.0 }
            val minT = temps.min()
            val maxT = temps.max().coerceAtLeast(minT + 1)
            fun y(t: Double) = (curveBottom - (t - minT) / (maxT - minT) * (curveBottom - curveTop)).toFloat()

            // Dotted baseline with an hour tick for every hour.
            drawLine(ink.copy(alpha = 0.3f), Offset(pad, baseline), Offset(size.width - pad, baseline), hair, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
            hours.indices.forEach { i ->
                val tx = x(i.toFloat())
                val tick = if (i % 3 == 0) 6.dp.toPx() else 3.dp.toPx()
                drawLine(ink.copy(alpha = 0.35f), Offset(tx, baseline), Offset(tx, baseline + tick), hair)
            }

            // Rain: fine vertical ticks rising from the baseline, height = chance.
            hours.forEachIndexed { i, hour ->
                val chance = (hour.precipitationProbability ?: 0) / 100f
                if (chance > 0.05f) {
                    val tx = x(i.toFloat())
                    drawLine(accent, Offset(tx, baseline), Offset(tx, baseline - rainMax * chance), 3.dp.toPx(), cap = StrokeCap.Round)
                }
            }

            // Sunrise / sunset: dashed marker with a small arrow label.
            sunEvents.forEach { (event, isSunrise) ->
                val ex = x(((event - hours.first().time) / 3600f))
                drawLine(ink.copy(alpha = 0.35f), Offset(ex, curveTop - 4.dp.toPx()), Offset(ex, baseline), hair, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                val label = measurer.measure((if (isSunrise) "↑ " else "↓ ") + text.clock(event), labelStyle)
                drawText(label, topLeft = Offset((ex + 4.dp.toPx()).coerceAtMost(size.width - label.size.width.toFloat()), baseline - label.size.height - 2.dp.toPx()))
            }

            // Temperature: one thin line.
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
            drawPath(path, accent, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

            // Every 3 hours: icon, temperature, point, time.
            labeled.forEachIndexed { n, i ->
                val px = x(i.toFloat())
                val hour = hours[i]
                with(icons[n]) {
                    translate(px - iconPx / 2, 0f) {
                        draw(Size(iconPx, iconPx), colorFilter = ColorFilter.tint(ink))
                    }
                }
                val temp = measurer.measure(text.temperature(hour.temperature), dataStyle)
                drawText(temp, topLeft = Offset(px - temp.size.width / 2f, tempRow))
                val py = y(hour.temperature ?: minT)
                drawCircle(accent, if (i == 0) 4.dp.toPx() else 2.5.dp.toPx(), Offset(px, py))
                val time = measurer.measure(if (i == 0) nowLabel else text.hourLabel(hour.time).uppercase(), labelStyle)
                drawText(time, topLeft = Offset(px - time.size.width / 2f, timeRow))
            }
        }
    }
}

// endregion

// region 02 Precipitation, 03 UV, 04 Sun

@Composable
private fun RainCard(insights: Insights, text: WeatherText) {
    val rain = insights.rain
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    WeatherCard(stringResource(R.string.card_precipitation), index = "02") {
        Text(text.rainHeadline(rain), style = MaterialTheme.typography.titleLarge)
        Text(text.rainDetail(rain), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rain.bars.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp),
            ) {
                val bars = rain.bars
                val step = size.width / bars.size
                val hair = 1.dp.toPx()
                // Baseline + one fine tick per hour; chance drawn as a thicker stroke above it.
                drawLine(ink.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height), hair)
                bars.forEachIndexed { i, bar ->
                    val cx = step * (i + 0.5f)
                    drawLine(ink.copy(alpha = 0.25f), Offset(cx, size.height), Offset(cx, size.height - 4.dp.toPx()), hair)
                    val chance = bar.probability / 100f
                    if (chance > 0.02f) {
                        val h = (size.height - 2.dp.toPx()) * chance
                        drawLine(accent, Offset(cx, size.height), Offset(cx, size.height - h.coerceAtLeast(2.dp.toPx())), 4.dp.toPx(), cap = StrokeCap.Butt)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                val labels = listOf(
                    stringResource(R.string.time_now).uppercase(),
                    text.hourLabel(rain.bars[rain.bars.size / 2].time).uppercase(),
                    text.hourLabel(rain.bars.last().time).uppercase(),
                )
                labels.forEachIndexed { i, label ->
                    Text(
                        label,
                        style = AppType.voices.monoLabel,
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
    WeatherCard(stringResource(R.string.card_uv), modifier, index = "03") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text.uv(uvLevel(index)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${index.roundToInt()}/11", style = AppType.voices.monoData, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text.uvAdvice(uv, isDay), style = AppType.voices.narrativeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SunCard(sun: SunInfo, text: WeatherText, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    WeatherCard(stringResource(R.string.card_sun), modifier, index = "04") {
        Text(text.sunTitle(sun), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(32.dp),
        ) {
            // Thin half-circle path with a dotted horizon.
            val stroke = 1.5.dp.toPx()
            val dot = 4.5.dp.toPx()
            val r = minOf(size.width / 2, size.height) - dot
            val center = Offset(size.width / 2, size.height - stroke)
            drawLine(ink.copy(alpha = 0.3f), Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
            drawArc(
                ink.copy(alpha = 0.35f), 180f, 180f, useCenter = false,
                topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            sun.dayProgress?.let { progress ->
                val angle = (180f + 180f * progress) * PI / 180
                drawCircle(accent, dot, Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat()))
            }
        }
        Text(text.sunDetail(sun).uppercase(), style = AppType.voices.monoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
