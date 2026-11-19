# Material Symbols

All UI icons come from Google's **Material Symbols**, style **Rounded**, weight 400, grade 0, fill 0
(fill 1 only for on/selected states), downloaded 2026-11-19 from
<https://github.com/google/material-design-icons> (`symbols/web/<name>/materialsymbolsrounded/`).

- License: **Apache License 2.0** (<https://www.apache.org/licenses/LICENSE-2.0>), © Google.
- In Compose code: `app/src/main/kotlin/org/breezyweather/ui/expressive/components/SolsticeIcons.kt`,
  each icon at optical size 24 and 20 (`SolsticeIcons.Small`), paths copied from the `_24px.svg` and
  `_20px.svg` files.
- In XML: `app/src/main/res/drawable/ic_*.xml` (optical size 24). File names are upstream Breezy
  Weather's; the artwork was replaced, so each file names its symbol in a comment.

Not Material Symbols (artwork, unchanged): the weather condition packs (`weather_*`), `shortcuts_*`,
`widget_*`, the moon phases, `ic_sunshine_duration`, and the launcher icon.

Used by Solstice instead of the old Material Icons (`material-icons-extended`, frozen since 2022).
