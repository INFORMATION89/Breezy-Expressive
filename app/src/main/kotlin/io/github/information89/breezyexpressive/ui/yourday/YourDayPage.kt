package io.github.information89.breezyexpressive.ui.yourday

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.data.model.Daily
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.insight.BringItem
import io.github.information89.breezyexpressive.insight.BringKind
import io.github.information89.breezyexpressive.ui.components.DataColors
import io.github.information89.breezyexpressive.ui.components.WeatherCard
import io.github.information89.breezyexpressive.ui.components.WeatherIcon
import io.github.information89.breezyexpressive.ui.forecast.ForecastUiState
import io.github.information89.breezyexpressive.ui.material.glass
import io.github.information89.breezyexpressive.ui.text.WeatherText
import io.github.information89.breezyexpressive.ui.text.rememberWeatherText
import io.github.information89.breezyexpressive.ui.theme.AppType
import java.time.ZoneId

/**
 * Your day: what to bring today and the week ahead.
 * The full timeline (turning points, routines) builds on this in M4.
 */
@Composable
fun YourDayPage(state: ForecastUiState, modifier: Modifier = Modifier) {
    val weather = state.weather
    val insights = state.insights
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.page_your_day),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        )
        if (weather == null || insights == null) return@Column
        val text = rememberWeatherText(ZoneId.of(weather.timeZone), state.now)

        Text(
            text.summary(insights.summary),
            style = AppType.voices.narrativeMedium,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        BringCard(insights.bring, text)
        WeekCard(weather, text, state.now)
        Footer(weather, text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BringCard(items: List<BringItem>, text: WeatherText) {
    WeatherCard(stringResource(R.string.card_bring)) {
        if (items.isEmpty()) {
            Text(stringResource(R.string.bring_nothing), style = AppType.voices.narrativeSmall)
            return@WeatherCard
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { BringChip(it, text) }
        }
    }
}

@Composable
private fun BringChip(item: BringItem, text: WeatherText) {
    val (label, icon) = when (item.kind) {
        BringKind.UMBRELLA -> R.string.bring_umbrella to R.drawable.ic_b_umbrella
        BringKind.RAIN_JACKET -> R.string.bring_rain_jacket to R.drawable.ic_w_rainy
        BringKind.JACKET -> R.string.bring_jacket to R.drawable.ic_b_checkroom
        BringKind.WARM_LAYERS -> R.string.bring_warm_layers to R.drawable.ic_b_ac_unit
        BringKind.SUNSCREEN -> R.string.bring_sunscreen to R.drawable.ic_w_sunny
        BringKind.WATER -> R.string.bring_water to R.drawable.ic_b_water_drop
        BringKind.MASK -> R.string.bring_mask to R.drawable.ic_b_masks
    }
    val whenText = when {
        item.from != null && item.until != null -> stringResource(R.string.bring_from_until, text.time(item.from), text.time(item.until))
        item.from != null -> stringResource(R.string.bring_from, text.time(item.from))
        item.until != null -> stringResource(R.string.bring_until, text.time(item.until))
        else -> null
    }
    Row(
        Modifier
            .glass(CircleShape, glow = false)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 8.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
            if (whenText != null) {
                Text(whenText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeekCard(weather: Weather, text: WeatherText, now: Long) {
    val days = remember(weather, now) { weather.daily.filter { it.date + 86_400 > now }.take(7) }
    if (days.isEmpty()) return
    val weekMin = days.mapNotNull { it.temperatureMin }.minOrNull() ?: return
    val weekMax = days.mapNotNull { it.temperatureMax }.maxOrNull() ?: return
    WeatherCard(stringResource(R.string.your_day_week)) {
        days.forEach { day -> DayRow(day, text, weekMin, weekMax) }
    }
}

@Composable
private fun DayRow(day: Daily, text: WeatherText, weekMin: Double, weekMax: Double) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(text.dayName(day.date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(56.dp))
        WeatherIcon(day.weatherCode, isDay = true, size = 22.dp)
        val chance = day.precipitationProbabilityMax ?: 0
        Text(
            if (chance >= 30) "$chance%" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(40.dp)
                .padding(start = 4.dp),
        )
        Text(
            text.temperature(day.temperatureMin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
        val bar = MaterialTheme.colorScheme.primary
        val low = day.temperatureMin ?: weekMin
        val high = day.temperatureMax ?: weekMax
        Canvas(
            Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp),
        ) {
            val span = (weekMax - weekMin).coerceAtLeast(1.0)
            val start = ((low - weekMin) / span).toFloat() * size.width
            val end = ((high - weekMin) / span).toFloat() * size.width
            drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2))
            drawRoundRect(
                bar,
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
    Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
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
