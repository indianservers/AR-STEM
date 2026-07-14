# AR Math Arena Architecture

## Module Shape

AR Math Arena lives under `app/src/main/java/com/indianservers/ai_stem/feature/games` and is isolated from the older AR viewer. The module is split by responsibility:

- `api`: game catalog contracts, runtime contracts and plugin registration.
- `catalog`: registered game plugins and future game slots.
- `navigation`: Compose navigation entry points.
- `matharena`: screen and ViewModel orchestration.
- `arcore`: AR availability, session configuration and feature policy.
- `spatial`: shared origin, calibration, surface fallback and anchor transform models.
- `multiplayer`: local room state, protocol payloads, QR join, validation and guards.
- `multiplayer/network`: Android NSD discovery, TCP transport and network inspection.
- `mission`: math mission generation, validation, role clues and scoring.
- `basedefense`: AR Math Base Defence match state and reducer.
- `classroom`: local dashboard, accessibility, safety, analytics and export models.
- `performance`: renderer tier budgets with fairness-preserving simulation ticks.
- `recovery`: recovery snapshot codec for resumable match metadata.
- `diagnostics`: AR and device readiness reporting.

## Public Extension Contract

Future games register through `GamePlugin`, `GameRuntimeContract` and `GameNavigationRegistry`. The app shell reads `GamesCatalog.games`; game-specific runtime behavior remains behind each plugin contract. AR Math Arena declares a host-authoritative, shared-origin, local-Wi-Fi runtime contract.

## Runtime Flow

1. Player opens Games Library.
2. AR Math Arena launches through `GamesNavigation`.
3. Host creates a local room; QR text and NSD discovery carry LAN join data.
4. Players join, choose roles, become ready.
5. Shared AR origin is calibrated by printed marker or surface fallback.
6. Host starts mission/base-defence state.
7. Reducers own authoritative state; AR renderers consume shared transforms.
8. Local analytics/export is generated without cloud services.

## Production Boundaries

Camera frames stay on device. LAN traffic is local TCP/NSD, not a cloud backend. Physical AR tracking quality and two-device LAN behavior still require real-device validation before classroom rollout.
