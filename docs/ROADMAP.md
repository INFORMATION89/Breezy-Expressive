# Solstice roadmap

Everything still to do, sorted by area of the app and by type. `PLAN.md` keeps the decisions and the
dated log; this file is the to-do list. Keep it updated: tick items off (or delete them) when they
ship, and add new notes under the right area.

- **Functionality:** something new the app can do.
- **Fix:** something broken or wrong.
- **Refinement:** polish to something that already works.
- 🗓 = scheduled · ⭐ = high priority · 💬 = needs your OK (a design or motion proposal) before building.

---

## 🗓 Scheduled for Friday

- [ ] **Fix · Places:** dragging a card that's partly out of view breaks the drag.
- [ ] **Fix · Places:** dragging a card to the edge of the screen doesn't scroll the list.
- [ ] **Fix · Moon:** the moon card doesn't open when you're visiting a place.

## ⭐ Next session priorities

1. Rework what needs redoing and make everything consistent (spacing, materials, components).
2. The starred place folding into a compact version (💬 propose the animation).
3. The small fixes below, plus the realistic moon.

---

## Foundations: systems and consistency

### Functionality
- [ ] ⭐ **Unify systems and components:** one toolbar, page scaffold, card, message and sheet,
  reused everywhere. Use well-maintained open-source libraries where they make the app lighter and
  faster.
- [ ] **A reusable "move and transform into place" system** (morphs, for buttons too). Search for
  libraries first. First use: a visited place's loading state (see Postcard window).
- [ ] **Design frameworks:** spacing scale, type scale, motion tokens, component rules.
- [ ] **Icon style:** Google's new Material icons (Material Symbols). Find a library we can use
  properly.

### Refinement
- [ ] ⭐ **One blur system,** with the same materials everywhere. Blur behind all card backgrounds,
  including the moon button.
- [ ] Look at how Android 17 blurs (live, cheap) and move glass and edge blur onto it.
- [ ] Review the design against Apple's Human Interface Guidelines.
- [ ] **Never a plain fade-in:** things morph into other things. Applies to every new motion.

---

## Toolbar and navigation

### Fix
- [ ] **Tab tap speeds:** tapping the next tab still looks faster than the edge-to-edge jump. Use the
  average of the two speeds for both (icon taps only; swipes stay as they are).

### Refinement
- [ ] A little spring on the edge-to-edge jump.
- [ ] A slight gradient on toolbar buttons: lighter on top, darker at the bottom, derived from each
  button's own colour.
- [ ] Long pages: the toolbar label and the page heading never show at the same time (decide the
  hand-off rules).
- [ ] 💬 Refine the bud (button coming out of the toolbar).

---

## Forecast screen

### Functionality
- [ ] **Last updated time** for the weather data.
- [ ] **Refresh weather** when opening a place and when the app first opens.
- [ ] **Feels-like everywhere the data has it** (hourly, This week), not only the main temperature.
- [ ] Edit the Forecast blocks in place, like Pixel Weather.
- [ ] Weather effects (rain, thunder, snow) on the sky.
- [ ] Alerts as cards, and Live Updates.

### Fix
- [ ] **Text colours follow the sky:** in dark mode, opening a place where it's day, the "Visiting"
  text doesn't adapt. All Forecast text follows the sky; only This week adapts to any background.
- [ ] **Rendering past the edges while the page recedes** (opening search, floating cards): draw the
  background and edge blur a bit beyond the visible area.

### Refinement
- [ ] **A small icon before the time.**
- [ ] A unique grain per sky picture (no repeating condition pictures).
- [ ] Skies visibly animated (they barely seem to move), plus a sky motion setting.
- [ ] 💬 A transition when a new background loads (e.g. from a dark sky, with the sun or moon moving
  into place), and a nicer loading UI.

## Precipitation (Forecast)

### Fix
- [ ] Check the rain card with real rain (built, not yet seen).

### Functionality
- [ ] A precipitation element for This week.

## Moon

### Functionality
- [ ] ⭐ **Realistic moon** from NASA's CGI Moon Kit colour map (download approval pending),
  shader-lit by phase, with earthshine. One component for the button, the moon card and the sun
  path. Lunaria is a reference.
- [ ] 💬 **Opening morph:** the moon button grows into the card.
- [ ] Decide the moon card's size and position, or a full moon page instead (pros and cons).

### Refinement
- [ ] **Finer, lighter vibrations** on the dial's small steps (6-hour ticks), with the stronger
  tick per day.
- [ ] **Inertia on the dial** (a flick keeps it turning, then it settles).
- [ ] **An arrow towards today** when today is out of view on the dial.
- [ ] Make the moon card match our design (it looks straight out of iOS); follow This week's
  direction.
- [ ] Moon button: remove its small moon icon (redundant); use a proper moon-phase icon from the
  icon library on the Forecast page.
- [ ] Day labels on the dial (Tue · Today · Thu).

## This week

### Functionality
- [ ] Tapping a day (a pill, or a day named in the summary) opens that day's floating card.

### Refinement
- [ ] Shorter pills, with more space between the pills and the text above and below.
- [ ] Wording: "It's raining two days this week".
- [ ] A more visible card background.

---

## Places

### Functionality
- [ ] ⭐💬 **The starred place folds into a compact version.**
- [ ] **When another place is starred, the current location becomes a one-line card.**
- [ ] 💬 **Swapping main and pinned when you travel:** e.g. Mexico City is main and Tijuana pinned;
  move to Tijuana and they swap (Tijuana main, Mexico City pinned). Research how Apple Maps and
  Apple Weather handle it before building.

### Refinement
- [ ] Remove the grain paper effect for now; use the same background and edge fade as This week.
- [ ] Reset the scroll position when you leave Places.

## Postcard window (visiting a place)

### Functionality
- [ ] 💬 **Loading state:** start with "Visiting Tokyo · 2:33 PM, 14 hours ahead" under the title;
  when the data arrives, the buttons move and transform into place (uses the morph system).

### Refinement
- [ ] A small icon before the "Visiting" text.
- [ ] Pin icon: white outline; when pinned, filled (white in dark mode, black in light mode).

## Search pane

### Functionality
- [ ] **A button to clear recent searches.**

### Fix
- [ ] **Back closes the search card.**
- [ ] **The keyboard's dismiss button expands the search card to full height,** and the card is
  drawn behind the navigation bar.

### Refinement
- [ ] 💬 Opening search: the search icon should move towards the card as it opens.

---

## Data and sources

- [x] **AccuWeather disabled for now** (2026-11-19). It's no longer a selectable or default source
  (alerts fall back to WMO, pollen to Open-Meteo, no normals source), and places that still name it
  skip it quietly.
- [ ] Re-test that a searched city keeps its name (e.g. "Puebla").

## Settings

- [ ] Simplify settings (fewer trend options).
- [ ] App drawer name: choose "Solstice" or "Weather".

## Release

- [ ] Rewrite `PRIVACY.md` and `INSTALL.md`.
- [ ] Re-check a release build end to end.
- [ ] Tablets: rotation and layout.
- [ ] App icon: the softer, rounder icon search.
