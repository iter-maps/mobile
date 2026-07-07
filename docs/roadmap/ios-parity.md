# iOS shell parity (in progress)

The iOS app tracks the Android shell feature-for-feature on the shared core,
rendering it in SwiftUI with Liquid Glass materials (ADR 0009). This file is
the parity ledger; Android is the reference implementation of the UX, not of
the look.

- **Built:** XcodeGen project (ADR 0010), shared-framework integration,
  MapLibre iOS via SPM, the map + sheet shell, and the full page set — search,
  place detail with Wikimedia enrichment, planning with profile chips and
  delay hints, boards, offline area management, settings.
- **In progress:** visual pass on Liquid Glass chrome (glass button styles,
  sheet materials, morphing transitions) with the pre-26 material fallback.
- **Planned:** onboarding, Live Activities for trips
  ([`turn-by-turn-navigation.md`](turn-by-turn-navigation.md)); porting the
  Android design pass 2 UX (depart-at/arrive-by control, my-location endpoint
  pick, resilient board polling with stale caption and manual refresh,
  live-delay tinting on the leg timeline, transfer arrival times, skeleton
  loading, labeled/actionable place facets, nearby-state model, delete
  confirmation) — Android remains the UX reference.
- **Note:** built on non-Mac hosts the Swift sources are review-verified only;
  CI's macOS lane is the compile gate ([`release-hardening.md`](release-hardening.md)).

Decision: ADR 0002, 0009, 0010.
