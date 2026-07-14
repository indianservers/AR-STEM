package com.indianservers.ai_stem.feature.games.equationescape

import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.Rational
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole

enum class EscapeMode { SinglePlayer, CooperativeTeam, TimedClassroomChallenge }
enum class EscapeLockType { Arithmetic, Algebra, Equation, Pattern, Logic, Coordinate }
enum class EscapeInteractionCapability { ArMarkerInteraction, SharedRoomState, TimedChallenge, TeamClueDistribution }
enum class EscapeArState { NotStarted, Scanning, AnchorReady, WeakTracking, Paused }

sealed interface EscapeAnswer {
    data class NumberAnswer(val value: Rational) : EscapeAnswer
    data class CoordinateAnswer(val x: Rational, val y: Rational) : EscapeAnswer
    data class OrderedSequenceAnswer(val ids: List<String>) : EscapeAnswer
}

data class EscapeRoomDefinition(
    val roomId: String,
    val title: String,
    val mode: EscapeMode,
    val plannedTopics: Set<MathTopic>,
    val lockIds: List<String>,
    val clueIds: List<String>
)

data class EscapeLockDefinition(
    val lockId: String,
    val type: EscapeLockType,
    val dependsOnClueIds: Set<String>,
    val unlocksClueIds: Set<String>,
    val promptTemplateId: String
)

data class EscapeClueDefinition(
    val clueId: String,
    val title: String,
    val assignedRole: ArenaRole?,
    val visibleAfterLockIds: Set<String>
)

data class EscapePuzzleGraph(
    val roomId: String,
    val nodes: List<String>,
    val directedDependencies: List<Pair<String, String>>
)

data class EscapePuzzle(
    val id: String,
    val title: String,
    val type: EscapeLockType,
    val prompt: String,
    val roleHint: String,
    val expected: EscapeAnswer,
    val dependsOn: Set<String> = emptySet(),
    val points: Int = 100,
    val arPlacementLabel: String
)

data class EscapeLevel(
    val id: String,
    val title: String,
    val story: String,
    val puzzles: List<EscapePuzzle>,
    val finalPuzzleId: String
) {
    init {
        require(puzzles.any { it.id == finalPuzzleId }) { "Final puzzle must exist in the level." }
    }
}

data class EscapeValidationResult(
    val correct: Boolean,
    val message: String,
    val awardedPoints: Int,
    val unlockedPuzzleIds: Set<String>
)

data class EscapeProgress(
    val levelId: String,
    val solvedPuzzleIds: Set<String> = emptySet(),
    val score: Int = 0,
    val attempts: Int = 0,
    val arState: EscapeArState = EscapeArState.NotStarted
) {
    val completed: Boolean get() = solvedPuzzleIds.isNotEmpty()
}

interface EscapePuzzleSequenceEngine {
    fun validateGraph(graph: EscapePuzzleGraph): Result<Unit>
}

class EquationEscapeEngine : EscapePuzzleSequenceEngine {
    val levels: List<EscapeLevel> = buildLevels()

    fun availablePuzzles(level: EscapeLevel, progress: EscapeProgress): List<EscapePuzzle> =
        level.puzzles.filter { puzzle -> puzzle.dependsOn.all { it in progress.solvedPuzzleIds } }

    fun currentPuzzle(level: EscapeLevel, progress: EscapeProgress): EscapePuzzle? =
        availablePuzzles(level, progress).firstOrNull { it.id !in progress.solvedPuzzleIds }

    fun isLevelComplete(level: EscapeLevel, progress: EscapeProgress): Boolean =
        level.finalPuzzleId in progress.solvedPuzzleIds

    fun validate(level: EscapeLevel, progress: EscapeProgress, puzzleId: String, rawAnswer: String): EscapeValidationResult {
        val puzzle = level.puzzles.firstOrNull { it.id == puzzleId }
            ?: return EscapeValidationResult(false, "Puzzle not found.", 0, emptySet())
        if (!puzzle.dependsOn.all { it in progress.solvedPuzzleIds }) {
            return EscapeValidationResult(false, "Unlock the previous clue first.", 0, emptySet())
        }
        val parsed = parseAnswer(rawAnswer, puzzle.expected)
            ?: return EscapeValidationResult(false, "Answer format does not match this lock.", 0, emptySet())
        val correct = answersEqual(parsed, puzzle.expected)
        val unlocked = if (correct) {
            level.puzzles.filter { puzzle.id in it.dependsOn && it.dependsOn.all { dependency -> dependency in progress.solvedPuzzleIds + puzzle.id } }
                .map { it.id }
                .toSet()
        } else {
            emptySet()
        }
        return EscapeValidationResult(
            correct = correct,
            message = if (correct) "Lock opened. New AR clue is available." else "The lock rejects that answer. Check each step.",
            awardedPoints = if (correct) puzzle.points else 0,
            unlockedPuzzleIds = unlocked
        )
    }

    fun apply(progress: EscapeProgress, puzzle: EscapePuzzle, result: EscapeValidationResult): EscapeProgress =
        progress.copy(
            solvedPuzzleIds = if (result.correct) progress.solvedPuzzleIds + puzzle.id else progress.solvedPuzzleIds,
            score = progress.score + result.awardedPoints,
            attempts = progress.attempts + 1,
            arState = if (result.correct) EscapeArState.AnchorReady else progress.arState
        )

    override fun validateGraph(graph: EscapePuzzleGraph): Result<Unit> = runCatching {
        require(graph.nodes.distinct().size == graph.nodes.size) { "Puzzle graph contains duplicate nodes." }
        val known = graph.nodes.toSet()
        graph.directedDependencies.forEach { (from, to) ->
            require(from in known && to in known) { "Puzzle graph dependency references an unknown node." }
        }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun dfs(node: String) {
            if (node in visited) return
            require(node !in visiting) { "Puzzle graph has a cycle." }
            visiting += node
            graph.directedDependencies.filter { it.first == node }.forEach { dfs(it.second) }
            visiting -= node
            visited += node
        }
        graph.nodes.forEach(::dfs)
    }

    fun parseAnswer(raw: String, expected: EscapeAnswer): EscapeAnswer? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return when (expected) {
            is EscapeAnswer.NumberAnswer -> parseRational(trimmed)?.let(EscapeAnswer::NumberAnswer)
            is EscapeAnswer.CoordinateAnswer -> {
                val clean = trimmed.removePrefix("(").removeSuffix(")")
                val parts = clean.split(",").map { it.trim() }
                if (parts.size != 2) null else {
                    val x = parseRational(parts[0])
                    val y = parseRational(parts[1])
                    if (x == null || y == null) null else EscapeAnswer.CoordinateAnswer(x, y)
                }
            }
            is EscapeAnswer.OrderedSequenceAnswer -> EscapeAnswer.OrderedSequenceAnswer(
                trimmed.split(",", ">", "-").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            )
        }
    }

    private fun answersEqual(actual: EscapeAnswer, expected: EscapeAnswer): Boolean = when {
        actual is EscapeAnswer.NumberAnswer && expected is EscapeAnswer.NumberAnswer ->
            actual.value.normalized == expected.value.normalized
        actual is EscapeAnswer.CoordinateAnswer && expected is EscapeAnswer.CoordinateAnswer ->
            actual.x.normalized == expected.x.normalized && actual.y.normalized == expected.y.normalized
        actual is EscapeAnswer.OrderedSequenceAnswer && expected is EscapeAnswer.OrderedSequenceAnswer ->
            actual.ids == expected.ids
        else -> false
    }

    private fun parseRational(value: String): Rational? {
        val cleaned = value.trim().replace(" ", "")
        return when {
            "/" in cleaned -> {
                val parts = cleaned.split("/")
                if (parts.size != 2) null else parts[0].toIntOrNull()?.let { numerator ->
                    parts[1].toIntOrNull()?.takeIf { it != 0 }?.let { Rational(numerator, it).normalized }
                }
            }
            "." in cleaned -> cleaned.toDoubleOrNull()?.let { decimal ->
                val scale = 1000
                Rational((decimal * scale).toInt(), scale).normalized
            }
            else -> cleaned.toIntOrNull()?.let { Rational(it, 1) }
        }
    }

    private fun buildLevels(): List<EscapeLevel> = listOf(
        EscapeLevel(
            id = "algebra-lab",
            title = "Algebra Laboratory",
            story = "Equation panels float on shelves. Solve each AR lock to power the final exit prism.",
            finalPuzzleId = "alg-final",
            puzzles = listOf(
                EscapePuzzle("alg-1", "Number Door", EscapeLockType.Arithmetic, "Find 18 - 7 + 4.", "Tap the door panel beside the desk.", EscapeAnswer.NumberAnswer(Rational(15, 1)), arPlacementLabel = "Desk door"),
                EscapePuzzle("alg-2", "Balance Beam", EscapeLockType.Equation, "Solve 2x + 6 = 20.", "The beam stays level only for the correct x.", EscapeAnswer.NumberAnswer(Rational(7, 1)), dependsOn = setOf("alg-1"), arPlacementLabel = "Balance beam"),
                EscapePuzzle("alg-3", "Fraction Fuse", EscapeLockType.Algebra, "Solve x/3 + 2 = 7.", "Fuse value is the variable.", EscapeAnswer.NumberAnswer(Rational(15, 1)), dependsOn = setOf("alg-2"), arPlacementLabel = "Wall fuse"),
                EscapePuzzle("alg-4", "Pattern Key", EscapeLockType.Pattern, "Sequence: 3, 6, 12, 24, ?", "The AR key doubles its glow.", EscapeAnswer.NumberAnswer(Rational(48, 1)), dependsOn = setOf("alg-3"), arPlacementLabel = "Key stand"),
                EscapePuzzle("alg-5", "Coordinate Drawer", EscapeLockType.Coordinate, "Move 2 right and 3 up from (1, 1).", "Enter the ordered pair.", EscapeAnswer.CoordinateAnswer(Rational(3, 1), Rational(4, 1)), dependsOn = setOf("alg-4"), arPlacementLabel = "Drawer grid"),
                EscapePuzzle("alg-final", "Exit Prism", EscapeLockType.Logic, "Order the clues by operation strength: multiply, add, solve.", "Use M,A,S.", EscapeAnswer.OrderedSequenceAnswer(listOf("M", "A", "S")), dependsOn = setOf("alg-5"), points = 180, arPlacementLabel = "Exit prism")
            )
        ),
        EscapeLevel(
            id = "geometry-temple",
            title = "Geometry Temple",
            story = "Stone runes become AR measurement locks for perimeter, area and angle reasoning.",
            finalPuzzleId = "geo-final",
            puzzles = listOf(
                EscapePuzzle("geo-1", "Perimeter Gate", EscapeLockType.Arithmetic, "A rectangle is 8 m by 5 m. Perimeter?", "Walk the glowing rectangle edge.", EscapeAnswer.NumberAnswer(Rational(26, 1)), arPlacementLabel = "Floor gate"),
                EscapePuzzle("geo-2", "Area Tile", EscapeLockType.Arithmetic, "Area of a 9 m by 4 m rectangle?", "Count the tile rows.", EscapeAnswer.NumberAnswer(Rational(36, 1)), dependsOn = setOf("geo-1"), arPlacementLabel = "Tile floor"),
                EscapePuzzle("geo-3", "Angle Rune", EscapeLockType.Equation, "Triangle angles are 50, 60 and x degrees. x?", "Aim at the missing vertex.", EscapeAnswer.NumberAnswer(Rational(70, 1)), dependsOn = setOf("geo-2"), arPlacementLabel = "Triangle rune"),
                EscapePuzzle("geo-4", "Scale Pillar", EscapeLockType.Algebra, "A model length 6 cm uses scale 1 cm = 2 m. Real length?", "The pillar grows by scale.", EscapeAnswer.NumberAnswer(Rational(12, 1)), dependsOn = setOf("geo-3"), arPlacementLabel = "Scale pillar"),
                EscapePuzzle("geo-5", "Grid Torch", EscapeLockType.Coordinate, "Midpoint of (2, 2) and (8, 6).", "Enter the midpoint.", EscapeAnswer.CoordinateAnswer(Rational(5, 1), Rational(4, 1)), dependsOn = setOf("geo-4"), arPlacementLabel = "Grid torch"),
                EscapePuzzle("geo-final", "Temple Seal", EscapeLockType.Logic, "Order by dimension: line, area, volume. Use L,A,V.", "Seal opens from 1D to 3D.", EscapeAnswer.OrderedSequenceAnswer(listOf("L", "A", "V")), dependsOn = setOf("geo-5"), points = 180, arPlacementLabel = "Temple seal")
            )
        ),
        EscapeLevel(
            id = "coordinate-station",
            title = "Coordinate Space Station",
            story = "A room-scale coordinate grid helps repair the station by solving plotted AR locks.",
            finalPuzzleId = "coord-final",
            puzzles = listOf(
                EscapePuzzle("coord-1", "Docking Pin", EscapeLockType.Coordinate, "Reflect (4, -3) across the x-axis.", "Pin moves across the x-axis.", EscapeAnswer.CoordinateAnswer(Rational(4, 1), Rational(3, 1)), arPlacementLabel = "Docking pin"),
                EscapePuzzle("coord-2", "Slope Cable", EscapeLockType.Algebra, "Slope from (1, 2) to (5, 10).", "Trace rise over run.", EscapeAnswer.NumberAnswer(Rational(2, 1)), dependsOn = setOf("coord-1"), arPlacementLabel = "Cable line"),
                EscapePuzzle("coord-3", "Distance Hatch", EscapeLockType.Arithmetic, "Horizontal distance from x=-2 to x=7.", "Count grid units.", EscapeAnswer.NumberAnswer(Rational(9, 1)), dependsOn = setOf("coord-2"), arPlacementLabel = "Hatch"),
                EscapePuzzle("coord-4", "Equation Relay", EscapeLockType.Equation, "Solve 3x - 9 = 12.", "Relay needs x.", EscapeAnswer.NumberAnswer(Rational(7, 1)), dependsOn = setOf("coord-3"), arPlacementLabel = "Relay"),
                EscapePuzzle("coord-5", "Translation Beacon", EscapeLockType.Coordinate, "Translate (2, 5) by (-3, 4).", "Enter the new beacon.", EscapeAnswer.CoordinateAnswer(Rational(-1, 1), Rational(9, 1)), dependsOn = setOf("coord-4"), arPlacementLabel = "Beacon"),
                EscapePuzzle("coord-final", "Station Core", EscapeLockType.Logic, "Order transformations: translate, reflect, rotate. Use T,F,R.", "Core accepts the exact sequence.", EscapeAnswer.OrderedSequenceAnswer(listOf("T", "F", "R")), dependsOn = setOf("coord-5"), points = 180, arPlacementLabel = "Station core")
            )
        )
    )
}
