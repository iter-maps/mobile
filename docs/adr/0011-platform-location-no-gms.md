# 0011 — Platform location APIs only, no Google Play Services

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

The app needs the user's position for search bias, nearby results, and
on-route progress. On Android the path of least resistance is
`FusedLocationProviderClient` from Google Play Services: better fixes with
lower battery use, but a proprietary runtime dependency that (a) is absent on
de-Googled devices, (b) is awkward next to GPL-3.0 code (ADR 0003), and
(c) blocks F-Droid inclusion. On iOS there is no such fork in the road —
`CoreLocation` is the platform API.

## Decision

We will use only platform location APIs: `LocationManager` (via the androidx
`LocationManagerCompat`/`LocationCompat` helpers) on Android — preferring the
system `fused` provider where the OS offers one — and `CoreLocation` on iOS.
No `play-services-location`, no GMS artifacts anywhere in the dependency
graph. Location plumbing stays per-platform behind a small shared-facing
interface; the shared core treats position as data, not as a capability it
owns.

## Consequences

- The APK runs identically on GrapheneOS/microG/stock; F-Droid inclusion
  stays possible; the license posture stays clean.
- On older Android without a system fused provider, GPS-only fixes are slower
  and costlier; acceptable for a transit app that mostly needs coarse-to-block
  accuracy, and revisitable per-feature if turn-by-turn demands more.
- We own interval/priority tuning that FusedLocation would have handled.

## Alternatives considered

- **play-services-location** — best fix quality, but proprietary, GMS-only,
  and an F-Droid blocker.
- **Runtime GMS-detection with dual backends** — twice the surface to test for
  a marginal gain the target audience largely can't use.
