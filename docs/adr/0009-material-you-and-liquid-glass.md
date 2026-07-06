# 0009 — Material You on Android, Liquid Glass on iOS

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

ADR 0002 keeps the UI shells native; this record fixes what "native" means
concretely. Both platforms now ship strong, opinionated design systems:
Material 3 with dynamic color ("Material You") on Android, and the Liquid
Glass material language introduced with iOS 26 (`glassEffect`, glass button
styles, sheet materials). A transit app also carries a second, non-negotiable
color system: line identities (metro line colors, category colors) and
status/severity semantics that must never be re-tinted by a wallpaper-derived
palette.

## Decision

- **Android:** Material 3 throughout. Dynamic color is offered and ON by
  default on Android 12+, with a brand-seeded static tonal scheme as the
  fallback and an opt-out toggle. Transit line colors, status colors, and
  severity colors are semantic tokens resolved outside the Material color
  scheme so they are stable under any palette.
- **iOS:** SwiftUI with system materials; chrome (floating controls, search
  field, sheet) adopts Liquid Glass via the native APIs, falling back to
  regular materials on older OS versions. No custom re-implementation of the
  glass look — only the real thing or the system fallback.
- Both shells share the same semantic token *names* (documented in
  `docs/design/`) so the two implementations stay conceptually aligned.

## Consequences

- The app looks and behaves like a first-class citizen of each OS, including
  future OS-level evolutions of both design languages, at zero porting cost.
- Two theming implementations to maintain; parity is by convention (shared
  token vocabulary), not by compiler.
- Liquid Glass APIs require a current iOS SDK; the fallback path must be kept
  honestly usable, not an afterthought.

## Alternatives considered

- **One shared custom design system on both platforms** — brand-consistent but
  permanently foreign on both; the opposite of this product's premise.
- **Material on iOS (Compose Multiplatform)** — rejected with ADR 0002.
- **Dynamic color OFF by default** — protects line-color harmony, but throws
  away the single most distinctive Android personalization feature; solved
  instead by keeping transit colors semantic and palette-independent.
