# CLAUDE.md

Guidance for working in this repo. Keep it lean — follow the links rather than
inlining their content here.

`iter-maps/mobile` is the **Kotlin Multiplatform mobile client** of Iter Maps:
a `shared/` core (gateway wire contract + repositories) under two fully native
shells — Jetpack Compose/Material You in `androidApp/`, SwiftUI/Liquid Glass in
`iosApp/`. The current-state design is [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Decisions — use the ADR log

**Any architecturally-significant decision requires an ADR** in
[`docs/adr/`](docs/adr/README.md), written in the same change. Read
[`docs/adr/README.md`](docs/adr/README.md) for what counts and the format. When
in doubt, write one. This is not optional.

## Build, lint, test

```sh
./gradlew build                              # shared + android, tests, lint
./gradlew :shared:allTests                   # shared module only
cd iosApp && xcodegen generate               # iOS project (macOS)
```

## Quality bar (non-negotiable)

CI is strict: build, unit tests, Android lint, REUSE lint, iOS compile on the
macOS lane. **Run the Gradle build locally and make it green before
committing.** New behavior ships with its tests in the same change.

## Conventions

- **Conventional Commits** with DCO sign-off (`git commit -s`); one logical
  change per commit. Don't push or open PRs unless asked.
- 2-space indentation in Kotlin and Swift; write code that reads like the
  surrounding code. No AI-style over-explaining.
- The gateway wire contract is mirrored exactly in `shared/.../wire/` — never
  rename a wire field without a matching server contract change.
- No proprietary dependencies (ADR 0003, 0011): no GMS, no closed SDKs.
- Deferred work is tracked in [`docs/roadmap/`](docs/roadmap/README.md).
