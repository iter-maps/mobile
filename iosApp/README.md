# iosApp

The native SwiftUI shell (ADR 0002, 0008, 0009, 0010).

- Requirements: Xcode 26, [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`), a JDK for Gradle.
- Generate the project: `xcodegen generate` here, then open `Iter.xcodeproj` (the project file is git-ignored).
- The shared `IterCore` framework is built automatically by a pre-build Gradle
  phase (`:shared:embedAndSignAppleFrameworkForXcode`) — no manual step.
- MapLibre iOS comes in via Swift Package Manager, declared in `project.yml`.
- The dev gateway origin defaults to `http://localhost:8090` (simulator reaches
  the host loopback); change it in the app's Settings page.
