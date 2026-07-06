# 0003 — Layered licensing: GPL-3.0 code, CC-BY-4.0 docs, REUSE, DCO

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

This is a public repository intended to outlive its original authors. Licensing
has to be decided before meaningful code lands, because retrofitting a license
onto mixed contributions is somewhere between painful and impossible. The
companion server repo uses layered licensing (strong copyleft for code, CC for
prose) declared through the REUSE specification, with DCO sign-offs instead of
a CLA; the two repos should present one coherent posture.

## Decision

We will license all code GPL-3.0-or-later and all documentation CC-BY-4.0,
declared centrally in `REUSE.toml` (full texts under `LICENSES/`, SPDX-named)
rather than per-file headers. Contributions are accepted under the Developer
Certificate of Origin — `Signed-off-by` on every commit, no CLA.

GPL rather than AGPL because, unlike the server, this code ships as an
installed application: the network-interaction clause that motivates AGPL on
the gateway adds nothing for a client binary, and GPL keeps the app
distributable through F-Droid and similar channels without ambiguity.

## Consequences

- Every dependency must be GPL-compatible; this is a review criterion for any
  new library (notably: no GMS-only, no proprietary SDKs).
- REUSE lint can run in CI and fail the build on undeclared files.
- Forks must stay open — the point of copyleft on a privacy-first client.
- App-store distribution of GPL code requires care with added terms; this is a
  known, managed constraint rather than a surprise.

## Alternatives considered

- **AGPL-3.0 like the server** — no additional protection for an installed
  client; complicates app-store conversations for zero benefit.
- **Apache-2.0 / MIT** — maximizes adoption, but allows proprietary forks of a
  privacy-first app, which defeats its purpose.
- **CLA instead of DCO** — heavier contributor friction; DCO is enough for a
  repo that never intends to relicense.
