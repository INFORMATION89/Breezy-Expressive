package io.github.information89.breezyexpressive.ui.forecast

/*
 * TEMPORARY sample data for the layout mock. Replaced by real data from the M1 data layer.
 * The text here (words-first descriptions) will come from the insight rules engine and string resources.
 */

data class SampleHour(val time: String, val icon: String, val temp: Int, val rainChance: Int)

object ForecastSample {
    const val location = "Mexico City"
    const val temperature = "23°"
    const val condition = "Partly cloudy"
    const val highLow = "High 26° · Low 12°"
    const val summary = "Warm and sunny, storms likely after 6 pm."

    const val hourlyHeadline = "Clouding over, storms from 6 pm"
    val hours = listOf(
        SampleHour("Now", "⛅", 23, 0),
        SampleHour("2 pm", "☀️", 25, 0),
        SampleHour("3 pm", "☀️", 26, 5),
        SampleHour("4 pm", "⛅", 25, 15),
        SampleHour("5 pm", "☁️", 23, 40),
        SampleHour("6 pm", "⛈️", 19, 80),
        SampleHour("7 pm", "🌧️", 17, 70),
        SampleHour("8 pm", "🌧️", 16, 45),
        SampleHour("9 pm", "☁️", 15, 20),
        SampleHour("10 pm", "🌙", 14, 5),
    )

    const val rainHeadline = "Storms from 6 pm"
    const val rainDetail = "Heaviest around 6–7 pm, dry again by 10 pm"

    const val uvWord = "High"
    const val uvAdvice = "Sunscreen until 4 pm"
    const val uvIndex = 7

    const val sunsetTime = "7:42 pm"
    const val sunriseTime = "7:01 am"
    const val sunDetail = "Sunset in 5 h 40 min"
    /** How far through the daylight we are, 0 = sunrise, 1 = sunset. */
    const val dayProgress = 0.55f
}
