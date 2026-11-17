package io.github.information89.breezyexpressive.insight

import io.github.information89.breezyexpressive.data.model.AirQualityHour
import io.github.information89.breezyexpressive.data.model.Coverage
import io.github.information89.breezyexpressive.data.model.Current
import io.github.information89.breezyexpressive.data.model.Daily
import io.github.information89.breezyexpressive.data.model.Feature
import io.github.information89.breezyexpressive.data.model.FeatureCoverage
import io.github.information89.breezyexpressive.data.model.Hourly
import io.github.information89.breezyexpressive.data.model.Minutely
import io.github.information89.breezyexpressive.data.model.Weather
import io.github.information89.breezyexpressive.data.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineTest {

    // A day starting at local midnight T0; "now" is 13:00.
    private val t0 = 1_800_000_000L - 1_800_000_000L % 86_400
    private fun at(hour: Int, minute: Int = 0) = t0 + hour * 3600L + minute * 60L
    private val now = at(13)

    private fun hours(
        from: Int = 0,
        to: Int = 36,
        spec: (Int) -> Hourly.() -> Hourly = { { this } },
    ) = (from until to).map { h ->
        val base = Hourly(
            time = at(h),
            temperature = 24.0,
            apparentTemperature = 24.0,
            weatherCode = WeatherCode.CLEAR,
            isDay = h % 24 in 7..18,
            precipitationProbability = 0,
            precipitation = 0.0,
            windSpeed = 10.0,
            windGusts = 15.0,
            uvIndex = if (h % 24 in 10..15) 4.0 else 0.0,
        )
        spec(h)(base)
    }

    private fun weather(
        hourly: List<Hourly> = hours(),
        current: Current = Current(time = now, temperature = 24.0, apparentTemperature = 24.0, weatherCode = WeatherCode.CLEAR, isDay = true, uvIndex = 4.0),
        daily: List<Daily> = listOf(
            Daily(date = t0 - 86_400, apparentTemperatureMax = 25.0, sunrise = at(-17), sunset = at(-5)),
            Daily(date = t0, apparentTemperatureMax = 25.0, sunrise = at(7), sunset = at(19, 30), uvIndexMax = 4.0),
            Daily(date = t0 + 86_400, sunrise = at(31), sunset = at(43)),
        ),
        minutely: List<Minutely> = emptyList(),
        air: List<AirQualityHour> = emptyList(),
        preciseMinutely: Boolean = true,
    ) = Weather(
        updatedAt = now,
        timeZone = "UTC",
        current = current,
        hourly = hourly,
        daily = daily,
        minutely = minutely,
        airQuality = air,
        coverage = mapOf(Feature.MINUTELY to FeatureCoverage("test", if (preciseMinutely) Coverage.FULL else Coverage.ESTIMATED)),
    )

    @Test
    fun `warm and sunny with storms later, bring an umbrella`() {
        val w = weather(
            hourly = hours { h ->
                {
                    if (h in 18..20) {
                        copy(weatherCode = WeatherCode.THUNDERSTORM, precipitationProbability = 80, precipitation = 4.0, isDay = h < 19)
                    } else {
                        this
                    }
                }
            },
        )
        val insights = InsightEngine.analyze(w, now)

        assertEquals(Lead.Feel(TempFeel.WARM, Sky.SUNNY), insights.summary.lead)
        assertEquals(Followup.PrecipLater(PrecipKind.STORM, at(18), likely = false), insights.summary.then)
        assertEquals(BringItem(BringKind.UMBRELLA, from = at(18), until = at(21)), insights.bring.first())
        val rain = insights.rain as RainOutlook.Later
        assertEquals(at(21), rain.episode.end)
        assertEquals(Intensity.MODERATE, rain.episode.peakIntensity)
        // "Clouding over, storms from 6 pm"
        assertEquals(SkyTrend.CLOUDING_OVER, insights.hourly.trend)
        assertEquals(at(18), insights.hourly.nextPrecip!!.start)
    }

    @Test
    fun `raining now and stopping in 20 minutes`() {
        val w = weather(
            current = Current(time = now, apparentTemperature = 16.0, weatherCode = WeatherCode.RAIN, precipitation = 0.6),
            hourly = hours { h -> { if (h == 13) copy(weatherCode = WeatherCode.RAIN, precipitationProbability = 90, precipitation = 1.0) else this } },
            minutely = listOf(
                Minutely(at(12, 45), 0.4), Minutely(at(13), 0.3), Minutely(at(13, 15), 0.2), Minutely(at(13, 30), 0.0),
            ),
        )
        val lead = InsightEngine.analyze(w, at(13, 10)).summary.lead

        assertEquals(Lead.PrecipNow(PrecipKind.RAIN, endsAt = at(13, 30), precise = true), lead)
    }

    @Test
    fun `rain starting soon uses 15-minute data and says when it is estimated`() {
        val w = weather(
            hourly = hours { h -> { if (h == 14) copy(weatherCode = WeatherCode.RAIN, precipitationProbability = 70, precipitation = 1.2) else this } },
            minutely = listOf(Minutely(at(13), 0.0), Minutely(at(13, 15), 0.0), Minutely(at(13, 30), 0.0), Minutely(at(13, 45), 0.3)),
            preciseMinutely = false,
        )
        val summary = InsightEngine.analyze(w, now).summary

        assertEquals(Lead.PrecipSoon(PrecipKind.RAIN, startsAt = at(13, 45), precise = false), summary.lead)
        assertEquals(Followup.ClearingAt(at(15)), summary.then)
    }

    @Test
    fun `dry pleasant day`() {
        val insights = InsightEngine.analyze(weather(), now)

        assertEquals(Followup.DryAllDay, insights.summary.then)
        assertTrue(insights.rain is RainOutlook.Dry)
        assertEquals(12, insights.rain.bars.size)
        assertTrue(insights.bring.none { it.kind == BringKind.UMBRELLA })
    }

    @Test
    fun `much colder than yesterday`() {
        val w = weather(
            daily = listOf(
                Daily(date = t0 - 86_400, apparentTemperatureMax = 30.0),
                Daily(date = t0, apparentTemperatureMax = 21.0, sunrise = at(7), sunset = at(19)),
            ),
        )
        assertEquals(Followup.VsYesterday(warmer = false, much = true), InsightEngine.analyze(w, now).summary.then)
    }

    @Test
    fun `turning cold tonight means a jacket for later`() {
        val w = weather(
            hourly = hours { h -> { if (h >= 20) copy(apparentTemperature = 11.0, temperature = 12.0) else this } },
        )
        val insights = InsightEngine.analyze(w, now)

        assertEquals(Followup.TurningCold(at(20), TempFeel.CHILLY), insights.summary.then)
        assertEquals(BringItem(BringKind.JACKET, from = at(20)), insights.bring.single { it.kind == BringKind.JACKET })
    }

    @Test
    fun `high UV means sunscreen until it drops`() {
        val w = weather(
            hourly = hours { h -> { if (h in 11..15) copy(uvIndex = 7.0) else this } },
        )
        val sunscreen = InsightEngine.analyze(w, at(10)).bring.single { it.kind == BringKind.SUNSCREEN }

        assertEquals(at(11), sunscreen.from)
        assertEquals(at(16), sunscreen.until)
    }

    @Test
    fun `windy rain means a rain jacket instead of an umbrella`() {
        val w = weather(
            hourly = hours { h ->
                { if (h in 16..17) copy(weatherCode = WeatherCode.RAIN, precipitationProbability = 80, precipitation = 2.0, windSpeed = 40.0, windGusts = 65.0) else this }
            },
        )
        val kinds = InsightEngine.analyze(w, now).bring.map { it.kind }

        assertTrue(BringKind.RAIN_JACKET in kinds)
        assertTrue(BringKind.UMBRELLA !in kinds)
    }

    @Test
    fun `unhealthy air means a mask`() {
        val w = weather(air = listOf(AirQualityHour(time = at(13), usAqi = 170)))
        val insights = InsightEngine.analyze(w, now)

        assertEquals(AirLevel.UNHEALTHY, insights.air!!.level)
        assertTrue(insights.bring.any { it.kind == BringKind.MASK })
        assertNull(insights.air!!.pollen)
    }

    @Test
    fun `sun position and next event`() {
        val sun = InsightEngine.analyze(weather(), now).sun!!

        assertEquals(SunEvent(isSunrise = false, at = at(19, 30)), sun.next)
        assertEquals(6f / 12.5f, sun.dayProgress!!, 0.001f)

        val night = InsightEngine.analyze(weather(), at(22)).sun!!
        assertEquals(SunEvent(isSunrise = true, at = at(31)), night.next)
        assertNull(night.dayProgress)
    }

    @Test
    fun `words for numbers`() {
        assertEquals(TempFeel.CHILLY, tempFeel(12.7))
        assertEquals(TempFeel.HOT, tempFeel(30.0))
        assertEquals(WindLevel.BREEZY, windLevel(25.0))
        assertEquals(WindLevel.STRONG, windLevel(20.0, gustsKmh = 70.0))
        assertEquals(UvLevel.HIGH, uvLevel(7.0))
        assertEquals(AirLevel.MODERATE, airLevel(79))
        assertEquals(PollenLevel.HIGH, pollenLevel(120.0))
    }
}
