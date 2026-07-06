# 0007 — MapLibre Native with PMTiles on both platforms

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

The gateway serves the basemap as MapLibre style-spec v8 JSON documents whose
vector source is a single clustered PMTiles v3 archive fetched over HTTP byte
ranges, plus SDF glyphs, a sprite atlas, and GeoJSON transit overlays. The
client's map engine must therefore speak the MapLibre style spec and the
`pmtiles://` protocol, render vector tiles on-device, and work fully offline
against a downloaded bundle. Proprietary SDKs (Google Maps, Apple MapKit
tiles) cannot render this stack at all.

## Decision

We will render with **MapLibre Native** on both platforms — `org.maplibre.gl:
android-sdk` on Android and the MapLibre iOS distribution via Swift Package
Manager. The map is pointed at one gateway style URL
(`/styles/{light|dark|transit-light|transit-dark}.json`) and everything else —
tiles, glyphs, sprite, overlays — resolves through the style's
`__BASE_URL__`-rewritten URLs. PMTiles support is native in the SDK (11.7+);
no plugin layer. Overlay *rendering* is the client's job: styles wire overlay
sources (on transit styles) but never overlay layers, so the map layer adds its
own circle/line/symbol layers, treating an empty FeatureCollection as "draw
nothing".

## Consequences

- One engine, one style spec, both platforms — map work is done twice only at
  the thin platform-wrapper level.
- Offline rendering is the same engine pointed at `file://` URLs after a
  bundle unpack rewrites the `__BASE_URL__` token.
- MapLibre Native's API differs between Android (Java/Kotlin) and iOS
  (Objective-C/Swift); the wrappers cannot be shared and must be kept in
  behavioral parity by hand.
- Text rendering depends on the gateway's glyph fallback (`NotoSans-Regular`);
  symbol layers added by the client pin that font stack explicitly so a
  missing font can never stall a symbol bucket.

## Alternatives considered

- **Google Maps SDK / Apple MapKit** — cannot render self-hosted vector tiles
  or MapLibre styles; proprietary; contradicts the self-hostable server.
- **Mapbox SDK** — license changed away from BSD precisely to prevent this use;
  MapLibre is its community fork.
- **maplibre-compose / other wrappers** — young abstraction layers over the
  same engine; direct SDK use keeps full control of overlays and camera.
