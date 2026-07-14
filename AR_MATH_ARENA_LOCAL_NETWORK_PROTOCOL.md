# AR Math Arena Local Network Protocol

## Transport

The local multiplayer path uses Android NSD for room discovery and TCP sockets for message transport. The protocol version is `ARENA_PROTOCOL_VERSION = 1`, and each packet uses `ArenaProtocolCodec`.

## Message Envelope

Every `GameMessage` includes:

- `protocolVersion`
- `messageId`
- `roomId`
- `senderPlayerId`
- `sequence`
- `timestampEpochMs`
- `payload`

The codec caps raw message size with `MAX_ARENA_MESSAGE_BYTES`.

## Join Security

Join requests require the six-character room code plus the temporary room token encoded in the QR payload. The reducer validates duplicate names, room capacity, room lock state and token match.

## Phase 7 Hardening

`ArenaProtocolGuard` adds:

- per-sender rate limiting.
- duplicate `messageId` rejection.
- monotonic sequence enforcement per sender.
- malformed packet rejection through `decodeAndInspect`.
- explicit host-authority checks for snapshots, match start, mission state, score updates, spatial origin changes and base-defence state.

Client-origin payloads are limited to join, ready, role selection, heartbeat, calibration observations, hint requests, answer submissions and participation updates.

## Privacy

The protocol does not require internet servers, Firebase, Cloud Anchors or camera uploads. Room tokens and reconnect tokens are match-scoped. Recovery snapshots intentionally avoid IP addresses, Android IDs and camera identifiers.
