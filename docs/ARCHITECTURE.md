# Architecture

The current-state design of `iter-maps/mobile`. The history of *why* lives in
the [ADR log](adr/README.md); this document describes *what is*.

## Shape

One repository, three modules:

```
shared/      Kotlin Multiplatform — the app's brain
androidApp/  Jetpack Compose shell (Material You)
iosApp/      SwiftUI shell (Liquid Glass), XcodeGen-generated project
```

The `shared` module compiles to an Android library and an iOS framework. It
contains everything that is true on both platforms; the shells contain
everything the user touches. ViewModels/state holders are deliberately
per-platform (ADR 0002) — thin adapters between shared repositories and each
platform's reactive UI idiom.

## The shared core

Package root `it.iterapp.core`, organized by responsibility:

- **`wire`** — `@Serializable` DTOs mirroring the gateway contract
  byte-for-byte (ADR 0005): exact field names, exact absent-vs-null
  semantics, gateway-additive routing fields modeled as optional extras.
- **`api`** — `IterGatewayClient`, a Ktor client bound to **one base URL**
  (ADR 0004) with per-surface functions: geocoding, routing (hand-rolled OTP
  GraphQL — ADR 0006), places, live trains, reliability, offline downloads,
  health/manifest, and the opt-in telemetry/sync surfaces. Errors decode into
  a typed `IterApiException` carrying the envelope's stable `code`; non-JSON
  failures degrade to status-only. Fail-soft surfaces (empty-200s, bare 404s
  for disabled features) are honored as the contract defines them.
- **`model`** — domain types the shells render (places, itineraries, legs,
  boards…), mapped explicitly from wire types.
- **`repo`** — repositories composing api + mapping + caching policy.
- **`geo`** — pure math: polyline decoding, distance/projection, bbox helpers.
- **`settings`** — Multiplatform-Settings-backed client preferences (gateway
  origin, theme, map style).

Dependency wiring is Koin; the graph is assembled once per app process
(`initIterCore(...)`).

## The shells

Both implement the same interaction model (ADR 0008): a persistent MapLibre
map (ADR 0007) under a universal bottom sheet whose internal back-stack hosts
every page — home, search, place detail, planning, boards, layers, settings.
The map never unloads; camera moves target the band the sheet leaves visible.

- **Android** — Compose + Material 3. Dynamic color on by default (Android
  12+) with a brand-seeded static fallback; transit line/status colors are
  semantic tokens outside the Material scheme (ADR 0009). The sheet is a
  custom `AnchoredDraggableState` scaffold with peek/half/full anchors and a
  nested-scroll bridge. Location via `LocationManagerCompat` — no Play
  Services anywhere (ADR 0011).
- **iOS** — SwiftUI. System sheet with custom detents; chrome adopts Liquid
  Glass with a regular-material fallback pre-26. MapLibre iOS via SPM;
  project generated from `project.yml` (ADR 0010).

## Data flow

```
UI event → ViewModel/StateObject → repository (shared) → IterGatewayClient
                                                          → gateway origin
UI state ← StateFlow / @Published  ← mapped domain model ←
```

All I/O is suspend/Flow-based on the Kotlin side; the iOS shell consumes the
shared core through its Objective-C framework surface with thin async
wrappers.

## Configuration

Exactly one runtime setting matters: the **gateway origin**. Debug builds
default to a local/dev origin; the setting is user-visible because
self-hosting is a first-class deployment (ADR 0004). Everything else the map
needs (tiles, glyphs, sprite, overlays) resolves through the style document's
server-rewritten URLs — the client never assembles asset URLs by hand.

## Invariants honored

- **No accounts, no tracking.** Telemetry and E2EE sync are opt-in,
  default-OFF client features against opt-in, default-OFF server surfaces.
- **One origin.** No engine is addressed directly; server topology stays
  server-side.
- **Contract fidelity.** Wire types match the gateway's published contract;
  additive server fields must never break the app.
- **GPL-clean dependency graph.** No proprietary SDKs (ADR 0003, 0011).
- **Native or nothing.** Each platform's design language is adopted through
  its real APIs, not imitated (ADR 0009).

## Roadmap

What's deliberately not here yet — trip guidance, saved places, alerts, sync —
is tracked in [`roadmap/`](roadmap/README.md), each item pointed at its
decision record.
