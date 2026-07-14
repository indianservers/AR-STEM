# Games Module Future Game Developer Guide

## Add A Game

1. Create a feature package under `feature/games/<game>`.
2. Define pure domain models and reducers first.
3. Add a `GamePlugin` with `GameDefinition` and `GameRuntimeContract`.
4. Register it in `GamesCatalog`.
5. Add a route in `GamesNavigation`.
6. Add unit tests for catalog registration, reducers, protocol messages and privacy-sensitive exports.

## Plugin Contract Rules

- IDs must be unique.
- `runtimeContract.gameId` must match `definition.id`.
- player ranges must be valid.
- AR games should explicitly state whether shared origin and host authority are required.

## Architecture Rules

- Keep gameplay reducers pure where possible.
- Keep Android networking in `multiplayer/network`.
- Keep ARCore lifecycle and capability checks in `arcore`.
- Never put cloud-only assumptions in the shared games API.
- Do not make visual performance tiers change simulation fairness.

## Release Checklist

- `compileDebugKotlin`
- `testDebugUnitTest`
- `compileDebugAndroidTestKotlin`
- `assembleDebug`
- physical connected Android tests
- at least one low-end and one high-end real-device AR run
- privacy scan for IP/device/camera identifiers in exports
