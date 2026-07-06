# 0012 — Strict CI: build, test, lint, REUSE on every push

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

A two-platform client rots fast without an automated bar: the iOS shell is
often edited on machines that cannot compile it (ADR 0010 context), the
shared module compiles to three targets, and licensing hygiene (ADR 0003) is
only real if a machine checks it. The companion server repo set this
precedent (its ADR 0006) and it has kept quality boringly consistent.

## Decision

Every push and PR runs three lanes, all required:

- **android** (Linux): full Gradle `build` — shared + Android compilation,
  all unit tests, Android Lint — plus the iOS klib compilations
  (`compileKotlinIosArm64`/`SimulatorArm64`), which type-check the shared
  module's iOS side without a Mac.
- **ios** (macOS): XcodeGen project generation and an unsigned simulator
  build of the app, compiling the Swift shell against the real shared
  framework.
- **reuse**: REUSE-spec license lint.

A red lane blocks merge; fixing it beats overriding it, always.

## Consequences

- Swift written on non-Mac machines gets compile-verified within one push.
- macOS runner minutes are the price of the ios lane; acceptable for a
  public repo where they are free-tier.
- Tooling drift on runners (Xcode versions especially) becomes our problem
  to pin and maintain.

## Alternatives considered

- **No macOS lane, trust review** — Swift that has never compiled is not
  code, it's prose.
- **Nightly instead of per-push** — failures land a day late, after the
  context has evaporated.
