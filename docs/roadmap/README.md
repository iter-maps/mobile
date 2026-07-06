# Roadmap

An honest map of everything **not yet built**, each item pointed at its
decision record so there are no silent gaps. Three groups: the core app
experience, the platform shells, and forward-looking features.

The architecture this plugs into is in [`../ARCHITECTURE.md`](../ARCHITECTURE.md):
a Kotlin Multiplatform **shared core** (wire contract + repositories) under two
fully native shells — **Compose/Material You** on Android, **SwiftUI/Liquid
Glass** on iOS. "Decision" pointers below name the relevant ADRs by number
(e.g. "ADR 0007").

## 1. Core experience

- **Map & styles** ✅ done (ADR 0007) — MapLibre Native pointed at the gateway
  styles with PMTiles, light/dark and transit variants, overlay layers drawn
  client-side. **Remaining:** a dedicated transit-overlay and stop-marker
  redesign — the first-pass rendering is illegible
  ([`transit-overlay-redesign.md`](transit-overlay-redesign.md)); initial
  camera on the user's region; style-preview thumbnails in the layer picker;
  scoped overlay refresh.
- **Search & places** ✅ done (ADR 0004, 0005) — Photon typeahead with location
  bias, reverse geocoding, place details with Wikimedia enrichment.
  **Remaining:** civic-number two-query fallback; editorial collections
  surfaces ([`place-discovery.md`](place-discovery.md)).
- **Journey planning** ✅ done (ADR 0006) — OTP GraphQL plan queries with
  rerank profiles and historical delay prediction, itinerary cards and leg
  detail. **Remaining:** filters UI (modes, transfers, wheelchair), arrive-by,
  per-factor "why ranked here" explanations
  ([`reliability-ui.md`](reliability-ui.md)).
- **Live train boards** ✅ done — station autocomplete + departures/arrivals
  polling at the contract's cadence. **Remaining:** favorite stations; board →
  plan hand-off ([`live-boards-alerts.md`](live-boards-alerts.md)).
- **Offline maps** ✅ done — bundle download, atomic unpack, and area
  management UI (download the viewport, list, delete) ship on both shells.
  **Remaining:** automatic offline style fallback when the gateway is
  unreachable; freshness re-download via `/manifest` etags
  ([`offline-maps.md`](offline-maps.md)).
- **Turn-by-turn guidance** — not started
  ([`turn-by-turn-navigation.md`](turn-by-turn-navigation.md)).
- **Saved places & history** — not started
  ([`saved-places-history.md`](saved-places-history.md)).

## 2. Platform shells

- **Android (Compose, Material You)** ✅ core done (ADR 0008, 0009) — universal
  bottom sheet over a persistent map, dynamic color with semantic transit
  tokens. **Remaining:** onboarding flow; tablet/large-screen layouts;
  accessibility pass.
- **iOS (SwiftUI, Liquid Glass)** ✅ core done (ADR 0002, 0009, 0010) —
  project generation, shared-framework wiring, and the full page set (home,
  search, place detail with enrichment, planning, boards, offline, settings)
  are built; Swift compiles only on the CI macOS lane.
  **Remaining:** onboarding, Live Activities, visual polish
  ([`ios-parity.md`](ios-parity.md)).
- **Localization** — Italian and English strings only; framework for more
  pending ([`localization.md`](localization.md)).
- **Release hardening** — signing, store packaging, migration policy, CI
  release lanes ([`release-hardening.md`](release-hardening.md)).

## 3. Forward-looking features

Documented designs, largely unbuilt. One short file each; where a decision is
already committed the file names its ADR — otherwise the ADR lands with the
first implementation change.

| Feature | Plugs into | File |
|---|---|---|
| Place discovery (collections, related places) | shared places repo | [`place-discovery.md`](place-discovery.md) |
| Reliability & rerank explanation UI | planning results | [`reliability-ui.md`](reliability-ui.md) |
| Service alerts & monitored lines | boards + notifications | [`live-boards-alerts.md`](live-boards-alerts.md) |
| Private sync (E2EE) | settings + saved places | [`sync-e2ee.md`](sync-e2ee.md) |
| Opt-in crowd telemetry | trip guidance | [`telemetry-optin.md`](telemetry-optin.md) |

Privacy invariants hold throughout: no accounts, no tracking, telemetry and
sync are **opt-in and default OFF** exactly like their server counterparts, and
everything works against a self-hosted gateway (ADR 0004).
