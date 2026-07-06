# Opt-in crowd telemetry (not started)

Contributing anonymous presence/occupancy samples during a guided trip so the
network's crowding model improves — strictly opt-in, default OFF, and honest
about what leaves the device.

- **Planned:** a consent surface that shows the exact payload shape (route,
  stop, coarse timestamp, occupancy signal — no coordinates, no identifiers
  beyond a rotating opaque token), wired to `POST /telemetry` only while a
  trip is active.
- **Planned:** batching within the contract's caps (128 samples, 64 KiB),
  fire-and-forget semantics (a 202 is not a durability ack; never
  retry-loop), silent no-op when the server has the feature off (bare 404).
- **Note:** the sample schema is `deny_unknown_fields` server-side — the
  client must never attach extra keys.

Decision: ADR lands with the first implementation change.
