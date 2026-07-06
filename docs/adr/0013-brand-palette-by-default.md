# 0013 — Brand palette by default, wallpaper tint opt-in

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** 0009 (the dynamic-color default only)
- **Superseded by:** —

## Context

ADR 0009 shipped Material You dynamic color ON by default. In practice the
first-launch impression is then decided by whatever wallpaper the device
happens to have: on muted wallpapers the whole app renders in drab grays, the
brand disappears, and transit UI reads as unfinished. A transit app's identity
also leans on stable, saturated surfaces that its semantic line/status colors
sit well against. The designed tonal palette seeded from the brand color is
the look the product was reviewed and approved with.

## Decision

The brand-seeded tonal palette (seed `#888FFA`, TonalSpot) is the default on
all devices. Material You wallpaper tinting remains fully supported as the
same Settings toggle, now opt-in. Nothing else in ADR 0009 changes.

## Consequences

- Every install opens with the designed look; the brand survives first
  contact regardless of wallpaper.
- Users who want wallpaper harmony flip one switch and get real dynamic
  color.
- We give up "feels personalized out of the box" — a fair trade for a
  predictable first impression.

## Alternatives considered

- **Keep dynamic-ON default (ADR 0009)** — first impression hostage to the
  wallpaper; the observed result was gray-on-gray.
- **Dynamic only when the wallpaper palette is vibrant** — heuristic,
  surprising, and untestable across OEM palettes.
