# Overview

## What it does

Iter Maps is a public-transport journey planner and map. This repository is the
mobile client — one Android app and one iOS app sharing a Kotlin Multiplatform
core — for the [Iter Maps server](https://github.com/iter-maps/server), a
self-hostable gateway that fronts routing, geocoding, live transit data, place
enrichment, and map assets on a single origin.

The app plans door-to-door journeys across metro, bus, tram, rail and walking;
searches addresses and places with photo-and-summary details; renders a vector
basemap with transit overlays in light, dark and transit-emphasis styles; shows
live train boards with delays; and pre-downloads areas for offline use.

## Architecture

The split is simple: everything below presentation is written once in Kotlin
(`shared/` — wire contract, HTTP client, repositories, domain math), and each
platform keeps a fully native shell. Android renders in Jetpack Compose with
Material You dynamic color; iOS renders in SwiftUI with Liquid Glass materials.
Both shells share one interaction model: a persistent full-screen map with a
draggable bottom sheet hosting every content page.

The current-state design is in [`ARCHITECTURE.md`](ARCHITECTURE.md); the *why*
behind each significant choice is in the [ADR log](adr/README.md).

## Status

Pre-release. The Android shell carries the core experience (map, search,
places, planning, boards, settings); the iOS shell is in progress behind it;
offline management, trip guidance, and saved places are on the
[roadmap](roadmap/README.md).
