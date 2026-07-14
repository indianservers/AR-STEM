package com.indianservers.ai_stem.feature.games.mission

import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole

enum class MathTopic { Integers, Fractions, Decimals, RatiosPercentages, Algebra, LinearEquations, Angles, GeometryMeasurement, CoordinateGeometry, Trigonometry }
enum class MathSkill {
    IntegerOperations,
    FractionAddSubtract,
    FractionMultiplyDivide,
    DecimalOperations,
    PercentageCalculation,
    RatioSimplification,
    OneStepEquation,
    TwoStepEquation,
    Substitution,
    AngleRelationships,
    TriangleAngleSum,
    Area,
    Perimeter,
    CoordinateIdentification,
    GridDistance,
    SlopeBasics,
    RightTriangleTrigonometry
}
enum class GradeBand { Grade6, Grade7, Grade8, Grade9, Grade10, Mixed }
enum class DifficultyLevel { Beginner, Easy, Medium, Hard, Expert, Adaptive }
enum class QuestionType {
    NumericAnswer,
    IntegerAnswer,
    DecimalAnswer,
    FractionAnswer,
    SimplifiedFractionAnswer,
    MultipleChoice,
    MultipleSelection,
    ExpressionEquivalence,
    EquationSolution,
    CoordinateAnswer,
    OrderedPair,
    AngleAnswer,
    MeasurementAnswer,
    SequenceOrdering,
    DragAndArrange,
    MultiStepTeamAnswer
}
enum class InteractionType { NonArCalculation, ArLocation, ArConstruction, ArMeasurement, TeamMultiDevice }
enum class ValidationStatus { Correct, CorrectButNeedsSimplification, PartiallyCorrect, Incorrect, InvalidFormat, MissingUnit, WrongCoordinateOrder, TimedOut, AlreadySubmitted }
enum class MissionState { NotStarted, Briefing, Active, HintAvailable, AnswerSubmitted, ValidationPending, Correct, IncorrectRetryAllowed, Failed, Completed, Cancelled }
enum class UnitKind { None, Degrees, Metres, SquareMetres, Centimetres, Percent }

data class MathMission(
    val id: String,
    val topic: MathTopic,
    val skill: MathSkill,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val questionType: QuestionType,
    val prompt: MathPrompt,
    val expectedAnswer: AnswerDefinition,
    val solutionSteps: List<SolutionStep>,
    val hints: List<Hint>,
    val misconceptions: List<CommonMisconception>,
    val scoringRule: ScoringRule = ScoringRule(),
    val timeLimitSeconds: Int,
    val interactionType: InteractionType,
    val seed: Long
)

data class MathPrompt(
    val studentText: String,
    val analystClue: String? = null,
    val navigatorClue: String? = null,
    val builderClue: String? = null,
    val commanderClue: String? = null,
    val choices: List<String> = emptyList()
)

sealed interface AnswerDefinition {
    data class IntegerAnswer(val value: Int) : AnswerDefinition
    data class DecimalAnswer(val value: Double, val tolerance: Double = 0.01, val unit: UnitKind = UnitKind.None) : AnswerDefinition
    data class FractionAnswer(val value: Rational, val requireSimplified: Boolean) : AnswerDefinition
    data class MultipleChoiceAnswer(val correctChoiceIds: Set<String>, val allowPartial: Boolean = false) : AnswerDefinition
    data class ExpressionAnswer(val expression: LinearExpression) : AnswerDefinition
    data class EquationAnswer(val variable: String, val value: Rational) : AnswerDefinition
    data class CoordinateAnswer(val x: Rational, val y: Rational) : AnswerDefinition
    data class AngleAnswer(val degrees: Double, val tolerance: Double = 0.5) : AnswerDefinition
    data class MeasurementAnswer(val value: Double, val unit: UnitKind, val tolerance: Double = 0.05) : AnswerDefinition
    data class SequenceAnswer(val orderedIds: List<String>) : AnswerDefinition
    data class MultiStepAnswer(val parts: List<AnswerDefinition>, val requiredCorrectParts: Int = parts.size) : AnswerDefinition
}

data class SolutionStep(val order: Int, val text: String)
data class Hint(val order: Int, val text: String, val unlockAfterSeconds: Int = 20)
data class CommonMisconception(val pattern: String, val feedback: String)

data class ValidationResult(
    val status: ValidationStatus,
    val message: String,
    val scoreMultiplier: Double,
    val correctParts: Int = 0,
    val totalParts: Int = 1,
    val attemptMayRevealSolution: Boolean = false
) {
    val correct: Boolean get() = status == ValidationStatus.Correct
}

data class ScoringRule(
    val correctAnswer: Int = 100,
    val correctProcessSteps: Int = 40,
    val accurateArInteraction: Int = 50,
    val teamParticipation: Int = 30,
    val noHint: Int = 25,
    val fastCompletionMax: Int = 20,
    val correctExplanation: Int = 30,
    val wrongAttemptPenalty: Int = -10,
    val repeatedGuessPenalty: Int = -8
)

data class MissionAttempt(
    val playerId: String,
    val role: ArenaRole,
    val answer: String,
    val submittedAtEpochMs: Long,
    val elapsedSeconds: Int,
    val timedOut: Boolean = false,
    val stepAnswers: List<String> = emptyList()
)

data class RoleClue(
    val role: ArenaRole,
    val clue: String,
    val canSubmit: Boolean,
    val availableActions: List<String>
)

data class MissionConfiguration(
    val topics: Set<MathTopic> = MathTopic.entries.toSet(),
    val gradeBand: GradeBand = GradeBand.Mixed,
    val difficulty: DifficultyLevel = DifficultyLevel.Medium,
    val missionCount: Int = 8,
    val adaptiveDifficulty: Boolean = true,
    val timeLimitSeconds: Int? = null,
    val hintsEnabled: Boolean = true,
    val maxRetries: Int = 2,
    val roleRotation: Boolean = true,
    val explanationRequired: Boolean = false,
    val cooperativeScoring: Boolean = true
)

data class MissionProgress(
    val mission: MathMission,
    val state: MissionState = MissionState.NotStarted,
    val attempts: List<MissionAttempt> = emptyList(),
    val hintCount: Int = 0,
    val usedPlayerIds: Set<String> = emptySet(),
    val lastSubmitterId: String? = null,
    val validationResult: ValidationResult? = null,
    val startedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null
)

data class MissionScore(
    val teamId: String,
    val missionId: String,
    val total: Int,
    val answerPoints: Int,
    val processPoints: Int,
    val arPoints: Int,
    val participationPoints: Int,
    val hintBonus: Int,
    val speedBonus: Int,
    val explanationPoints: Int,
    val penalties: Int,
    val contributionByPlayer: Map<String, Int>,
    val topicMasteryDelta: Map<MathTopic, Double>
)

data class Rational(val numerator: Int, val denominator: Int) : Comparable<Rational> {
    init {
        require(denominator != 0) { "Denominator cannot be zero." }
    }
    val normalized: Rational by lazy {
        val sign = if (denominator < 0) -1 else 1
        val g = gcd(kotlin.math.abs(numerator), kotlin.math.abs(denominator)).coerceAtLeast(1)
        Rational((numerator / g) * sign, kotlin.math.abs(denominator) / g)
    }
    val simplified: Boolean get() = gcd(kotlin.math.abs(numerator), kotlin.math.abs(denominator)) == 1 && denominator > 0
    fun toDouble(): Double = numerator.toDouble() / denominator
    operator fun plus(other: Rational): Rational = Rational(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator).normalized
    operator fun minus(other: Rational): Rational = Rational(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator).normalized
    operator fun times(other: Rational): Rational = Rational(numerator * other.numerator, denominator * other.denominator).normalized
    operator fun div(other: Rational): Rational = Rational(numerator * other.denominator, denominator * other.numerator).normalized
    override fun compareTo(other: Rational): Int = (numerator * other.denominator).compareTo(other.numerator * denominator)
    override fun toString(): String = normalized.let { if (it.denominator == 1) "${it.numerator}" else "${it.numerator}/${it.denominator}" }
}

data class LinearExpression(val coefficient: Rational, val constant: Rational, val variable: String = "x") {
    fun equivalentTo(other: LinearExpression): Boolean =
        variable == other.variable && coefficient.normalized == other.coefficient.normalized && constant.normalized == other.constant.normalized
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
