# Development

## Prerequisites

- **JDK 17+** and the **Android SDK** (`compileSdk` 36). Point `local.properties`
  at it (`sdk.dir=…`) or export `ANDROID_HOME`.
- **Xcode 26+** and **[XcodeGen](https://github.com/yonaskolb/XcodeGen)** for
  the iOS app (macOS only; the Kotlin side builds everywhere).
- A gateway to talk to — run
  [`iter-maps/server`](https://github.com/iter-maps/server) locally
  (`http://localhost:8090`) or point the app at any deployed origin from its
  settings page.

## Android

```sh
./gradlew :androidApp:assembleDebug        # build the APK
./gradlew :androidApp:installDebug         # install on a connected device
./gradlew :shared:check :androidApp:testDebugUnitTest lintDebug
```

The debug build talks to `http://10.0.2.2:8090` (the emulator's host loopback)
by default; change the gateway origin at runtime in **Settings → Server**.

## iOS

```sh
cd iosApp
xcodegen generate          # produces Iter.xcodeproj (git-ignored)
open Iter.xcodeproj
```

The Xcode build runs the shared framework's `embedAndSignAppleFrameworkForXcode`
Gradle task as a build phase — the first build compiles Kotlin and is slow;
later ones are incremental.

## Shared module

```sh
./gradlew :shared:allTests                 # common + platform unit tests
```

Tests use Ktor's MockEngine — no network, no server needed.

## Quality bar

CI runs build, unit tests, Android lint, and [REUSE](https://reuse.software)
compliance on every push. Run locally before committing:

```sh
./gradlew build
reuse lint        # pipx install reuse
```

Conventions: [Conventional Commits](https://www.conventionalcommits.org/) with
DCO sign-off (`git commit -s`); 2-space Kotlin/Swift indentation; an
[ADR](adr/README.md) accompanies every architecturally-significant change.
