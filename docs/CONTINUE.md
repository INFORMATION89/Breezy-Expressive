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
- **Motion rules:** [MOTION.md](MOTION.md) (agreed 2026-11-19); values in
  `ui/expressive/components/SolsticeMotion.kt`. Only the moon card uses them so far.
- **Open on the moon sheet:** a dark pill-sized frame at the start of opening, "· 71 %" drifting at
  the end of closing, a hitch after closing.

## Done since the last handoff (2026-11-19, without the phone)

Design tokens (`theme/Tokens.kt`, used in 12 files), the `:motion` module (M1 core + M2 morphs,
Apache-2.0, 14 tests), and the Haze glass prototype behind Settings › Debug. Not checked on the
phone yet. Details in the PLAN.md log. **First thing with the phone:** install a debug build, look at
every page for the ~40 spacings that snapped to the 4 dp grid, then try the Haze switch on a release
build and compare frame times. Then M3. Open decision: the Solstice `MotionScheme` mapping (PLAN.md,
"Motion framework").

## Pick up here (next session)

The current focus is **refining the UI to iOS-level polish with shared systems**, not new features.
Read these three, in order:

1. [UI_SYSTEMS.md](UI_SYSTEMS.md): audit of every UI system (counts of hand-typed spacing, corners,
   colours, springs, gestures) and the libraries checked (Haze, Reorderable, Navigation 3,
   `AnchoredDraggable`, Material 3 `MotionScheme`, graphics-shapes, Backdrop, Glasmio).
2. PLAN.md, **"Motion framework"**: one motion value per interaction, morphs drawn from anchors
   instead of a seeked `SharedTransitionLayout`, choreography windows, iOS-style springs (response +
   damping) and release rules, a standalone `:motion` module. Steps M0 to M5.
3. PLAN.md, **"Design system foundations"**: tokens first, then the Haze prototype, the component
   catalog and the library swaps.

M0 is decided (2026-11-19): only the five current springs; `:motion`, Apache-2.0.

Tokens (`98647ef`), `:motion` M1 (`ceae727`) and M2 (`e0310e9`) and the Haze prototype (`96b022b`)
are done; see "Done since the last handoff" above.

### Code review of those commits (2026-11-19, not yet fixed)

Unit tests run in CI (`:motion:test`), since the cloud sessions have no Android SDK.

**Fixed 2026-11-19 (user agreed), items 1–3 below:** `MotionPhysics.unresist` (+ `unRubberBand`),
used by `MotionValue.stop()`; `motionDrag` starts at once while moving; 7 `MotionValue` tests
(22 in the module; the overshoot test fails on the old code).

**Fix before M3 (bugs in `motion/…/MotionValue.kt`):**

1. **Catching an overshoot jumps back.** `stop()` sets `raw = animatable.value`, but past an end the
   value is already rubber-banded, so the next `drag()` resists it twice. On a 600 dp range: ~6 dp
   jump when caught 2 % past open, ~17 dp at 5 %, ~30 dp at 8 % (the container spring overshoots
   ~2 % even from rest). Fix: in `stop()`, set `raw` to the inverse of `MotionPhysics.resist` (add an
   `unresist(shown, min, max, overMin, overMax)` to `MotionPhysics`, unit-tested as the inverse).
2. **Can't catch it mid-flight right away.** `Modifier.motionDrag` uses `draggable` without
   `startDragImmediately`, so a moving surface keeps going under the finger until the touch slop,
   then jumps. Fix: `startDragImmediately = value.isRunning`.
3. Add unit tests for `MotionValue` (drag, stop mid-flight, release past an end, fast/slow flick);
   today only `MotionPhysics` and `MorphGeometry` are tested.

These are bug fixes, not new motion, but confirm with the user before pushing (motion rule).

**Smaller, when touching the code:**

- `Morph` content is laid out at the target size and placed at the moving surface's **top-left**, so
  a pill near the bottom reveals the sheet's top corner first. Add a content alignment (e.g. align to
  the source's edge), and decide it with the user for the moon card.
- `Morph` returns early (draws nothing) until both anchors are measured. Keep in mind for the moon
  card's "dark frame at the start of opening".
- `Morph` creates a `RoundedCornerShape` every frame in `graphicsLayer`; reuse one shape object.
- Haze glass reads `alpha` only when composed (already noted in the log).
- ~~Reduced motion is really a snap~~: PLAN.md and `Morph`'s KDoc now say so (2026-11-19).
- `Motion.control` and `Motion.settle` are the same spring (0.31 s / 1): merge or tell them apart.
- `rememberMotionValue` ignores later changes to `restPoints`/`overMin`/`overMax` (not keys);
  ~~`motionDrag` writes `slowFlick`/`fastFlick` during composition~~ (now in a `SideEffect`).
- Edge case: `unresist` caps at 99.9 % of the over-pull limit, so an overshoot past `overMax` (a very
  hard fling) still snaps back to just under the limit when caught. Raise `overMax` for that surface
  if it shows.
- `RoundedCornerShape` per frame in `Morph`: left as is (the radius changes every frame anyway, so
  the outline is rebuilt regardless).
- CI now runs `./gradlew :motion:test` on every push.

**Tokens pass:** it rounded off-grid values (6 → 4/8, 10 → 8/12, 14 → 12/16), so some spacing moved
by 2 dp. Check on the phone. Still left in `ui/expressive/`: 175 `.dp` literals (most common: 10 dp
×10, 6 dp ×6, 18 dp ×6, 7 dp ×4, 22 dp ×4), 13 `RoundedCornerShape(…)`, 33 `Color(0x…)`. The 28
`spring(…)` and 12 `tween(…)` move in M4.

### Next, in order

1. ~~Fix the two `MotionValue` bugs + tests~~ (done 2026-11-19).
2. ~~On the Pixel 7: token pass and Haze comparison~~ (done 2026-11-19: tokens fine; Haze ties on
   speed and looks the same, see the PLAN.md log). Smoothness checks: `./gradlew assembleBasicProfile`
   installs as `io.github.information89.solstice.profile`. Debug builds from different computers
   are signed with different keys: copy `~/.android/debug.keystore` between them to avoid
   reinstalling.
3. Decide the Solstice `MotionScheme` (PLAN.md, "Motion framework": switches get slower with only
   five tokens; accept or allow a `snappy` token).
4. M3: the moon card on `Morph`/`MotionValue`, fixing its three open issues.
5. Finish the tokens (remaining literals) alongside M4.

Rules that still apply: propose and agree any new or changed motion before building it; keep the
PLAN.md log and [ROADMAP.md](ROADMAP.md) up to date; new code in `ui/expressive/`.

Reference used for the iOS behaviour: `NEORUAA/Glasmio` (Apache-2.0), especially
`docs/ios-sheet-motion.md` and `docs/dropdown-motion.md`. Use it as research, not as a dependency
(it needs Android 12+ and bundles Apple artwork).

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
