# Phase 3 Shared AR Origin and Spatial Sync Completion Report

## Summary

Phase 3 adds the shared spatial foundation for AR Math Arena: a marker-based classroom origin, host surface-placement fallback models, serializable shared transforms, calibration readiness, host-authoritative spatial protocol messages, anchor lifecycle tracking, and a calibration lobby after the local Wi-Fi lobby.

Cloud Anchors remain disabled and are not required for LAN play.

## Marker Design

Created an original classroom marker:

- Name: `AR Math Arena Origin`
- Runtime marker width: `0.18 m`
- Printable asset: `app/src/main/assets/ar_math_arena_origin_marker.svg`
- High-resolution PNG: `docs/ar_math_arena_origin_marker_2400.png`
- Instructions: `docs/ar_math_arena_origin_marker_readme.md`

Marker properties:

- High contrast.
- Strong non-repeating features.
- Clear orientation arrow.
- Known physical width.
- No copyrighted image assets.
- QR join area reserved separately so it does not replace image tracking.

ARCore configuration builds an `AugmentedImageDatabase` at runtime with the generated marker bitmap and known physical width.

## Coordinate System

Defined in `SharedSpatialModels.kt`:

- Origin: center of printed marker.
- +X: marker right.
- +Y: normal outward from marker plane.
- +Z: marker arrow forward.
- Units: metres.
- Rotation: normalized quaternion.
- Version: `SHARED_COORDINATE_VERSION = 1`

Serializable structures added:

- `Vector3Dto`
- `QuaternionDto`
- `SharedTransform`
- `LocalPoseDto`
- `SharedOriginDefinition`
- `SpatialSnapshot`
- `SharedObjectTransform`

## Transform Mathematics

Implemented:

- Quaternion normalization.
- Finite-number validation.
- Scale validation.
- Local AR pose to marker-relative transform.
- Marker-relative transform to local AR pose.
- Transform interpolation.
- Snapshot reconciliation with origin-version checks.
- Coordinate-version and origin-version mismatch rejection.

Renderer or ARCore object references are not transmitted.

## Calibration Algorithm

Implemented in `CalibrationReadinessEvaluator`.

Inputs:

- AR camera tracking quality.
- Augmented Image tracking quality.
- Marker extent stability.
- Pose variance over a sample window.
- Device movement.
- Distance from marker.
- Viewing angle.
- Lighting estimate.
- Tracking interruptions.

States:

- Searching
- MarkerDetected
- Stabilizing
- Ready
- TrackingWeak
- MarkerLost
- RecalibrationRequired

Final acceptance requires stable full tracking; approximate or lost tracking cannot finalize placement.

## ARCore Features Used

Added `ArMathArenaOriginMarker`:

- Configures ARCore Augmented Images with `AR Math Arena Origin`.
- Enables horizontal and vertical plane detection for fallback.
- Disables Cloud Anchors, Geospatial, Streetscape Geometry.
- Enables Automatic Depth only if supported.
- Enables Environmental HDR lighting.
- Allows Instant Placement only as preview.
- Converts ARCore `Pose` into serializable local pose DTOs.

Added optional feature helpers:

- `ArenaArFeaturePolicy`
- `ArenaTorchController`

Torch behavior:

- Checks flash support.
- Does not force torch.
- Can turn off on leaving screen.
- Shows unsupported/error messages.
- Warns about heat/battery.

## Surface Placement Fallback

Implemented `SurfacePlacementModels.kt`:

- Scanning.
- Approximate Instant Placement preview.
- Valid tracked plane.
- Invalid surface.
- Confirmed.
- Lost tracking.

Policy checks:

- Polygon-aware hit.
- Minimum plane size.
- Maximum distance.
- Horizontal-up or vertical orientation.
- Instant Placement cannot be finalized as authoritative.

This mode is explicitly less precise than printed-marker calibration.

## Depth, Occlusion, HDR Fallbacks

Depth:

- Uses `Config.DepthMode.AUTOMATIC` only when supported.
- Keeps gameplay playable when unsupported.

HDR:

- Uses Environmental HDR in shared-origin configuration.
- Existing AR config already falls back to ambient intensity where needed.

Instant Placement:

- Preview only.
- Not accepted by `SurfacePlacementPolicy` as final placement.

## Shared Calibration Lobby

Updated AR Math Arena shell:

- Normal lobby now has `Calibrate AR`.
- Added `CalibrationLobby` mode.
- Host can choose:
  - Marker Origin
  - Surface Fallback
- Players see calibration instructions and quality state.
- Non-AR analyst option is available for unsupported/disconnected players.
- `startMatch()` now blocks if required connected players are not calibrated or allowed as non-AR analysts.

## Network Synchronization

Extended `GamePayload` with spatial messages:

- `CalibrationStarted`
- `MarkerDetected`
- `CalibrationQualityUpdated`
- `CalibrationAccepted`
- `CalibrationRejected`
- `SharedOriginDefined`
- `SharedOriginVersionChanged`
- `PlayerCalibrationState`
- `SpatialSnapshotMessage`
- `AnchorDefinition`
- `AnchorRemoved`
- `RecalibrationRequested`

The host owns:

- Origin version.
- Origin definition.
- Anchor definitions/removal.
- Recalibration decisions.
- Match start gating.

Clients own only local conversion between shared coordinates and their AR session coordinates.

## Anchor Management

Added `SharedAnchorRegistry`.

Tracks:

- Anchor ID.
- Shared transform.
- Anchor type.
- Owner.
- Creation sequence.
- Active state.
- Origin version.
- Local anchor reference stored separately from serializable records.
- Last synchronization time.

Cleanup supports:

- Origin version changes.
- Session close.
- Object removal.
- Recalibration.

## Recording and Playback

Added support detection in AR feature policy for ARCore recording/playback status. A full developer-only recording UI is deferred until physical AR device validation is available.

## Files Created

- `app/src/main/java/com/indianservers/ai_stem/feature/games/spatial/SharedSpatialModels.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/spatial/CalibrationModels.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/spatial/AnchorRegistry.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/spatial/SurfacePlacementModels.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/spatial/SpatialSessionReducer.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/arcore/ArMathArenaOriginMarker.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/arcore/ArenaArFeaturePolicy.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/arcore/ArenaTorchController.kt`
- `app/src/main/assets/ar_math_arena_origin_marker.svg`
- `docs/ar_math_arena_origin_marker_2400.png`
- `docs/ar_math_arena_origin_marker_readme.md`
- `app/src/test/java/com/indianservers/ai_stem/ArenaSharedSpatialCoreTest.kt`

## Files Modified

- `app/src/main/java/com/indianservers/ai_stem/feature/games/multiplayer/ArenaProtocol.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/matharena/ArenaLobbyViewModel.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/matharena/ArMathArenaScreen.kt`

## Tests Added

`ArenaSharedSpatialCoreTest` covers:

- Transform conversion.
- Quaternion normalization.
- Invalid transform rejection.
- Coordinate/origin-version mismatch.
- Transform snapshot reconciliation.
- Marker calibration readiness.
- Tracking weak/lost prevention through readiness rules.
- Anchor registry cleanup.
- Instant Placement finalization rejection.
- Spatial-message serialization.
- Host placement authority.

Existing Phase 2 LAN tests continue to cover local TCP transport and lobby reducers.

## Verification Results

Passed:

- `.\gradlew.bat compileDebugKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain`
- `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat assembleDebug --no-daemon --console=plain`

Attempted:

- `.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain`

Result:

- Blocked by environment: `No connected devices!`

## Physical-Device Test Evidence

Physical ARCore device testing could not be completed in this workspace because no Android device or emulator is connected.

Still required for final field acceptance:

- Two ARCore-supported devices scan the same printed marker.
- Both devices reach `Ready`.
- A host-authoritative test object appears at corresponding physical locations.
- Marker loss does not crash.
- Reacquiring the marker restores alignment.
- Recalibration increments origin version and cleans old anchors.
- Surface fallback works on a tracked plane.
- Unsupported Depth devices remain playable.

## Alignment Limitations

- Marker alignment depends on print scale, flatness, lighting, camera quality, and ARCore tracking stability.
- Surface fallback is intentionally less precise and should be used only when the marker is unavailable.
- QR area should not cover or replace the tracking marker.
- Physical accuracy cannot be claimed until two-device ARCore testing is performed.

## Deferred Phase 4 Work

- Real gameplay object renderer using `SharedTransform`.
- Live camera calibration screen with SceneView/ARCore rendering.
- QR bitmap renderer inside the printable QR area.
- Developer-only recording/playback UI.
- Physical two-device alignment tuning.
- Depth occlusion material integration with game objects.
- Smooth visual reconciliation of authoritative object snapshots.

## Regression Verification

Games remains isolated under `feature/games`.

Existing non-Games modules were not restructured. Phase 3 builds on the Phase 1 ARCore foundation and Phase 2 local LAN lobby without making Cloud Anchors mandatory.
