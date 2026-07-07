# Changelog

Notable changes to `iter-maps/mobile`. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); pre-release, so everything
sits under Unreleased until the first tag.

## Unreleased

### Added

- Repository scaffold: Kotlin Multiplatform workspace, layered licensing
  (GPL-3.0-or-later code, CC-BY-4.0 docs, REUSE), ADR log with founding
  decisions 0001–0012, two-level roadmap.
- **Shared core** (`shared/`): wire types mirroring the gateway contract
  byte-for-byte; single-origin Ktor client covering geocoding, OTP GraphQL
  journey planning (with rerank profiles and historical delay prediction),
  places enrichment/collections/related, live train boards, reliability,
  health/manifest, streaming offline downloads, and the opt-in telemetry/sync
  surfaces; pure-Kotlin STORE-zip extractor with atomic offline area
  installs; Multiplatform-Settings preferences; Koin wiring; a Swift-facing
  `IterCore` facade; MockEngine test suite.
- **Android app** (`androidApp/`): Material 3 with dynamic color and a
  brand-seeded fallback; persistent MapLibre map (PMTiles styles straight
  from the gateway) under a custom multi-anchor bottom sheet hosting home,
  search, place details (with Wikimedia enrichment), journey planning with
  ranking profiles and delay hints, leg-timeline detail, live train boards,
  offline area management, settings, and a basemap picker; transit overlay
  layers; tap-to-identify; platform location without Play Services.
- **iOS app** (`iosApp/`): SwiftUI shell with Liquid Glass chrome (system
  material fallback pre-26), MapLibre iOS, native detent sheet mirroring the
  Android page set; XcodeGen project, shared framework via Gradle.
- CI: Android build/test/lint + shared iOS klib type-check on Linux, unsigned
  simulator build on macOS, REUSE lint.

### Changed

- Design restoration pass: real brand assets (wordmark, launcher icon), the
  brand tonal palette by default with Material You opt-in (ADR 0013), the
  home sheet rebuilt in the product's design language (search pill + avatar,
  quick chips, session recents, nearby stations), and a 44-finding design
  polish across the shell (measured peek, tonal map FABs, live compass,
  anchor memory, initial camera on the user's area), planning (duration-first
  cards, labeled segments, icon-led fields, richer leg timeline), search
  (category icons), boards (pill station picker, restructured rows) and
  place pages.

### Fixed

- Verification pass 1 (shared core vs the published contract): tolerate the
  gateway's synthetic string ids in geocoding results and hand station
  results straight to the boards surface; query-parameter encoding for
  Commons image names; true streaming for offline bundles with a realistic
  timeout; atomic offline installs that never destroy an existing area;
  ZIP end-of-directory validation; decimal OTP leg durations; haversine
  stability at antipodes.
- Verification pass 2 (both shells): `@Throws` on every Swift-facing suspend
  function so gateway errors bridge to catchable Swift errors instead of
  crashing; system-back now runs the same per-page teardown as the in-sheet
  arrows; offline downloads capture the visible map band and pre-flight the
  area cap; plan times use the router's timezone; place photos load with
  attribution on both platforms; iOS place enrichment, offline retry/installing
  states and error-code parity; corrected Gradle-wrapper licensing.
