# Continuing on another computer

Where things stand and how to pick the work up anywhere. The plan, decisions and log are in
[PLAN.md](PLAN.md).

## Where we are (2026-11-19)

- **Repo:** this one (`INFORMATION89/Solstice`): upstream Breezy Weather's files at `5ec3b9b`
  **without upstream's git history** (see `UPSTREAM_BASE`). Never push upstream's history here.
- **F0 done (2026-11-18):** snapshot, app name "Solstice", `applicationId`
  `io.github.information89.solstice` (renamed 2026-11-18; was `…breezyexpressive`), CI (`.github/workflows/build.yml`), first run on the
  Pixel 7, own launcher icon with themed-icon layer (regenerate with `tools/make_icon.py`).
  `./gradlew assembleBasicDebug` builds (first build 6–20 min).
- **F1 done:** Expressive theme in `ui/expressive/theme/` (Google Sans Flex, expressive motion,
  larger corners, sky-blue/amber fallback). `Theme.kt` delegates to it.
- **F2 done** (except real blur): settings rows with colored icon shapes and press morph, inline
  button groups, bottom sheets, icon switches, collapsing large title, Compose reorder screens
  (`ui/expressive/reorder/`). New UI code lives in `ui/expressive/`; upstream files touched are
  listed in PLAN.md.
- **Built since F2 (2026-11-18/19, details in the PLAN.md log):** the Solstice shell
  (`ui/expressive/shell/`): Forecast, This week and Places pages in a pager, the floating glass
  toolbar with the button budding out of it, the redesigned search (`ui/expressive/search/`), the
  postcard window for visiting a place, messages, the moon card as a sheet that morphs out of its
  pill, frosted glass (`components/Glass.kt`) and progressive edge blur (`components/EdgeBlur.kt`).
  AccuWeather is disabled as a source.
- **Motion rules:** [MOTION.md](MOTION.md) (agreed 2026-11-19). Values come from the `:motion`
  module (`motion/`, Apache-2.0, 22 tests run in CI); `ui/expressive/components/SolsticeMotion.kt`
  forwards to it. Only the moon card runs on `:motion` so far.

## Done on 2026-11-19 (details in the PLAN.md log)

- **Design tokens** (`theme/Tokens.kt`), checked on the Pixel 7.
- **`:motion` M1 to M3:** tokens with response + damping and a sixth `snappy`, `MotionValue` with the
  UIKit release rules, `Morph` and anchors, the **Solstice `MotionScheme`**, and the **moon card on
  `:motion`** (janky frames ~10 % to 3.3 %). The review's bugs are fixed (catching an overshoot,
  catching mid-flight).
- **Glass on Haze 2.1.0** for good; **edge blur stays ours** (Haze measured slower there).
- **Component catalog, first pass:** pages split out of `ShellPages.kt`, `SolsticeCard`,
  `shell/Toolbar.kt`, `shell/PageFrame.kt` shared with the postcard window.
- **Library swaps:** Reorderable 3.1.0 runs `ReorderList.kt`; Material 3 controls everywhere except
  the toolbar and the postcard.
- **Icons on Material Symbols Rounded** (2026-11-19, PR #1), CI green, not yet seen on the phone.
- **Navigation 3: decided and started** (user, 2026-11-19). As of the last check the migration was
  not on `main` yet: pull, or ask where it lives, before touching Settings or pushed screens.

## Pick up here (next session)

The focus is still **iOS-level polish with shared systems**. The icon task is done (to check on
the phone); next is M4. Background: [UI_SYSTEMS.md](UI_SYSTEMS.md), PLAN.md "Icons", "Motion framework" and
"Design system foundations".

### Icons: done 2026-11-19 (PR #1, merged), to check on the phone

Material Symbols **Rounded** everywhere (rules in PLAN.md, "Icons"). What to look at on the Pixel 7:

- **Every screen:** shell pages, toolbar, search, postcard window, messages, Places swipe, reorder,
  Settings, details, Management. The shapes changed from sharp/old Material to Rounded, and upstream's
  Pictogrammers icons (search, location, wind, gauge, eye, delete, drag, back…) are now Symbols.
- **Weather condition icons** changed glyphs: clear night is `clear_night` (moon and stars, was the
  bedtime moon), partly cloudy is `partly_cloudy_day`/`_night`, rain is `rainy` (our `RainCloud` is
  gone), snow is `weather_snowy` (was a snowflake), fog is `foggy` (was three lines).
- **Small icons** (12 to 20 dp: postcard, Forecast "near me", rain card, messages, switch thumbs) use
  optical size 20, so their strokes look a touch heavier than before.
- `ic_gauge` became `speed`, `ic_uv` became `wb_sunny`, `ic_wind` became `air`, `ic_eye` became
  `visibility`: check that they still read right in the details screens.

How to add an icon: `tools/symbols.txt` + `python3 tools/make_symbols.py` (PLAN.md, "Icons").
Navigation 3: if that migration adds icons, use `SolsticeIcons` (CI fails on the old ones).

### Still open from before

- **M4:** search (`search/SearchOverlay.kt`, 906 lines) and the rest onto `:motion`; brings one
  `SolsticeSheet` and a page scaffold. Propose search's open/close options first (motion rule).
  27 `spring(…)`, 7 `tween(…)` and 12 `animate*AsState` left in `ui/expressive/`.
- **Moon card by feel:** a flick just under 1000 dp/s that projects to about half way stays open
  (the UIKit rule as specified). Lower the fast-flick speed for it if it feels wrong.
- **Tokens left:** 174 `.dp` literals, 13 `RoundedCornerShape(…)`, 33 `Color(0x…)`; finish alongside
  M4 and I2.
- Smaller: `Motion.control` and `Motion.settle` are the same spring (merge or tell apart);
  `rememberMotionValue` ignores later changes to its rest points; `Modifier.searchBlur` in
  `SolsticeShell.kt` is unused; `unresist` caps at 99.9 % of the over-pull limit (a very hard fling
  past `overMax` snaps back slightly when caught).
- **M5:** the motion lab screen and the `:motion` README for other projects.

Rules that still apply: propose and agree any new or changed motion before building it; keep the
PLAN.md log and [ROADMAP.md](ROADMAP.md) up to date; new code in `ui/expressive/`; check on the
Pixel 7, smoothness on the `profile` build (`./gradlew assembleBasicProfile`, installs as
`io.github.information89.solstice.profile`). Debug builds from different computers are signed with
different keys: copy `~/.android/debug.keystore` between them to avoid reinstalling.

Reference for iOS behaviour: `NEORUAA/Glasmio` (Apache-2.0), `docs/ios-sheet-motion.md` and
`docs/dropdown-motion.md`. Research only, not a dependency.

- **Performance:** judge smoothness on a release build; debug is much slower. Release builds take
  a long time the first time (R8).
- **Windows:** `.gitattributes` keeps LF so Spotless passes. In Git Bash, set
  `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` (Android Studio's JDK 25 works).

## Set up a new computer

1. Install **Android Studio** (it bundles a JDK; the build needs **Java 21**), or install JDK 21 and
   the Android command-line tools yourself.
2. In the SDK manager, install **Android SDK Platform 37** and **Build-Tools 37**.
3. Clone and build:
   ```bash
   git clone https://github.com/INFORMATION89/Solstice.git
   cd Solstice
   ./gradlew assembleBasicDebug
   ```
   If not using Android Studio, create `local.properties` with `sdk.dir=<path to your Android SDK>`
   and set `JAVA_HOME` to a JDK 21. On Windows use `gradlew.bat`.
4. Install on a phone with USB debugging on:
   ```bash
   adb install -r app/build/outputs/apk/basic/debug/app-basic-arm64-v8a-debug.apk
   ```
   The debug app is `io.github.information89.solstice.debug`.

**Never** build with `-Pbreezy`: that config is reserved for the official Breezy Weather app.

## Updating from upstream (no shared history)

Keep a separate full clone of upstream, then apply the changes between our base and the new one:

```bash
git clone https://github.com/breezy-weather/breezy-weather.git breezy-weather-upstream
cd breezy-weather-upstream
git diff 5ec3b9bcb <new-commit> -- . ':!.github' ':!README.md' > ../upstream.patch
cd ../Solstice   # your clone of this repo (folder name may differ)
git apply -3 ../upstream.patch
```

Fix any conflicts, update `UPSTREAM_BASE` with the new commit and date, and note it in the PLAN.md log.

## Conventions

- Commit author: `INFORMATION89 <30944609+INFORMATION89@users.noreply.github.com>`.
- New code goes in `org.breezyweather.ui.expressive`; keep edits to upstream files small and list
  them under "Upstream files touched" in PLAN.md.
- Keep the PLAN.md log up to date.

## Related

- `INFORMATION89/Weather-App-Experiment` (private): the earlier standalone app. Its custom design
  (sky renderer, glass, sun path) was dropped for this fork; kept only as an archive.
