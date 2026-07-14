# Maths AR Future Game Developer Guide

## Adding A New Maths AR Game

1. Add a game package under `feature/games`.
2. Register the game in `GamesCatalog`.
3. Define gameplay inputs through shared mission, learning or authoring models.
4. Add tournament adapter support if the game has competitive scoring.
5. Add authoring validation for game-specific constraints.
6. Keep AR dependencies behind capability checks and provide tabletop or non-AR alternatives.
7. Add unit tests for math correctness, safety, privacy and replay behavior.

## Shared Contracts

Use the existing catalog, mission, learning, tournament and authoring packages. Avoid game-specific copies of validation, preview, import or backup behavior unless the game has a genuine domain rule.

