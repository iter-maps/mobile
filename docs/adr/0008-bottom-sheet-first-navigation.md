# 0008 — Bottom-sheet-first navigation over a persistent map canvas

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

A maps app has one primary surface — the map — and everything else (search,
place details, journey planning, boards, settings) is a conversation held
*over* it. Screen-stack navigation, the platform default, destroys the map
context on every push: the map unloads or freezes behind an opaque screen, and
returning re-renders it. The best-in-class interaction model for this problem
is the one Apple Maps established: a single persistent map with a draggable
bottom sheet that hosts stacked content pages at a few well-known heights.

## Decision

We will keep exactly one full-screen map canvas per platform, with a universal
bottom sheet as the only content container. The sheet has three anchors —
peek, half, full (plus an optional content-fit anchor for compact pages) — and
hosts an internal page back-stack (home, search, place detail, planning,
boards, settings, …) that never tears down the map. System back/swipe pops the
sheet stack before it exits the app. On Android this is a custom
anchored-draggable implementation (Material's bottom sheet cannot express
multi-anchor page stacks); on iOS it builds on the native sheet with custom
detents so it inherits Liquid Glass materials for free. Map camera and sheet
height cooperate: the visible map centers itself above the sheet, and
selection-driven camera moves target the uncovered map band.

## Consequences

- The map lives forever: no reload flashes, camera state and loaded tiles
  survive every navigation.
- Sheet physics, nested-scroll handoff (inner lists scroll only at full
  height), and per-page anchor memory are ours to implement and tune on
  Android — this is the hardest UI code in the repo and needs tests around its
  state logic.
- All page state hangs off one host screen; ViewModels must be scoped
  deliberately so page state resets when a page is popped.
- Deep links and state restoration route through the sheet stack, not the
  platform nav graph.

## Alternatives considered

- **Platform navigation stacks (NavHost / NavigationStack) per feature** —
  kills the persistent-map premise; map reloads on every hop.
- **Material `BottomSheetScaffold`** — two effective states, no page stack, no
  per-page anchor control; was evaluated and cannot express this model.
- **Multiple modal sheets stacked ad hoc** — z-order and gesture chaos;
  one sheet with an internal stack is strictly more predictable.
