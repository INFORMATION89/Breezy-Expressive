package io.github.information89.breezyexpressive.insight

/*
 * What the rules engine produces: structured facts, not text. The UI renders them with string
 * resources, so they translate cleanly and the rules stay unit-testable. Times are epoch seconds.
 */

data class Insights(
    val now: NowInsight,
    val summary: Summary,
    val bring: List<BringItem>,
    val hourly: HourlyOutlook,
    val rain: RainOutlook,
    val uv: UvOutlook?,
    val sun: SunInfo?,
    val air: AirOutlook?,
)

data class NowInsight(
    val feel: TempFeel,
    val sky: Sky,
    val wind: WindLevel,
    /** "Muggy" / "Dry air" only when notable. */
    val humidity: HumidityNote?,
)

enum class HumidityNote { MUGGY, DRY }

/** The one-line summary: a lead clause and an optional follow-up ("Warm and sunny, storms likely after 6 pm"). */
data class Summary(val lead: Lead, val then: Followup?)

sealed interface Lead {
    data class Alert(val title: String) : Lead
    data class PrecipNow(val kind: PrecipKind, val endsAt: Long?, val precise: Boolean) : Lead
    data class PrecipSoon(val kind: PrecipKind, val startsAt: Long, val precise: Boolean) : Lead
    data class Feel(val feel: TempFeel, val sky: Sky) : Lead
}

sealed interface Followup {
    data class PrecipLater(val kind: PrecipKind, val startsAt: Long, val likely: Boolean) : Followup
    data class ClearingAt(val at: Long) : Followup
    data class VsYesterday(val warmer: Boolean, val much: Boolean) : Followup
    data class TurningCold(val at: Long, val feel: TempFeel) : Followup
    data class WindyLater(val at: Long, val level: WindLevel) : Followup
    data class StrongUv(val until: Long) : Followup
    data class PoorAir(val level: AirLevel) : Followup
    data object DryAllDay : Followup
}

enum class BringKind { UMBRELLA, RAIN_JACKET, JACKET, WARM_LAYERS, SUNSCREEN, WATER, MASK }

/** Something to bring, and when it's needed (null [from] = already now). */
data class BringItem(val kind: BringKind, val from: Long? = null, val until: Long? = null)

/** A stretch of hours with likely precipitation. */
data class PrecipEpisode(
    val kind: PrecipKind,
    val start: Long,
    /** First dry moment after the episode. */
    val end: Long,
    val peakAt: Long,
    val peakIntensity: Intensity,
    val maxProbability: Int,
) {
    val likely: Boolean get() = maxProbability < 70
}

enum class SkyTrend { STAYING_CLEAR, STAYING_CLOUDY, CLOUDING_OVER, CLEARING_UP, MIXED }

data class HourlyOutlook(val trend: SkyTrend, val nextPrecip: PrecipEpisode?)

sealed interface RainOutlook {
    val bars: List<RainBar>

    data class Dry(override val bars: List<RainBar>) : RainOutlook
    data class Now(val episode: PrecipEpisode, override val bars: List<RainBar>) : RainOutlook
    data class Later(val episode: PrecipEpisode, override val bars: List<RainBar>) : RainOutlook
}

/** One hour of the precipitation chart. */
data class RainBar(val time: Long, val probability: Int, val amount: Double)

data class UvOutlook(
    val now: UvLevel,
    val todayMax: UvLevel,
    /** When sunscreen is needed (UV high or above), null if not today. */
    val sunscreenFrom: Long?,
    val sunscreenUntil: Long?,
)

data class SunInfo(
    val sunrise: Long,
    val sunset: Long,
    val next: SunEvent,
    /** 0 at sunrise, 1 at sunset; null at night. */
    val dayProgress: Float?,
)

data class SunEvent(val isSunrise: Boolean, val at: Long)

data class AirOutlook(val level: AirLevel, val usAqi: Int, val pollen: PollenLevel?)
