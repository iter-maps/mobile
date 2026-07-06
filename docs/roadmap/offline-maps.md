# Offline maps (core built, management UI in progress)

Pre-download a bbox as a self-contained map: tiles, styles, glyphs, sprite and
overlays, rendering fully offline through the same MapLibre stack.

- **Built:** shared-core client for `GET /offline/bundle` (streaming download)
  and `GET /offline/extract`, honoring the contract's guards (6 deg² area cap,
  z14 clamp, 503-BUSY backoff), plus bundle unpack with the
  `__BASE_URL__` → `file://<unpack-dir>` style rewrite the wire contract
  prescribes.
- **In progress:** area management UI — pick a region on the map, size
  estimate, download progress, delete; automatic offline style fallback when
  the gateway is unreachable.
- **Planned:** freshness via `GET /manifest` etags — offer a re-download when
  the server's artifacts are newer than the local bundle.
- **Note:** the bundle ZIP is STORE-compressed by contract; unpack must guard
  against zip-slip and use data-descriptor-tolerant extraction.

Decision: ADR 0004, 0007.
