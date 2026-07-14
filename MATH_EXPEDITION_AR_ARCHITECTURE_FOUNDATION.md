# Math Expedition AR Architecture Foundation

## Concept
Math Expedition AR is planned as a teacher-approved outdoor mathematics route with checkpoints, map tasks and optional AR missions.

## Stable ID
`math_expedition_ar`

## Planned Modes
Solo Explorer, Team Expedition, Campus Survey, Mathematics Treasure Route.

## Mathematics Scope
Distance, scale, bearing, coordinates, speed, time, area, route optimisation, estimation and statistics.

## Reusable Games Systems
Games catalog, math mission engine, accessibility, analytics and future optional ARCore mission placement.

## Game-Specific Domain Models
`feature/games/mathexpedition` defines expedition, route, checkpoint, safe boundary, no-go zone, map coordinate, local mission coordinate, route approval, checkpoint verification and offline-map package metadata.

## ARCore Requirements
Optional checkpoint AR missions and optional Geospatial enhancement in a future phase.

## Network Requirements
Team expedition can reuse local networking later. No map or GPS gameplay is active.

## Permissions
No location permission is requested in this foundation. Location permission must be added only in the real map/GPS implementation phase with runtime education and safety controls.

## Map Dependency And Licensing
Future implementation should evaluate MapLibre or another native OpenStreetMap-compatible renderer. Offline map packages must document provider terms, attribution and ODbL/licensing implications.

## Safety
Teacher-approved routes only, walking mode, no roads, no-go zones, maximum play boundary, adult supervision and stop when surroundings are unsafe.

## Future Phases
Map renderer selection, offline packages, route authoring, checkpoint verification, AR mission handoff, route summary analytics.

## Not Implemented
No fake map, GPS location, route, checkpoint gameplay, AR mission or release date.
