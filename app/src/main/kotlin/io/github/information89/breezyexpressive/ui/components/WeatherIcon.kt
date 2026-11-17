package io.github.information89.breezyexpressive.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.data.model.WeatherCode

@DrawableRes
fun weatherIconRes(code: WeatherCode?, isDay: Boolean): Int = when (code) {
    null, WeatherCode.CLEAR -> if (isDay) R.drawable.ic_w_sunny else R.drawable.ic_w_clear_night
    WeatherCode.PARTLY_CLOUDY -> if (isDay) R.drawable.ic_w_partly_cloudy_day else R.drawable.ic_w_partly_cloudy_night
    WeatherCode.CLOUDY -> R.drawable.ic_w_cloud
    WeatherCode.RAIN -> R.drawable.ic_w_rainy
    WeatherCode.SNOW -> R.drawable.ic_w_weather_snowy
    WeatherCode.SLEET, WeatherCode.HAIL -> R.drawable.ic_w_weather_hail
    WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> R.drawable.ic_w_thunderstorm
    WeatherCode.FOG, WeatherCode.HAZE -> R.drawable.ic_w_foggy
    WeatherCode.WIND -> R.drawable.ic_w_air
}

/** Weather icon with a color that hints at the condition (sun warm, rain blue, storms violet). */
@Composable
fun WeatherIcon(code: WeatherCode?, isDay: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp, contentDescription: String? = null) {
    val tint = when (code) {
        null, WeatherCode.CLEAR, WeatherCode.PARTLY_CLOUDY -> if (isDay) Color(0xFFFFB02E) else MaterialTheme.colorScheme.secondary
        WeatherCode.RAIN -> Color(0xFF4A9FE0)
        WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> Color(0xFF9A7BE0)
        WeatherCode.SNOW, WeatherCode.SLEET, WeatherCode.HAIL -> Color(0xFF8CC8F0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        painter = painterResource(weatherIconRes(code, isDay)),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
