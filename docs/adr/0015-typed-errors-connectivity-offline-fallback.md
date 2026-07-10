# 0015 — Typed error model, connectivity monitor, and offline-map fallback

- **Status:** Accepted
- **Date:** 2026-07-09
- **Supersedes:** —
- **Superseded by:** —

## Context

Error classification was deferred to each ViewModel. The same gateway failure
turned into different UX per platform and per screen, and iOS routinely lost
information — a network error surfaced as "no results" or a bare line of red
text. There was also no notion of whether the device was online, so a transport
failure could not be told apart from a real "server is down": both looked
identical, and offline zones (ADR 0007) that could still serve a map got the
same dead-end treatment as everything else. The client speaks to exactly one
gateway origin (ADR 0004), so when that origin is unreachable the whole app has
to degrade honestly, in one place.

## Decision

Classify failures **once**, in the shared core, at the `shared→shell` boundary:

- A `sealed interface AppFailure` (in `core/api`) is the single classification
  type both shells consume, computed by `Throwable.toAppFailure(isOnline)`.
- `IterTransportException` gains a `kind` (`TIMEOUT` vs `UNREACHABLE`), set at
  the gateway funnel by inspecting the Ktor exception chain — not re-derived
  downstream.
- A new `Connectivity` abstraction (interface in `commonMain`,
  `AndroidConnectivity` over `ConnectivityManager`, `IosConnectivity` over
  `NWPathMonitor` — GMS-free per ADR 0011) exposes `isOnline: StateFlow<Boolean>`.
  That flag splits a bare transport failure into `NoConnection` (offline zones
  still work) vs `ServerUnreachable`.

Both shells render **one inline state component per failure category** — no
system alerts. When offline and a downloaded `OfflineArea` covers the viewport,
the map falls back to the offline style (ADR 0007); live-only surfaces
(planning, boards, search) show a `NoConnection` state whose copy is honest
about what offline zones can and cannot do.

## Consequences

- One classification, one vocabulary; the same failure looks the same on both
  platforms and every screen, and iOS stops collapsing network errors into
  empty results.
- `AppFailure` is now a shared type both shells depend on, and connectivity is
  a per-platform capability that must stay GMS-free — two more points on the
  shared/platform boundary to keep in parity by hand.
- The app degrades in a defined way when the single gateway is unreachable:
  offline map where a zone covers the view, honest live-surface states
  elsewhere.
- ViewModels get simpler (they consume `AppFailure`, they don't infer it) but
  the gateway funnel now owns getting `kind` right from the Ktor chain.

## Alternatives considered

- **Keep per-ViewModel classification** — the status quo that produced
  divergent, information-losing UX; the reason for this ADR.
- **One generic "something went wrong" state** — cheap, but can't offer the
  offline-map fallback or connection-specific copy that the single-origin
  design needs.
- **Probe the gateway to decide online/offline** — a request to the thing that
  may be down can't tell you the radio is off; the platform reachability API
  answers the actual question, offline, for free.
