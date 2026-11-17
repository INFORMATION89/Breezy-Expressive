# Background redesign: notes and plan

Working notes for the sky background rework. Status: **planned, not started** (no code yet).
Update the log at the bottom whenever something is decided or done.

## Problems with the current background

Current code: `ui/material/WeatherBackground.kt` (AGSL shader on Android 13+, gradient + noise tile
on 12), `ui/material/Glass.kt` (Haze blur), `ui/material/WeatherMood.kt` (palettes),
wired up in `ui/navigation/MainPager.kt`.

- **Performance:** every animation frame forces every glass panel (cards + toolbar) to re-blur
  through Haze. Workarounds in place: ~12 fps drift, pause while scrolling, stop 20 s after the
  last interaction.
- The orb ends up hidden behind cards.
- Harsh black vignette edges at night.
- The background is decoration, not information.

## What the references have in common

Four reference images (a "Seoul" weather screen, a "New York" weather screen, a "Sunset" screen and a
grainy purple blob texture):

- Soft color fields with **heavy film grain**, not a sharp orb.
- Seoul / Sunset: vertical sky gradient, warm glow band at the horizon.
- New York: one large soft saturated glow (dot-matrix numbers and dotted arc are UI, not sky).
- Purple: a few blurry blobs fading into a deep base, coarse grain.
- **Day colors are pastel and light** (lavender, peach, cream), unlike today's deep blues.

So: the halftone sun idea (old option 3) is **dropped**. Grain, not halftone.

## Rendering approach (performance)

1. **Sky drawn once, tiny:** render the color field into a small bitmap (~1/8 of the screen, about
   135×300 px). Redraw only when something changes (weather, sun position, an animation frame if
   drift is on). Cheap enough to do on the CPU off the main thread.
2. **Grain = pre-made tile:** noise texture generated once, drawn as a repeated `ImageShader`.
   Full-resolution work is just one filtered upscale + one tile. Grain also hides gradient banding.
3. **"Shader glass" instead of blur:** each card / the toolbar draws the same small sky bitmap at
   its own window position, lifted toward white, + white tint + grain + hairline (see Decisions).
   Looks like frosted glass over a smooth sky. **Remove Haze entirely.**
4. **One code path for Android 12+:** no AGSL needed for the small bitmap.

Tricky part: cards move during scroll; their window position must update their *drawing* only
(no recomposition/relayout), e.g. a `Modifier.Node` that reads coordinates in `onPlaced` and
calls `invalidateDraw()`. Main thing to get right and measure.

## Sky driven by weather data

- **Sun elevation → glow position:** low and orange at sunrise/sunset, high and pale at noon,
  updated every minute (from `sunrise`/`sunset` + time).
- **Cloud cover → saturation/diffusion:** clear = saturated, overcast = muted, fog = almost washed out.
- **Feels-like temperature → warmer/cooler hue.**
- **Wind → blob drift speed** (still in calm air).
- **Rain → slight vertical streaking in the grain.**
- **Night → deep blue-violet base** (like the purple reference), soft falloff instead of black corners.

## Motion

Since drawing becomes cheap, the 20 s stop and the scroll pause can go. **Slow drift runs all the
time** (speed from wind, near-still in calm air), with the meaningful moments on top:
- Open the app: the glow rises into place.
- Pull to refresh: a pulse.
- Conditions change: slow color shift.
- Swipe between pages: slight parallax shift.

Auto-static in battery saver, with system animations off, and on slow phones.

## Decisions

1. **Text color:** dark text by day (pastel skies), white at night. Text color follows sky
   brightness.
2. **Motion:** slow wind-driven drift always on, plus the meaningful moments above. Still
   auto-static in battery saver / animations off / slow phones.
3. **Glass:** no real blur, Haze removed. For now cards and toolbar are **white tinted glass like
   the Seoul reference**: sky bitmap at the card's position lifted toward white, white tint, grain
   and a hairline edge.

## Order of work

1. **Measure first:** add a scroll + page-swipe macrobenchmark (FrameTimingMetric) to the existing
   `baselineprofile` module to get baseline numbers.
2. **New renderer, same look:** small sky bitmap + grain tile + shader glass, remove Haze. Measure.
3. **New look:** pastel palettes from the references, horizon glow + blobs, sky driven by data,
   white tinted glass, dark day / white night text. Check every condition with the debug sky
   preview (`SkyPreview.kt`).
4. **Motion:** always-on drift + meaningful moments, then automatic static mode.

## Log

- **2026-11-17:** Reviewed current code and the four references; wrote this plan. No code yet.
  Waiting for answers to the open questions.
- **2026-11-17:** Open questions answered (see Decisions): dark day / white night text, slow drift
  always, white tinted glass like Seoul with Haze removed. Ready to start step 1.
- **2026-11-17:** Step 1 done. Added `ScrollBenchmark` (FrameTimingMetric, 10 iterations,
  `CompilationMode.Partial`) to `baselineprofile`; run with
  `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`. Needed androidx.benchmark
  `1.6.0-alpha01`: 1.5.0 fails on Android 17 (`no such table: expected_frame_timeline_slice`).
  Baseline on Pixel 7 (Android 17), current Haze background:

  | Test           | CPU P50 | P90     | P95     | P99     | Overrun P50 | P90    | P95    | P99    |
  |----------------|---------|---------|---------|---------|-------------|--------|--------|--------|
  | forecastScroll | 5.7 ms  | 11.8 ms | 13.0 ms | 16.1 ms | -4.1 ms     | 0.8 ms | 2.2 ms | 8.4 ms |
  | pageSwipe      | 8.0 ms  | 12.0 ms | 13.1 ms | 16.3 ms | -2.9 ms     | 1.1 ms | 2.6 ms | 8.0 ms |

  Positive overrun = a missed frame: roughly the slowest 10 % of frames miss on both tests.
  Also: toolbar's selected pill now uses `CircleShape` to match the toolbar.
- **2026-11-17:** Step 2 done: new renderer, same look. `ui/material/Sky.kt`: `SkyRenderer` draws
  the old shader's math on the CPU into a 1/8-size bitmap (3 rotating buffers, off the main
  thread, only when inputs change); `Grain` is one repeated white/black noise tile.
  `WeatherBackground` scales the bitmap up + grain (AGSL removed, one path for Android 12+).
  `Glass.kt`: a `Modifier.Node` draws the same bitmap at the panel's window position + tint +
  grain + hairline, invalidating only its draw when the position changes. Haze removed
  (dependency, README credit). Baseline profile regenerated. Dropped: the 4x4 Bayer dithering
  and the slightly stronger grain on the orb (not visible in side-by-side screenshots).
  Drift, scroll pause and 20 s stop unchanged (step 4).

  | Test           | CPU P50 | P90     | P95     | P99     | Overrun P50 | P90     | P95    | P99    |
  |----------------|---------|---------|---------|---------|-------------|---------|--------|--------|
  | forecastScroll | 5.9 ms  | 11.9 ms | 12.9 ms | 17.2 ms | -5.5 ms     | 0.3 ms  | 1.4 ms | 8.0 ms |
  | pageSwipe      | 6.2 ms  | 10.7 ms | 12.3 ms | 16.3 ms | -6.4 ms     | -1.1 ms | 0.6 ms | 6.0 ms |

  Page swipe clearly better (median CPU 8.0 -> 6.2 ms, missed frames ~10 % -> ~5 %). Scrolling
  only slightly better: its slow frames come mostly from elsewhere (card content/charts), worth
  a trace before step 3. Open: the Blur setting now only changes tint opacity.
- **2026-11-17:** Design changes from review, ahead of step 3:
  - Toolbar pills follow the pager's live position; Settings is a 4th swipeable page.
  - Day: white glass (60–68 % white) with a scoped light theme (`GlassTheme`): dark text,
    accents and pale icons deepened to read on white. Night glass unchanged. Text on the sky
    stays white until pastel day skies (step 3).
  - Orb back in the upper middle (y 0.36, r ≈ 0.27), larger and brighter, on the new renderer.
  - Forecast fits on one screen above the toolbar; no overscroll when nothing scrolls.
  - Settings › Debug (debug builds): preview any sky and Day/Night independently.
  - Still open: the Blur slider only changes tint opacity now.
