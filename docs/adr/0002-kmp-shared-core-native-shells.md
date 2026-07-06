# 0002 — Kotlin Multiplatform shared core, fully native UI shells

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

One app, two platforms. The gateway wire contract, the domain models, the
journey-plan mapping, and the geometry math are identical on both; the parts
users actually touch — navigation feel, sheet physics, typography, system
integration — are exactly the parts where Android and iOS have diverged the
most (Material You dynamic color on one side, Liquid Glass on the other).
A cross-platform UI toolkit would average the two into something that feels
native on neither, and both platform design systems are moving targets that
first-party frameworks track for free.

## Decision

We will build a Kotlin Multiplatform `shared` module containing everything
below the presentation layer — wire types, the gateway client, repositories,
settings, and pure domain logic — compiled to an Android library and an iOS
framework. Each platform keeps a fully native shell: Jetpack Compose with
Material 3 on Android, SwiftUI on iOS. ViewModels/observable state holders stay
per-platform; they are thin and their duplication cost is far below the cost of
bridging reactive state across the Kotlin/Swift boundary.

## Consequences

- The wire contract and domain logic are written and tested once; a contract
  change is one edit in `shared`.
- Each platform's UI can adopt its design system idiomatically and immediately
  — no waiting on a cross-platform toolkit to wrap new system APIs.
- Presentation state logic is written twice, and feature parity between the two
  shells must be maintained by discipline (tracked in `docs/roadmap/`).
- iOS consumes the shared module as an Objective-C-interop framework, so the
  shared API surface must stay interop-friendly (no exposed generics-heavy
  Kotlin idioms on the boundary).

## Alternatives considered

- **Compose Multiplatform for both UIs** — single UI codebase, but iOS output
  is Canvas-rendered Material; irreconcilable with a native Liquid Glass look.
- **Flutter / React Native** — same native-feel objection, plus a second
  language/runtime and no reuse of the Kotlin ecosystem.
- **Two fully separate apps** — maximum nativeness, but the entire data and
  domain layer written and maintained twice against a moving server contract.
