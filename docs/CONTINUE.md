# Continuing on another computer

Where things stand and how to pick the work up anywhere. The plan, decisions and log are in
[PLAN.md](PLAN.md).

## Where we are (2026-11-18)

- **Repo:** this one (`INFORMATION89/Breezy-Expressive`): upstream Breezy Weather's files at `5ec3b9b`
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
- **Next:** F3 (Locations + floating Forecast/Locations toolbar). Also on the list (PLAN.md,
  "Planned next"): in-place block editing like Pixel Weather, Expressive icons, simpler settings,
  no block fade-ins, edge blur; translucent overlays need the Transit Go license check first.
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
   git clone https://github.com/INFORMATION89/Breezy-Expressive.git
   cd Breezy-Expressive
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
cd ../Breezy-Expressive
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
