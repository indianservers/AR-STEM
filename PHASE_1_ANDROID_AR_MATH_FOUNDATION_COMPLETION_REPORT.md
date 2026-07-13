# Phase 1 Android AR Math Foundation Completion Report

## 1. Executive Summary

Implemented Phase 1 of **AI STEM - Augmented Reality Learning** as a native Kotlin Android app using package/application ID `com.indianservers.ai_stem`. The app now launches into a Compose welcome flow, shows all STEM subjects with only Mathematics enabled, opens a Mathematics dashboard, and provides an AR Mathematics Playground backed by ARCore + SceneView/Filament.

The AR playground includes camera permission handling, AR availability fallback UI, horizontal-plane hit testing, one active ARCore anchor at a time, a selectable Phase 1 object tray, generated cube/grid/sine primitives, object replacement, move-by-retap mode, reset, delete, help, and plane-visualization toggling.

## 2. Architecture

- Module structure: single Android application module `app`.
- Package structure: `app`, `core/designsystem`, `core/model`, `domain/mathematics`, `feature/onboarding`, `feature/subjects`, `feature/mathematics`, `feature/arviewer`.
- Navigation: single-activity Compose Navigation with typed route constants in `AppDestination`.
- State management: `ArViewerViewModel` exposes `StateFlow<ArViewerUiState>`.
- AR session architecture: `ARSceneView` owns ARCore session lifecycle; the screen keeps the active `Anchor` and detaches it on replace/delete/dispose.
- Rendering architecture: SceneView primitives render through Filament. `AnchorNode` attaches generated math content to real ARCore anchors.
- Mathematical object abstraction: `MathObjectType`, `MathObjectDefinition`, and `MathematicsCatalogue.phaseOneObjects`.
- Interaction-command architecture: `ObjectInteractionCommand`, `ObjectInteractionSource`, `TouchInteractionSource`, and `reduceInteraction`. A future hand source can emit the same commands; it must share ARCore camera frames instead of opening a second camera session.

## 3. Technology Versions

- Android Gradle Plugin: `9.1.1`
- Gradle: `9.3.1`
- Kotlin / Compose compiler plugin: `2.4.0`
- Compile SDK: `36.1` because SDK 37 is not installed locally
- Target SDK: `36`
- Minimum SDK: `31`; chosen to match the modern AR/Filament stack and the existing project baseline
- Compose BOM: `2026.06.01`
- Activity Compose: `1.12.0`
- Navigation Compose: `2.9.8`
- Lifecycle Compose/ViewModel: `2.10.0`
- Coroutines: `1.10.2`
- ARCore: `1.50.0`
- SceneView AR: `4.22.0`
- AndroidX Core KTX: `1.17.0`
- JUnit: `4.13.2`
- AndroidX Test JUnit: `1.3.0`
- Espresso: `3.7.0`

## 4. Implemented Screens

- Welcome: required title, headline, supporting text, feature indicators, and Start Exploring button.
- Subject selection: Mathematics enabled; Physics, Chemistry, Biology visible and disabled with future-phase messaging.
- Mathematics home: AR Playground enabled; requested math categories visible as future modules.
- AR viewer: permission/availability states, AR scene, object tray, back/help/move/reset/delete/show planes controls.
- Unsupported AR fallback: back and retry actions with clear ARCore physical-device requirement.

## 5. Implemented AR Functionality

- Horizontal plane mode configured with `Config.PlaneFindingMode.HORIZONTAL`.
- Environmental HDR light estimation configured.
- Depth mode is enabled only when supported.
- Plane renderer can be shown/hidden.
- Per-frame horizontal plane detection updates placement guidance.
- Tap hit testing accepts tracked upward-facing horizontal planes with pose-in-polygon validation.
- Placement creates an ARCore anchor and attaches a SceneView `AnchorNode`.
- Only one active object is supported; replacing/deleting detaches the previous anchor.
- Move mode allows retapping a valid surface to create a new anchor.
- SceneView editable node gestures provide touch manipulation for placed content.
- Reset forces recreation of the editable child node via a transform revision token.
- Delete detaches the anchor and returns to scanning/placement state.

## 6. Mathematical Objects

- Coordinate Plane: generated from line primitives; includes X/Y axes, major/minor-style grid lines, origin plane orientation, and visible point corresponding to `(2, 2)`. Known limitation: text labels are represented through UI/object metadata rather than 3D text labels.
- Cube: generated procedural cube at roughly 24 cm with visible edge line primitives and a label chip: `Cube - 6 faces - 12 edges - 8 vertices`.
- Sine Curve: sampled from `y = sin(x)` over `-2pi` to `2pi`, rendered with line segments plus axes. Unit tests verify boundary values and key sine values.

All objects use scale limits `0.25x..4.0x` in domain transform logic.

## 7. Permissions and Device Compatibility

- Requests only `android.permission.CAMERA`, and only when entering AR.
- Handles not requested, granted, denied, and permanently denied UI paths.
- Permanent denial routes to Android app settings.
- Checks ARCore availability, missing AR services, outdated AR services, unsupported devices, and a best-effort OpenGL major version.
- Unsupported devices never enter an indefinite loading state.

Known limitation: install/update AR services currently shows a recovery screen and retry action, but does not launch the Play Store installer flow.

## 8. Tests

| Test | Type | Command | Result | Evidence |
|---|---|---|---|---|
| Subject availability | Unit | `.\gradlew.bat testDebugUnitTest` | Passed | `PhaseOneDomainTest` |
| Math object catalogue | Unit | `.\gradlew.bat testDebugUnitTest` | Passed | `PhaseOneDomainTest` |
| Sine curve sampling/boundaries | Unit | `.\gradlew.bat testDebugUnitTest` | Passed | `PhaseOneDomainTest` |
| Scale clamping / rotation normalization | Unit | `.\gradlew.bat testDebugUnitTest` | Passed | `PhaseOneDomainTest` |
| Interaction reducer reset/delete/transform | Unit | `.\gradlew.bat testDebugUnitTest` | Passed | `PhaseOneDomainTest` |
| Welcome/subject/math navigation | Compose Android test | `.\gradlew.bat connectedDebugAndroidTest` | Not run | No attached device/emulator |
| Disabled subject snackbar | Compose Android test | `.\gradlew.bat connectedDebugAndroidTest` | Not run | No attached device/emulator |
| Physical AR placement | Manual device test | Manual checklist | Not run | No ARCore device available in this environment |

## 9. Build Results

- `.\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug --console=plain`: Passed
- `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain`: Passed after final AR reset fix
- `.\gradlew.bat testDebugUnitTest --console=plain`: Passed
- `.\gradlew.bat lintDebug --console=plain`: Passed
- `.\gradlew.bat connectedDebugAndroidTest`: Not run; SDK `adb` reported no attached devices
- APK path: `C:\Indian Servers\AR-STEM\app\build\outputs\apk\debug\app-debug.apk`
- APK size: `98,436,363` bytes
- Git commit hash: not available; this directory is not a Git repository

## 10. Physical-Device Verification

No physical ARCore-supported Android device was attached or available. Device model, Android version, ARCore version, screenshots, and physical acceptance results remain pending.

Manual command once a device is attached:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Manual AR acceptance should cover launch/navigation, permission denial/permanent denial, plane detection, cube placement, coordinate plane placement, sine curve placement, lifecycle recovery, and tracking recovery.

## 11. Screenshots Or Evidence

No screenshots were captured because no emulator/device/browser rendering session was available for this native Android app. Screenshot evidence remains pending for:

- Welcome screen
- Subject selection
- Mathematics home
- Surface scanning
- Surface detected
- Cube placed
- Coordinate plane placed
- Sine curve placed
- Selected object controls
- Unsupported-device screen

## 12. Known Issues

- Physical AR behavior has not been verified on an ARCore device.
- Instrumented Compose tests exist but were not executed because no device/emulator was attached.
- Coordinate-plane and sine labels are not rendered as 3D text labels in Phase 1; the app avoids blurry bitmap text and exposes names/metadata through UI controls.
- ARCore install/update handling is a clear fallback screen plus retry, not an installer launch flow.
- SceneView editable gestures handle touch manipulation; deeper per-frame transform telemetry is reserved for a later interaction phase.

## 13. Phase 2 Readiness

- `MathematicsCatalogue` can grow into a larger object catalogue.
- `MathObjectDefinition` carries metadata, scale bounds, and object type.
- `SineCurveSampler` is pure and testable for future graph controls.
- `ObjectInteractionCommand` and `ObjectInteractionSource` allow touch and future MediaPipe hand sources to issue shared commands.
- AR rendering is isolated in `feature/arviewer`, while subject and math navigation remain independent.

## 14. Changed-File Inventory

- `gradle/libs.versions.toml`: Compose, ARCore, SceneView, test, and Kotlin/AGP version catalogue.
- `build.gradle.kts`, `app/build.gradle.kts`: Compose/AR dependencies and Gradle configuration.
- `app/src/main/AndroidManifest.xml`: package permissions, ARCore metadata, portrait activity.
- `app/src/main/res/values/strings.xml`: app name.
- `app/src/main/res/values/themes.xml`, `values-night/themes.xml`: no-action-bar base theme.
- `MainActivity.kt`: single-activity Compose entry.
- `app/AiStemApp.kt`, `AppDestination.kt`: Compose navigation.
- `core/designsystem/AiStemTheme.kt`: Material 3 color system.
- `core/model/Subjects.kt`: Phase 1 subject availability.
- `domain/mathematics/MathObjects.kt`: object catalogue, sine sampler, transform math.
- `feature/onboarding/WelcomeScreen.kt`: welcome UI.
- `feature/subjects/SubjectSelectionScreen.kt`: subject grid and disabled-subject snackbar.
- `feature/mathematics/MathematicsHomeScreen.kt`: math dashboard and AR Playground entry.
- `feature/arviewer/ArViewerState.kt`: AR UI state and interaction reducer.
- `feature/arviewer/ArViewerViewModel.kt`: availability, permission, object, and control state.
- `feature/arviewer/ArViewerScreen.kt`: ARCore/SceneView runtime and controls.
- `PhaseOneDomainTest.kt`: JVM tests for subjects, catalogue, sine, transforms, reducer.
- `PhaseOneComposeTest.kt`: instrumented Compose navigation tests, pending device execution.
- Removed unused XML fragment/template resources and fragment Kotlin classes from the starter template.
