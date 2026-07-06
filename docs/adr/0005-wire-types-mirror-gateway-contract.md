# 0005 — Wire types mirror the gateway contract byte-for-byte

- **Status:** Accepted
- **Date:** 2026-07-06
- **Supersedes:** —
- **Superseded by:** —

## Context

The gateway publishes an exact wire contract: camelCase field names, a uniform
`{ "error": { code, message, details? } }` envelope, documented
absent-vs-null semantics (a key omitted when empty versus a key always present
and nullable), fail-soft empty-200 surfaces, and feature-flagged routes that
return bare 404s. Clients that "approximately" model such contracts accumulate
silent drift: a renamed field deserializes to a default and nobody notices
until a user does.

## Decision

We will keep one `wire` package in `shared` whose `@Serializable` types mirror
the gateway DTOs exactly — same names, same optionality, same null semantics —
with `Json { ignoreUnknownKeys = true }` so additive server fields never break
older clients. Gateway-additive routing fields (`rerankScore`,
`predictedDelay`, …) are modeled as optional extras, never required. Domain
types live in a separate `model` package; mapping from wire to domain is
explicit and total. Errors are decoded into a typed `IterApiException` carrying
the envelope's stable `code`; non-JSON error bodies (static files, disabled
features, passthrough upstreams) degrade to status-only errors, and branching
is on `code`/status, never on `message`.

## Consequences

- Contract changes are mechanical: update the wire type, the compiler finds
  the fallout.
- Two model layers (wire + domain) is more code than deserializing straight
  into UI models — accepted; it is what keeps UI churn out of the contract.
- Absent-vs-null distinctions must be encoded carefully (`String?` with
  default `null` vs no default); tests pin the tricky ones.

## Alternatives considered

- **OpenAPI codegen from a machine-readable spec** — generated Kotlin is
  interop-hostile, drags a generator toolchain into the build, and the
  contract's loosest surfaces (OTP plan passthrough, GeoJSON) need
  hand-written types anyway.
- **Deserialize directly into domain/UI models** — couples rendering to the
  wire; every UI refactor risks the contract.
