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
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.CornerRadius
import io.github.information89.breezyexpressive.insight.RainOutlook
import io.github.information89.breezyexpressive.insight.PrecipEpisode
import io.github.information89.breezyexpressive.insight.Intensity
import io.github.information89.breezyexpressive.data.model.Minutely
import io.github.information89.breezyexpressive.ui.material.SkyPreview
import io.github.information89.breezyexpressive.insight.PrecipKind
import io.github.information89.breezyexpressive.insight.UvLevel
import io.github.information89.breezyexpressive.data.model.WeatherCode
import io.github.information89.breezyexpressive.ui.components.CardShape
import io.github.information89.breezyexpressive.ui.material.GlassTheme
import io.github.information89.breezyexpressive.ui.material.glass
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
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
import io.github.information89.breezyexpressive.ui.components.weatherIconTint
import io.github.information89.breezyexpressive.ui.components.DataColors
import io.github.information89.breezyexpressive.insight.AirLevel
import io.github.information89.breezyexpressive.insight.AirOutlook
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                // Normally fits on one screen; still scrolls with large fonts or small phones.
                // No stretch effect when there's nothing to scroll.
                val scroll = rememberScrollState()
                val overscroll = rememberOverscrollEffect()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll, overscrollEffect = if (scroll.maxValue > 0) overscroll else null)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 104.dp), // room for the floating toolbar
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Header(state)
                    Hero(state, weather, insights, text)
                    insights.sun?.let { SunPath(it, weather, text, state.now) }
                    NextHoursCard(weather, insights, text, state.now)
                    // Same height for both small sections.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UvCard(weather, text, Modifier.weight(1f).fillMaxHeight())
                        insights.air?.let { AirCard(it, Modifier.weight(1f).fillMaxHeight()) }
                    }
                    Precipitation(weather, insights.rain, text, state.now)
                }
            }
        }
    }
}

// region Header + hero

/** The place, quietly, over a hairline. No date, time or coordinates on screen. */
@Composable
private fun Header(state: ForecastUiState) {
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text(
            state.location.name.uppercase(),
            style = AppType.voices.monoLabel,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

// region Sun path (on the sky, under the hero)

/**
 * The sun's path today, drawn straight on the sky like the Seoul reference: a thin curve over a
 * horizon line, the part of the day already gone traced in sun orange up to "now", sunrise on the
 * left, sunset on the right, and the sun's strength (UV) in the middle.
 */
@Composable
private fun SunPath(sun: SunInfo, weather: Weather, text: WeatherText, now: Long) {
    val sunrise = sun.sunrise
    val sunset = sun.sunset
    if (sunset <= sunrise) return
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val orange = DataColors.sun
    val isDay = now in sunrise..sunset
    val center = if (isDay) {
        stringResource(R.string.daylight_left, text.duration(sunset - now))
    } else {
        stringResource(R.string.daylight, text.duration(sunset - sunrise))
    }
    // Text sits right on the sky, sometimes over the bright glow: a soft shadow keeps it readable.
    val onSky = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), blurRadius = 16f))
    val description = "${stringResource(R.string.sunrise)} ${text.clock(sunrise)}, ${stringResource(R.string.sunset)} ${text.clock(sunset)}. $center"

    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                // The horizon runs edge to edge, past the page padding.
                .layout { measurable, constraints ->
                    val bleed = 16.dp.roundToPx()
                    val width = constraints.maxWidth + 2 * bleed
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
                },
        ) {
            val bleed = 16.dp.toPx()
            val inset = 8.dp.toPx()
            val riseX = bleed + inset
            val setX = size.width - bleed - inset
            fun x(t: Double) = (riseX + (t - sunrise) / (sunset - sunrise) * (setX - riseX)).toFloat()
            fun t(x: Float) = sunrise + (x - riseX) / (setX - riseX) * (sunset - sunrise).toDouble()
            // Sun height: a cosine over 24 h peaking at solar noon; 0 at sunrise/sunset.
            val noon = (sunrise + sunset) / 2.0
            fun elevation(t: Double) = cos(2 * PI * (t - noon) / 86_400.0)
            val e0 = elevation(sunrise.toDouble())
            val horizon = size.height - 14.dp.toPx()
            val amplitude = horizon - 6.dp.toPx()
            fun y(t: Double) = (horizon - (elevation(t) - e0) / (1 - e0) * amplitude).toFloat()

            drawLine(ink.copy(alpha = 0.35f), Offset(0f, horizon), Offset(size.width, horizon), 1.dp.toPx())

            val steps = 120
            val path = Path()
            for (i in 0..steps) {
                val px = size.width * i / steps
                if (i == 0) path.moveTo(px, y(t(px))) else path.lineTo(px, y(t(px)))
            }
            drawPath(path, ink.copy(alpha = 0.85f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

            // The day so far, in sun orange, fading in from the left.
            val nowX = x(now.toDouble()).coerceIn(0f, size.width)
            if (nowX > 0f) {
                val past = Path()
                val pastSteps = (steps * nowX / size.width).toInt().coerceAtLeast(1)
                for (i in 0..pastSteps) {
                    val px = nowX * i / pastSteps
                    if (i == 0) past.moveTo(px, y(t(px))) else past.lineTo(px, y(t(px)))
                }
                drawPath(
                    past,
                    Brush.horizontalGradient(listOf(orange.copy(alpha = 0f), orange), startX = riseX - 24.dp.toPx(), endX = nowX),
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawCircle(ink, 3.dp.toPx(), Offset(riseX, horizon))
            drawCircle(ink, 3.dp.toPx(), Offset(setX, horizon))
            if (now.toDouble() in t(0f)..t(size.width)) {
                val marker = Offset(nowX, y(now.toDouble()))
                if (isDay) {
                    drawCircle(orange.copy(alpha = 0.35f), 11.dp.toPx(), marker)
                    drawCircle(orange, 7.dp.toPx(), marker)
                } else {
                    drawCircle(ink, 5.dp.toPx(), marker)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text.clockShort(sunrise), style = MaterialTheme.typography.titleLarge.merge(onSky))
                Text(stringResource(R.string.sunrise), style = MaterialTheme.typography.bodyMedium.merge(onSky), color = muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (isDay) R.drawable.ic_w_sunny else R.drawable.ic_w_clear_night),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(center, style = MaterialTheme.typography.bodyLarge.merge(onSky).copy(fontWeight = FontWeight.Medium))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(text.clockShort(sunset), style = MaterialTheme.typography.titleLarge.merge(onSky))
                Text(stringResource(R.string.sunset), style = MaterialTheme.typography.bodyMedium.merge(onSky), color = muted)
            }
        }
    }
}

// endregion

// region Cards

/** The next 12 hours as simple columns: temperature, icon, time (every 2 hours). */
@Composable
private fun NextHoursCard(weather: Weather, insights: Insights, text: WeatherText, now: Long) {
    val hours = remember(weather, now) { weather.hourly.filter { it.time + 3600 > now }.take(11).filterIndexed { i, _ -> i % 2 == 0 } }
    if (hours.isEmpty()) return
    val summary = text.hourlyHeadline(insights.hourly).replaceFirstChar { it.lowercase() }
    WeatherCard(stringResource(R.string.card_next_hours), summary = summary) {
        Row(Modifier.fillMaxWidth()) {
            hours.forEachIndexed { i, hour ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text.temperature(hour.temperature), style = MaterialTheme.typography.bodyLarge, color = if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        painterResource(weatherIconRes(hour.weatherCode, hour.isDay)),
                        contentDescription = null,
                        tint = weatherIconTint(hour.weatherCode, hour.isDay),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        if (i == 0) stringResource(R.string.time_now) else text.hourShort(hour.time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** UV now, as a word and a number, on a five-step color scale. */
@Composable
private fun UvCard(weather: Weather, text: WeatherText, modifier: Modifier) {
    val index = weather.current?.uvIndex ?: 0.0
    val level = uvLevel(index)
    val bands = listOf(0.0, 3.0, 6.0, 8.0, 11.0, 14.0)
    WeatherCard(stringResource(R.string.card_uv), modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text.uv(level), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${index.roundToInt()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScaleBar(
            colors = UvLevel.entries.map { DataColors.uv(it) },
            position = bandPosition(index, bands),
            marker = DataColors.uv(level),
            description = "${text.uv(level)}, UV ${index.roundToInt()}",
        )
    }
}

/** Air quality on a five-step color scale with a marker, plus one short phrase. */
@Composable
private fun AirCard(air: AirOutlook, modifier: Modifier) {
    val phrase = stringResource(
        when (air.level) {
            AirLevel.GOOD -> R.string.air_phrase_good
            AirLevel.MODERATE -> R.string.air_phrase_moderate
            AirLevel.SENSITIVE -> R.string.air_phrase_sensitive
            AirLevel.UNHEALTHY -> R.string.air_phrase_unhealthy
            AirLevel.VERY_UNHEALTHY -> R.string.air_phrase_very_unhealthy
            AirLevel.HAZARDOUS -> R.string.air_phrase_hazardous
        },
    )
    // Each segment is one US AQI band (0–50, 51–100, …, 201–300); 300+ sits at the end.
    val bands = listOf(0.0, 50.0, 100.0, 150.0, 200.0, 300.0)
    WeatherCard(stringResource(R.string.card_air), modifier) {
        Text(phrase, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
        ScaleBar(
            colors = listOf(AirLevel.GOOD, AirLevel.MODERATE, AirLevel.SENSITIVE, AirLevel.UNHEALTHY, AirLevel.VERY_UNHEALTHY).map { DataColors.air(it) },
            position = bandPosition(air.usAqi.toDouble(), bands),
            marker = DataColors.air(air.level),
            description = phrase,
        )
    }
}

/** 0..1 along a scale of equal-width segments, one per band. */
private fun bandPosition(value: Double, bands: List<Double>): Float {
    val v = value.coerceIn(bands.first(), bands.last())
    val band = bands.indexOfLast { v >= it }.coerceAtMost(bands.size - 2)
    val within = (v - bands[band]) / (bands[band + 1] - bands[band])
    return ((band + within) / (bands.size - 1)).toFloat()
}

@Composable
private fun ScaleBar(colors: List<Color>, position: Float, marker: Color, description: String) {
    val ring = MaterialTheme.colorScheme.surface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics { contentDescription = description },
    ) {
        val h = 6.dp.toPx()
        val y = size.height / 2
        val segment = size.width / colors.size
        colors.forEachIndexed { i, color ->
            drawLine(color, Offset(segment * i + h / 2, y), Offset(segment * (i + 1) - h / 2, y), h, cap = StrokeCap.Round)
        }
        val mx = (size.width * position).coerceIn(h, size.width - h)
        drawCircle(ring, 8.dp.toPx(), Offset(mx, y))
        drawCircle(marker, 6.dp.toPx(), Offset(mx, y))
    }
}

// endregion

// region Precipitation timeline

/** Debug preview: rain from ~50 min from now, building to heavy, easing off after ~2.5 h. */
private fun previewRainOutlook(now: Long): RainOutlook {
    val start = now - now % 900 + 900 * 4
    val episode = PrecipEpisode(PrecipKind.RAIN, start = start, end = start + 900 * 10, peakAt = start + 900 * 4, peakIntensity = Intensity.HEAVY, maxProbability = 90)
    return RainOutlook.Later(episode, emptyList())
}

private fun previewRainMinutely(now: Long): List<Minutely> {
    val first = now - now % 900
    val shape = listOf(0, 0, 0, 0, 0.2, 0.5, 0.9, 1.6, 2.4, 2.0, 1.2, 0.7, 0.4, 0.3, 0.2, 0.1, 0, 0, 0, 0, 0, 0, 0, 0)
    return shape.mapIndexed { i, mm -> Minutely(first + i * 900L, mm.toDouble()) }
}

/**
 * Rain and snow as a timeline: fine bars over Light / Moderate / Heavy guides, with a callout
 * where it starts. Uses the 15-minute forecast when the rain falls within it, hourly otherwise.
 * With nothing coming it shrinks to a one-line banner.
 */
@Composable
private fun Precipitation(weather: Weather, liveRain: RainOutlook, text: WeatherText, now: Long) {
    val preview = SkyPreview.rain.value
    val previewMinutely = remember(preview, now) { if (preview) previewRainMinutely(now) else null }
    val rain = if (preview) previewRainOutlook(now) else liveRain
    val episode = when (rain) {
        is RainOutlook.Dry -> null
        is RainOutlook.Now -> rain.episode
        is RainOutlook.Later -> rain.episode
    }
    if (episode == null) {
        GlassTheme {
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(CardShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_b_umbrella), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text.rainHeadline(rain), style = MaterialTheme.typography.titleSmall)
                Text(
                    text.rainDetail(rain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
        }
        return
    }

    // (start time, mm per hour) every 15 minutes.
    val series = remember(weather, rain, now) {
        val minutely = (previewMinutely ?: weather.minutely).filter { it.time >= now - 900 }
        if (minutely.size >= 8 && episode.start < minutely.last().time) {
            minutely.map { it.time to (it.precipitation ?: 0.0) * 4 }
        } else {
            rain.bars.filter { it.time + 3600 > now }.take(12).flatMap { bar ->
                (0 until 4).map { q -> bar.time + q * 900L to bar.amount }
            }
        }
    }
    if (series.isEmpty()) return
    val color = DataColors.precip(episode.kind)
    val levels = listOf(R.string.rain_level_light, R.string.rain_level_moderate, R.string.rain_level_heavy).map { stringResource(it) }
    val startIcon = painterResource(weatherIconRes(if (episode.kind == PrecipKind.SNOW) WeatherCode.SNOW else WeatherCode.RAIN, true))
    val headline = text.rainHeadline(rain)

    WeatherCard(stringResource(R.string.card_precipitation), summary = headline) {
        val ink = MaterialTheme.colorScheme.onSurface
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        val measurer = rememberTextMeasurer(cacheSize = 32)
        val labelStyle = MaterialTheme.typography.labelMedium.copy(color = muted)
        val calloutStyle = MaterialTheme.typography.labelLarge.copy(color = ink)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .semantics { contentDescription = headline },
        ) {
            val labels = levels.map { measurer.measure(it, labelStyle) }
            val left = labels.maxOf { it.size.width } + 10.dp.toPx()
            val right = size.width - 4.dp.toPx()
            val top = 32.dp.toPx() // room for the callout
            val axis = size.height - 22.dp.toPx()
            val plot = axis - top
            val first = series.first().first
            val last = series.last().first + 900
            fun x(t: Long) = left + (t - first).toFloat() / (last - first) * (right - left)
            // mm/h to height: three equal bands for light (< 2.5), moderate (< 7.6), heavy.
            fun level(v: Double): Float = when {
                v <= 0.0 -> 0f
                v < 2.5 -> (v / 2.5 / 3).toFloat()
                v < 7.6 -> (1.0 / 3 + (v - 2.5) / 5.1 / 3).toFloat()
                else -> (2.0 / 3 + ((v - 7.6) / 7.6).coerceAtMost(1.0) / 3).toFloat()
            }

            // Guides with their names on the left.
            labels.forEachIndexed { i, label ->
                val gy = axis - plot * (i + 1) / 3f
                drawLine(ink.copy(alpha = 0.12f), Offset(left, gy), Offset(right, gy), 1.dp.toPx())
                drawText(label, topLeft = Offset(0f, gy - label.size.height / 2f))
            }
            drawLine(ink.copy(alpha = 0.4f), Offset(left, axis), Offset(right, axis), 1.dp.toPx())

            // Bars.
            val step = (right - left) / series.size
            val barWidth = (step * 0.38f).coerceIn(2.dp.toPx(), 5.dp.toPx())
            series.forEach { (t, v) ->
                val h = level(v) * plot
                if (h > 0f) {
                    val bx = x(t) + step / 2
                    drawLine(color, Offset(bx, axis - 1.dp.toPx()), Offset(bx, axis - h.coerceAtLeast(3.dp.toPx())), barWidth, cap = StrokeCap.Round)
                }
            }

            // Hour ticks and labels.
            val spanHours = (last - first) / 3600
            val every = if (spanHours > 6) 3 else 1
            var tick = ((first + 3599) / 3600) * 3600
            var n = 0
            while (tick < last) {
                val tx = x(tick)
                drawLine(ink.copy(alpha = 0.4f), Offset(tx, axis), Offset(tx, axis + 4.dp.toPx()), 1.dp.toPx())
                if (n % every == 0) {
                    val label = measurer.measure(text.clockShort(tick), labelStyle)
                    val lx = (tx - label.size.width / 2f).coerceIn(left, right - label.size.width)
                    drawText(label, topLeft = Offset(lx, axis + 6.dp.toPx()))
                }
                tick += 3600; n++
            }

            // Callout where it starts (or "now" if it's already falling).
            val startT = episode.start.coerceAtLeast(first)
            if (startT < last) {
                val cx = x(startT)
                drawLine(ink.copy(alpha = 0.5f), Offset(cx, top - 6.dp.toPx()), Offset(cx, axis), 1.dp.toPx())
                val label = measurer.measure(text.clockShort(episode.start.coerceAtLeast(now)), calloutStyle)
                val iconPx = 16.dp.toPx()
                val padH = 8.dp.toPx()
                val w = padH + iconPx + 4.dp.toPx() + label.size.width + padH
                val h = 26.dp.toPx()
                val bx = (cx - w / 2).coerceIn(0f, size.width - w)
                drawRoundRect(ink.copy(alpha = 0.1f), Offset(bx, 0f), Size(w, h), CornerRadius(h / 2))
                with(startIcon) {
                    translate(bx + padH, (h - iconPx) / 2) { draw(Size(iconPx, iconPx), colorFilter = ColorFilter.tint(color)) }
                }
                drawText(label, topLeft = Offset(bx + padH + iconPx + 4.dp.toPx(), (h - label.size.height) / 2))
            }
        }
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
