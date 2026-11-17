package io.github.information89.breezyexpressive.insight

import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Weather
import kotlin.math.abs

/**
 * Turns weather data into plain-language facts: the summary sentence, what to bring, and the
 * headline of each card. Pure and deterministic (pass [now]), so every rule is unit-tested.
 */
object InsightEngine {

    private const val HOUR = 3600L

    /** How far ahead "the rest of the day" looks. */
    const val HORIZON_SECONDS = 12 * HOUR
    private const val WET_PROBABILITY = 50
    private const val WET_AMOUNT_MM = 0.3
    private const val MINUTELY_WET_MM = 0.1 // per 15 minutes

    fun analyze(weather: Weather, now: Long): Insights {
        val window = weather.hourly.filter { it.time + HOUR > now && it.time < now + HORIZON_SECONDS }
        val currentHour = window.firstOrNull()
        val episodes = precipEpisodes(window, now)
        val nowInsight = nowInsight(weather, currentHour)
        val preciseMinutely = weather.coverage[Feature.MINUTELY]?.coverage == Coverage.FULL

        return Insights(
            now = nowInsight,
            summary = summary(weather, now, window, episodes, nowInsight, preciseMinutely),
            bring = bring(weather, now, window, episodes),
            hourly = hourlyOutlook(window, episodes),
            rain = rainOutlook(weather, now, episodes),
            uv = uvOutlook(weather, now, window),
            sun = sunInfo(weather, now),
            air = airOutlook(weather, now),
        )
    }

    // region Now

    private fun nowInsight(weather: Weather, currentHour: Hourly?): NowInsight {
        val c = weather.current
        val apparent = c?.apparentTemperature ?: c?.temperature
            ?: currentHour?.apparentTemperature ?: currentHour?.temperature ?: 20.0
        val humidityNote = when {
            (c?.dewPoint ?: 0.0) >= 20 -> HumidityNote.MUGGY
            c?.humidity != null && c.humidity < 25 -> HumidityNote.DRY
            else -> null
        }
        return NowInsight(
            feel = tempFeel(apparent),
            sky = sky(c?.weatherCode ?: currentHour?.weatherCode, c?.isDay ?: currentHour?.isDay ?: true),
            wind = windLevel(c?.windSpeed ?: currentHour?.windSpeed, c?.windGusts ?: currentHour?.windGusts),
            humidity = humidityNote,
        )
    }

    // endregion

    // region Precipitation

    private fun Hourly.isWet() =
        (precipitationProbability ?: 0) >= WET_PROBABILITY || (precipitation ?: 0.0) >= WET_AMOUNT_MM

    /** Contiguous wet hours in the window, in order. */
    internal fun precipEpisodes(window: List<Hourly>, now: Long): List<PrecipEpisode> {
        val episodes = mutableListOf<PrecipEpisode>()
        var run = mutableListOf<Hourly>()
        fun close() {
            if (run.isEmpty()) return
            val peak = run.maxBy { (it.precipitation ?: 0.0) * 100 + (it.precipitationProbability ?: 0) }
            episodes += PrecipEpisode(
                kind = run.maxOf { precipKind(it.weatherCode.takeIf { c -> c?.isPrecipitation == true }) },
                start = maxOf(run.first().time, now),
                end = run.last().time + HOUR,
                peakAt = peak.time,
                peakIntensity = intensity(peak.precipitation ?: 0.0),
                maxProbability = run.maxOf { it.precipitationProbability ?: 100 },
            )
            run = mutableListOf()
        }
        window.forEach { if (it.isWet()) run += it else close() }
        close()
        return episodes
    }

    private fun isPrecipitatingNow(weather: Weather, now: Long): Boolean {
        val minutelyNow = weather.minutely.lastOrNull { it.time <= now }?.precipitation
        val current = weather.current
        return (minutelyNow ?: 0.0) >= MINUTELY_WET_MM ||
            (current?.precipitation ?: 0.0) >= 0.1 ||
            current?.weatherCode?.isPrecipitation == true && (current.precipitation ?: 0.0) > 0.0
    }

    /** First 15-minute step from now on that's wet (or dry, when [wet] is false), within 3 hours. */
    private fun firstMinutely(weather: Weather, now: Long, wet: Boolean): Long? = weather.minutely
        .filter { it.time >= now - 15 * 60 && it.time <= now + 3 * HOUR }
        .firstOrNull { ((it.precipitation ?: 0.0) >= MINUTELY_WET_MM) == wet }
        ?.time

    // endregion

    // region Summary

    private fun summary(
        weather: Weather,
        now: Long,
        window: List<Hourly>,
        episodes: List<PrecipEpisode>,
        nowInsight: NowInsight,
        precise: Boolean,
    ): Summary {
        val activeAlert = weather.alerts.firstOrNull { (it.start ?: 0) <= now && (it.end ?: Long.MAX_VALUE) > now }
        val raining = isPrecipitatingNow(weather, now)
        // Precipitation about to start: 15-minute data first, else the next wet hour.
        val soon = if (raining) {
            null
        } else {
            firstMinutely(weather, now, wet = true)?.takeIf { it > now && it <= now + 90 * 60 }
                ?: episodes.firstOrNull()?.start?.takeIf { it <= now + HOUR }
        }
        // The episode the lead talks about (ongoing or imminent), so the follow-up can skip it.
        val currentEpisode = episodes.firstOrNull { it.start <= (soon ?: now) + HOUR }
            ?.takeIf { raining || soon != null }

        val lead: Lead = when {
            activeAlert != null -> Lead.Alert(activeAlert.title)
            raining -> Lead.PrecipNow(
                kind = currentEpisode?.kind ?: precipKind(weather.current?.weatherCode),
                endsAt = firstMinutely(weather, now, wet = false)?.takeIf { it > now } ?: currentEpisode?.end,
                precise = precise,
            )
            soon != null -> Lead.PrecipSoon(currentEpisode?.kind ?: PrecipKind.RAIN, maxOf(soon, now), precise)
            else -> Lead.Feel(nowInsight.feel, nowInsight.sky)
        }

        val laterEpisodes = when (lead) {
            is Lead.PrecipNow, is Lead.PrecipSoon -> episodes.drop(if (currentEpisode != null) 1 else 0)
            else -> episodes
        }
        val then: Followup? = when (lead) {
            is Lead.PrecipNow -> laterEpisodes.firstOrNull()?.toFollowup()
            is Lead.PrecipSoon -> currentEpisode?.end?.let { Followup.ClearingAt(it) }
            is Lead.Alert -> laterEpisodes.firstOrNull()?.toFollowup()
            is Lead.Feel -> laterEpisodes.firstOrNull()?.toFollowup()
                ?: vsYesterday(weather, now)
                ?: turningCold(window, nowInsight)
                ?: windyLater(window, nowInsight)
                ?: strongUv(window)
                ?: poorAir(weather, now)
                ?: Followup.DryAllDay.takeIf { window.any { it.isDay } }
        }
        return Summary(lead, then)
    }

    private fun PrecipEpisode.toFollowup() = Followup.PrecipLater(kind, start, likely)

    private fun vsYesterday(weather: Weather, now: Long): Followup? {
        val todayIndex = weather.daily.indexOfLast { it.date <= now }
        if (todayIndex < 1) return null
        val today = weather.daily[todayIndex].apparentTemperatureMax ?: weather.daily[todayIndex].temperatureMax
        val yesterday = weather.daily[todayIndex - 1].apparentTemperatureMax ?: weather.daily[todayIndex - 1].temperatureMax
        if (today == null || yesterday == null) return null
        val diff = today - yesterday
        if (abs(diff) < 4) return null
        return Followup.VsYesterday(warmer = diff > 0, much = abs(diff) >= 7)
    }

    private fun turningCold(window: List<Hourly>, nowInsight: NowInsight): Followup? {
        if (nowInsight.feel < TempFeel.COOL) return null
        val start = window.firstOrNull()?.apparentTemperature ?: return null
        val cold = window.firstOrNull { (it.apparentTemperature ?: start) <= start - 6 && tempFeel(it.apparentTemperature!!) <= TempFeel.CHILLY }
            ?: return null
        return Followup.TurningCold(cold.time, tempFeel(cold.apparentTemperature!!))
    }

    private fun windyLater(window: List<Hourly>, nowInsight: NowInsight): Followup? {
        if (nowInsight.wind >= WindLevel.WINDY) return null
        val windy = window.drop(1).firstOrNull { windLevel(it.windSpeed, it.windGusts) >= WindLevel.WINDY } ?: return null
        return Followup.WindyLater(windy.time, windLevel(windy.windSpeed, windy.windGusts))
    }

    private fun strongUv(window: List<Hourly>): Followup? {
        val strong = window.filter { it.isDay && (it.uvIndex ?: 0.0) >= 8 }
        return strong.lastOrNull()?.let { Followup.StrongUv(it.time + HOUR) }
    }

    private fun poorAir(weather: Weather, now: Long): Followup? {
        val aqi = currentAir(weather, now)?.usAqi ?: return null
        val level = airLevel(aqi)
        return Followup.PoorAir(level).takeIf { level >= AirLevel.SENSITIVE }
    }

    // endregion

    // region What to bring

    private fun bring(weather: Weather, now: Long, window: List<Hourly>, episodes: List<PrecipEpisode>): List<BringItem> {
        val items = mutableListOf<BringItem>()
        fun startOrNow(time: Long) = time.takeIf { it > now + HOUR / 2 }

        val wetEpisodes = episodes.filter { it.kind != PrecipKind.SNOW }
        if (wetEpisodes.isNotEmpty()) {
            val first = wetEpisodes.first()
            val windyDuringRain = window.any { h ->
                wetEpisodes.any { h.time >= it.start - HOUR && h.time < it.end } &&
                    windLevel(h.windSpeed, h.windGusts) >= WindLevel.WINDY
            }
            items += BringItem(
                kind = if (windyDuringRain) BringKind.RAIN_JACKET else BringKind.UMBRELLA,
                from = startOrNow(first.start),
                until = wetEpisodes.last().end,
            )
        }

        val apparent = window.mapNotNull { h -> (h.apparentTemperature ?: h.temperature)?.let { h to it } }
        val snowing = episodes.any { it.kind == PrecipKind.SNOW }
        val freezing = apparent.firstOrNull { it.second < 0 }
        when {
            snowing || freezing != null -> items += BringItem(
                BringKind.WARM_LAYERS,
                from = startOrNow(freezing?.first?.time ?: episodes.first { it.kind == PrecipKind.SNOW }.start),
            )
            else -> apparent.firstOrNull { it.second < 14 }?.let { (hour, _) ->
                items += BringItem(BringKind.JACKET, from = startOrNow(hour.time))
            }
        }

        val sunny = window.filter { it.isDay && (it.uvIndex ?: 0.0) >= 6 }
        if (sunny.isNotEmpty()) {
            items += BringItem(BringKind.SUNSCREEN, from = startOrNow(sunny.first().time), until = sunny.last().time + HOUR)
        }

        apparent.firstOrNull { it.second >= 32 }?.let { (hour, _) ->
            items += BringItem(BringKind.WATER, from = startOrNow(hour.time))
        }

        weather.airQuality
            .filter { it.time + HOUR > now && it.time < now + HORIZON_SECONDS }
            .firstOrNull { (it.usAqi ?: 0) > 150 }
            ?.let { items += BringItem(BringKind.MASK, from = startOrNow(it.time)) }

        return items
    }

    // endregion

    // region Cards

    private fun hourlyOutlook(window: List<Hourly>, episodes: List<PrecipEpisode>): HourlyOutlook {
        val next = window.take(8)
        val nowCloud = cloudiness(next.firstOrNull()?.weatherCode)
        val later = next.drop(1).map { cloudiness(it.weatherCode) }
        val trend = when {
            later.isEmpty() -> if (nowCloud <= 1) SkyTrend.STAYING_CLEAR else SkyTrend.STAYING_CLOUDY
            later.all { it <= 1 } && nowCloud <= 1 -> SkyTrend.STAYING_CLEAR
            later.all { it >= 2 } && nowCloud >= 2 -> SkyTrend.STAYING_CLOUDY
            nowCloud <= 1 && later.takeLast(3).average() >= 2 -> SkyTrend.CLOUDING_OVER
            nowCloud >= 2 && later.takeLast(3).average() <= 1 -> SkyTrend.CLEARING_UP
            else -> SkyTrend.MIXED
        }
        return HourlyOutlook(trend, episodes.firstOrNull())
    }

    private fun rainOutlook(weather: Weather, now: Long, episodes: List<PrecipEpisode>): RainOutlook {
        val bars = weather.hourly
            .filter { it.time + HOUR > now }
            .take(12)
            .map { RainBar(it.time, it.precipitationProbability ?: 0, it.precipitation ?: 0.0) }
        val first = episodes.firstOrNull()
        return when {
            first == null -> RainOutlook.Dry(bars)
            first.start <= now || isPrecipitatingNow(weather, now) -> RainOutlook.Now(first, bars)
            else -> RainOutlook.Later(first, bars)
        }
    }

    private fun uvOutlook(weather: Weather, now: Long, window: List<Hourly>): UvOutlook? {
        val uvNow = weather.current?.uvIndex ?: window.firstOrNull()?.uvIndex ?: return null
        val todayMax = weather.daily.lastOrNull { it.date <= now }?.uvIndexMax ?: uvNow
        val high = window.filter { it.isDay && (it.uvIndex ?: 0.0) >= 6 }
        return UvOutlook(
            now = uvLevel(uvNow),
            todayMax = uvLevel(todayMax),
            sunscreenFrom = high.firstOrNull()?.time,
            sunscreenUntil = high.lastOrNull()?.time?.plus(HOUR),
        )
    }

    private fun sunInfo(weather: Weather, now: Long): SunInfo? {
        val todayIndex = weather.daily.indexOfLast { it.date <= now }
        val today = weather.daily.getOrNull(todayIndex) ?: return null
        val sunrise = today.sunrise ?: return null
        val sunset = today.sunset ?: return null
        val next = when {
            now < sunrise -> SunEvent(isSunrise = true, at = sunrise)
            now < sunset -> SunEvent(isSunrise = false, at = sunset)
            else -> weather.daily.getOrNull(todayIndex + 1)?.sunrise?.let { SunEvent(isSunrise = true, at = it) }
                ?: return null
        }
        val progress = if (now in sunrise until sunset) (now - sunrise).toFloat() / (sunset - sunrise) else null
        return SunInfo(sunrise, sunset, next, progress)
    }

    private fun currentAir(weather: Weather, now: Long) =
        weather.airQuality.lastOrNull { it.time <= now && it.usAqi != null }

    private fun airOutlook(weather: Weather, now: Long): AirOutlook? {
        val hour = currentAir(weather, now) ?: return null
        val aqi = hour.usAqi ?: return null
        val pollenToday = weather.airQuality
            .filter { it.time + HOUR > now && it.time < now + HORIZON_SECONDS }
            .mapNotNull { it.pollen?.max }
            .maxOrNull()
        return AirOutlook(airLevel(aqi), aqi, pollenToday?.let(::pollenLevel))
    }

    // endregion
}
