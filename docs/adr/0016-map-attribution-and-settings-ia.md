# 0016 — Map attribution relocated off-map; settings information architecture

- **Status:** Accepted
- **Date:** 2026-07-09
- **Supersedes:** —
- **Superseded by:** —

## Context

The basemap is OpenStreetMap data via OpenMapTiles (ODbL, ADR 0007), so
attribution is a legal obligation: the credit must stay visible or be one tap
away, and it cannot simply be dropped. MapLibre satisfies this with an on-map
"i" button, but that button sits on the map face fighting the floating chrome
and the bottom sheet (ADR 0008), and its default sheet is a foreign
system surface. Separately, Settings had grown into a flat list where a
developer-only field (gateway origin, ADR 0004) sat next to theme and map
options with no hierarchy.

## Decision

**(a) Attribution.** Remove the on-map MapLibre "i" button from the map face.
Replace it with a small always-visible, tappable "© OpenStreetMap" credit over
the map, plus a dedicated in-app **"About the map"** screen reached from
Settings carrying the full "© OpenStreetMap contributors" and "© OpenMapTiles"
credits and the OSS licenses. This changes *how* the legal obligation is met,
not whether it is met. **Removing attribution with no replacement is
prohibited.**

**(b) Settings IA.** Reorganize Settings into sections:

- **Appearance** — theme (kept first).
- **Map & Location** — map style + Offline link.
- **About & Legal** — version + attribution / licenses ("About the map").
- **Help** — replay onboarding + permission shortcut.
- **Advanced** — gateway origin, demoted to the bottom as a dev field.

A persisted `hasSeenOnboarding` flag is added. Onboarding is minimal for now —
a "Replay onboarding" row, no full first-run flow yet; the full flow remains a
tracked roadmap item.

## Consequences

- The map face is clean while the ODbL credit stays visible and one-tap
  expandable — the obligation is met more clearly than the default button did.
- Attribution copy and licenses now live in an app screen that must be kept
  current whenever the basemap sources change; a future contributor cannot
  "tidy away" the credit without breaking the license.
- Settings has a hierarchy that scales; the gateway field stops reading as a
  first-class user setting.
- `hasSeenOnboarding` is persisted state that the eventual first-run flow will
  build on rather than re-invent.

## Alternatives considered

- **Keep the on-map "i" button** — meets the license, but clutters the map face
  and drops users into a foreign system sheet.
- **Static credit only, no About screen** — satisfies the minimum, but leaves
  nowhere to carry the full OpenMapTiles credit and OSS licenses.
- **Leave Settings flat** — no new capability, but the dev field keeps reading
  as a user setting and the page doesn't scale.
