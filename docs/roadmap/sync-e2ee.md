# Private sync across devices (not started)

Saved places, history and settings synced between a user's devices with
end-to-end encryption, against the gateway's contentless blob store — the
server can never read a byte of it, and the feature is opt-in and default OFF
on both sides.

- **Planned:** client-side key derivation (the `keyId` path credential derives
  from a user secret; the encryption key never leaves the device), a single
  encrypted state blob, and ETag/`If-Match` optimistic concurrency with
  client-side merge on `409 VERSION_CONFLICT` — the server never merges.
- **Planned:** honest failure UX for the contract's deliberate ambiguity: a
  404 means disabled feature, wrong key, or absent blob, indistinguishable by
  design.
- **Note:** respect the caps (256 KiB per blob) by syncing deltas of a compact
  serialized model, not raw databases; never send `If-Match: *` (deliberately
  unsupported server-side).

Decision: ADR lands with the first implementation change.
