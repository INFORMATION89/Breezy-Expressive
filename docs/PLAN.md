# Breezy Expressive (working title): Plan

> Status: **approved direction, starting M0**. Change anything at any time.

A practical, city-life weather app for Android 12+, built from scratch with Material 3 Expressive.
It has grown beyond a Breezy Weather fork: it's **its own app** that reuses Breezy Weather's proven data
logic (models, Open-Meteo and AccuWeather clients) under the LGPL-3.0, with credit.

**About the name:** "Breezy Expressive" is a placeholder. Before the first public release we pick a new name
and package ID, because a package ID can't change once published. The name, package and theme are kept in one place
(`applicationId`, `namespace`, `app_name`, `Theme`) so the rename is mechanical. The final name should not include
"Breezy", to avoid being confused with the original project.

---

## 1. Decisions so far

| Topic | Decision |
|---|---|
| Approach | Slim rebuild; copy only essential Breezy code |
| App name / ID | Working title "Breezy Expressive" / `io.github.information89.breezyexpressive`; rename before first release |
| Min Android | Android 12 (minSdk 31), target/compile SDK 37 |
| Default source | Open-Meteo: forecast, 15-min precipitation, air quality, pollen (Europe), search |
| Optional source | AccuWeather with the user's own API key: alerts and optionally the forecast |
| v1 features | Current, hourly, 7-day daily, saved locations + GPS, alerts, air quality and pollen, Live Updates, Your Day timeline (weather + routines, no calendar) |
| Design principle | Made for city life: understandable without reading numbers (5.0) |
| Pages | Forecast · Your Day · Locations (swipeable), Settings from a toolbar gear |
| Fonts | Roboto Flex for data; serif (Roboto Serif first) for explanatory text |
| Language | English only for v1; every string in resources, locale-aware formatting, ready for translation |
| v2 | Calendar integration in Your Day |
| Later | Home screen widgets, rain radar, more sources, translations |
| License | LGPL-3.0 for the whole project (simplest, since copied Breezy files must stay LGPL); keep Breezy copyright headers on copied files and credit it in About + README |

---

## 2. Architecture

**One app module, organized by package.** No multi-module setup: at this size, modules add build
time and ceremony without a payoff. We can split later if it grows.

```
app/src/main/kotlin/io/github/information89/breezyexpressive/
├── data/
│   ├── model/          # Weather, Location, Alert, AirQuality… (trimmed from Breezy `domain`)
│   ├── source/
│   │   ├── WeatherSource.kt        # one small interface
│   │   ├── openmeteo/              # adapted from Breezy
│   │   └── accu/                   # adapted from Breezy (developer API only)
│   ├── location/       # GPS (platform LocationManager, no Google Play Services)
│   ├── routine/        # user routines (calendar access joins here in v2)
│   ├── repository/     # WeatherRepository: cache-first, merge sources
│   └── store/          # DataStore (settings, appearance, saved locations) + JSON file cache
├── insight/            # rules engine: summary sentence, words-not-numbers, "what to bring" (pure Kotlin, unit-tested)
├── liveupdate/         # ongoing notification / Android 16 Live Update + worker
├── ui/
│   ├── theme/          # Expressive theme, dynamic color, two type voices, shapes, motion, Appearance
│   ├── material/       # gradient + grain background shader, glass surfaces
│   ├── components/     # shared Expressive building blocks
│   ├── forecast/  yourday/  locations/  details/  settings/  onboarding/
│   └── navigation/     # pager + floating toolbar; Nav 3 for Settings and location detail
└── App.kt              # manual dependency container (no Hilt)
```

**The source interface** (the only abstraction we need):

```kotlin
interface WeatherSource {
    val id: String
    val features: Set<Feature>          // FORECAST, CURRENT, MINUTELY, ALERTS, AIR_QUALITY, POLLEN
    suspend fun fetch(location: Location, features: Set<Feature>): WeatherPart
}
```

The repository asks each feature's chosen source for its part, then merges the parts into one
`Weather`. For example: Open-Meteo gives forecast + air quality + minutely, AccuWeather gives alerts.

**Data flow:** UI ← `StateFlow<UiState>` ← ViewModel ← Repository ← (cache file → network).
The UI shows the cache instantly and refreshes in the background. Saved data loads offline.

### Libraries (deliberately few)

| Need | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3 `1.5.x` (Expressive APIs) | Expressive components, shapes and motion |
| Navigation | Navigation 3 | Modern, type-safe, simple back stack |
| Network | Retrofit 3 + OkHttp + kotlinx.serialization | Lets us reuse Breezy's API interfaces almost unchanged |
| Storage | DataStore + JSON cache files | Avoids a database for a handful of locations |
| Background | WorkManager | Live Update refresh, alert checks |
| Location | Platform `LocationManager` | Works on de-Googled phones, no Play Services dependency |
| Charts | Custom Compose `Canvas` | Lighter than a chart library, fully on-brand |
| Glass blur | Haze | Backdrop blur for translucent surfaces (Android 12+) |
| Fonts | Roboto Flex (variable) + room for more | Expanded width axis for the hero |
| Dropped from Breezy | Hilt, RxJava, SQLDelight, Vico, AboutLibraries, View/XML UI | Simplicity and size |

**Performance targets:** release build with R8 + baseline profile, cold start < 500 ms on a
mid-range phone, APK < 10 MB (the two variable fonts are ~2–4 MB of that; we subset them to Latin), no work on the
main thread, one network round per source per refresh.

**Biggest performance risk:** a live-blurred glass layer over an *animated* background forces the blur to re-render
every frame. Mitigations: the background animates slowly at a capped frame rate, stops while scrolling or swiping
pages, pauses off-screen, and the slider/setting can turn animation off. We measure this in M2 before building more on it.

### What we copy from Breezy

- `domain/weather/model/*` → trimmed to the fields we display.
- `domain/weather/reference/WeatherCode.kt`, `AlertSeverity.kt`.
- `sources/openmeteo/**` (~1,500 lines) → adapted to our interface.
- `src_nonfreenet/.../accu/**` (developer API parts only) → adapted.
- Unit/conversion helpers we actually use (from `weather-unit`).

---

## 3. Data sources

**Open-Meteo** (default, no key, no setup)
- `api.open-meteo.com/v1/forecast`: current, hourly, daily, `minutely_15`.
  - Caveat: true 15-minute data exists mainly for Europe and North America. Elsewhere it's interpolated from
    hourly data, so the wording adapts: "Rain in 12 min" where data is precise, "Rain likely within the hour" where it isn't.
- Terms: free for non-commercial use, with attribution shown in the app.
- `air-quality-api.open-meteo.com/v1/air-quality`: AQI, pollutants, pollen (Europe only; card hidden elsewhere).
- `geocoding-api.open-meteo.com/v1/search`: city search.

**AccuWeather** (optional, Settings → Sources → paste API key)
- Developer API: location key, current, hourly, daily, alerts.
- Each refresh takes ~3–5 calls per location, and free keys have a small daily quota. So:
  - cache the AccuWeather location key per saved location (it never changes),
  - show AccuWeather's required attribution wherever its data appears,
  - refresh alerts at most every 30–60 min,
  - Live Updates always use Open-Meteo `minutely_15`, never AccuWeather.
- The key is stored only on the device, never in the repo. For dev builds, an optional
  `local.properties` entry (`accu.key=`) is git-ignored.
- Per-feature choice: "Forecast from: Open-Meteo / AccuWeather", "Alerts from: AccuWeather / Off".

**Coverage is shown, not hidden.** When choosing a source (and in the source footer of each page), the app says
whether you get **full** or **partial** data *for your locations*. Each source declares its coverage per feature
(as Breezy's `isFeatureSupportedForLocation` does), and the picker shows it in plain words:

```
Open-Meteo                         ● Partial here
  ✓ Forecast, hourly, 7 days
  ✓ Air quality
  ~ Rain timing: estimated (precise 15-min data not available in Mexico)
  ✗ Pollen: Europe only
  ✗ Alerts: not provided, choose an alerts source
```

- Labels: **Full** (everything available), **Partial** (some features missing or estimated), with the
  missing pieces listed. Features from another source fill gaps automatically and say so.
- The UI never shows an empty card: a feature without data is hidden, and the picker explains why.

---

## 4. Live Updates (like Gradient Weather)

An ongoing notification that appears **only while something is happening** and dismisses itself.

| Trigger | Content |
|---|---|
| Rain/snow starting within ~60 min | "Rain in 12 min · light" with a countdown and an intensity bar |
| Precipitation ongoing | "Rain stops in ~25 min" |
| Severe alert active | Severity, title, ends-at time |

- **Android 16+:** a promoted ongoing notification (Live Update) with `ProgressStyle`: a status bar chip,
  lock screen, and Samsung's Now Bar.
- **Android 12–15:** the same content as a standard ongoing notification.
- **Engine:** a periodic WorkManager job every 15 min (Android's minimum) checks the primary location's
  `minutely_15` data and posts, updates or cancels the notification. While an event is near, it chains one-time
  jobs for extra checks. The countdown itself ticks on its own (a notification chronometer counting to the event
  time), so it doesn't need refreshes every minute. The user can toggle it in Settings
  and choose which location it follows. Default: the current location, falling back to the starred one if
  location is off or unavailable.

---

## 5. UX (for discussion before any UI code)

### 5.0 Core principle: understandable without numbers

A glance should tell you what's going on **even if you never read a number**. Numbers are there for
detail, never as the only way to get the message. Every piece of data has a non-numeric layer:

| Data | Non-numeric layer |
|---|---|
| Overall conditions | The summary sentence (5.3) + the background gradient's mood (sunny, stormy, night) |
| Temperature | Words ("Warm", "Chilly", "Much colder than yesterday") + warm/cool color along range bars |
| Precipitation | "Rain in 40 min", "Dry all day" + intensity drawn as bars/drops, not just % |
| Wind | "Calm", "Breezy", "Strong gusts" + an animated icon whose motion scales with speed |
| UV | "Low" … "Extreme" + a "Sunscreen" hint when relevant |
| Air quality / pollen | "Good", "Moderate", "Unhealthy" + a color-coded chip |
| Alerts | Severity color + plain title ("Thunderstorm warning until 9 pm") |
| Humidity, pressure, visibility | Words only when notable ("Muggy", "Foggy"); otherwise kept quiet in tiles |

Rule of thumb for every component: **word or picture first, number second, in smaller type.**

### 5.1 Structure: 3 pages + floating toolbar

```
┌──────────────────────────────┐
│  gradient + grain background │   pages swipe horizontally (HorizontalPager)
│  ┌────────────────────────┐  │   and stay in sync with the toolbar
│  │ translucent cards      │  │
│  └────────────────────────┘  │
│                              │
│   ╭──────────────────────╮   │   Expressive floating toolbar:
│   │ ☀ Forecast  ◷  ⌕  │ ⚙ │   │   translucent + blurred, pill shape,
│   ╰──────────────────────╯   │   selected item morphs/expands with its label
└──────────────────────────────┘
```

| Page | Content |
|---|---|
| 1. Forecast | Primary location (current location or the starred one) |
| 2. Your Day | A timeline of today: what to bring and when, optionally merged with your calendar (5.8) |
| 3. Locations | Search bar + rich list of saved locations |
| ⚙ Settings | Not a swipe page: a gear at the toolbar's end opens it as a full screen (5.5) |

Why this order: Forecast and Your Day both answer "what does today mean for me", so they sit next to each other.
Locations is where you go to look elsewhere. Settings is visited rarely, so it doesn't take a page.

- **Swipe between pages**, plus tap on the toolbar. The toolbar indicator follows the swipe position.
- **Gesture conflicts:** the hourly strip and charts scroll horizontally inside a horizontally swiping page.
  Inner scrollers consume the drag first; the page swipes only from their edges or from outside them.
  We'll test this early in M2, because it's the main UX risk of swipeable pages.
- The toolbar hides on scroll-down and returns on scroll-up (Expressive behavior), so content gets the full screen.
- Predictive back: on Your Day or Locations, back returns to Forecast. Back from Settings returns to the page you came from.

### 5.2 Visual language: materials

- **Background:** an abstract, slowly animated **gradient that represents the weather and time of day**.
  For example, warm amber → coral for a sunny afternoon, slate → teal for rain, deep indigo with soft
  highlights for a clear night. Plus a **film grain** on top: noticeable by default (an Arc-browser-like
  texture), with a **grain slider** in Settings → Appearance (from off to strong).
  - Android 13+: an AGSL `RuntimeShader` (gradient + noise in one GPU pass, cheap).
  - Android 12: a static gradient + a pre-rendered tiling noise texture.
  - Respects "Remove animations": static when animations are off; pauses when the app isn't visible.
- **Translucent, blurred surfaces:** everything that floats over something else (cards, toolbar,
  sheets, search bar) is frosted glass: a backdrop blur + a translucent tint from the color scheme + a faint
  1 px highlight border + the same grain, so surfaces feel physical.
  - Implementation: the **Haze** library (backdrop blur via `RenderEffect`, available from Android 12, which is our min).
  - Performance budget: one blur source (the background) and blurred children that share it. Blur radius tuned
    on a mid-range phone. If a device is slow, fall back to tint-only (no blur) automatically.
- **Color:** the dynamic (wallpaper) scheme drives the text and accent colors. The weather gradient drives the
  background. Contrast is checked so text on glass always meets WCAG AA.
- **Typography, in two voices:**
  - **Data voice (sans):** **Roboto Flex** for numbers, labels and the expanded hero temperature
    (variable, with a width axis for the expanded style; OFL license).
  - **Narrative voice (serif, to experiment with):** for text that *explains* the weather: the hero summary
    sentence, Your Day advice, Live Update text. First candidate: **Roboto Serif** (variable, designed to pair with
    Roboto Flex, OFL). Others to try later: Fraunces, Newsreader, Source Serif 4.
  - The type roles (`data*`, `narrative*`, `display*`) are defined in one place, so changing a font is a one-line change.
- **Customizable UI, built in from the start:** all "look" values (grain, blur, background animation, fonts,
  weather tint) live in one `Appearance` settings object that the theme reads. New sliders and options can be
  added later without touching the screens.

### 5.3 Forecast page: everything important without scrolling

The goal is that the first screen answers "what's it like now, and what's coming today and this week" with no swipe.

```
┌──────────────────────────────┐
│ ◉ Mexico City · now     ★  ⟳ │  location chip (tap → Locations)
│ 23°  Partly cloudy           │  WIDE, SHORT hero, expanded font
│      H 26° L 12° · Feels 22° │
│ "Warm and sunny, storms      │  one-line summary sentence
│  likely after 6 pm."         │
├──────────────────────────────┤
│ ⚠ Thunderstorm warning   ›   │  alert card (only when active)
├──────────────────────────────┤
│ Rain in 40 min ▁▂▃▅▆▅▃▁      │  next-hour card (only when rain is near)
├──────────────────────────────┤
│ Now 14 15 16 17 18 19 20 …   │  hourly strip: icon, temp curve, % rain
├──────────────────────────────┤
│ Today  ☀  12° ━━━━●━━ 26°    │  daily (7 days): first 5 visible,
│ Wed    ⛅  11° ━━━━━━  24°    │  range bars
│ …                            │
├──────────────────────────────┤
│ [UV 7] [AQI 42] [Wind 12]    │  compact tiles row/grid (scroll for more)
│ [Humid] [Sunset] [Pollen]    │
└──────────────────────────────┘
```

- The hero is a horizontal band (temperature left, condition + high/low/feels like right), not a tall block.
  Saving height there is what fits hourly + daily on the first screen.
- **Summary sentence:** one short, plain-language line that tells you what's going on at a glance,
  without reading numbers. Examples:
  - "Warm and sunny, storms likely after 6 pm."
  - "Rain stopping in about 20 minutes, then clearing."
  - "Much colder than yesterday, bring a jacket."
  - "Clear night, poor air quality."
  - It's generated **on the device by simple rules** (no AI, no network, works offline). The rules pick the most
    important fact from a priority list: active alert > precipitation starting/stopping soon > big change
    vs. yesterday > notable conditions later today (storm, wind, UV, heat) > the general feel of the day.
  - Template-based, with the templates in string resources, so it can be translated later (English only for v1).
  - Set in the narrative (serif) voice.
  - The same engine can write the Live Update text and the location card subtitle.
- Everything below the fold is extra: more days, the rest of the tiles, attribution.
- Tap a card → it expands in place or opens a sheet (hourly chart, a day's details, air quality, alert text).
- Pull-to-refresh with the Expressive loading indicator.

### 5.4 Locations page: maximum info before opening

- **Search bar at the top** (translucent). Typing turns the list into results from Open-Meteo geocoding.
  The "Use my location" chip sits under it.
- **Rich location cards:** name + local time, temperature (expanded font), condition, high/low,
  a **mini 6–8 hour strip**, the summary sentence (shortened), plus badges for an active alert, rain soon and air quality.
  Each card's tint follows its own weather.
- Long-press to star (becomes the Forecast page), drag to reorder, swipe to delete (with undo).
- **Opening a location → floating card that expands (recommended).** The tapped card morphs into the
  full-screen forecast with a shared-element/container transform. It keeps your place, feels physical and
  is very Expressive. Back or a swipe down shrinks it back into the list. A list "rolling down" from the top
  is less standard on Android and conflicts with the system notification-shade gesture.
- The expanded location view reuses the Forecast page layout, so we build it once.

### 5.5 Settings (full screen from the toolbar gear)
- Units (temperature, wind, precipitation, pressure; "auto from locale" default)
- Sources (forecast source, alerts source, AccuWeather key)
- Live Updates (on/off, which location)
- Your Day (routine times)
- Appearance (dynamic color, animated background, grain slider, blur intensity, theme light/dark/system)
- About (version, licenses, Breezy Weather credit, source attributions)

Later idea: a **Radar** (animated precipitation map, e.g. RainViewer: free, but needs a map library), after v1.

### 5.6 First launch
- One screen over the animated background: "Use current location" or "Search a city". Location permission is asked
  only when chosen. The notification permission is asked only when Live Updates are enabled.

### 5.7 Accessibility & motion
- All text on glass meets AA contrast. Bigger font sizes reflow the hero to two lines.
- TalkBack labels on every value ("23 degrees, partly cloudy"). The summary sentence is read first.
- Spring motion everywhere, but reduced/disabled when the system "Remove animations" setting is on.

### 5.8 Your Day: a timeline of what the weather means for you

The page that makes the app practical for city life: instead of reading hourly numbers, you see **your day**
and what to do about the weather at each point.

```
┌──────────────────────────────┐
│ Your day                     │
│ "Take an umbrella, it rains  │  narrative (serif) summary of the day
│  on your way home."          │
│                              │
│ [☂ Umbrella] [🧥 Jacket after 7]│  "bring" chips: the outcome at a glance
│                              │
│ 08:30 ● Leave home    ☀ Mild │  timeline: your moments + weather there/then
│   │                          │
│ 10:00 ● Team meeting  ☀ Warm │  (calendar event)
│   │   ░░░ rain 16:00–18:00   │  weather events drawn as bands on the line
│ 18:30 ● Gym           🌧 Wet  │  "Rain will still be falling, leave by 15:45
│   │                          │   or bring the umbrella"
│ 21:00 ● Dinner        🌙 Cool │  "Gets chilly, take a jacket"
└──────────────────────────────┘
```

**Works without the calendar (default).**
- The timeline shows the rest of today (and tonight) with the weather's turning points: rain starts/stops,
  big temperature drops, wind picks up, sunset, UV peak.
- **Routines (optional, no permission needed):** you set times like "Leave home 8:30" and "Head back 18:00"
  (weekday/weekend). They appear on the timeline like events. This covers the commute, the most common
  city-life question, with zero privacy cost.

**Calendar integration: moved to v2.** v1 focuses on getting the weather app right. Design kept here for later.

**Calendar integration (v2; opt-in, never on by default).** We know calendar access can feel scary, so:
- It's off until you turn it on in Your Day or Settings. A clear explainer screen comes first: *"Breezy reads
  the times of your events to show the weather for them. Your events never leave your phone."*
- **Read-only** (`READ_CALENDAR`). You pick which calendars to include, and you can turn it off at any time.
- We only use each event's **start/end time and title**. Titles are shown locally and never sent anywhere.
- **Weather location:** v1 uses your current/starred location for all events. Weather at an event's own place
  (e.g. a meeting across town or in another city) would mean sending the event's address to a geocoding service,
  so it's a later, separate opt-in.
- Only today + tomorrow morning are read, and nothing from the calendar is stored in the app's cache.

**The "what to bring" engine** (same rules engine as the summary sentence, on device):

| Condition at your moments | Advice |
|---|---|
| Precipitation likely while you're out | ☂ Umbrella (with the time window) |
| Temperature drops notably by a later moment, or chilly evening | 🧥 Jacket for later |
| High UV while you're outdoors | 🧴 Sunscreen |
| Strong wind | "Windy, skip the umbrella and take a hood" |
| Big heat | 💧 Water, "find shade around 15:00" |
| Poor air quality | 😷 "Air is unhealthy, consider a mask" |

- Each piece of advice carries its *why* on tap ("Rain from 16:00 to 18:00, 80% chance"), so numbers stay
  available but optional.
- The same advice can show in the Live Update ("Rain in 20 min, you leave at 18:00: take the umbrella")
  and as a once-a-morning notification (opt-in, later).

---

## 6. Milestones (push to GitHub at the end of each)

| # | Milestone | Done when |
|---|---|---|
| M0 | Project skeleton | Builds and runs; theme with both font voices, Appearance settings object, the 3-page pager + floating toolbar shell, Settings screen stub; LICENSE + README with credits |
| M1 | Data layer + rules engine | Open-Meteo fetch + cache, summary sentence and "what to bring" rules, unit tests on parsing, merging and rules |
| M2 | Materials + Forecast page | Weather gradient + grain (with slider), glass cards, real data, no-scroll layout; gesture conflicts tested |
| M3 | Locations page + search + GPS | Rich cards, star/reorder/delete, card-to-detail expand transition |
| M4 | Your Day | Timeline of the day's weather turning points, routines, bring chips |
| M5 | Air quality, pollen, details | All tiles and detail sheets |
| M6 | AccuWeather + alerts | Key entry, source picker with Full/Partial coverage, alert card and details |
| M7 | Live Updates | Rain countdown, alerts and routine-aware advice as an ongoing notification |
| M8 | Polish & performance | Baseline profile, R8, blur budget, accessibility pass, start-time check |

---

## 7. Answered questions (2026-11-17)

1. Daily forecast: **7 days**.
2. Third page: **Your Day timeline** (from your calendar idea). Settings moves to a toolbar gear.
3. Opening a location: **expanding floating card**.
4. Live Updates: **current location, fallback to starred**.
5. Fonts: **Roboto Flex** for data; **experiment with serifs** for explanatory text (Roboto Serif first).
6. Grain: **noticeable (Arc-like) by default + slider**; build the UI to be customizable.
7. Language: **English only for v1**, built for localization.

## 8. Still open

1. Your Day: show tomorrow morning on the same timeline after ~20:00, or switch to "Tomorrow" automatically?
2. Routines: just "leave" and "head back", or any number of named routines?
3. Morning notification with the day's advice: v1 or later?
