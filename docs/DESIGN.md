# Design principles

Rules for what Solstice shows and how it's organised, as [MOTION.md](MOTION.md) is for how it
moves and [WRITING.md](WRITING.md) for how it speaks. Added 2026-11-21 from two talks the user chose
(Kole Jain: "Everything you need to know about Mobile App UIs in 8 minutes" and the one on why apps
feel "vibe coded"), checked against what Solstice already does.

**Use it as a gate:** before proposing any new screen, card, button, text or piece of information,
answer the checks below in the proposal to the user (CLAUDE.md: consult the user first).

## What Solstice is for (its opinion)

Generated software tends to be the average of everything it has seen; a product needs an opinion on
why it exists and who it's for, and every choice follows from it. The user's own description:

> A weather app with unique UI and UX conventions, fast, light, nice to use with smooth animations
> and good design.

To write with the user (open): who it's for, the few things it must answer at a glance, and what it
deliberately leaves out. Until then, a feature that doesn't serve "fast, light, nice to use" doesn't
go in.

## The checks

1. **One screen, one job.** Name the job of the screen or card. If it does two, split it, or move
   the second into a sheet or a deeper view. Competing actions on one screen make people stall.
2. **Someone needs it.** Say who needs it and what they do with it. Nothing goes in because "weather
   apps usually have it"; the average app is not the target.
3. **Complexity is ours, not the user's** (Tesler's law: it can be moved, not removed). The app does
   the work: refreshes on its own, picks sensible defaults, offers a picker instead of typing, works
   out the place. A setting only exists for a real choice people disagree on.
4. **Show, don't tell.** No explainer pop-ups or onboarding slides. A first use, an empty state or a
   new feature shows the thing working, with at most one short line.
5. **Every affordance matches what it does.** A chevron points the way the thing opens; a menu or
   sheet grows from what was touched (the morph rule); an icon means one thing everywhere; back goes
   where you came from. A detail copied from another app without its reason is the "vibe coded" tell.
6. **Connected, not siloed.** Information links to related information where it is (a day's
   temperature in the This week summary opens that day; a place opens its postcard), with a preview
   before a full jump where it helps. No page you can only reach or leave by backing out through
   levels.
7. **Essentials first, details on demand.** A card shows its title and the one value that matters;
   the rest is a tap away. Group related data in cards and lists that scan in a second.
8. **Sized for a hand.** Touch targets of 48 dp (PLAN.md "Icons"), comfortable edge padding, and a
   clear hierarchy from the type scale only: screen title, section, body, secondary detail.
9. **Navigation people always understand.** 3 to 5 top destinations in the toolbar; a contained task
   in a sheet or window that keeps your place underneath; every pushed screen has a clear way back
   (back gesture with predictive back).
10. **Gestures instead of clutter, never gesture-only.** Secondary actions can live in swipes and
    long presses instead of extra buttons, but each one also has a findable path and an accessibility
    action (as reorder has "move up / move down").
11. **Motion explains depth.** How a layer moves says where it came from and that you can go back
    (a sheet over the page, a window out of a postcard). The how is in [MOTION.md](MOTION.md).
12. **Every state is designed.** For each screen and card: loading, empty, error and offline, not only
    the full-data case. An empty state says what will appear there and offers the one action that gets
    there (add a place, try another search).
13. **Android first, iOS-quality feel.** Material structure (system back, system bars, Material
    controls, Material Symbols) with motion at iOS's level. iOS-only visuals (a long press that blurs
    the whole screen, the page zooming out behind a sheet) only when agreed, and adapted to Android.
14. **Edge to edge.** Content runs behind the status and navigation bars; nothing tappable or
    important sits under system UI or the display cut-out.
15. **Any size and orientation.** Portrait phone is the main case, but landscape, foldables and
    tablets must not break (today `ui/expressive/` has no size-class handling: to audit).

## Audits these rules call for (ROADMAP.md)

- Empty, loading, error and offline states on every screen and card (check 12).
- Affordances: chevrons, arrows and icons against what they actually do (check 5).
- Dead ends and silos: which information should link to which (check 6).
- Landscape, foldable and tablet layouts (check 15).
- Settings that exist only because a default was hard to choose (check 3).
