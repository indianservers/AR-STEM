# Maths AR Tournament Scoring Validation Report

## Purpose

This report documents normalized scoring for Maths AR Tournament Hub.

Raw game scores are not compared directly. Every game round is converted into a transparent 0-100 normalized tournament score.

## Normalized Components

Default weights:

- Accuracy: 0.28
- Completion: 0.22
- Difficulty: 0.16
- Time efficiency: 0.12
- Hint independence: 0.08
- Team participation: 0.08
- Game objective: 0.04
- Safety compliance: 0.02

Formula:

`100 * (0.28*accuracy + 0.22*completion + 0.16*difficulty + 0.12*time + 0.08*hint + 0.08*participation + 0.04*objective + 0.02*safety)`

Each input is clamped to the range 0.0 to 1.0.

## Teacher Weight Controls

Weights are configurable through `TournamentScoreWeights`.

Validation:

- Total must equal 1.0.
- Each individual component must be in a safe range.

## Game Adapter Inputs

All six adapters declare these scoring inputs:

- Accuracy.
- Completion.
- Difficulty.
- Time.
- Hints.
- Participation.
- Objective.
- Safety.

## Tie-Break Logic

Default deterministic order:

1. Higher accuracy.
2. Higher difficulty completed.
3. Fewer hints.
4. Better participation balance.
5. Faster validated completion.
6. Teacher-approved tie-break mission.

Random draw is not used unless explicitly selected.

## Host Authority

Tournament results must be:

- Host validated.
- Confirmed by the event host.
- Unique by result ID.

Client-authoritative final results are rejected.

## Fairness Notes

Accessible modes remain tournament-valid because normalized scoring can compare learning outcomes instead of raw physical speed.

Safety compliance is a small but visible score component. It should not be used to shame teams; it exists to prevent unsafe play from being rewarded.

## Test Evidence

Tests verify:

- Score range.
- Transparent formula.
- Teacher confirmation requirement.
- Duplicate result rejection.
- Host mismatch rejection.
- Deterministic tie-breaks.
- All six adapters expose required rules.

Command passed:

- `.\gradlew.bat testDebugUnitTest --no-daemon --console=plain`
