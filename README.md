# Breezy Expressive (working title)

A practical, city-life weather app for Android 12+, built with Jetpack Compose and Material 3 Expressive.
Made so you understand the weather at a glance, without reading numbers.

> **Status:** early development (M2 done: live Forecast page with weather background and frosted glass; locations come in M3). See [docs/PLAN.md](docs/PLAN.md) for the plan.

## Planned for v1

- Forecast: current conditions, hourly, 7 days, with a plain-language summary of what's going on
- Your Day: a timeline of the day's weather and what to bring
- Saved locations with search and GPS
- Weather alerts, air quality and pollen
- Live Updates for rain starting or stopping, and active alerts
- Open-Meteo by default (no setup); AccuWeather optional with your own API key

## Building

Requirements: Android Studio (or JDK 17+) and the Android SDK with API 37.

```bash
./gradlew :app:assembleDebug
```

## Credits

- This app is built on data code from **[Breezy Weather](https://github.com/breezy-weather/breezy-weather)**
  (LGPL-3.0). It is an independent project and is not affiliated with or endorsed by Breezy Weather.
- Weather data: [Open-Meteo](https://open-meteo.com/) (CC BY 4.0) and, optionally, AccuWeather.
- Fonts: [Roboto Flex](https://github.com/googlefonts/roboto-flex) and
  [Roboto Serif](https://github.com/googlefonts/roboto-serif), SIL Open Font License 1.1
  (see [third_party/fonts](third_party/fonts)).
- Weather icons: [Material Symbols](https://github.com/google/material-design-icons), Apache License 2.0.
- Frosted glass: [Haze](https://github.com/chrisbanes/haze), Apache License 2.0.

## License

[GNU LGPL-3.0](LICENSE), which builds on the [GNU GPL-3.0](COPYING), with the
[additional terms](LICENSE_ADDITIONAL) inherited from Breezy Weather.
