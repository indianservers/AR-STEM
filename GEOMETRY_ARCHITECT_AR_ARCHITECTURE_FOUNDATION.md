# Geometry Architect AR Architecture Foundation

## Concept
Geometry Architect AR is planned as a design game where students construct mathematically valid AR structures on desks, floors or classroom surfaces.

## Stable ID
`geometry_architect_ar`

## Planned Modes
Single Designer, Collaborative Team, Classroom Design Competition.

## Mathematics Scope
Geometry, area, perimeter, surface area, volume, scale and transformations.

## Reusable Games Systems
Games catalog, AR capability checker, plane/surface policy, shared transforms, math validation, accessibility and local analytics.

## Game-Specific Domain Models
`feature/games/geometryarchitect` defines design briefs, vertices, edges, shape constraints, scale, material budget, validation result and measurement service contract.

## ARCore Requirements
Plane detection, optional depth and surface measurement in a future gameplay phase.

## Network Requirements
Optional collaboration can reuse local Wi-Fi later. No fake shared editor is exposed.

## Permissions
No new permissions beyond existing camera path for future AR.

## Safety
Use stable surfaces, avoid table edges, no backward walking while viewing AR.

## Future Phases
Measurement engine, construction editor, constraint validation, collaborative review, design scoring.

## Not Implemented
No fake geometry editor, construction tools, scores, generated structures or AR scene.
