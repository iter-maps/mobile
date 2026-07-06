# Live boards, service alerts & monitored lines (boards built)

Live departures/arrivals are table stakes; alerts and line monitoring turn
them into a reason to check the app before leaving the house.

- **Built:** live train boards — station autocomplete
  (`/trenitalia/stations/search`, no coordinates by contract), coordinate
  lookup via `/trenitalia/stations`, departures/arrivals polling no faster
  than the surface's `max-age=20`, delay coloring, platform display.
- **Planned:** favorite stations pinned to the home sheet.
- **Planned:** service alerts — OTP GraphQL `alerts` surfaces with severity
  filtering and per-day grouping, cached for offline/cold-start display.
- **Planned:** monitored lines — user-chosen routes whose alerts surface
  proactively (notification policy per platform).
- **Note:** the boards contract is polling-only (no push); any notification
  feature is client-side scheduling, and battery honesty matters more than
  freshness theater.

Decision: ADR 0004, 0005.
