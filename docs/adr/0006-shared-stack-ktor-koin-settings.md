# 0006 — Shared-core stack: Ktor client, kotlinx.serialization, Koin, Multiplatform-Settings

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

The shared module needs an HTTP client, JSON (de)serialization, dependency
wiring, and small key-value persistence — all running in `commonMain` and
compiled to both an Android library and an iOS framework. Every candidate must
be GPL-compatible (ADR 0003) and must not drag platform-exclusive services into
the core.

## Decision

We will use:

- **Ktor client** with platform engines (OkHttp on Android, Darwin on iOS) —
  the de-facto KMP HTTP client, with plugin-based timeouts, logging, and
  content negotiation.
- **kotlinx.serialization** for JSON — compiler-plugin based, no reflection,
  and the natural partner for the mirrored wire types of ADR 0005.
- **Koin** for dependency injection — runtime DI with first-class KMP support;
  the module graph is small enough that compile-time DI buys nothing.
- **Multiplatform-Settings** for preferences — a thin expect/actual over
  `SharedPreferences`/`NSUserDefaults`, enough for the handful of client
  settings (base URL, theme, map style); anything richer waits until a real
  need appears.

Structured GraphQL for routing is hand-rolled (a query builder plus mirrored
response types), not Apollo: there is exactly one operation family (`plan`),
and the OTP schema's custom scalars are easier to control by hand.

## Consequences

- All four libraries are stable, actively maintained, Apache-2.0, and carry no
  Google-Play-Services or other proprietary dependency.
- Koin resolution errors surface at runtime, not compile time — mitigated by a
  DI smoke test.
- Hand-rolled GraphQL means the plan query text lives in this repo and must
  track the OTP version the server deploys; the flip side is zero codegen and
  exact control over the leg fields the gateway's rerank/predict transforms
  key on.

## Alternatives considered

- **Retrofit/OkHttp directly** — Android-only; no iOS story.
- **Apollo Kotlin for OTP GraphQL** — schema download + codegen pipeline for
  one query family; custom scalars (`CoordinateValue`, `Reluctance`) still
  need hand care.
- **Hilt/Dagger** — no multiplatform support.
- **DataStore KMP** — heavier, file-based; nothing here needs it yet.
