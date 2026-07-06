# Transit overlay & stop marker redesign (dedicated pass, not started)

The first-pass overlay rendering is functional but visually illegible and
needs a dedicated design round — it is explicitly *not* polish-by-tweaks.

- **Problem:** the transit-lines overlay renders as a uniform thin purple
  stroke with no per-line identity, no casing hierarchy against the basemap,
  and no zoom-dependent width; metro/station points render as small
  white-and-indigo dots that are indistinguishable from one another and from
  generic map noise — nothing communicates "this is a metro station" or
  "this is a bus stop", let alone which line serves it.
- **Planned:** a real marker system — category-shaped/colored station glyphs
  (metro vs rail vs bus), line-colored strokes derived from the overlay
  GeoJSON properties with white casing, zoom-driven size/visibility ramps
  (dots → glyphs → labeled badges), selection states, and collision-aware
  labels. Same pass covers both shells so the vocabulary matches.
- **Planned:** initial map camera on the user's position/region instead of
  the world view, so first launch never lands on empty low-zoom tiles.
- **Note:** blocked on nothing server-side — the overlay sources already
  carry per-feature properties; this is client rendering work. Coordinate
  with any server-side overlay geometry cleanups if they land first.

Decision: ADR lands with the implementation.
