# Breezy Expressive: plan

**Breezy Expressive** is a fork of [Breezy Weather](https://github.com/breezy-weather/breezy-weather) redesigned in full
**Material You Expressive**: wallpaper (dynamic) colors, the M3 Expressive components, shapes and
motion. Weather data, sources, widgets and everything else stay Breezy Weather's.

Status: **draft, not started**. Upstream base: `5ec3b9b` (2026-11-17).

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

## Design direction

- **Color:** dynamic (wallpaper) color by default, everywhere; a tuned seed palette when it's off or
  on Android < 12. No custom sky/glass look in this fork.
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
- Fork on GitHub under INFORMATION89; `upstream` remote = breezy-weather/breezy-weather.
- Follow upstream's fork rules: new app name and icons in `res_fork`, new `applicationId`, never
  build releases with the `breezy` flag.
- Keep upstream syncable: Expressive code in its own package (`ui/expressive/`) where possible,
  small edits in upstream files, merge upstream regularly (weekly or per upstream release).
- CI: build `basic` and `freenet` debug + lint on every push.

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
1. Icon and `applicationId` for the fork. Name decided: **Breezy Expressive** (2026-11-17).
   Upstream grants no trademark rights and asks forks to avoid confusion with Breezy Weather;
   the README must say it is an unofficial fork, and we rename if upstream asks.
2. Which flavor do we ship: `basic`, `freenet`, or both?
3. Is Weather-App-Experiment's design (sky, sun path, glass) kept for a later main-screen phase,
   or dropped in favor of pure Material You?
4. Contribute generic fixes back upstream (e.g. the location list rewrite)?

## Log
- **2026-11-17:** Studied upstream (`5ec3b9b`); wrote this draft. Name: Breezy Expressive.
