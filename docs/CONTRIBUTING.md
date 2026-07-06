# Contributing to Iter Maps — mobile

## Ground rules (the ethos)

- **Privacy is the product.** No feature lands that phones home, adds an
  account, or embeds a tracker — including "just analytics". Opt-in means
  default OFF.
- **Native or nothing.** Android UI is Material 3; iOS UI is SwiftUI with
  system materials. Don't imitate one platform on the other.
- **The contract is sacred.** The gateway wire types in `shared/` mirror the
  server's published contract exactly; if the server changes, the wire types
  change with it, never "approximately".
- **GPL-compatible dependencies only.** No Google Play Services, no
  proprietary SDKs (ADR 0003, 0011).

## Building and testing

See [DEVELOPMENT.md](DEVELOPMENT.md). `./gradlew build` and `reuse lint` must
be green before you open a PR; new behavior ships with its tests in the same
change.

## Commit convention

[Conventional Commits](https://www.conventionalcommits.org/):
`<type>(<scope>): <description>` — types `feat`, `fix`, `refactor`, `chore`,
`docs`, `test`, `style`, `perf`; scopes are module names (`shared`, `android`,
`ios`) or features. One logical change per commit; subject under 72 chars.

## Architecture decisions

Any architecturally-significant decision requires an [ADR](adr/README.md) in
the same PR. Read the ADR README for what counts. When in doubt, write one.

## CI is strict

Every push builds both the shared module and the Android app, runs all unit
tests and Android lint, checks REUSE compliance, and (on the macOS lane)
compiles the iOS app. A red lane blocks merge — fix it, don't override it.

## Sign-off: DCO, not a CLA

Every commit carries a `Signed-off-by:` line (`git commit -s`) certifying the
[Developer Certificate of Origin](https://developercertificate.org/). No CLA,
ever — your copyright stays yours, licensed under the repo's terms.

## Licensing of files

Code is GPL-3.0-or-later, docs are CC-BY-4.0, declared via `REUSE.toml`. New
files under existing paths are covered automatically; a new top-level path
needs a `REUSE.toml` entry. Where an inline header is warranted, it is the
usual two comment lines — an `SPDX-FileCopyrightText` line naming
"Iter Maps contributors" and an `SPDX-License-Identifier` line naming the
license — exactly as the REUSE specification prescribes.

## Reporting bugs and security issues

Bugs and features go through GitHub issues. Anything security- or
privacy-sensitive goes through [SECURITY.md](SECURITY.md) instead of a public
issue.
