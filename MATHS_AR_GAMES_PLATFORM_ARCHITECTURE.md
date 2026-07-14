# Maths AR Games Platform Architecture

## Module Shape

The Maths AR games platform is split into reusable layers:

- `api`: public game definitions, AR requirements, availability and hero metadata.
- `catalog`: the central game registry consumed by Games Library, tournaments and authoring.
- `mission`: shared math mission primitives.
- `arcore`, `spatial`, `multiplayer`: shared AR session policy, origin, sync and safety foundations.
- game packages: Arena, Equation Escape, Fraction Factory, Geometry Architect, Coordinate Conquest and Math Expedition.
- `learning`: curriculum, procedural generation, mastery and campaigns.
- `tournament`: event creation, scoring, brackets, spectators, replay and recovery.
- `authoring`: Teacher Authoring Studio models, validation, preview, content packs, backup and diagnostics.

## Authoring Flow

Teacher Authoring Studio creates local content objects, validates them, previews them without awarding progress, and packages them for import/export. Production game engines remain the source of gameplay behavior; authoring creates playable inputs for those engines.

## Safety Boundaries

Outdoor and geospatial routes remain approval-gated. Content packs reject unsafe paths, executable signatures, oversized archives and unapproved external URLs. Diagnostics exclude raw camera imagery, precise location history, display names and IP history by default.

