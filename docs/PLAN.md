# Solstice: plan

**Solstice** (working name until 2026-11-18: Breezy Expressive) is a fork of [Breezy Weather](https://github.com/breezy-weather/breezy-weather) redesigned in full
**Material You Expressive**: wallpaper (dynamic) colors, the M3 Expressive components, shapes and
motion. Weather data, sources, widgets and everything else stay Breezy Weather's.

Status: **F0, F1 and F2 done** (2026-11-18; F2 blur pending); next F3. Upstream base: `5ec3b9b` (2026-11-17).

## What we start from

Upstream is already Compose with `material3` 1.5.0-rc01, so the Expressive APIs are available
without a migration. It uses plain `MaterialTheme` and no Expressive API yet.

| Area | Today | Size |
|---|---|---|
| Theme | `ui/theme/compose/Theme.kt`: `MaterialTheme`, dynamic color on Android 12+ | 71 lines |
| Settings screens | Compose: 11 screens in `ui/settings/compose/` | ~2.8k lines |
| Settings building blocks | Compose: `ui/settings/preference/composables/` (switch, list, multi-list, edit text, time picker, section) | ~1.9k lines |
| Reorder screens | Views + RecyclerView: card display, daily/hourly trend display | 3 activities + adapters |
| Location list | `ui/main/fragments/ManagementFragment.kt`: Compose shell + RecyclerView (`LocationAdapter`, `item_location_card.xml`, drag/swipe via `ItemTouchHelper`) | ~1k lines |
| Location search | `ui/search/SearchActivity.kt`: Compose | 391 lines |

## Decisions

| Topic | Decision (2026-11-17) |
|---|---|
| Name | **Solstice** (decided 2026-11-18; replaces the working name Breezy Expressive) |
| `applicationId` | `io.github.information89.breezyexpressive` (debug: `.debug` suffix, as upstream) |
| Flavor shipped | `basic` (all sources) |
| Code in this repo | Yes, as a snapshot of upstream **without its history** (29 MB; the full history is ~1 GB) |
| Weather-App-Experiment design (sky, glass, sun path) | Dropped: pure Material You Expressive |
| Fallback color (dynamic color off / Android < 12) | Sky blue seed (2026-11-18) |
| Typeface | Google Sans Flex, bundled; emphasized headlines via weight (2026-11-18; check license before bundling) |
| Shapes | Full Expressive: large corners, pill buttons, morph on press/selection (2026-11-18) |
| Launcher icon | Sun partly wrapped by a wind swirl, plus a monochrome layer (2026-11-18) |

## Rename to Solstice (decided 2026-11-18)
- **Why it's required, not just cosmetic:** upstream's license terms say a fork must not use the
  Breezy name. In release builds, `BreezyWeather.isImpersonatingBreezyWeather` is true when the app
  name **or** the applicationId contains "breezy"; the app then hides all locations and shows a
  blocking "License compliance" dialog that closes the app (`ui/main/dialogs/LicenseComplianceDialog`).
  Debug builds are exempt. Today both our name and ID contain "breezy", so **release builds are
  unusable until the rename.**
- **What changes:** `brand_name` in `res_fork/values/strings.xml` → "Solstice";
  `applicationId` → e.g. `io.github.information89.solstice` (a new app ID means a separate install:
  settings and locations on the phone don't carry over; fine at this stage); README, CLAUDE.md,
  CONTINUE.md, PRIVACY.md/INSTALL.md wording; `app.github.release_prefix`; optionally the GitHub
  repo name (GitHub redirects the old URL). The code package `org.breezyweather` stays (keeps
  upstream merges easy; the license check doesn't look at it).
- Check before release: no confusing clash with other apps called Solstice in the stores.

## Design direction

- **Color:** dynamic (wallpaper) color by default, everywhere; a tuned seed palette when it's off or
  on Android < 12. No custom sky/glass look in this fork (the Weather-App-Experiment design is dropped).
- **Theme:** `MaterialExpressiveTheme` with `MotionScheme.expressive()` (spring motion), Expressive
  shapes and a type scale with emphasized styles for headlines.
- **Components:** `LargeFlexibleTopAppBar`, segmented list groups (rounded group, small inner
  corners, like Android 16 Settings), connected `ButtonGroup`s for small choices, `Switch` with
  icons, `LoadingIndicator`, Expressive `SearchBar`, `FloatingToolbar`/FAB menu for actions,
  shape morphing on press and selection.
- **Days page (added 2026-11-18):** the multi-day forecast moves to **its own page to the left of
  Forecast**: swipe with the finger moving right from Forecast to reach it. Pages, left to right:
  **Days ← Forecast → Locations**; Forecast stays the screen the app opens on. Decided 2026-11-18: the pill
  toolbar has **three tabs: Days · Forecast · Places**. A small "next days" preview card appears on
  Forecast **only near the end of the day** (evening/night, when tomorrow matters more than the
  rest of today); tapping it opens the Days page. Exact switch-over time to tune (e.g. after
  sunset or after the last daytime hour).
- **Navigation (updated 2026-11-18):** two pages side by side. **Forecast is the first screen**;
  **Locations is to its right**: swipe with the finger moving left to reach it, right to come back.
  The page follows the finger and can reverse mid-swipe. A floating pill toolbar (M3 Expressive
  `HorizontalFloatingToolbar`) **stays at the bottom** with the two tabs for tapping. Replaces
  upstream's pushed Locations page (phones) and drawer (tablets).
  - Consequence: the sideways swipe on Forecast now moves between pages, so it no longer switches
    between locations (upstream behaviour). You change location by tapping a card in Locations,
    which slides back to Forecast showing it.
- **Forecast screen top (2026-11-18):** **nothing at the top but the location's title.** No toolbar
  buttons, no menu, **no "edit this location" button**. For the current (GPS) location the title is
  the place's real name with a small current-location icon next to it, not "Current location".
- **Editing a location (2026-11-18):** in the Locations list, swipe a location card sideways to
  reveal **Edit** (location details: name, weather sources…); swipe the other way to delete with
  undo. Edit opens upstream's location settings, restyled as in F2.
- **Motion: fluid and interruptible, iOS-like refinement.** Priority whenever we animate:
  - Every animation can be interrupted and retargeted mid-flight, keeping its current velocity; no
    animation blocks input or has to finish first.
  - Springs, not fixed-duration tweens, for anything the user can touch (`spring()` /
    `MotionScheme.expressive()`, `Animatable.animateTo`, `animate*AsState`); tweens only for things
    nobody can interrupt (e.g. a fade-in of loaded data).
  - Gesture-driven where possible: transitions track the finger (swipes, predictive back, drag to
    reorder) and settle from the release velocity.
  - Shared elements between screens instead of cross-fades where the same thing appears in both.
  - 60/120 Hz smooth: no layout work during animations, measure with frame timing on the Pixel 7.
- **Translucent overlays (added 2026-11-18):** surfaces that sit on top of other surfaces (bottom
  sheets, the floating toolbar, dialogs, menus, top bar when content scrolls under it) are
  translucent, letting what's behind show through, instead of opaque.
  - **Reference:** the settings page of the **Transit Go for Smartspacer** app, which is
    translucent. The user wants that implementation copied. To do before building: find its
    source and **check its license** (Smartspacer plugins are typically GPL-3.0; this repo is
    LGPL-3.0, so code may not be copyable as is; if not, re-implement the same look ourselves).
  - Likely technique: blur of the content behind (`RenderEffect`/`Modifier.blur`, Android 12+)
    plus a tinted, partly transparent surface color; plain translucent tint as fallback on
    Android < 12 and when the phone is in battery saver.
  - Must stay cheap: blur costs GPU time, so measure frame times on the Pixel 7 when adding it
    (see Performance).
- **Performance first (added 2026-11-18):** the app must feel fast before new features land. Test on
  a release (R8-minified, non-debuggable) build, not debug, when judging smoothness.
- **Accessibility first:** font scale up to 200 %, TalkBack order and labels, 48 dp targets, RTL.

## Release 1 scope: Settings, Locations and the tab toolbar

Out of scope for release 1: the main weather screen's content (it sits in the Forecast tab unchanged),
widgets, notifications, live wallpaper.

### F0: Fork setup
1. **Snapshot:** one commit with upstream's files at the base commit (now `5ec3b9b`), no history.
   Record the base in `UPSTREAM_BASE` (commit hash + date).
2. **Identity** (upstream's fork rules):
   - `res_fork/values/strings.xml`: `brand_name` = "Breezy Expressive".
   - New launcher icon in the `res_fork` mipmaps, with a monochrome layer for themed icons.
   - `applicationId` → `io.github.information89.breezyexpressive`; `namespace` stays
     `org.breezyweather` (keeps upstream code and diffs untouched).
   - Never build with the `breezy` flag; README says unofficial, not affiliated.
3. **Build here:** JDK 21 (as upstream) + Android SDK (already on this Mac), `./gradlew assembleBasicDebug`,
   run on the Pixel 7.
4. **Upstream updates without shared history:** keep a local full clone of upstream; to update,
   apply `git diff <old base>..<new base>` from upstream onto this repo (`git apply -3`), fix
   conflicts, bump `UPSTREAM_BASE`. Do it per upstream release.
5. **Keep diffs small:** new code in `org.breezyweather.ui.expressive`; edits in upstream files
   only to switch screens over. A list of touched upstream files lives in this doc (below).
6. **CI:** GitHub Actions builds `basicDebug` and runs lint on each push.

### F1: Expressive theme
- `Theme.kt` → `MaterialExpressiveTheme`, expressive motion, shapes, emphasized typography.
- Check the whole app still looks right (it is all themed through this file).
- Done: `ui/expressive/theme/` (`ExpressiveTheme`, `ExpressiveShapes`, `ExpressiveTypography`).
  Only Compose screens use it; the View screens (main Forecast, Locations list) keep the XML
  `Theme.Material3Expressive` theme. Upstream's settings rows hardcode their shapes, so the larger
  corners show up only once F2 restyles them.

### F2: Settings
- Restyle the shared building blocks first (`preference/composables/`): every settings screen
  changes at once with few edits upstream.
  - Sections → segmented list groups with a quiet section title (upstream already groups rows with
    `Material3ExpressiveCardListItem` first/last corners; adopt the theme's larger corners).
  - Switch rows with a check/close icon in the thumb.
  - List and multi-list choices open a **bottom sheet** (decided 2026-11-18): drag to dismiss,
    interruptible, options as a segmented list with a check on the selected one.
  - Small fixed choices (2–4 options: dark mode, units) shown **inline as connected button
    groups** (decided): one tap, the selected button morphs to a pill.
- `AppBar.kt` → `LargeFlexibleTopAppBar`: **large title that shrinks** into the bar as you scroll,
  tracking the finger (decided).
- Root settings: each category icon in its **own colored Expressive shape** (circle, rounded
  square, petal…), colors derived from the wallpaper palette, like Android 16 Settings (decided).
  Short summary line under each title.
- Mockup reviewed 2026-11-18: home with colored shapes, Appearance with button groups and an icon
  switch, Language as a bottom sheet.
- Reorder screens (cards, daily/hourly trends): rewrite the 3 View activities in Compose with
  drag handles and haptics (LazyColumn + a small reorder helper), same stored data.

### F3: Locations
- Location list (`ManagementFragment`) → pure Compose `LazyColumn`: location cards with current
  conditions, drag to reorder, swipe to delete with undo, current-location card pinned on top.
  Removes the Compose/RecyclerView mix (upstream notes it causes scroll stutter).
- Add / search: Expressive `SearchBar` expanding to full screen, results as segmented list,
  `LoadingIndicator` while searching, clear empty and error states.
- Location settings screen (`LocationSettingsScreen.kt`) follows F2.
- **Tab toolbar:** floating pill with Forecast and Locations at the bottom, tap or swipe to switch
  (Forecast first, Locations to the right; see Design direction). New shell in `ui.expressive`
  hosting both; `MainActivity` switches to it in one small edit. Tapping a location in the
  Locations tab slides to Forecast showing it.
- **Location cards:** swipe one way to Edit (location details), the other way to delete with undo.
- **Location cards wear their own weather (added 2026-11-18):** each card's background is the
  same condition/time-of-day gradient that location's Forecast would show (gradient field + grain,
  see "Forecast background"), so the list reads at a glance: sunny afternoon here, night and rain
  there. Performance: render each card's gradient once per weather update (small cached bitmap or
  a cheap shader), never per frame; text contrast checked on every gradient.
- **Forecast top:** only the location title (current location: real place name + location icon);
  remove upstream's toolbar actions, including "edit this location", from the Forecast screen.

### F4: Polish and release
- Motion pass (shared transitions between list and search, press/selection morphs).
- Check: light/dark, dynamic color on/off, Android 12 and 17, font scale 200 %, RTL, TalkBack.
- Release build with our name/icon; changelog; screenshots.

## Planned next (added 2026-11-18, not yet scheduled into a phase)

### Edit the Forecast blocks in place, like Pixel Weather
- Instead of (or as well as) the separate Blocks list in Settings: long-press the Forecast screen
  to enter an edit mode where the real blocks shrink slightly and can be dragged into a new order
  right there, hidden with a remove button, and added back from a tray of hidden blocks. Done
  ends edit mode. Reference: how the Pixel Weather app reorders its home screen cards.
- Same motion rules: the dragged block follows the finger, others slide with springs, haptic
  ticks, interruptible; TalkBack move up/down actions.
- Depends on the main Forecast screen being Compose (upstream's is View/RecyclerView today), so it
  comes with or after a Compose Forecast screen. Reuses the stored `cardDisplayList` and the drag
  logic from `ui/expressive/reorder/ReorderList`.

### Forecast screen: no fade-ins, progressive blur at the edges
- **Don't fade in blocks.** Upstream animates the Forecast blocks in as they appear; remove that so
  blocks are simply there when the screen opens or scrolls (feels faster, less busy). Check the
  "animations" options in Main screen settings, which control this upstream.
- **Progressive blur at the top and bottom edges:** content scrolling out of view under the top
  bar / floating toolbar gets gradually more blurred (and dimmed) towards the edge instead of being
  cut off hard, like iOS. Android 12+ (`RenderEffect`), plain gradient fade as fallback.
- Performance first: blur is costly on the GPU, so measure frame times on the Pixel 7 (release
  build) and keep the blurred band small; same technique as the translucent overlays above.

### Forecast layout reference (added 2026-11-18)
The user likes how this reference lays out the information and wants it considered for the
Forecast screen (top to bottom):
1. **Location title**, centred (our rule: nothing else at the top; the reference's menu and bell
   buttons are dropped: Locations is a swipe/tab away, alerts appear as a card).
2. **Big temperature**, very large display numerals.
3. **Condition headline** in plain words ("Clear and calm"), then **one plain-language line**
   ("Feels like 17°, wind 9 km/h from the sea").
4. **Sun path** (liked most): a curve across the full width from sunrise to sunset with a dot at
   the sun's current position, the part already travelled highlighted; **sunrise time left,
   sunset time right**, and a short sun/UV note in the middle ("Mild sun, UV 3"). At night the
   same element can show the moon or night length.
5. **Today** card: next hours (temperature, icon, time; "Now" first) with a one-line summary in the
   header ("clear all day, cooler after 21").
6. **This week** card (moves to the Days page, see Navigation; Forecast may keep a short teaser): days as small bars at their temperature height, the day that needs
   attention highlighted, and a one-line summary ("only Thursday needs you").
7. **Alert card** when something severe is coming: when, what, and a short practical line.
- Cards are translucent over the gradient background (see translucent overlays), large corners.
- Builds on upstream's data (sun/moon times, UV, hourly, daily, alerts); the plain-language lines
  need a small summary generator (the old Weather-App-Experiment had a rules engine for this).
- Depends on a Compose Forecast screen (with in-place block editing); these elements would become
  blocks the user can reorder/hide.

### Forecast background: soft gradient (added 2026-11-18, style still open)
- The Forecast screen gets a soft, grainy gradient background reflecting the weather and time of
  day, like the references the user shared: blurry colour fields with film grain (lavender/blue/
  near-black at night; peach/orange/yellow for sun; blue sky into warm sunset). This updates the
  earlier "no custom sky look" decision for the Forecast background only; the rest of the app
  stays Material You.
- **Decided 2026-11-18 (from mockups A/B/C): gradient field (A).** Soft blurred colour fields with
  film grain, no sun/moon orb; the sun path stays a drawn curve with a dot for the sun, sunrise
  left and sunset right.
- Colours: derived from conditions and time of day, harmonised with the wallpaper (dynamic) palette
  so cards and text stay readable; text contrast checked in both themes.
- Performance first: draw it cheaply, e.g. a few blurred shapes / a small pre-rendered bitmap scaled
  up, or an AGSL shader on Android 13+; grain as a tiled noise texture. No per-frame redraw unless
  something moves; measure frame times on the Pixel 7 (release build).

### App drawer name: "Solstice" or "Weather" (added 2026-11-18)
- A setting (Appearance) to choose the name shown under the icon in the app drawer/home screen:
  **Solstice** or **Weather**, so the app can sit where people expect "Weather".
- How: two `<activity-alias>` launcher entries for the main activity, each with its own label;
  the setting enables one and disables the other with
  `PackageManager.setComponentEnabledSetting(..., DONT_KILL_APP)`.
- Caveats to handle: some launchers take a moment to refresh or remove a home-screen shortcut
  when the alias switches (warn in the setting's summary); app shortcuts and widgets must point at
  the real activity, not the alias. The app's own name elsewhere (About, notifications) stays
  "Solstice".

### New Material You Expressive icons everywhere
- Replace upstream's UI icons (settings, toolbar, actions, rows) with Material Symbols in the
  Expressive style: Rounded, filled when selected, consistent weight/grade, matching the type.
- Approach: map each upstream `ic_*` drawable to a Material Symbol and regenerate the vector
  drawables under the same names, so call sites don't change (small upstream diff). Check the
  Material Symbols license (Apache 2.0) and record it.
- Weather condition icons (icon packs) are separate: keep upstream's packs for now; an Expressive
  weather icon set is its own later decision.

### Simplify settings
- The Daily trends and Hourly trends options are too much. Proposal to discuss before building:
  fold them into the Forecast blocks (configure from the block itself / in-place edit mode), show
  only the few that matter by default, and move the rest under a single "More" or "Advanced"
  section.
- Then a pass over all settings screens: remove or merge rarely used options, shorter summaries,
  fewer levels of nesting. Nothing removed from upstream's stored settings, only from what's shown,
  so upstream merges stay easy.

### Simplify and lighten the app (decided 2026-11-18)
- **Approach: cut whole modules.** Delete entire features and sources we don't want as clean cuts
  (whole packages, their resources, settings and manifest entries), and keep everything else
  syncable with upstream. Not "hide only" (no size win), not "rewrite freely" (too costly to keep
  taking upstream fixes). Each cut gets listed under "Upstream files touched" and is applied again
  after each upstream sync (deleted files stay deleted; conflicts only where we cut references).
- **Remove:** live wallpaper; notification widget (persistent weather in the shade).
- **Keep:** data sharing with other apps (Gadgetbridge, watches, launchers: content provider and
  broadcasts).
- **Widgets: later.** Maybe keep one or two, decided much later; no widget work until then.
- **Weather sources (decided 2026-11-18): worldwide + North America.**
  - Keep: Open-Meteo (default), MET Norway, WMO Severe Weather, FOSS Public Alert Server,
    AccuWeather / OpenWeather / Pirate Weather (work only with the user's own key: the fork has
    none of upstream's keys), NWS and NCEI (US), ECCC (Canada).
  - Keep (infrastructure): Android geocoder, Nominatim, Natural Earth (offline fallback), GeoNames
    (with own key), Breezy Time Zone, IP.SB, Gadgetbridge, update notifier, debug, common.
  - Cut: all European (~25), Asian/Pacific (~17, incl. Baidu IP) and African (ClimWeb) national
    services. Mexico has no national source upstream; WMO alerts cover it.
  - Expectation: ~5 MB of source code goes, but the APK is mostly libraries, so the main win is
    simpler source lists in settings; measure the release APK before/after.
  - Upstream sync: deleted source packages stay deleted; `SourceManager` and the Hilt wiring are the
    only shared edits.
- **Use platform components** where they do the job: Android's own geocoder, system location,
  stock Material 3 components instead of upstream's custom ones as screens move to Compose.
- **Measure:** debug APK today is 43 MB (31 MB code). Compare release APK size, startup and frame
  times on the Pixel 7 before and after each cut.

### Current-location name: Android built-in geocoder (decided 2026-11-18)
- Use the phone's own geocoder (`nativegeocoder` source, available in the `basic` build) as the
  default address lookup for the current location: no key, no setup, good local names.
- Fallback when it isn't available (phones without Google services): Nominatim. It fails today only
  because the fork's contact link is empty (`app.report_issue` in `gradle.properties`, required by
  Nominatim's usage policy). Fill in the fork's links (`app.report_issue`, `app.source_code_link`,
  `app.privacy_policy_link`, …) pointing at this GitHub repo; that also fixes the build warnings
  and the About screen links.

## Risks
- **Upstream merges:** F3 rewrites `ManagementFragment`; conflicts there are likely. Mitigation:
  new file next to it, switch over in one small edit; merge upstream often.
- **Reordering in Compose:** needs careful drag/scroll/haptics and TalkBack actions
  (move up/down) as an alternative.
- **Swipe conflict (settled 2026-11-18):** the sideways swipe on Forecast goes to Locations, so
  upstream's swipe between locations is dropped; locations are switched from the Locations list.
  Watch for conflicts with horizontally scrolling blocks on Forecast (hourly/daily charts): those
  scroll first, the page swipe takes over only at their ends or from the screen edge.
- **Current location title:** needs a reverse-geocoded place name. Upstream's Nominatim address
  lookup currently fails in this fork ("Required API key missing"), so fix that first or use
  another reverse-geocoding source; fall back to "Current location" + icon when no name exists.
- **Tab shell vs `MainActivity`:** upstream's activity (910 lines) manages the pushed page and drawer;
  replacing that touches more upstream code than other phases. Keep the edit to one switch-over.
- **Expressive APIs are experimental:** pin `material3`, expect API changes on updates.

## Open questions
1. Launcher icon: direction chosen (sun + wind swirl); exact drawing still to agree.
2. Contribute generic fixes back upstream (e.g. the location list rewrite)?
3. Distribution: GitHub releases only, or also an F-Droid-style repo (e.g. Obtainium/IzzyOnDroid)?

## Upstream files touched
- `app/src/main/kotlin/org/breezyweather/ui/theme/compose/Theme.kt`: `BreezyWeatherTheme` delegates
  to `ExpressiveTheme`; upstream's placeholder purple schemes removed (F1).
- `app/src/res_fork/**`: launcher icon (F0; fork-only resources).
- `ui/settings/preference/composables/SwitchPreference.kt`: check/close icon in the switch thumb (F2).
- `ui/settings/preference/composables/ListPreference.kt`: inline button group for small choices;
  bottom sheet replaces the list dialog (old dialog code removed) (F2).
- `ui/settings/preference/composables/Preference.kt`: leading icon → `IconBadge` (F2).
- `ui/common/widgets/insets/FitSystemBarComposeWrappers.kt`: `LargeTopAppBar` →
  `LargeFlexibleTopAppBar` (F2).
- `ui/common/widgets/Material3Widgets.kt`: `Material3ExpressiveCardListItem` morphs its inner corners
  while pressed (F2).
- `ui/settings/preference/composables/MultiListPreference.kt`: `AlertDialogNoPadding` → `SheetDialog`;
  row icons → `IconBadge` (also in `ListPreference.kt`, `SwitchPreference.kt`) (F2).
- `common/utils/helpers/IntentHelper.kt`: the three reorder screens open `ReorderActivity` (F2).
- `AndroidManifest.xml`: `ReorderActivity` entry (F2).
- **Cut (2026-11-18):** deleted packages `wallpaper/` (live wallpaper) and
  `remoteviews/presenters/notification/` (notification widget), `res/xml/live_wallpaper.xml`.
  References removed in `AndroidManifest.xml`, `background/receiver/BootReceiver.kt`,
  `ui/settings/compose/ModulesSettingsScreen.kt`; `sources/RefreshHelper.kt` keeps
  `updateNotificationIfNecessary` / `updateWallpaperIfNecessary` as no-op stubs so their callers stay
  unchanged. Their strings and settings keys are left in place (harmless).
- **Cut (2026-11-18), weather sources:** deleted 40 source packages (European, Asian/Pacific and
  African national services, incl. Infoplaza and Baidu IP) from `main`, `src_nonfreenet` and
  `src_freenet`, plus `sources/FmiServiceStub.kt` and `sources/InfoplazaServiceStub.kt`. Only
  `sources/SourceManager.kt` referenced them (imports, constructor, lists; national list now typed
  `persistentListOf<Source>`). Their strings, API-key build fields and settings keys are left in
  place (harmless). Also removed the live wallpaper's leftover unit test
  (`test/.../wallpaper/FramePacingTest.kt`).
- `sources/SourceManager.kt`: default address lookup is Android's geocoder when present (Nominatim
  otherwise).
- `gradle.properties`: fork links filled in (`app.report_issue`, source, releases, install, privacy,
  GitHub org/repo/prefix).
- Added: `res/values/strings_expressive.xml` (new strings kept out of upstream's `strings.xml`;
  English only so far).
- Added (no upstream equivalent): `.gitattributes` (LF, so Spotless passes on Windows),
  `app/src/main/res/font/google_sans_flex.ttf`, `third_party/fonts/GoogleSansFlex-OFL.txt`,
  `tools/make_icon.py`, `app/src/main/kotlin/org/breezyweather/ui/expressive/**`.

## Log
- **2026-11-17:** Studied upstream (`5ec3b9b`); wrote this draft. Name: Breezy Expressive.
- **2026-11-17:** Decided: code as a no-history snapshot in this repo, `basic` flavor,
  `applicationId` `io.github.information89.breezyexpressive`, old custom design dropped. F0 detailed.
- **2026-11-17:** F0 started: upstream snapshot at `5ec3b9b` without history (`.github/` and README
  excluded, see `UPSTREAM_BASE`); app name "Breezy Expressive" and `applicationId`
  `io.github.information89.breezyexpressive`; CI builds `basicDebug` + spotless (Java 21).
  `assembleBasicDebug` builds locally (JDK 21, ~20 min cold). Not yet run on the phone; launcher icon
  still upstream's generic fork icon.
- **2026-11-18:** First run on the Pixel 7: `basicDebug` installs and starts without crashing, opens
  on the empty Locations screen with dynamic color. Built on Windows with Android Studio's bundled
  JDK 25 (works; no toolchain pin), 6 min cold; Gradle fetched Build-Tools 37 itself.
- **2026-11-18:** Design decisions for F1: sky-blue fallback seed, Google Sans Flex, full Expressive
  shapes and motion; launcher icon direction: sun + wind swirl.
- **2026-11-18:** Added to the design: floating pill tab toolbar (Forecast / Locations, tap or swipe)
  in release 1 (F3), and a motion principle: every animation interruptible, spring-based and
  gesture-driven, aiming for iOS-like fluidity.
- **2026-11-18:** Launcher icon (option C): sun with two curled gusts on sky blue `#4A90E2`, gaps
  cut out of the sun so the monochrome (themed) layer works. New `ic_launcher_monochrome`; the
  foreground is also the splash icon. All icon files are generated by `tools/make_icon.py`
  (`python tools/make_icon.py app/src/res_fork <preview.png>`). **F0 done.**
- Noticed: the build warns that `app.source_code_link` and `app.privacy_policy_link` are missing
  from `gradle.properties` (shown in About). Set them before release (F4).
- **2026-11-18:** **F1 done.** `ExpressiveTheme`: `MaterialExpressiveTheme`, `MotionScheme.expressive()`,
  larger corners, dynamic color with upstream's blue/amber `md_theme_*` palette as fallback (so
  Compose matches the View theme). Google Sans Flex (OFL 1.1, from google/fonts) trimmed with
  fontTools `varLib.instancer` to wght 300–900 + opsz 14–72 (3.9 MB → 424 KB); each type role
  pins an optical size. Checked on the Pixel 7: Settings now in Google Sans Flex, no crash.
  Added `.gitattributes` (LF): on Windows Spotless otherwise expected CRLF in every file.
- Noticed: with a current location, the main screen shows "Nominatim (Address lookup): Required
  API key missing". Forks must configure their own keys/contact for some sources; look at it
  before release (F4).
- **2026-11-18:** F2 design agreed from a mockup: colored Expressive icon shapes on the settings
  home, bottom sheets for lists, inline connected button groups for small choices, large title
  that shrinks on scroll, icon switches.
- **2026-11-18:** F2 part 1 built and checked on the Pixel 7 (debug build): `ui/expressive/components/`
  `IconBadge` (category icon in its own colored `MaterialShapes` shape, hue fixed per category, tone
  from the wallpaper scheme), `InlineChoiceButtons` (2–3 short options as a connected
  `ToggleButton` group; labels drop a trailing "(details)", which stays in the summary; long
  translations fall back to the sheet), `ChoiceSheet` (bottom sheet, segmented options,
  slightly translucent), `SwitchThumbIcon`. Settings title is now `LargeFlexibleTopAppBar`.
  **F2 left:** multi-select lists still use upstream's dialog; list/switch rows with icons still
  show plain icons; press/selection shape morph on rows; reorder screens in Compose; real blur
  for translucent overlays (needs the Transit Go license check first).
- Performance, quick look: the release build was too slow to wait for, so work continues on debug.
  Debug frame stats on the Pixel: median 7 ms, ~4–5 % janky frames, worst 1 % at 125–250 ms.
  Judge smoothness on a release build before the next milestone.
- **2026-11-18:** **F2 done** (except real blur). Multi-select lists open `SheetDialog` (same slots as
  upstream's dialog, so the call site changed one name); all settings rows with icons use
  `IconBadge`; segmented rows morph their inner corners while pressed (spring, interruptible).
  New Compose `ReorderActivity` + `ReorderList` replace the three reorder screens (blocks, daily,
  hourly trends): drag the handle or long-press a row, others slide with springs, auto-scroll at the
  edges, haptic ticks, TalkBack move up/down/remove actions, hidden items as chips, reset to
  default, saves on leave. Upstream's old activities stay in place, unused.
  Checked on the Pixel 7: drag reorder works and saves (test drag was undone afterwards), press
  morph visible. Not seen: the multi-select sheet with items (no compatible app installed).
- **2026-11-18:** Added to "Planned next": in-place block editing on the Forecast screen (like Pixel
  Weather), Material You Expressive icons everywhere, and simplifying settings (Daily/Hourly
  trends options first).
- **2026-11-18:** Added to "Planned next": no fade-in of Forecast blocks, progressive blur at the
  top and bottom edges of the Forecast screen.
- **2026-11-18:** Navigation and Forecast decisions: Forecast first, Locations to the right (finger
  moves left), pill toolbar stays at the bottom; only the location title at the top of Forecast
  (current location: place name + icon); no "edit this location" button; swipe a location card to
  edit its details. Side effect noted: no more swiping between locations on Forecast.
- **2026-11-18:** Decided: simplify by cutting whole modules (remove live wallpaper and
  notification widget; keep data sharing; widgets much later; weather sources to pick). Current
  location name from Android's built-in geocoder, Nominatim as fallback once the fork's contact
  links are set.
- **2026-11-18:** Added: soft grainy gradient background for Forecast (references shared); open
  question: gradient field vs a circular sun/moon shape.
- **2026-11-18:** Cuts done: live wallpaper and notification widget removed (settings now show only
  General and Data sharing in External modules). Debug APK 42.86 → 42.81 MB: these features were
  small; the real size win will come from weather sources and libraries. Lookup fix: fork links set,
  so Nominatim works again (the Forecast title shows the real place name, no "API key missing"
  error); new locations default to Android's built-in geocoder. Existing locations keep their stored
  lookup service (the current location still uses Nominatim; switch it in its location settings).
  To do: `PRIVACY.md` and `INSTALL.md` are still upstream's text; the fork links point at them, so
  rewrite them for Breezy Expressive before release.
- **2026-11-18:** Weather sources decided: keep worldwide + North America, cut the European, Asian
  and African national services (list in "Simplify and lighten").
- **2026-11-18:** Added a Forecast layout reference (title, big temperature, plain-language
  lines, sun path with sunrise/sunset, Today and This week cards with one-line summaries, alert card).
- **2026-11-18:** Weather sources cut (40 packages, 628 files). Debug APK 42.81 → 41.45 MB. Unit
  tests compile and pass again (the wallpaper cut had left a test behind). On the Pixel: forecast
  refreshes, default source picker shows Open-Meteo, MET Norway, AccuWeather / OpenWeather /
  Pirate Weather (need own keys) and Debug; NWS/ECCC apply to US/Canada locations automatically.
- **2026-11-18:** Added: multi-day forecast gets its own page to the left of Forecast
  (Days ← Forecast → Locations); toolbar third tab and Forecast teaser still open.
- **2026-11-18:** Forecast mockups reviewed: gradient field background (A), three-tab toolbar
  (Days · Forecast · Places), next-days preview only near the end of the day.
- **2026-11-18:** New name: **Solstice**. Found that release builds are blocked by upstream's
  license-compliance check while name or app ID contain "breezy"; rename needed before release
  builds can be used or measured. Release APK size (arm64): 15.1 MB vs 41.5 MB debug.
- **2026-11-18:** Added: setting to switch the app drawer name between "Solstice" and "Weather".
- **2026-11-18:** Renamed to **Solstice** (`io.github.information89.solstice`, debug `.debug`). The
  release build now opens normally (no license-compliance block). Same scripted settings scroll
  test on the Pixel 7, debug vs release (R8, signed with the debug key): janky frames 6.7 % → 1.75 %,
  median 7 → 6 ms, 95th percentile 38 → 13 ms, worst 1 % 77 → 34 ms; arm64 APK 41.5 → 15.1 MB.
  So most of the lag the user felt was the debug build; the remaining release jank (worst 1 % at
  34 ms) is the target for a performance pass (baseline profile, the View screens). Added: Location
  cards use their location's condition gradient.
