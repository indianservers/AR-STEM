# Maths AR Content Pack Security

## Import Checks

Content pack import validation rejects:

- packs larger than `5,000,000` bytes.
- entry counts outside `1..1000`.
- path traversal such as `../` and `..\`.
- executable or script signatures such as script tags, runtime execution, shared libraries or executable extensions.
- external URLs outside approved local, school, example or map-source patterns.
- missing checksum metadata.

## Conflict Handling

Incoming versions are compared with existing versions. Locally modified content imports as a copy by default, newer content requires confirmation, older content keeps existing data, and identical content is skipped.

