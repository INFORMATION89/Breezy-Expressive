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
- **Navigation 3: decided and started** (user, 2026-11-19). As of the last check the migration was
  not on `main` yet: pull, or ask where it lives, before touching Settings or pushed screens.

## Pick up here (next session)

The focus is still **iOS-level polish with shared systems**. The current task is **consistent
Material icons**. Background: [UI_SYSTEMS.md](UI_SYSTEMS.md), PLAN.md "Icons", "Motion framework" and
"Design system foundations".

### Icons: where things are (counted 2026-11-19)

Three sources that don't match:

1. **`material-icons-extended`** (Compose's old Material Icons). Google froze that set in 2022 and the
   Compose artifact stopped shipping after Compose 1.7, so it gets no new icons. Use in the whole app:
   48 `Icons.Outlined`, 22 `Icons.Filled`, 6 `Icons.AutoMirrored`, 2 `Icons.Default`. In
   `ui/expressive/`: 34 distinct icons, 40 Outlined and 14 Filled. Filled is used both for "selected"
   (`Star`, `PushPin`) and just because (`Search`, `NearMe`, `Check`, `Close`, `Add`).
2. **54 upstream `res/drawable/ic_*.xml`** (old Material Icons paths, 43 referenced from code), plus
   `ic_drag` and `ic_close` used in `ui/expressive/`.
3. **Our own vectors:** `components/SolsticeIcons.kt` (`RainCloud`) and the 8 moon phase drawables.

Not icons for this task (they're artwork): the weather condition packs (`weather_*`, 99 files),
`shortcuts_*` (30) and `widget_*` (16). Condition glyphs in the UI (`WbSunny`, `WbCloudy`,
`Thunderstorm`, `AcUnit`, `Dehaze`, `Cloud`, `Bedtime`) **are** in scope.

Sizes: 29 `Icon(` calls in `ui/expressive/`; 9 use the `Sizes.Icon*` tokens, the rest literals
(18, 32, 28, 22, 14, 12 dp).

### Icons: options to decide with the user

1. **Style:** Material Symbols **Rounded** (matches Expressive's soft shapes and Google Sans Flex),
   Outlined, or Sharp. One style app-wide.
2. **Weight and fill rule:** one weight (400 suggested, or match the text weight); **unfilled by
   default, filled only for "on/selected"** (starred, pinned, current tab, location in use).
3. **Delivery:**
   - **A. Vector drawables** downloaded from fonts.google.com/icons at the chosen style/weight
     (filled and unfilled where a state needs both). Smallest, works on minSdk 24, no extra
     dependency; fill changes are a crossfade, not an animation.
   - **B. The variable font** (Material Symbols Rounded, subset to the icons used) through
     `FontVariation`: fill, weight, grade and optical size can animate (e.g. the star filling in).
     Variable axes need Android 8+ (minSdk is 7, so a static fallback), and icons become text
     (accessibility labels by hand).
   - **C. A library:** `io.github.hlcaptain:symbols-material-rounded` (Material-Icons-like API,
     vectors or variable font; minimum AGP 9.1.1, Kotlin 2.4, compileSdk 37; very new) or
     `dev.vicart:compose-material-symbols` (font-based, 1.1.6, 2026-09-04). Check license, size and
     upkeep first.
   - Suggested: **A**, plus B only for the few icons that animate a state, if the user wants that
     (animating fill is motion: agree it first).
4. **Scope:** `ui/expressive/` first; then upstream screens still in use (Settings rows, details),
   keeping edits to upstream files small; widgets and shortcuts last or never.

### Icons: proposed steps (after the decisions)

1. **I1, catalog:** one `ui/expressive/components/SolsticeIcons.kt` object naming every icon the app
   uses (`SolsticeIcons.Search`, `.Star(filled)`, …) so call sites never pick a source or style.
   Sizes from `Sizes.Icon*` only (add `IconXSmall`/`IconXLarge` if the 12/14/28/32 dp ones are real).
2. **I2, `ui/expressive/`:** replace all 34 `Icons.*` and `ic_drag`/`ic_close` with the catalog.
   Check every screen on the Pixel 7 (screenshots before/after).
3. **I3, upstream screens:** swap the 43 referenced `ic_*.xml` for the same style (same file names
   where possible, so upstream merges stay easy).
4. **I4:** drop `material-icons-extended` (big dependency; R8 strips most of it, but the build gets
   faster and nothing old can sneak back in). Add a check (e.g. a Spotless/ktlint rule or a grep in CI)
   that fails on `Icons.` outside the catalog.
5. **I5 (optional, agree first):** animated fill for state icons (star, pin) with option B.

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
