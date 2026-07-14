# Phase 2 Local Wi-Fi Multiplayer Lobby Completion Report

## Scope Completed

Phase 2 implements the non-AR game shell and reusable local multiplayer lobby foundation for AR Math Arena. The lobby is intentionally outside the AR session path so users can host, join, test networking, configure player identity, and check AR readiness before camera/AR startup.

## User-Facing Shell

- Added direct-user AR Math Arena modes:
  - Play on Local Wi-Fi
  - Host Game
  - Join Game
  - How to Play
  - AR Device Check
  - Local Network Test
  - Teacher Mode entry
  - Accessibility entry
  - Game Settings entry
- Host setup creates a room with a safe six-character room code.
- Host lobby shows players, teams, roles, readiness, lock state, start eligibility, and QR join payload text.
- Join screen supports discovered local rooms plus QR payload text input.
- Local Network Test reports Wi-Fi/LAN status, metered state, active local addresses, and explains that Android `INTERNET` is used only for LAN sockets.
- AR Device Check remains available but does not run by default on lobby landing.

## Reusable Multiplayer Modules

New reusable modules were added under `feature/games/multiplayer`:

- `ArenaMultiplayerModels.kt`
  - Room settings, players, teams, roles, grade/topic/difficulty/mode settings, connection states, code/token generation.
- `ArenaProtocol.kt`
  - Versioned `GameMessage` envelope.
  - Explicit payload types instead of generic maps.
  - Bounded message size.
  - Compact encode/decode path for lobby traffic.
- `ArenaSecurity.kt`
  - Safe display/room name validation.
  - Room-code validation.
  - Join validation.
  - Duplicate/stale/invalid message rejection.
  - Room token enforcement.
- `ArenaLobbyReducer.kt`
  - Immutable host-authoritative state transitions.
  - Join, ready, team assignment, role assignment, auto assign, lock/unlock, disconnect, reconnect, remove, start, shutdown.
  - Rejects unauthorized host-only actions.
- `LocalPlayerProfileStore.kt`
  - Local UUID, display name, avatar seed, accessibility settings, preferred role, last team color.
- `ArenaQrJoinCodec.kt`
  - Strict, temporary QR join payloads with protocol version, room code, host info, port, token, and expiry.

## Local Wi-Fi / LAN Networking

New reusable network modules were added under `feature/games/multiplayer/network`:

- `AndroidNsdArenaDiscovery.kt`
  - Registers and discovers AR Math Arena rooms using Android NSD/mDNS service type `_aistem-arena._tcp.`.
  - Publishes room code, room name, and protocol metadata.
- `ArenaTcpTransport.kt`
  - Real TCP host/client transport over the local network.
  - Newline-delimited protocol messages.
  - Client connect/disconnect/message/error events.
  - Lifecycle close support for sockets and server socket.
- `LocalNetworkInspector.kt`
  - Reports Wi-Fi/LAN transport, metered network, active local addresses, and classroom-friendly network guidance.
- `ArenaNetworkModels.kt`
  - Shared discovery, diagnostics, inbound message, event, and handle types.

## Android Permissions

Added:

- `android.permission.INTERNET`
  - Required by Android for TCP sockets, including same-Wi-Fi LAN sockets. This phase does not add Firebase, a remote server, Cloud Anchors, or any internet backend.
- `android.permission.ACCESS_NETWORK_STATE`
  - Used by the Local Network Test screen.
- `android.permission.CHANGE_WIFI_MULTICAST_STATE`
  - Supports reliable mDNS/NSD discovery on local Wi-Fi.

Existing AR/camera/location permissions remain optional from Phase 1 and earlier AR work.

## Security and Robustness

- Host is authoritative for lobby state.
- Clients cannot lock/start/assign unless the reducer sees host authority.
- Room token is required for joining.
- QR payloads are temporary and version checked.
- Duplicate message IDs are rejected.
- Protocol version mismatch is rejected.
- Room code validation uses a safe six-character format.
- Display names are bounded and sanitized.
- Reconnection token validation is implemented in the reducer.
- Transport handles are closed from the ViewModel lifecycle.

## Tests Added

Added `ArenaMultiplayerCoreTest` covering:

- Protocol round trip for explicit join request payloads.
- Invalid room token rejection.
- Unauthorized host command rejection.
- Duplicate message rejection.
- Team/role/readiness start eligibility.
- Disconnect and reconnection token flow.
- QR payload version/expiry/field validation.
- Real localhost TCP protocol message transfer.

## Verification

Passed:

- `.\gradlew.bat compileDebugKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain`
- `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat assembleDebug --no-daemon --console=plain`

Attempted but blocked by environment:

- `.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain`
  - Result: failed only because no Android devices were connected: `No connected devices!`

## Important Physical Device Follow-Up

The LAN transport is implemented and unit-tested on localhost, but true two-device validation still needs physical Android devices on the same Wi-Fi. Test cases to run manually:

- Host room on device A, discover on device B.
- Join via QR payload with room token.
- Verify host sees player join.
- Verify ready/team/role updates.
- Turn Wi-Fi off/on on one device and validate reconnect UX.
- Test on school Wi-Fi, guest Wi-Fi, and hotspot because some networks block peer discovery or client-to-client sockets.

## Known Phase 2 Boundaries

- Match gameplay is not started yet; this phase owns the shell and lobby.
- QR display is currently text payload, ready for a QR bitmap renderer in a later polish pass.
- Room-code-only join cannot securely bypass the room token; discovery plus QR/token flow is the safe path.
- Player-side full lobby snapshot rendering over TCP is scaffolded through protocol broadcasts, but physical two-device UX still needs live-device verification and iteration.
- Teacher Mode, Accessibility, and Game Settings have entry points and backing models; deeper screens can expand in Phase 3 without changing the network core.
