# AR Math Arena ARCore Implementation

## Device Capability

ARCore is optional in the manifest so unsupported devices can still open the app and use non-AR roles. The diagnostic path reports ARCore availability, camera permission, renderer readiness and session status.

## Session Modes

The module separates AR responsibilities from game state:

- marker-based shared origin for classroom alignment.
- surface fallback when printed marker calibration is unavailable.
- non-AR analyst role when a player cannot use AR safely.
- device diagnostics before match start.

## Shared Coordinate Model

The shared origin uses `SharedOriginDefinition`, `SharedTransform`, `SharedAnchorRecord`, `SpatialSnapshot` and reducer commands. Host origin changes increment origin version and anchor records carry the version they belong to.

## Rendering Contract

AR renderers should consume domain objects from `spatial`, `mission` and `basedefense` rather than owning gameplay state. `ArenaPerformancePolicy` gives tier budgets for visual load while preserving the same simulation tick rate across devices.

## Known Validation Gap

Code compiles and pure spatial tests pass, but physical AR validation is pending because no Android device was connected during Phase 7. Required before production rollout:

- printed marker detection on at least two devices.
- shared-origin alignment drift measurement.
- surface fallback classroom calibration.
- ARCore lifecycle pause/resume on real devices.
- depth/occlusion behavior on supported and unsupported devices.
