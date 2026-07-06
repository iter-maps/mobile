# 0004 — The client speaks to one gateway origin

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

The server side exposes routing (OpenTripPlanner), geocoding (Photon), live
train boards, place enrichment, map assets, reliability, and offline downloads.
A client could point at each engine separately — one base URL per engine — and
early prototypes of this product did exactly that. It multiplies configuration,
network-security policy, TLS setup, and failure modes by the number of engines,
and it leaks the server's internal topology into every installed binary.
`iter-maps/server`'s gateway (ADR 0004 there) already fronts every engine on a
single origin, rewrites style asset URLs per request via `__BASE_URL__`, and
normalizes errors into one envelope.

## Decision

We will configure exactly one base URL — the gateway origin — and derive every
request from it. The client never addresses OTP, Photon, or any other engine
directly; engine topology is the server's private business. The base URL is a
user-visible setting (self-hosters exist) with a sensible default.

## Consequences

- Configuration, certificate policy, and error handling exist once.
- Server-side re-architecture (moving an engine, adding a cache tier) never
  requires a client release.
- The single origin is a single point of failure — by design; availability is
  the deployment's problem, and the client's job is honest offline/error UX.
- Any per-endpoint quirk (e.g. passthrough error bodies) must be handled by the
  shared client layer, not by pointing at engines directly.

## Alternatives considered

- **Per-engine base URLs** — flexible but leaks topology, multiplies config
  and security policy; already rejected server-side by fronting everything.
- **Hardcoded production origin only** — hostile to self-hosting, which the
  server explicitly supports.
