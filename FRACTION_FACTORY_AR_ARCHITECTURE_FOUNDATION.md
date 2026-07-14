# Fraction Factory AR Architecture Foundation

## Concept
Fraction Factory AR is planned as a tabletop AR production game where students combine quantities, convert units and inspect products.

## Stable ID
`fraction_factory_ar`

## Planned Modes
Single Factory Operator, Team Production Line, Timed Order Challenge.

## Mathematics Scope
Fractions, decimals, ratios, percentages, proportion and unit conversion.

## Reusable Games Systems
Games catalog, math mission/rational models, answer validation patterns, AR capability checker, accessibility and analytics.

## Game-Specific Domain Models
`feature/games/fractionfactory` defines production orders, ingredient quantities, containers, machines, production results and quality inspection contracts.

## ARCore Requirements
Tabletop plane detection and object manipulation in a future phase.

## Network Requirements
Optional team production line may reuse local Wi-Fi later.

## Permissions
No new permissions. Camera is only needed when real tabletop AR gameplay is implemented.

## Safety
Clear tabletop, steady hands, no real liquids or materials required.

## Future Phases
Order generator, machine interactions, rational validation, inspection feedback, team workflow.

## Not Implemented
No fake machines, production line, score, AR factory or active order gameplay.
