# Coordinate Conquest AR Architecture Foundation

## Concept
Coordinate Conquest AR is planned as a safe-movement AR grid game for plotting points, vectors, transformations and graph territories.

## Stable ID
`coordinate_conquest_ar`

## Planned Modes
Solo Practice, Team Territory Game, Cooperative Coordinate Mission.

## Mathematics Scope
Coordinates, quadrants, distance, midpoint, slope, vectors, transformations and graphs.

## Reusable Games Systems
Games catalog, AR capability checker, shared marker origin, shared coordinate transforms, local Wi-Fi teams, accessibility and safety guidance.

## Game-Specific Domain Models
`feature/games/coordinateconquest` defines grid scale, point/line missions, territory zones, capture rules, transformation missions and vector movement.

## ARCore Requirements
Shared marker, floor plane and safe movement area validation in future gameplay.

## Network Requirements
Team territory mode can reuse local Wi-Fi later. No fake shared grid exists.

## Permissions
No new permissions beyond camera in a future AR phase.

## Safety
Walk only, clear floor, no stairs, maintain awareness of surroundings.

## Future Phases
Grid calibration, mission generator, capture validation, team territory sync, movement safety HUD.

## Not Implemented
No fake floor grid, beacons, territory capture, scoring or AR scene.
