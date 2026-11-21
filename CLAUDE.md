# Solstice

Unofficial fork of Breezy Weather, redesigned in Material You Expressive.

- Start here: `docs/CONTINUE.md` (current state, setup on a new computer, upstream updates).
- Plan, scope, decisions and a dated log: `docs/PLAN.md`. Keep its log up to date as work happens.
- Keep upstream syncable: put new code in `ui/expressive/` where possible; keep edits to upstream files small.
- Never push upstream's git history to this repo; never build with `-Pbreezy`.
- **Design rules: `docs/DESIGN.md`** (checks every new screen, card, button or text must pass), motion rules:
  `docs/MOTION.md`, writing rules: `docs/WRITING.md`, UI systems and libraries to use: `docs/UI_SYSTEMS.md`.
- Animations: always propose options to the user and agree on them before building any new or changed motion.
- To-do list: `docs/ROADMAP.md` (by app area and Functionality / Fix / Refinement). Keep it updated as items ship or come in.
- **Consult the user before adding anything they'll see** (added by the user 2026-11-21): pages,
  screens, interfaces, buttons, text, information. Suggest it, say exactly what would be added, and ask
  whether the user needs it before building it.
- **Plans are made in the chat environment** (the user's Claude project "Solstice App"), and land in
  `docs/CONTINUE.md`, `PLAN.md` and `ROADMAP.md`. Before coding, pull and read them; build only what's
  planned and agreed there. Coding sessions are mostly for carrying out approved work and fixes; anything
  new goes back to the user first.
- **Use what exists before writing anything by hand** (crucial): check the guidelines (`MOTION.md`,
  `UI_SYSTEMS.md`, the rules in `PLAN.md`) and the systems already in place (`:motion` and its tokens,
  `theme/Tokens.kt`, `SolsticeIcons`, the component catalog: `SolsticeCard`, `PageScaffold`, the
  toolbar, sheets, messages) or an existing library. If a system is missing a piece, extend the system
  (e.g. add it to `:motion`) rather than building a one-off, which would have to be redone later.
