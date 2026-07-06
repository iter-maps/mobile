# Turn-by-turn transit guidance (not started)

Active-trip guidance for a chosen itinerary: a glanceable banner with the
current maneuver, a leg timeline with progress, and alight alerts — the app's
reason to stay open in the pocket.

- **Planned:** a pure-Kotlin progress engine in `shared` (GPS map-matching
  against decoded leg polylines with a schedule-time fallback), so both shells
  render the same progress state. Platform pieces: an Android foreground
  service (type `location`) with a persistent notification, and iOS Live
  Activities for the lock screen.
- **Planned:** proximity alerting ("prepare to alight" N stops out) driven by
  the same engine.
- **Planned:** underground progress estimation when GPS dies in tunnels —
  schedule-anchored at minimum; sensor-based stop counting is a research item.
- **Note:** requires the route-polyline rendering built for planning detail;
  the camera's follow mode (course-up, gesture-break) is shell work tracked
  under each platform.

Decision: ADR lands with the first implementation change.
