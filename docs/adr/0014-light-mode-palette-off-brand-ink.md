# 0014 — Light-mode palette re-anchored off brand ink

- **Status:** Accepted
- **Date:** 2026-07-09
- **Supersedes:** 0013 (the light-mode seed only)
- **Superseded by:** —

## Context

ADR 0013 seeds the whole Material 3 scheme from a single brand color
(`#888FFA`, `PaletteStyle.TonalSpot`, `contrastLevel=0.0`) via MaterialKolor,
for both light and dark. Dark reads well — dark surfaces pull a high tone of
the seed, so the primary is vivid and the container ladder separates. Light
does not: `#888FFA` is a very light seed (L≈76%), and on near-white surfaces at
`contrastLevel=0` it maps to a low tone, so the primary comes out pale and
muddy and the surface containers bunch up near white with no visible elevation
step. The seed that flatters dark actively works against light.

## Decision

Keep **dark exactly as reviewed**: seed `#888FFA` (`brand.seed`),
`PaletteStyle.TonalSpot`, `contrastLevel=0.0`.

Re-anchor **light only**:

- seed `#4248C9` (`brand.ink`, a darker brand tone that already exists),
- `PaletteStyle.Vibrant` — retains chroma at the low tones light mode uses, so
  the primary reads branded rather than washed out,
- `contrastLevel=0.3` — spreads the surface-container ladder so elevation is
  visible and darkens the primary.

`#888FFA` stays the documented brand identity color; only the light *scheme
seed* changes. iOS mirrors the intent — `.tint` is `#4248C9` in light and
`#888FFA` in dark.

## Consequences

- Light mode reads as a branded, legible transit surface with real elevation
  separation instead of gray-on-near-white.
- The two modes are now seeded from different tones and styles, so a palette
  tweak has to be checked in both — they are no longer one knob.
- `brand.seed` and `brand.ink` are both load-bearing for theming; neither can
  be retired without revisiting this.

## Alternatives considered

- **Keep the single `#888FFA` + TonalSpot seed (ADR 0013)** — the observed
  result was a pale, muddy light scheme with no elevation contrast.
- **Push `contrastLevel` up on the same seed** — darkens and separates, but a
  light seed at high contrast still yields a low-chroma primary; Vibrant off a
  darker tone is what makes it read branded.
- **Hand-authored light scheme, bypassing MaterialKolor** — loses the
  single-seed derivation that keeps every role coherent; more to maintain.
