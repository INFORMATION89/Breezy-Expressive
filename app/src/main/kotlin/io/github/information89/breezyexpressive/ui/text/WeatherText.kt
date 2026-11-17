package io.github.information89.breezyexpressive.ui.text

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import io.github.information89.breezyexpressive.R
import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.insight.AirLevel
import io.github.information89.breezyexpressive.insight.Followup
import io.github.information89.breezyexpressive.insight.HourlyOutlook
import io.github.information89.breezyexpressive.insight.Intensity
import io.github.information89.breezyexpressive.insight.Lead
import io.github.information89.breezyexpressive.insight.PrecipKind
import io.github.information89.breezyexpressive.insight.RainOutlook
import io.github.information89.breezyexpressive.insight.Sky
import io.github.information89.breezyexpressive.insight.SkyTrend
import io.github.information89.breezyexpressive.insight.Summary
import io.github.information89.breezyexpressive.insight.SunInfo
import io.github.information89.breezyexpressive.insight.TempFeel
import io.github.information89.breezyexpressive.insight.UvLevel
import io.github.information89.breezyexpressive.insight.UvOutlook
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns insights into localized text. All wording lives in string resources (strings_weather.xml);
 * this class only picks and combines them. Times are shown in the location's time zone.
 */
class WeatherText(private val context: Context, private val zone: ZoneId, private val locale: Locale, private val now: Long) {

    private val res = context.resources
    private fun s(id: Int, vararg args: Any) = res.getString(id, *args)

    private val is24h = DateFormat.is24HourFormat(context)
    private val hourFormat = formatter(if (is24h) "H" else "j")
    private val hourMinuteFormat = formatter(if (is24h) "Hmm" else "jmm")
    private val dayFormat = formatter("EEE")

    private fun formatter(skeleton: String) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

    private fun local(epoch: Long) = Instant.ofEpochSecond(epoch).atZone(zone)

    // region Basics

    fun temperature(celsius: Double?): String = celsius?.let { "${it.roundToInt()}°" } ?: "–"

    /** "6 PM", "6:30 PM", or "tomorrow 7 AM". */
    fun time(epoch: Long): String {
        val t = local(epoch)
        val text = (if (t.minute == 0) hourFormat else hourMinuteFormat).format(t)
        val days = t.toLocalDate().toEpochDay() - local(now).toLocalDate().toEpochDay()
        return if (days >= 1) s(R.string.time_tomorrow, text) else text
    }

    /** Hour label for the hourly strip, without "tomorrow". */
    fun hourLabel(epoch: Long): String = hourFormat.format(local(epoch))

    fun dayName(dateEpoch: Long): String {
        val days = local(dateEpoch).toLocalDate().toEpochDay() - local(now).toLocalDate().toEpochDay()
        return when (days) {
            0L -> s(R.string.time_today)
            else -> dayFormat.format(local(dateEpoch))
        }
    }

    fun duration(seconds: Long): String {
        val minutes = (seconds / 60).coerceAtLeast(1)
        return if (minutes >= 60) s(R.string.duration_h_min, minutes / 60, minutes % 60) else s(R.string.duration_min, minutes)
    }

    fun feel(feel: TempFeel): String = s(
        when (feel) {
            TempFeel.FREEZING -> R.string.feel_freezing
            TempFeel.COLD -> R.string.feel_cold
            TempFeel.CHILLY -> R.string.feel_chilly
            TempFeel.COOL -> R.string.feel_cool
            TempFeel.MILD -> R.string.feel_mild
            TempFeel.WARM -> R.string.feel_warm
            TempFeel.HOT -> R.string.feel_hot
            TempFeel.VERY_HOT -> R.string.feel_very_hot
        },
    )

    private fun skyAdjective(sky: Sky) = s(
        when (sky) {
            Sky.SUNNY -> R.string.sky_adj_sunny
            Sky.CLEAR_NIGHT -> R.string.sky_adj_clear_night
            Sky.PARTLY_CLOUDY -> R.string.sky_adj_partly_cloudy
            Sky.CLOUDY -> R.string.sky_adj_cloudy
            Sky.FOGGY -> R.string.sky_adj_foggy
            Sky.HAZY -> R.string.sky_adj_hazy
            Sky.WINDY -> R.string.sky_adj_windy
            Sky.RAINY -> R.string.sky_adj_rainy
            Sky.SNOWY -> R.string.sky_adj_snowy
            Sky.SLEETY -> R.string.sky_adj_sleety
            Sky.STORMY -> R.string.sky_adj_stormy
        },
    )

    fun skyTitle(sky: Sky): String = s(
        when (sky) {
            Sky.SUNNY -> R.string.sky_sunny
            Sky.CLEAR_NIGHT -> R.string.sky_clear_night
            Sky.PARTLY_CLOUDY -> R.string.sky_partly_cloudy
            Sky.CLOUDY -> R.string.sky_cloudy
            Sky.FOGGY -> R.string.sky_foggy
            Sky.HAZY -> R.string.sky_hazy
            Sky.WINDY -> R.string.sky_windy
            Sky.RAINY -> R.string.sky_rainy
            Sky.SNOWY -> R.string.sky_snowy
            Sky.SLEETY -> R.string.sky_sleety
            Sky.STORMY -> R.string.sky_stormy
        },
    )

    fun precip(kind: PrecipKind, lowercase: Boolean = false, intensity: Intensity? = null): String {
        val noun = s(
            when (kind) {
                PrecipKind.RAIN -> if (lowercase) R.string.precip_rain_lc else R.string.precip_rain
                PrecipKind.SNOW -> if (lowercase) R.string.precip_snow_lc else R.string.precip_snow
                PrecipKind.SLEET -> if (lowercase) R.string.precip_sleet_lc else R.string.precip_sleet
                PrecipKind.HAIL -> if (lowercase) R.string.precip_hail_lc else R.string.precip_hail
                PrecipKind.STORM -> if (lowercase) R.string.precip_storm_lc else R.string.precip_storm
            },
        )
        if (intensity != Intensity.HEAVY) return noun
        val heavy = s(R.string.precip_heavy, precip(kind, lowercase = true))
        return if (lowercase) heavy.lowercase(locale) else heavy
    }

    fun uv(level: UvLevel): String = s(
        when (level) {
            UvLevel.LOW -> R.string.uv_low
            UvLevel.MODERATE -> R.string.uv_moderate
            UvLevel.HIGH -> R.string.uv_high
            UvLevel.VERY_HIGH -> R.string.uv_very_high
            UvLevel.EXTREME -> R.string.uv_extreme
        },
    )

    fun air(level: AirLevel): String = s(
        when (level) {
            AirLevel.GOOD -> R.string.air_good
            AirLevel.MODERATE -> R.string.air_moderate
            AirLevel.SENSITIVE -> R.string.air_sensitive
            AirLevel.UNHEALTHY -> R.string.air_unhealthy
            AirLevel.VERY_UNHEALTHY -> R.string.air_very_unhealthy
            AirLevel.HAZARDOUS -> R.string.air_hazardous
        },
    )

    private fun String.capitalized() = replaceFirstChar { it.titlecase(locale) }

    private fun minutesUntil(epoch: Long) = (((epoch - now) / 60.0) / 5).roundToInt().coerceAtLeast(1) * 5

    // endregion

    // region Summary sentence

    fun summary(summary: Summary): String {
        val lead = lead(summary.lead)
        val then = summary.then?.let(::followup)
        return if (then == null) s(R.string.summary_only, lead) else s(R.string.summary_join, lead, then)
    }

    private fun lead(lead: Lead): String = when (lead) {
        is Lead.Alert -> lead.title
        is Lead.Feel -> s(R.string.summary_feel, feel(lead.feel), skyAdjective(lead.sky)).capitalized()
        is Lead.PrecipNow -> {
            val noun = precip(lead.kind)
            val end = lead.endsAt
            when {
                end == null -> s(R.string.summary_precip_now, noun)
                lead.precise && end - now <= 60 * 60 -> {
                    val minutes = minutesUntil(end)
                    res.getQuantityString(R.plurals.summary_precip_stopping_in, minutes, noun, minutes)
                }
                else -> s(R.string.summary_precip_until, noun, time(end))
            }
        }
        is Lead.PrecipSoon -> {
            val noun = precip(lead.kind)
            if (lead.precise) {
                val minutes = minutesUntil(lead.startsAt)
                res.getQuantityString(R.plurals.summary_precip_in, minutes, noun, minutes)
            } else {
                s(R.string.summary_precip_within_hour, noun)
            }
        }
    }

    private fun followup(then: Followup): String = when (then) {
        is Followup.PrecipLater -> s(
            if (then.likely) R.string.then_precip_likely_after else R.string.then_precip_from,
            precip(then.kind, lowercase = true),
            time(then.startsAt),
        )
        is Followup.ClearingAt -> s(R.string.then_clearing, time(then.at))
        is Followup.VsYesterday -> s(
            when {
                then.warmer && then.much -> R.string.then_much_warmer
                then.warmer -> R.string.then_warmer
                then.much -> R.string.then_much_colder
                else -> R.string.then_colder
            },
        )
        is Followup.TurningCold -> s(R.string.then_turning, feel(then.feel), time(then.at))
        is Followup.WindyLater -> s(
            if (then.level == io.github.information89.breezyexpressive.insight.WindLevel.STRONG) R.string.then_strong_wind else R.string.then_windy,
            time(then.at),
        )
        is Followup.StrongUv -> s(R.string.then_strong_uv, time(then.until))
        is Followup.PoorAir -> s(
            when (then.level) {
                AirLevel.SENSITIVE -> R.string.then_air_sensitive
                AirLevel.UNHEALTHY -> R.string.then_air_unhealthy
                else -> R.string.then_air_very_unhealthy
            },
        )
        Followup.DryAllDay -> s(R.string.then_dry)
    }

    // endregion

    // region Cards

    fun hourlyHeadline(outlook: HourlyOutlook): String {
        val trend = s(
            when (outlook.trend) {
                SkyTrend.STAYING_CLEAR -> R.string.trend_staying_clear
                SkyTrend.STAYING_CLOUDY -> R.string.trend_staying_cloudy
                SkyTrend.CLOUDING_OVER -> R.string.trend_clouding_over
                SkyTrend.CLEARING_UP -> R.string.trend_clearing_up
                SkyTrend.MIXED -> R.string.trend_mixed
            },
        )
        val precip = outlook.nextPrecip ?: return trend
        return s(R.string.trend_with_precip, trend, s(R.string.then_precip_from, precip(precip.kind, lowercase = true), time(precip.start)))
    }

    fun rainHeadline(outlook: RainOutlook): String = when (outlook) {
        is RainOutlook.Dry -> s(R.string.rain_dry)
        is RainOutlook.Now -> s(R.string.rain_now_until, precip(outlook.episode.kind, intensity = outlook.episode.peakIntensity), time(outlook.episode.end))
        is RainOutlook.Later -> s(
            if (outlook.episode.likely) R.string.rain_later_likely else R.string.rain_later_from,
            precip(outlook.episode.kind, intensity = outlook.episode.peakIntensity),
            time(outlook.episode.start),
        )
    }

    fun rainDetail(outlook: RainOutlook): String = when (outlook) {
        is RainOutlook.Dry -> s(R.string.rain_dry_detail)
        is RainOutlook.Now -> s(R.string.rain_detail, time(outlook.episode.peakAt), time(outlook.episode.end))
        is RainOutlook.Later -> s(R.string.rain_detail, time(outlook.episode.peakAt), time(outlook.episode.end))
    }

    fun uvAdvice(outlook: UvOutlook, isDay: Boolean): String {
        val from = outlook.sunscreenFrom
        val until = outlook.sunscreenUntil
        return when {
            until != null && from != null && from > now + 30 * 60 -> s(R.string.uv_sunscreen_from_to, time(from), time(until))
            until != null -> s(R.string.uv_sunscreen_until, time(until))
            !isDay -> s(R.string.uv_night)
            else -> s(R.string.uv_no_sunscreen)
        }
    }

    fun sunTitle(sun: SunInfo): String =
        if (sun.next.isSunrise) s(R.string.sun_sunrise_at, time(sun.next.at)) else s(R.string.sun_sunset_at, time(sun.next.at))

    fun sunDetail(sun: SunInfo): String = s(R.string.sun_in, duration(sun.next.at - now))

    /** "Full data here" or "Partial data here: rain timing is estimated, no pollen data, …". */
    fun coverage(weather: Weather): String {
        val missing = buildList {
            if (weather.coverage[Feature.MINUTELY]?.coverage == Coverage.ESTIMATED) add(s(R.string.missing_minutely))
            if (weather.coverage[Feature.AIR_QUALITY]?.coverage == Coverage.NONE) add(s(R.string.missing_air))
            if (weather.coverage[Feature.POLLEN]?.coverage == Coverage.NONE) add(s(R.string.missing_pollen))
            if (weather.coverage[Feature.ALERTS]?.coverage == Coverage.NONE) add(s(R.string.missing_alerts))
        }
        return if (missing.isEmpty()) s(R.string.coverage_full) else s(R.string.coverage_partial, missing.joinToString(s(R.string.list_separator)))
    }

    // endregion
}

@Composable
fun rememberWeatherText(zone: ZoneId, now: Long): WeatherText {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    return remember(context, zone, locale, now) { WeatherText(context, zone, locale, now) }
}
