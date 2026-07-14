# Equation Escape AR Architecture Foundation

## Concept
Equation Escape AR turns a safe physical room into a planned AR escape room with mathematical locks, hidden clues, number machines and coordinate puzzles.

## Stable ID
`equation_escape_ar`

## Planned Modes
Single Player, Cooperative Team, Timed Classroom Challenge.

## Mathematics Scope
Arithmetic, algebra, equations, patterns, logic and coordinates.

## Reusable Games Systems
Games catalog, profile, optional local Wi-Fi lobby, team roles, AR capability checker, shared marker transforms, math mission engine, accessibility and analytics.

## Game-Specific Domain Models
`feature/games/equationescape` defines room, lock, clue, puzzle graph and sequence-engine contracts.

## ARCore Requirements
Planned marker interactions and room scanning. No AR scene is implemented in this foundation.

## Network Requirements
Optional team clue distribution can reuse local Wi-Fi later. No fake lobby is exposed.

## Permissions
No new permissions. Camera is only required when real AR gameplay is implemented.

## Safety
Walk only, clear room, no moving furniture without adult approval.

## Future Phases
Puzzle authoring, dependency validation, AR clue placement, answer validation, timed/cooperative modes.

## Not Implemented
No playable puzzles, AR locks, fake rooms, scores or escape timers.
