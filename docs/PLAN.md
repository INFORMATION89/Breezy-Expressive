# Breezy Expressive: plan

**Breezy Expressive** is a fork of [Breezy Weather](https://github.com/breezy-weather/breezy-weather) redesigned in full
**Material You Expressive**: wallpaper (dynamic) colors, the M3 Expressive components, shapes and
motion. Weather data, sources, widgets and everything else stay Breezy Weather's.

Status: **F0 in progress** (snapshot, identity, CI done; icon and first run on the phone left). Upstream base: `5ec3b9b` (2026-11-17).

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
| Name | Breezy Expressive (unofficial fork; rename if upstream asks) |
| `applicationId` | `io.github.information89.breezyexpressive` (debug: `.debug` suffix, as upstream) |
| Flavor shipped | `basic` (all sources) |
| Code in this repo | Yes, as a snapshot of upstream **without its history** (29 MB; the full history is ~1 GB) |
| Weather-App-Experiment design (sky, glass, sun path) | Dropped: pure Material You Expressive |

## Design direction

- **Color:** dynamic (wallpaper) color by default, everywhere; a tuned seed palette when it's off or
  on Android < 12. No custom sky/glass look in this fork (the Weather-App-Experiment design is dropped).
- **Theme:** `MaterialExpressiveTheme` with `MotionScheme.expressive()` (spring motion), Expressive
  shapes and a type scale with emphasized styles for headlines.
- **Components:** `LargeFlexibleTopAppBar`, segmented list groups (rounded group, small inner
  corners, like Android 16 Settings), connected `ButtonGroup`s for small choices, `Switch` with
  icons, `LoadingIndicator`, Expressive `SearchBar`, `FloatingToolbar`/FAB menu for actions,
  shape morphing on press and selection.
- **Accessibility first:** font scale up to 200 %, TalkBack order and labels, 48 dp targets, RTL.

## Release 1 scope: Settings and Locations

Out of scope for release 1: main weather screen, widgets, notifications, live wallpaper.

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

### F2: Settings
- Restyle the shared building blocks first (`preference/composables/`): every settings screen
  changes at once with few edits upstream.
  - Sections → segmented list groups with a quiet section title.
  - Switch rows with icons; list/multi-list choices as Expressive dialogs or bottom sheets;
    small fixed choices (theme mode, units) as connected button groups.
- `AppBar.kt` → `LargeFlexibleTopAppBar` that collapses on scroll.
- Root settings: icon per category in a tonal shape, short summary line.
- Reorder screens (cards, daily/hourly trends): rewrite the 3 View activities in Compose with
  drag handles and haptics (LazyColumn + a small reorder helper), same stored data.

### F3: Locations
- Location list (`ManagementFragment`) → pure Compose `LazyColumn`: location cards with current
  conditions, drag to reorder, swipe to delete with undo, current-location card pinned on top.
  Removes the Compose/RecyclerView mix (upstream notes it causes scroll stutter).
- Add / search: Expressive `SearchBar` expanding to full screen, results as segmented list,
  `LoadingIndicator` while searching, clear empty and error states.
- Location settings screen (`LocationSettingsScreen.kt`) follows F2.

### F4: Polish and release
- Motion pass (shared transitions between list and search, press/selection morphs).
- Check: light/dark, dynamic color on/off, Android 12 and 17, font scale 200 %, RTL, TalkBack.
- Release build with our name/icon; changelog; screenshots.

## Risks
- **Upstream merges:** F3 rewrites `ManagementFragment`; conflicts there are likely. Mitigation:
  new file next to it, switch over in one small edit; merge upstream often.
- **Reordering in Compose:** needs careful drag/scroll/haptics and TalkBack actions
  (move up/down) as an alternative.
- **Expressive APIs are experimental:** pin `material3`, expect API changes on updates.

## Open questions
1. The launcher icon design.
2. Contribute generic fixes back upstream (e.g. the location list rewrite)?
3. Distribution: GitHub releases only, or also an F-Droid-style repo (e.g. Obtainium/IzzyOnDroid)?

## Upstream files touched
_(none yet)_

## Log
- **2026-11-17:** Studied upstream (`5ec3b9b`); wrote this draft. Name: Breezy Expressive.
- **2026-11-17:** Decided: code as a no-history snapshot in this repo, `basic` flavor,
  `applicationId` `io.github.information89.breezyexpressive`, old custom design dropped. F0 detailed.
- **2026-11-17:** F0 started: upstream snapshot at `5ec3b9b` without history (`.github/` and README
  excluded, see `UPSTREAM_BASE`); app name "Breezy Expressive" and `applicationId`
  `io.github.information89.breezyexpressive`; CI builds `basicDebug` + spotless (Java 21).
  `assembleBasicDebug` builds locally (JDK 21, ~20 min cold). Not yet run on the phone; launcher icon
  still upstream's generic fork icon.
