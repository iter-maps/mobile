# 0001 — Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

This client is a ground-up build. As it makes its own implementation choices —
module boundaries, which map engine to embed, how the two platforms share code —
that reasoning needs a home in the repo, or it evaporates into commit messages
and chat logs and gets re-litigated later. The companion `iter-maps/server`
repository keeps such a log and it has already paid for itself.

## Decision

We will keep an ADR log under `docs/adr/`, following the process in its README:
one immutable record per architecturally-significant decision, written in the
same PR as the change, mandatory for significant decisions and rejectable in
review when missing.

## Consequences

- New significant work carries a small writing tax (one short doc) — the point.
- The history of *why* survives contributor turnover and the original author's
  memory.
- `docs/ARCHITECTURE.md` stays the current-state view; the ADR log stays the
  history. They must not drift into duplicating each other.

## Alternatives considered

- **No ADRs, rely on commit messages / PR descriptions** — lost to search and
  squash-merges; no canonical place for "why".
- **A single growing DECISIONS.md** — merge conflicts and no per-decision
  status/supersession.
