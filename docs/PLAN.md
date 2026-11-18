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
- **Step 1 built 2026-11-18 (phones):** `ui/expressive/shell/` — `SolsticeShell` (pager Days ← Forecast →
  Places over the current location's animated sky; floating toolbar whose highlight follows the
  swipe; opens on Places when there are no locations), `ShellPages` (first simple Compose Forecast:
  title with current-location icon, big temperature, condition, feels-like line, sun path with
  sunrise/sunset, Today card with the next 6 hours, pull to refresh; Days list; Places with sky
  cards, tap to select and slide to Forecast, Add a place → upstream search, settings button).
  `MainActivity` hosts it in a new `solstice_shell` ComposeView (phone layout only); upstream's
  Forecast stays underneath, hidden, as **"More details"** (Back returns to the shell);
  `setManagementFragmentVisibility` routes to the Places page. Tablets keep upstream's drawer for now.
  **Left for F3:** swipe a card to edit/delete with undo, drag to reorder, current location pinned
  first + "add current location", pull-to-refresh/loading polish, transitions.
- **Location cards:** swipe one way to Edit (location details), the other way to delete with undo.
- **Telling places apart when their weather is the same (added 2026-11-18):** two overcast places
  get the same sky, so cards look alike. Idea from the user: a **generated map overlay** per place,
  e.g. a soft, low-contrast silhouette of the area around it (coastline, rivers, street grid or
  terrain contours drawn from OpenStreetMap / Natural Earth data, or a stylised outline of the
  city/region), blended into the card's gradient with the grain. Alternatives to weigh: a small
  local-time clock face, the country/region shape, a per-place accent hue from its coordinates.
  Must stay cheap (render once, cache per place) and keep text readable; data/license to check.
- **Swipe stages, animated (added 2026-11-18):** the change from the star stage to the delete stage
  should itself animate (the revealed colour sweeps from amber to red, the star morphs into the bin
  icon with a little scale/rotate, the card's corners round more as it nears delete), following the
  finger and reversible. On delete, a **fun discard animation** instead of a plain slide: e.g. the
  card shrinks and tumbles away with a slight spin and fade, or crumples like paper and drops,
  then the list closes the gap with a spring. Undo plays it in reverse.
- **Release ripple (added 2026-11-18, later refinement):** letting go of a swiped place sends a small
  spring wave through the other places, like Android's notification shade: neighbours nudge in the
  swipe direction and settle back, each a little later and softer the further away they are
  (staggered springs). Same when a card is deleted and the gap closes.
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
- **Palette approved 2026-11-18** (base colour + three soft blobs, film grain on top; text colour):
  | Condition | Base | Blob 1 | Blob 2 | Blob 3 | Text |
  |---|---|---|---|---|---|
  | Clear day | `#BFD3F2` | `#8FB0EE` | `#FFE1A8` | `#F3EBDD` | `#1E2340` |
  | Partly cloudy | `#F2D7C9` | `#9DAEE6` | `#F4A46B` | `#F7E3C8` | `#2A1A1A` |
  | Sunrise / sunset | `#F6B48A` | `#8C8FD6` | `#FF7B4A` | `#FFC870` | `#2B1408` |
  | Clear night | `#1D1A3D` | `#8E7CD0` (was `#C9B6F2`, darkened 2026-11-18 for title contrast) | `#5B5BD6` | `#0F0D22` | `#ECE9FF` |
  | Overcast | `#C9CBD3` | `#A7ABB8` | `#E2DED8` | `#B9B4C4` | `#22252E` |
  | Rain | `#7E8FA8` | `#4D6185` | `#9FB7C9` | `#5B6E7E` | `#F2F5FA` |
  | Thunderstorm | `#3A3550` | `#1E1B33` | `#7A5FB0` | `#4A5570` | `#F1EEFA` |
  | Snow | `#E7ECF4` | `#C3D2EC` | `#FFFFFF` | `#D8D2EA` | `#1F2633` |
  | Fog / haze | `#D9D6D1` | `#EDEAE4` | `#C4C0BA` | `#E4E0DA` | `#26241F` |
  Dawn/dusk blend between the day and night palettes over time; grain stronger on dark palettes.
- **Built 2026-11-18 (static version):** `ui/expressive/sky/` — `SkyCondition` (nine palettes above +
  mapping from upstream's `WeatherCode`, golden hour within 40 min of sunrise/sunset) and
  `SkyBackground` (base + three radial-gradient colour fields + tiled grain, baked once at half
  resolution and cached for the last 6 palette/size pairs). Debug-only `SkyPreviewActivity` swipes
  through all skies (`adb shell am start -n io.github.information89.solstice.debug/org.breezyweather.ui.expressive.sky.SkyPreviewActivity --ei page N`).
  Swiping between full-screen skies on the Pixel 7 (debug build): median 12 → 5 ms, GPU 8 → 1 ms
  after baking + cache.
- **Night skies (decided 2026-11-18): hand-tuned + blending.** Every condition gets its own designed
  night palette (rainy night, snowy night, foggy night…), and colours blend continuously with the
  sun's elevation: day → golden hour → short blue-violet twilight → night, and back at dawn.
- **Animation (decided 2026-11-18): calm drift + moving sun.** The colour fields drift very slowly
  (barely noticeable); one warm light field is the **sun**, placed where the real sun is (rises left,
  crosses the top, sets right, from the location's sunrise/sunset times) and moving with the time of
  day; after dark it becomes a cool **moon** glow. It is a soft light, not a hard disc (mockup B's orb
  was not chosen).
- **Technique (decided 2026-11-18):** one small AGSL shader on Android 13+ (GPU), frame rate throttled
  (e.g. 30 fps or less; the drift is slow), paused when off-screen, in battery saver and when the
  system "Remove animations" setting is on. Android < 13 (and the paused cases) use the baked static
  sky, with the sun field placed by time of day. Location cards stay static (baked) for scrolling.
- Colours: derived from conditions and time of day, harmonised with the wallpaper (dynamic) palette
  so cards and text stay readable; text contrast checked in both themes.
- Performance first: draw it cheaply, e.g. a few blurred shapes / a small pre-rendered bitmap scaled
  up, or an AGSL shader on Android 13+; grain as a tiled noise texture. No per-frame redraw unless
  something moves; measure frame times on the Pixel 7 (release build).

### Collapsing title: one title that moves and shrinks (added 2026-11-18)
- Today (settings and every screen using `FitStatusBarTopAppBar` / `LargeFlexibleTopAppBar`), the
  large title **cross-fades** into the small one when scrolling: both texts are visible at once
  for a moment. Replace it with a real transition: **a single title** whose position and size are
  driven by the scroll (`TopAppBarState.collapsedFraction`): it slides up and left from the large
  position into the bar and scales from the large to the small text size, never duplicated.
- Follows the finger exactly (no time-based animation), so it reverses mid-scroll; on release it
  settles to fully expanded or collapsed with the theme's spring (interruptible).
- How: our own top bar in `ui/expressive` (one `Text` with `graphicsLayer` scale/translation, measured
  once for both sizes; no re-layout per frame), swapped in at `FitStatusBarTopAppBar`, so all screens
  get it in one edit. Same component later for the Forecast/Days/Places titles.

### Screen transitions: no plain fade (added 2026-11-18)
- Tapping a settings entry (and opening any sub-screen) today **cross-fades** to the next screen:
  `SettingsActivity`'s `NavHost` sets no transitions, so Compose's default fade is used. Replace it
  with something smoother and more refined:
  - **Forward:** the new screen slides in from the right while the old one shifts slightly left and
    dims (parallax, Material shared-axis X / iOS-style push), spring-driven; **back** does the
    reverse.
  - **Predictive back:** the back gesture drags the screen with the finger (scales/peeks the
    previous one) and can be cancelled mid-way; release finishes with a spring.
  - Optional: the tapped row's **title flies into the new screen's large title** (shared element),
    so the user sees where they went.
- Interruptible, no fixed-duration fades; same transitions for the reorder and other expressive
  screens, and later for pages inside the app (Days/Forecast/Places use the pager swipe instead).
- How: `NavHost(enterTransition/exitTransition/popEnter/popExit)` with slide + slight scale using the
  theme's spatial springs, `SharedTransitionLayout` for the title; one shared definition in
  `ui/expressive`, one small edit in `SettingsActivity`.

### Days: a tactile (skeuomorphic) view, optional (added 2026-11-18)
- Besides the plain list of the next 10 days, an optional **skeuomorphic** presentation: a physical-
  feeling object for the week, e.g. a flip calendar / tear-off pages, a dial to turn through the days,
  or cards stacked like a deck that you flick through, with real depth, light and texture.
- A setting chooses between the clean list and the tactile view. Exact metaphor to decide with
  mockups; same motion rules (follows the finger, interruptible springs, haptics).

### Outlook as a real paper calendar (added 2026-11-18; extends the tactile view above)
Make Outlook look like an actual paper calendar. Proposals to mock up and compare:
1. **Tear-off day pad** ("page-a-day"): one page per day, big date numeral, weekday, the day's
   weather drawn like a stamp/illustration and the high/low; pull a page up/away to tear to the next
   day (page curl, torn perforation edge left behind, paper sound optional). Reference: classic
   page-a-day desk pads, Japanese *himekuri* daily calendars.
2. **Spiral desk flip calendar**: pages bound by metal rings at the top; flick a page over the rings
   to the next day; slight paper shadow and thickness of the remaining stack shows how many days are
   left. Reference: tent-style desk flip calendars.
3. **Wall month/week sheet**: a printed week or month grid on textured paper, each day's box with a
   small weather illustration and handwritten-style temperatures, today circled in pen. Reference:
   printed wall calendars, planner pages.
4. **Perpetual block calendar**: wooden/paper cubes or flip cards that roll to show day and weather.
   Reference: perpetual desk calendars (cubes, flip-card date displays).
- Shared details: paper texture (subtle grain, slight warmth), print-like typography (big numerals),
  real shadows/thickness, weather as ink illustrations in the day's palette colour; every gesture
  follows the finger and can be reversed mid-way (springs), haptic tick per page.
- References are for the look and feel only (no copying of any product's artwork). Choose one with
  mockups; the setting from "tactile view" switches between list and paper calendar.

### Places: visit vs pin, time difference (added 2026-11-18)
- Places behave like a map of places you **visit**: search somewhere else, look at its weather, and
  it stays only for the session (a "visiting" card) unless you **pin** it, which saves it
  permanently. Lets you consult other places without adding them for good.
- Pinned places are the saved list (the star from the swipe is the first form of this).
- Each place shows its **time difference** to you ("+1 h", "−6 h") and local time.
- Needs: a non-persisted "visiting" location in the view model (upstream saves every searched
  place), pin/unpin, and clearing visits after a while or on app restart.

### Alerts and Live Updates (added 2026-11-18)
Android's counterpart of iOS Live Activities is **Live Updates** (Android 16 promoted ongoing
notifications: lock screen, status bar chip, always-on display; plain ongoing notification on older
Android). Upstream already has weather-alert, precipitation and today/tomorrow notifications to
build on (the separate notification *widget* was removed).
- **Alerts for sudden changes and strong conditions:** a notification when the weather is about to
  change sharply (rain starting soon, temperature drop, wind jump, storm arriving) and warnings for
  strong conditions (official alerts from WMO/NWS/ECCC/MET Norway plus thresholds from the forecast:
  very high UV, heat, strong gusts, heavy rain). Plain language ("Heavy rain in 20 min, until
  about 16:00"), one notification per event, no repeats; per-type on/off and quiet hours.
- **Live Update for ongoing rain:** while rain is coming or falling at the current location, an
  ongoing Live Update shows the minute-by-minute picture (starts in / stops in, intensity bar from the
  nowcast). It appears ~30 min before rain, updates as it goes, and disappears when it stops. Uses
  the minutely (nowcast) data of Open-Meteo / MET Norway.
- **Live Update for the daily brief:** a short forecast brief for the day ("Clear and mild, 12–22°,
  rain after 18:00, take a jacket") as a Live Update at a **time the user chooses** (e.g. 7:00 on
  weekdays, 9:00 at weekends), staying until dismissed or until the evening; optionally a second
  brief in the evening for tomorrow. Shares the plain-language summary generator with the Forecast.
- Needs: notification permission flow, background refresh timed for the brief, battery-friendly
  scheduling (WorkManager, exact alarm only if needed), and a settings section for all three.

### Sky motion setting (added 2026-11-18)
- Appearance setting **"Sky motion"** as an inline button group: **Still · Calm · Gentle · Lively**.
  - Still: no drift (the baked static sky, no animation running at all; saves battery).
  - Calm: the first, very subtle drift (amplitude ~5–6 %, loops 79–151 s).
  - **Gentle (default):** current drift (amplitude 8.5–10 %, loops 47–97 s), user-approved 2026-11-18.
  - Lively: ~1.5× Gentle's amplitude and speed.
- How: one `motion` factor (0 / calm / 1 / 1.5) passed to `AnimatedSkyBackground` as a shader uniform
  scaling amplitude and speed; Still skips the shader. The weather effects (rain, snow, thunder) get
  their own on/off, but also stop when Sky motion is Still. System "Remove animations" and battery
  saver still override to Still.

### Weather effects: rain, thunder, snow (added 2026-11-18)
- Light, tasteful effects on top of the sky when it's actually happening at the location:
  - **Rain:** fine diagonal streaks (density from the precipitation intensity; drizzle sparse,
    downpour dense), slightly blurred, low contrast; optional droplets sliding on the "glass".
  - **Thunder:** occasional soft flash that brightens the sky for a moment (random intervals,
    rarer at low lightning probability), never strobing; no flashes if the user prefers reduced motion.
  - **Snow:** slow drifting flakes of a few sizes with gentle sway (parallax: big flakes faster).
  - Maybe later: fog banks drifting, wind making the streaks/flakes slant more.
- Same performance rules as the drift: GPU only (AGSL shader or a small particle shader), throttled
  frame rate, paused off-screen, in battery saver and with animations off (then: no effect, sky only).
  Effects stay behind the text and cards and are toned down under them for readability.
- Accessibility: flashes respect reduced-motion / "Remove animations"; option to turn effects off.

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

### Icon search: softer and rounder (added 2026-11-18)
The user wants icons that feel **softer and rounder** than today's (the toolbar and new screens
currently use Compose's *Outlined* Material icons; upstream's own `ic_*` drawables are sharper).
Compare options side by side on the real screens (toolbar, Places, settings rows, weather states)
before choosing; check each license.
- **Material Symbols Rounded** (Apache 2.0): same family as now but rounded corners and terminals;
  variable fill (filled when selected), weight and grade; fits Material You. Cheapest switch: the
  Compose `Icons.Rounded` set already in the app, or the variable font for finer control.
- **Phosphor** (MIT): very soft, friendly, round line caps; six weights incl. Duotone and Fill;
  has a good weather set.
- **Lucide** (ISC): clean, rounded strokes, consistent 2 px line; wide coverage.
- **Fluent UI System Icons** (MIT): rounded, soft, filled + regular pairs.
- **Weather condition icons** to consider alongside: **Meteocons** (MIT, soft, also animated),
  Phosphor weather, Material Symbols weather.
- Decide with a comparison mockup; then apply everywhere (toolbar, shell, settings rows,
  upstream `ic_*` drawables regenerated under the same names, see "New Material You Expressive
  icons everywhere").

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

### Search redesign: search bar at the bottom (added 2026-11-18)
- Redesign "Search somewhere else": the search field sits at the **bottom** of the screen (thumb
  reach, like a bottom address bar), results appear above it growing upwards, keyboard pushes the
  field up with it. Replaces upstream's top search screen (`SearchActivity`) with a Compose screen
  or sheet in `ui/expressive`; picking a result opens it as a visit (see Places: visit vs pin).
- **Transition (user's direction, 2026-11-18):** the "Search somewhere else" button **morphs into the
  search section** (container transform / shared element: the button grows into the bottom search
  bar and the results area, the list behind recedes), and closing morphs it back into the button,
  so everything feels fluid. Exact motion still to be proposed and agreed before building.

### Messages (snackbars) redesign (added 2026-11-18)
- Short, icon-led messages instead of sentences with buttons: e.g. "Guadalajara removed" becomes
  **[unpinned icon] Guadalajara unpinned**; "Visiting Puebla. Star it to keep it." becomes
  [compass icon] Visiting Puebla, etc.
- **No close (X) button:** dismiss by swiping the message away (sideways or down), following the
  finger; it still times out on its own. Undo stays as a compact action where it applies.
- Same glass look as the toolbar, sitting just above it. Motion (appear, swipe away) to propose to
  the user before building.

## Motion test checklist (added 2026-11-18)
Things to try on the phone before calling any motion done (debug and release builds):
- **Tapping toolbar tabs at the extremes:** from Outlook straight to Places (and back) the pager
  passes over Forecast: check the plain page fade, the sky, the highlight colour (sky colour on the
  way through Forecast?), the folding labels and the icon colours all stay smooth and don't flash;
  same when tapping during an ongoing swipe or animation (interrupt mid-way).
- **In-between states of the title hand-off:** half-scrolled titles (label half unfolded), a swipe
  between pages while a list is half-scrolled, scrolling while the pager is mid-swipe, fast flings
  and reversing direction; the label must never show twice or jump.
- Every gesture: hold still, release, reverse mid-way, interrupt with a second touch.

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
- `ui/main/MainActivity.kt` + `res/layout/activity_main.xml` (phone layout): host the Solstice shell in
  a `solstice_shell` ComposeView, hide/show upstream's HomeFragment as "More details", route
  `setManagementFragmentVisibility` to the Places page (F3).
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
- **2026-11-18:** Added: collapsing title should move and scale as one text instead of cross-fading
  between two.
- **2026-11-18:** Added: replace the fade between settings screens with a spring push/parallax
  transition, predictive back, optional shared title.
- **2026-11-18:** Gradient palette approved (nine conditions, table under "Forecast background").
- **2026-11-18:** Sky background built (static, baked, cached; debug preview screen). Decided: hand-tuned
  night palettes with continuous blending by sun elevation; calm drift + a sun/moon glow that follows
  the real time of day; AGSL shader on Android 13+, static fallback. Repo renamed on GitHub to
  `INFORMATION89/Solstice`; remote and links updated.
- **2026-11-18:** Night skies built: `SkyWeather` (7 families, each with a day and a night palette,
  approved night colours), `GoldenSky` and `TwilightSky`, `skyAt(weather, now, sunrise, sunset)`
  blending by the sun's height (clear/partly cloudy: day → golden → twilight → night; others:
  day ↔ night), and a sun/moon glow placed by time of day (rises left, peaks top, sets right). Debug
  preview now has one page per weather and a 24 h slider (`--ei page N --ef hour H`). Moon glow
  strengths lowered (clear 0.4, partly 0.3, snow 0.25) for readability. **Open:** on clear nights the
  approved top-left lavender (`#C9B6F2`) still sits pale behind white title text; options: darken it
  slightly, or add a soft legibility shade behind the title. Animated drift (AGSL) not built yet.
- **2026-11-18:** Clear-night top-left field darkened `#C9B6F2` → `#8E7CD0` (user's choice) so the white
  title reads at night (contrast with the title colour at the field's centre about 1.5:1 → 3:1; higher where it fades into the dark base).
- **2026-11-18:** System bar icons (clock, notifications, battery, gesture bar) now follow the sky's
  text colour (`ui/expressive/sky/SkySystemBars`): dark on light skies, light on dark skies, so icons
  and text always match. Used in the sky preview; the Forecast/Days/Places screens will call it too.
- **2026-11-18:** User: per-sky matching of icons and text is fine for now, but to **revisit later**
  (open: how text, system icons and notification icons should look together on the skies).
- **2026-11-18:** Drift built: `ui/expressive/sky/AnimatedSkyBackground` — one AGSL `RuntimeShader`
  (same three fields, sun glow and overlay grain as the baked sky; fields wander on 79–151 s loops,
  amplitude 5–6 % of the screen; the sun sways 1.5 %), redrawn at most 30 fps and only while the
  screen is resumed (no recomposition, draw-only). Falls back to the baked `SkyBackground` below
  Android 13, in battery saver and with animations off. Pixel 7 (debug build), 20 s idle on the
  preview: ~28 fps, GPU 4 ms median, 99th percentile frame 16 ms, 1.8 % janky. Looks the same as
  the static sky; drift is very subtle (mean change ~1/255 over 20 s): tune speed/amplitude after the
  user has watched it. Preview: `--ez static true` shows the baked version.
- **2026-11-18:** Added: alerts for sudden changes and strong conditions; Live Update for ongoing
  rain; Live Update daily brief at a user-chosen time.
- **2026-11-18:** Drift made more visible (amplitude 5–6 % → 8.5–10 %, loops 79–151 s → 47–97 s).
  Added to plans: weather effects (rain streaks, thunder flashes, snow).
  Cost after the change (20 s on the preview, debug build): ~28 fps, GPU 5 ms median, 99th
  percentile frame 17 ms.
- **2026-11-18:** Drift strength approved for now (Gentle). Added: "Sky motion" setting
  (Still · Calm · Gentle · Lively).
- **2026-11-18:** F3 step 1 built and checked on the Pixel 7: add a place via search → sky card in
  Places → tap → Forecast; swipe to Days; More details opens upstream's Forecast, Back returns.
  Found: new places default to AccuWeather for alerts/normals, which needs a key the fork doesn't
  have. To do: default alerts to WMO Severe Weather (AccuWeather stays available as an option,
  user wants to keep it).
- **2026-11-18:** Added: optional skeuomorphic Days view (setting); Places as visit vs pin with time
  difference.
- **2026-11-18:** Added (first version built with the toolbar): the Forecast tab adopts the current
  weather (condition icon, highlight colour from the sky) and morphs into the Material You colour
  as you swipe to Outlook or Places.
- **2026-11-18:** Built and checked on the Pixel 7: Days page renamed **Outlook** (calendar icon) on a
  plain white/black page with each day's card in its weather gradient; Places on the same plain page;
  the plain page fades in/out with the swipe position (draw-phase only, follows the finger,
  reverses); toolbar with icons (calendar, condition icon, map) whose titles fold into the icon and
  unfold for the selected tab, highlight follows the swipe and takes the sky's colour on Forecast;
  Places: two-stage right-to-left swipe (≥22 % star, ≥60 % delete, haptic tick per stage, spring
  back, interruptible), delete with Undo snackbar, star stored in app prefs (`solstice_places`),
  "Search somewhere else" button. Tested: star, delete, undo (place returns, still starred).
- **2026-11-18:** Added: search for softer, rounder icon options (Material Symbols Rounded, Phosphor,
  Lucide, Fluent; Meteocons for weather) and compare with a mockup before choosing.
- **2026-11-18:** Added: Outlook as a real paper calendar, four proposals (tear-off pad, spiral flip,
  wall sheet, perpetual blocks) to mock up. Toolbar is now frosted glass (backdrop blur); Outlook is
  edge to edge with progressive blur at the top and bottom edges.
- **2026-11-18:** Built: `ui/expressive/components/Glass.kt` (frosted glass: content records itself into
  a GraphicsLayer each frame, each glass element blurs only its own rectangle of it, + tint + hairline
  edge; Android 12+, opaque tint below) used on the toolbar; `EdgeBlur.kt` (progressive blur + fade
  at top/bottom edges, only the two bands blurred) on Outlook, now edge to edge. Pixel 7 debug build,
  scrolling Outlook: median 11 ms, GPU 6 ms, 99th 34 ms, 2.9 % janky. Re-check on release; apply edge
  blur to Places and Forecast once happy.
- **2026-11-18:** Added: animated stage change and a playful discard animation for the Places swipe.
- **2026-11-18:** Fixed: Places swipe could stay stuck where the finger was after hold-and-release
  (queued drag coroutines cancelled the release animation); the drag now moves the card directly and
  only the release animates. Added to plans: spring ripple through neighbouring places on release.
- **2026-11-18:** Title hand-off built: on Outlook and Places the toolbar tab is icon-only while the
  page's big title is on screen, and its label unfolds as the title scrolls away (driven by the list's
  scroll, so it follows the finger both ways); Forecast keeps its label (its page title is the place
  name). Checked on the Pixel 7, plus the stuck-swipe fix (hold 1.5 s then release: card springs back).
- **2026-11-18:** Long-press a place to edit it (opens upstream's place settings), with playful
  feedback: the card sinks while pressed, then pops, bumps (haptic) and wiggles. **Later refinement:**
  a more playful, physical long-press animation (e.g. the card lifts off the list with a shadow and
  a jelly squash-and-stretch, the others make room), shared with drag-to-reorder: lift then drag to
  move, lift then release to edit. Also: "Use my current location" in Places (current position always
  first), upstream's source chooser drawn by the shell, and new places default to WMO alerts
  (AccuWeather still selectable).
- **2026-11-18:** Checked on the Pixel 7: "Use my current location" opens the source chooser (cancelled
  in the test, nothing added); long-press opens "Edit this location" after the pop-and-wiggle (dialog
  now waits for the wiggle). Upstream's edit dialog is still the old style: restyle as a sheet later.
  **F3 left:** drag to reorder (lift then drag, sharing the long-press), visit vs pin.
- **2026-11-18 (agreed with the user):** Places reorder = **lift & drag**: long-press lifts the card
  (~3 % larger, deeper shadow, haptic); drag it, it tilts slightly with your speed; cards near it are
  nudged a little out of its way before it reaches them (feels like moving a physical object), then
  slide on springs as it passes, with a tick per slot; release without moving opens the place's
  settings. **Drop:** soft, non-bouncy settle of the card; the neighbours give a subtle bounce as it
  lands. Replaces the placeholder wiggle. **Visiting:** a search result opens as a visit (Forecast
  right away, not saved); in Places a lighter "Visiting" card with a dashed edge, local time and time
  difference sits at the top; star (swipe) to keep it, otherwise it's gone on the next app start.
- **2026-11-18:** Added a motion test checklist (tab taps at the extremes, title hand-off in-between
  states, interrupt/reverse every gesture). Built: Places lift & drag reorder (agreed spec).
- **2026-11-18 (user note):** don't let the app go into landscape on phones: lock to portrait
  (manifest / per-activity), decide later whether tablets keep rotation.
- **2026-11-18:** Reorder checked on the Pixel 7 (temporary second place): long-press lifts and
  tilts the card, drag passes over the neighbour, drop saves the order (kept after restart). Test
  place removed afterwards. Added: search redesign with the search bar at the bottom.
- **2026-11-18:** Added: the "Search somewhere else" button morphs into the search section.
- **2026-11-18:** Added: a way to tell apart places with the same current weather (generated map
  overlay idea).
- **2026-11-18:** Added: redesign messages/snackbars (icon + short text, no X, swipe to dismiss).
- **2026-11-18:** Visiting places built: a search result is added, marked as a visit
  (`ui/expressive/shell/Visits.kt`), selected and shown in Forecast with a "Visiting X. Star it to
  keep it." message; in Places it sits first, lighter, with a dashed edge and "Visiting · weather ·
  local time, ±h"; starring keeps it; unstarred visits are removed on the next app start (checked).
  Cards in another time zone show local time and difference. Found: upstream **recreates
  MainActivity when the location-based theme flips** (night here, day there), which dropped the
  pending page/message; now handed over process-wide. To do: in the shell, avoid that recreate
  (the sky already follows the place), it causes a visible flash. Fixed: Outlook's edge blur now
  fades in with the plain page instead of showing over the sky when a swipe starts.
- **2026-11-18:** Checked on the Pixel 7: picking a search result now lands on its Forecast (Tokyo test,
  even with the theme-flip recreate); half-started swipe to Outlook shows no edge darkening over the
  sky. **F3 done** for phones (shell, Outlook, Forecast v1, Places with star/delete, reorder, current
  location, edit, visits). Open F3 follow-ups: search redesign, message redesign, avoid the
  theme-flip recreate, tablets.
- **2026-11-18 (agreed with the user):** Search redesign: the "Search somewhere else" button **grows
  into a glass search bar at the bottom** (rises with the keyboard; Places dims and recedes; results
  rise from the bar; back / swipe down reverses into the button). Results: **simple rows** (name,
  region/country, local time and difference), **first result highlighted, Enter picks it**. Before
  typing: **cities nearby** and **recent searches**. Results **as you type** (~300 ms pause). Adding a
  place **skips the weather-sources panel** (default sources; change them later in the place's
  settings as an advanced option).
- **2026-11-18:** Search redesign built (`ui/expressive/search/`): `SearchOverlay` morphs out of the
  Places button into a bottom bar above the keyboard (one spring for open/close, page dims and
  recedes, toolbar steps aside, back / tap outside / swipe down closes), results as you type (300 ms),
  first result highlighted right above the bar, Enter picks it; before typing: Recent and Cities
  nearby. Picking adds the place as a visit with no weather-sources panel (`setSelectedLocation` then
  add directly), recent searches stored (`RecentSearches`). Cities nearby from a bundled GeoNames
  cities15000 list (`res/raw/solstice_cities.tsv`, 34 246 cities, 1.7 MB file / ~0.7 MB compressed,
  CC BY 4.0, credit in `third_party/geonames/`). Places: the selected place is first; "Visiting" shows
  only on the selected place. MainActivity window set to adjustResize on phones (the system was
  panning the window when the field got focus). Checked on the Pixel 7. Upstream's SearchActivity
  (and its sources panel) remains only for tablets.
- **2026-11-18 (user notes + agreed):** Search round 2 (Arc-search-like): **one big card** holding the
  results (segmented rows) and the field at its bottom, **fixed height with a handle**: drag up to
  grow and show more (springs to compact / tall), drag down from compact follows the finger and
  dismisses past a point (springs back if released early); **darker + blurred** content behind;
  **no-results** state; results **settle in place** when they change (rows keep their spot and slide
  on springs, new ones fade in with a small rise, removed ones fade out, card height eases). Search
  button becomes a **round bubble bottom-right** above the toolbar on Places, morphing into the card.
  Before typing: **5 big cities in your country (not within ~50 km) + 5 popular world cities**,
  instead of nearby localities. Current location naming: **neighbourhood first**, city on a smaller
  second line (Places card and Forecast), location icon next to the weather on the card; long names
  shrink to fit, then ellipsize.
- **2026-11-18 (user note):** **Remove the star feature**: the visiting system replaces it. Open
  question for when we build it: how a visit becomes a kept place once stars are gone (today,
  starring a visit is what keeps it). The first swipe stage would need a new job (for example
  "Keep" on visits).
- **2026-11-18 (built, checked on the Pixel):** Search round 2. One card with a handle (drag up
  grows it to tall, drag down dismisses; checked both). The page behind is darker and blurred
  (API 31+). "No places called …" shows only once the search has answered, with a loading
  indicator while waiting. Result rows settle in place (`animateItem` springs). The round search
  bubble sits bottom right on Places; it fades and scales with the swipe and the card grows out
  of it. Before typing: Recent, then **Popular places** (5 big cities in your country, at least
  50 km from you and from each other, then 5 world cities from a fixed list). The current position
  shows its neighbourhood first, the city smaller on the next line, and the location icon next to
  the weather. Long names shrink (`TextAutoSize`) and then ellipsize. The old `Cities.nearby` is
  unused now.
- **2026-11-18 (user note):** The search bubble should **come out of the Places icon on the
  toolbar**, a bit like Apple's Liquid Glass: when you land on Places, the bubble buds off the
  toolbar's Places pill (glass, gooey join) and settles **in line with the toolbar**, as a round
  glass button beside it on the same row (not floating above it), then merges back into the pill
  when you leave. Animation options still need to be proposed and agreed first (consult rule).
- **2026-11-18 (refinements, checked on the Pixel):** Phones are locked to portrait (every
  activity, when the smallest width is under 600 dp; tablets keep rotating). The shell no longer
  switches the activity's day/night mode per place, which recreated the activity and flashed the
  screen. The sky already follows the place; the app theme follows the system, and so does "More
  details". The search handle grabs a 40 dp strip but looks unchanged (user note). Removed the
  unused "Cities nearby" code and string.
- **2026-11-18 (animations agreed with the user):**
  - **Search bud: gooey bud.** On Places the search bubble swells out of the right edge of the
    toolbar's Places pill. A stretchy glass neck keeps it joined, then it snaps free into its slot
    in line with the toolbar on a soft spring. It is driven by the pager swipe (a half swipe gives
    a half-out bud) and merges back the same way.
  - **Messages: rise and swipe.** A message rises from behind the toolbar on a soft spring and
    auto-hides by sinking back. Swipe it sideways to dismiss: it follows the finger with a slight
    tilt and flies off past a threshold, or springs back.
  - **Collapsing titles: follow and settle.** A single title moves and shrinks exactly with the
    scroll. On release it springs to fully expanded or fully collapsed. No fades and never two
    titles at once.
  - **Screen transitions: iOS-style push.** The new screen slides in from the right while the old
    one shifts slightly left and dims; back reverses it. Predictive back drags with the finger and
    can be cancelled midway.
- **2026-11-18 (built, checked on the Pixel):** The gooey search bud. The toolbar pill, the neck
  and the search bubble are one glass outline (`BudShape` in `SolsticeShell.kt`). The pager swipe
  slides the bubble out of the pill's right cap, and the group recentres as it widens. Once they
  part, a neck pinches between them and thins with distance, then snaps away on a bouncy spring
  (damping 0.5) near the end of the swipe and regrows on the way back. Tapping the bubble opens the
  search card from it. The floating bubble above the toolbar is gone. Debug build while swiping:
  GPU 5 ms median, 7.8 % janky frames (debug). Not yet measured in release.
- **2026-11-18 (user note): first-time setup, redesigned and very simple.**
  1. **"Where are you?"** A short line of text and a search box (the same search as Places),
     plus "Use my current location".
  2. **Where the weather comes from.** A very simplified source choice (one recommended option
     preselected, a few plain-language alternatives), with a clear note that **you can change it
     later** in settings.
  Replaces upstream's onboarding. Screens and motion still to be proposed and agreed.
- **2026-11-18 (user note):** **Animations feel a bit broken after the latest changes** (search
  card, gooey bud, no theme-flip recreate). To do: go through the motion test checklist on the
  Pixel (tabs, swipes, search open/close, handle drag, bud in-between states, reorder) in a release
  build, find what regressed and fix it.
  - **When the toolbar changes size it breaks for a second** (tab label unfolding, search bud).
    Likely cause: the glass reads its own position from layout state one frame late, so for a
    frame the blurred backdrop is drawn from the wrong spot. Fix: read positions at draw time.
    **Fixed 2026-11-18:** the glass now clips while drawing, using an outline built from the size
    being drawn, and reads positions at draw time. The bud outline is measured from that size too.
    A screen recording shows the toolbar's ends stay round through the resize.
- **2026-11-18 (performance, release build on the Pixel 7 at 90 Hz):** Fixed the frame drops the user
  saw when swiping pages and opening search:
  - Search's open spring no longer recomposes the whole shell every frame. The card's size and
    place are worked out in layout only, and its results are built once the card is a third open.
  - The blur behind search runs on a quarter-size copy of the page with a fixed radius, fading in
    while the sharp page fades out. GPU peaks during the open went from 25 ms to about 14 ms, and
    slow frames from about 12 to 3–4 per open.
  - Glass caches its blur effect and outline. The bud uses a plain rounded outline when tucked in.
    The animated sky renders in its own offscreen layer.
  - All three pages stay built, so going Places to Forecast no longer builds Outlook mid-swipe
    (that was a 28–54 ms spike).
  - Fixed: the tucked-in search bubble swallowed taps on the Places tab.
  - The search card is now translucent like the toolbar (user note), and Popular places no
    longer repeats a city already under Recent.
- **2026-11-18 (agreed with the user):** Search list polish:
  - **Soft fade at the edges:** rows dissolve into a short fade above the field and under the
    handle, never sliced by a hard line.
  - **Results rise in, staggered:** when results first appear, rows rise a few dp and fade in one
    after another, starting from the field and going up (about 25 ms apart, soft spring).
  - **Changing results: settle + crossfade:** rows that stay slide to their new place, new rows
    rise in, gone rows fade out, and the best-match highlight eases instead of jumping.
  **Built 2026-11-18 (checked in a release build on the Pixel):** soft fades at the list's top and
  bottom edges; rows rise in staggered from the field upward when the card opens and when a new
  answer arrives; switching between suggestions, loading, no results and results crossfades;
  rows that stay slide on springs, gone rows fade out; the best-match highlight eases.
- **2026-11-18 (user note): stars stay, and visiting changes meaning.**
  - **Keep stars.** Starring a place makes it **the main place**.
  - **Visiting is temporary,** like opening a new window: when you go back, you return to the main
    place. Future: an animation that conveys the "new window" feeling (to be proposed and agreed).
  - **Remove the tap-to-Forecast jump:** tapping a place in Places no longer moves to Forecast.
    This is independent of how visiting ends up working.
  - Open questions are in the conversation (one main place or several starred; what a tap does;
    what "back" returns to).
- **2026-11-18 (user note): search should never look like it's loading.** Remove the loading
  indicator and the fade out/in around it (too much). Instead, show matches from the bundled
  city list instantly as you type (local prefix match, ranked by population). When the online
  answer arrives, the rows settle into it without a blank state. Also keep the previous results
  on screen while a new answer is on its way.
- **2026-11-18 (user note): the keyboard key should say "select", not "search".** Results already
  update as you type, so the keyboard's action key should show a select/confirm icon (e.g. the
  check mark of `ImeAction.Done`) instead of the magnifier. The highlighted best match should
  carry a hint that Enter picks it, e.g. "Example City · Select".
- **2026-11-18 (agreed + built, checked in the debug app on the Pixel):** **One star = the main place.**
  Starring a place moves the star there, and starring the main place again clears it. The app opens
  on the main place. **Tapping a card** selects it and stays on Places. **Back from any other place**
  returns to the main place; a place opened from search is then removed. Search's own back and the
  details screen come first. Visit message: "Visiting X. Go back to return, or star it to make it
  your main place." Still to propose: the "new window" animation for visits.
- **2026-11-18 (user note): tapping a place should open a new interface** with that place's Forecast
  and its This week page, like opening a window over Places (the "new window" visit idea). Back
  closes it and returns to the main place. Until that's built, a tap just selects the place and
  stays on Places. The screen layout and opening animation are still to be proposed.
- **2026-11-18:** Started thinking about **skeuomorphism** (ideas proposed in the conversation; none
  chosen yet).
- **2026-11-18 (built, checked in a release build on the Pixel):** **Never-loading search.** The
  loading indicator is gone. As you type, matches come from the bundled city list (prefix match,
  accents and case ignored, biggest first), and the index is built in the background when search
  opens. When the online answer arrives, the same city keeps its row (key: name + country) and its
  region fills in. While waiting with no local match, the last rows stay. "No places called …"
  only shows once the online answer is empty too. Mode switches are a short crossfade. The
  **keyboard key is a check mark** (Done) that picks the highlighted match, which reads
  "City · Select".
- **2026-11-18 (user picked): postcards.** Places become postcards: the sky as the picture, a stamp
  with the local time, a postmark with the country. Opening one (the new-window visit) pulls the
  postcard out and turns it over into that place's Forecast and This week. Card styles proposed:
  A classic split (picture left, address side right), B framed photo (white border, script name),
  C stamp on sky (today's card plus a stamp and postmark). Opening motion still to agree.
- **2026-11-18 (user picked): postcard style B, framed photo.** The sky is a photo inside a white
  border, with the place name in script, a stamp with the local time in the corner, and the
  temperature on the picture. Opening motion (pull out and flip, or pull out with no flip) still
  to agree.
- **2026-11-18 (user note): redesign the messages (toasts) and fix them; they don't go away on their
  own** (design in "Messages (snackbars) redesign" above). Likely cause: messages with an action
  (Undo) default to `SnackbarDuration.Indefinite` in Material 3. Fix: an explicit duration (Short,
  or Long with Undo), together with the agreed "rise and swipe" redesign.
- **2026-11-18 (agreed with the user): postcard opening, pull out with no flip.** Tapping a postcard
  lifts it with a slight tilt, and it grows into a full-screen window with that place's Forecast and
  This week. The white frame and stamp fade away while the sky photo grows into the window's sky,
  and the forecast fades in as it grows (readable almost at once). Back plays it in reverse,
  following the finger (predictive back, can be cancelled midway), and lands the card in its slot.
  Chosen over the flip because checking a forecast is the most frequent action: no reading delay,
  quick glance-and-back, and swiping between places inside the window later works like the pages.
- **2026-11-18 (built, checked in the debug app on the Pixel):** Messages redesigned and fixed.
  The shell's snackbars are replaced by `ShellMessageHost` (`shell/Messages.kt`): a glass pill with
  an icon and short text above the toolbar, and no close button. "[compass] Visiting Tokyo" and
  "[bin] Shinjuku City removed · Undo". It rises from behind the toolbar on a soft spring, and
  **goes away on its own** (3 s, or 5 s with Undo) by sinking back. The old ones stayed forever
  because Material 3 makes messages with an action Indefinite. Swipe it sideways to dismiss: it
  follows the finger with a slight tilt and flies off past 35 % (or a fling), or springs back.
  Holding it pauses the timer. A new message replaces the current one. Checked: the visit message
  sinks by itself after ~3 s, and swiping dismisses the removed message. Undo itself wasn't tapped
  in testing (same code as before). Upstream's View snackbars (source-settings dialog) are
  unchanged.
- **2026-11-18 (fixed, checked in the debug app on the Pixel):** Message dismiss. A message wider
  than the toolbar showed its ends beside and below the toolbar while sinking. It's now cut along
  the toolbar's top edge, so it slips behind it (sideways swipes still fly off freely). **All
  main-screen messages use the new pill:** upstream's `SnackbarHelper` hands its messages to the
  shell while it's on screen (location added, updated or already there, refresh errors with their
  action, "please wait"…), with an icon picked from the text. With "More details" open, or on
  other screens, the old snackbar is still used.
- **2026-11-18 (fixed, checked in the debug app on the Pixel):** A half-flicked message stayed where it
  was let go. Queued per-move jumps cancelled the spring-back, the same bug the Places swipe once
  had. The gesture now writes plain state and one cancellable settle runs on release, carrying the
  finger's velocity. **Added (user's request): vertical follow.** The message follows the finger
  down (0.7×) and resists going up (0.25×). Pulled down past a little, or flung down, it tucks
  behind the toolbar.
- **2026-11-18 (built, checked in a release build on the Pixel):** **Postcards (style B)** in Places:
  a cream paper frame (with a light shadow, stronger on the selected place), the place's sky as the
  photo, the name in script (`FontFamily.Cursive`, shrinks to fit), the city small above it for the
  current position, a perforated stamp with the local time (the top line keeps only the weather
  and the time difference), the temperature on the picture, and a star on the main place. Visits
  keep a dashed frame. **Postcard window** (`shell/PlaceWindow.kt`): tapping a postcard grows it
  (one spring) from its bounds into a full-screen window. It shows that place's Forecast, with
  This week a swipe to the left on the plain page. A slight tilt (up to 1.6°) peaks mid-way, the
  frame thins away, the stamp fades, and the forecast fades in from about a quarter of the way.
  Back (predictive, follows the finger, can be cancelled) shrinks it into its slot. The window
  doesn't change the selected or main place; "More details" selects the place first. The toolbar
  steps aside while it's open.
- **2026-11-18 (built, checked in a release build on the Pixel):** **Screen push** in settings
  (`components/ScreenPush.kt`, wired into `SettingsActivity`'s NavHost). The new screen slides in
  from the right while the one below shifts a quarter left and dims to 55 %; back does the reverse,
  with predictive back driven by Navigation. Springs have no bounce.
- **2026-11-18 (built, checked in a release build on the Pixel):** **Collapsing title, follow and
  settle** (`components/CollapsingTopBar.kt`, swapped in at `FitStatusBarTopAppBar`, so every
  settings screen gets it). One title is laid out once at the large size and scaled down
  (headlineMedium to titleLarge) as the bar collapses. It slides from its large place into the bar
  row, with the sideways move leading so it clears the back arrow. It follows the scroll exactly,
  never shows twice, and the existing scroll behaviour settles it open or closed. The bar takes on
  the scrolled colour as it collapses. State is read in layout and draw only.
