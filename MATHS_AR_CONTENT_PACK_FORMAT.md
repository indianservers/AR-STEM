# Maths AR Content Pack Format

## Version

Current authoring schema: `1`.

## Logical Contents

- metadata: pack id, title, version, author, minimum app version, attribution, capabilities and checksum.
- missions: authored playable math missions.
- campaigns: chapter and skill progression definitions.
- game presets: reusable game configuration.
- tournament presets: scoring and event configuration.
- expedition routes: teacher-approved route definitions.
- asset references: local or approved asset paths.
- taxonomy additions: curriculum skill extensions.

## Export

The current local export path produces a deterministic integrity-bearing payload with content counts and checksum. Rich archive serialization can be layered on this schema without changing the validator contract.

