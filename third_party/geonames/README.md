# GeoNames cities

`app/src/main/res/raw/solstice_cities.tsv` is derived from the GeoNames **cities15000** dump
(all cities with a population over 15 000, or capitals), downloaded 2026-11-18 from
<https://download.geonames.org/export/dump/>.

- Source: GeoNames, <https://www.geonames.org>
- License: Creative Commons Attribution 4.0 International (CC BY 4.0),
  <https://creativecommons.org/licenses/by/4.0/>
- Changes: trimmed to name, country code, latitude, longitude (rounded to 3 decimals), population
  and time zone; sorted by population.

Used offline by Solstice's search for "Cities nearby".
