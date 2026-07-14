# AR Math Arena QA Matrix

## Phase 7 Verification On 2026-07-14

| Area | Command / Evidence | Result |
|---|---|---|
| Kotlin compile | `.\gradlew.bat compileDebugKotlin --no-daemon --console=plain` | Passed |
| Unit tests | `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain` | Passed, 125 tests |
| Android test compile | `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon --console=plain` | Passed |
| Debug APK | `.\gradlew.bat assembleDebug --no-daemon --console=plain` | Passed |
| Connected Android tests | `.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain` | Blocked: `No connected devices!` |
| Release APK | `.\gradlew.bat assembleRelease --no-daemon --console=plain` | Blocked: command timed out twice; stale release artifact exists from previous date, not counted as Phase 7 evidence |
| No scaffold scan | `rg` over `feature/games` for TODO/FIXME/Placeholder/mock/fake/empty clicks/local IPs | Clean after Phase 7 UI cleanup |

## Added Phase 7 Coverage

- plugin runtime contract validation.
- host-only protocol payload rejection.
- duplicate, out-of-order, malformed and rate-burst packet rejection.
- `ScoreUpdated` protocol round-trip.
- performance tier fairness invariant.
- recovery snapshot round-trip without IP/device identifiers.

## Required Physical QA Before Classroom Use

- two-device LAN join, reconnect and host migration behavior.
- printed marker calibration with two or more devices.
- AR drift measurement over a 10-15 minute match.
- low-light, glare and crowded-room scan behavior.
- accessibility audit with seated play and large text.
- release packaging completion on the target build machine.
