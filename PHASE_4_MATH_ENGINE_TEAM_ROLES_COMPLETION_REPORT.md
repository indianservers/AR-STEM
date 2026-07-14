# Phase 4 Math Engine and Team Roles Completion Report

## Summary

Phase 4 adds an AR-independent mathematics mission engine for AR Math Arena and future Games. The engine generates deterministic missions, validates mathematical answers structurally, distributes role-specific clues, enforces anti-domination rules, runs a host-authoritative mission state machine, calculates scores, and exposes a non-AR mission test screen for teacher/developer verification.

The full Base Defence battle is still deferred.

## Topics Implemented

- Integers
- Fractions
- Decimals
- Ratios and percentages
- Basic algebra
- Linear equations
- Angles
- Area and perimeter
- Coordinate geometry
- Basic right-triangle trigonometry

## Generators Implemented

`MathMissionGenerator` implements deterministic seeded generators for:

1. Integer operations
2. Fraction addition/subtraction
3. Fraction multiplication/division
4. Decimal operations
5. Percentage calculations
6. Ratio simplification
7. One-step equations
8. Two-step equations
9. Substitution
10. Angle relationships
11. Triangle-angle sum
12. Rectangle/triangle area
13. Perimeter
14. Coordinate identification
15. Distance on coordinate grids
16. Slope basics
17. Right-triangle trigonometry

Each mission includes:

- Prompt
- Expected answer
- Worked solution steps
- Hints
- Common misconception feedback
- Time limit
- Interaction type
- Seed

## Validation Rules

`AnswerValidationEngine` supports:

- Exact integers
- Decimal tolerance
- Rational equivalence
- Simplification enforcement
- Unit handling
- Coordinate order detection
- Degree notation
- Safe linear expression equivalence
- Equation solutions
- Multi-part answers
- Partial correctness
- Duplicate/timed-out submissions

Implemented structured statuses:

- Correct
- CorrectButNeedsSimplification
- PartiallyCorrect
- Incorrect
- InvalidFormat
- MissingUnit
- WrongCoordinateOrder
- TimedOut
- AlreadySubmitted

## Seed Strategy

Generation is deterministic by `(skill, seed, grade, difficulty)`. The host can reproduce missions locally and remains authoritative for mission creation and validation.

## Role Design

`RoleClueDistributor` distributes different information by role:

- Navigator: AR location/navigation clue
- Solver: core prompt and submit permission
- Builder: construction/placement instruction
- Analyst: formula, pattern, or data clue
- Commander: resource/strategy clue

The system does not show all information to every player by default.

## Anti-Domination Design

`AntiDominationEngine` implements:

- Required role participation
- Rotating submitter / no immediate repeat submitter
- Per-player action tokens
- Team participation bonus
- Inactivity detection
- Teacher override configuration model

Tracking is limited to gameplay participation events, not invasive surveillance.

## Mission Flow

`MissionStateMachine` supports:

- NotStarted
- Briefing
- Active
- HintAvailable
- AnswerSubmitted path through validation
- Correct
- IncorrectRetryAllowed
- Failed
- Completed
- Cancelled

Host authorization is required for state transitions. Clients cannot mark missions correct locally.

## Scoring System

`MissionScoringEngine` calculates host-side score:

- Correct answer
- Correct process steps
- AR interaction accuracy
- Team participation
- No-hint bonus
- Fast completion bonus
- Explanation bonus
- Wrong-attempt penalty
- Repeated-guess penalty
- Individual contribution map
- Topic mastery delta

Scores are designed for constructive feedback and avoid public shaming mechanics.

## Question Bank Storage

Added:

- `MissionTemplateStore`
- `MissionTemplateMetadataCodec`

Storage is local/offline via SharedPreferences with versioned mission metadata. This supports future curated mission templates and content packs without a remote server.

## Teacher / Developer Test Screen

Added `MissionTest` mode inside AR Math Arena:

- Generate missions without AR.
- Select sample skills.
- View hints.
- View solution steps.
- View role-specific clues.
- Submit test answer.
- Validate answer through the domain engine.
- Show host-calculated score.

This is suitable for teacher/developer verification and does not place math validation inside UI code.

## Network Protocol

Extended `GamePayload` with mission messages:

- MissionCreated
- MissionAssigned
- RoleClueAssigned
- MissionStarted
- HintRequested
- HintApproved
- AnswerSubmitted
- AnswerValidated
- MissionStateChanged
- ScoreUpdated
- ParticipationUpdated
- MissionCompleted

The host remains authoritative for generated missions, validation, mission state, and scoring.

## Files Created

- `app/src/main/java/com/indianservers/ai_stem/feature/games/mission/MathMissionModels.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/mission/AnswerValidationEngine.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/mission/MathMissionGenerator.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/mission/TeamRoleAndMissionEngines.kt`
- `app/src/test/java/com/indianservers/ai_stem/ArenaMathMissionEngineTest.kt`

## Files Modified

- `app/src/main/java/com/indianservers/ai_stem/feature/games/multiplayer/ArenaProtocol.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/matharena/ArenaLobbyViewModel.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/games/matharena/ArMathArenaScreen.kt`

## Tests and Statistical Checks

`ArenaMathMissionEngineTest` covers:

- 100 deterministic seeds per generator.
- Repeatable generation.
- No invalid denominators.
- Generated expected answers validate.
- Fraction equivalence.
- Simplification enforcement.
- Decimal tolerance.
- Coordinate validation and reversed-order detection.
- Angle validation.
- Unit handling.
- Difficulty differentiation.
- Role clue separation.
- Anti-domination repeat submitter blocking.
- Participation scoring.
- Host-authoritative mission state transitions.
- Scoring.
- Offline template metadata versioning.
- Mission network serialization.

## Verification Results

Passed:

- `.\gradlew.bat compileDebugKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain`
- `.\gradlew.bat compileDebugAndroidTestKotlin --no-daemon --console=plain --stacktrace`
- `.\gradlew.bat assembleDebug --no-daemon --console=plain`

Attempted:

- `.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain`

Result:

- Blocked by environment: `No connected devices!`

## Known Mathematical Limitations

- Expression equivalence is intentionally limited to safe linear expressions.
- Trigonometry uses rounded degree answers for classroom-friendly basic trig.
- Multiple-choice distractor generation is modeled but not yet expanded into a full authored distractor bank.
- Step validation is supported through multi-part answers, but free-form proof/explanation grading is deferred.
- Adaptive difficulty has configuration support; a live adaptive scheduler is deferred.

## Deferred Phase 5 Work

- Full Base Defence battle loop.
- AR mission object placement tied to shared transforms.
- Live team resource economy.
- Expanded content-pack import/export.
- Rich multiple-choice distractors.
- Teacher dashboard for mission set curation.
- Adaptive scheduler using topic mastery deltas.
- Explanation-quality rubric UI.

## Regression Verification

Games remains isolated under `feature/games`. The math mission engine is independent of AR rendering and does not add validation logic to composables, activities, or renderer code. Existing app modules were not restructured.
