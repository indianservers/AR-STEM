package com.indianservers.ai_stem.feature.games.mission

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

object MathMissionGenerator {
    fun generate(skill: MathSkill, seed: Long, gradeBand: GradeBand, difficulty: DifficultyLevel): MathMission =
        when (skill) {
            MathSkill.IntegerOperations -> integerOperations(seed, gradeBand, difficulty)
            MathSkill.FractionAddSubtract -> fractionAddSubtract(seed, gradeBand, difficulty)
            MathSkill.FractionMultiplyDivide -> fractionMultiplyDivide(seed, gradeBand, difficulty)
            MathSkill.DecimalOperations -> decimalOperations(seed, gradeBand, difficulty)
            MathSkill.PercentageCalculation -> percentage(seed, gradeBand, difficulty)
            MathSkill.RatioSimplification -> ratio(seed, gradeBand, difficulty)
            MathSkill.OneStepEquation -> oneStepEquation(seed, gradeBand, difficulty)
            MathSkill.TwoStepEquation -> twoStepEquation(seed, gradeBand, difficulty)
            MathSkill.Substitution -> substitution(seed, gradeBand, difficulty)
            MathSkill.AngleRelationships -> angleRelationship(seed, gradeBand, difficulty)
            MathSkill.TriangleAngleSum -> triangleAngle(seed, gradeBand, difficulty)
            MathSkill.Area -> area(seed, gradeBand, difficulty)
            MathSkill.Perimeter -> perimeter(seed, gradeBand, difficulty)
            MathSkill.CoordinateIdentification -> coordinate(seed, gradeBand, difficulty)
            MathSkill.GridDistance -> gridDistance(seed, gradeBand, difficulty)
            MathSkill.SlopeBasics -> slope(seed, gradeBand, difficulty)
            MathSkill.RightTriangleTrigonometry -> trigonometry(seed, gradeBand, difficulty)
        }

    fun generateSequence(config: MissionConfiguration, seed: Long): List<MathMission> {
        val skills = MathSkill.entries.filter { it.topic() in config.topics }
        return (0 until config.missionCount.coerceIn(1, 40)).map { index ->
            generate(skills[index % skills.size], seed + index * 10_007L, config.gradeBand, config.difficulty)
        }
    }

    private fun integerOperations(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val range = difficulty.range()
        val a = r.nextInt(-range, range + 1).avoidZero()
        val b = r.nextInt(-range, range + 1).avoidZero()
        val op = if (difficulty.ordinal >= DifficultyLevel.Medium.ordinal) listOf("+", "-", "×").random(r) else listOf("+", "-").random(r)
        val answer = when (op) { "+" -> a + b; "-" -> a - b; else -> a * b }
        return mission(seed, MathTopic.Integers, MathSkill.IntegerOperations, grade, difficulty, QuestionType.IntegerAnswer,
            "Compute: $a $op $b", AnswerDefinition.IntegerAnswer(answer),
            listOf("Use the sign rules first.", "Then perform the operation."), listOf("Mistake: ignoring a negative sign."))
    }

    private fun fractionAddSubtract(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val maxD = difficulty.denominator()
        val a = Rational(r.nextInt(1, maxD), r.nextInt(2, maxD + 1))
        val b = Rational(r.nextInt(1, maxD), r.nextInt(2, maxD + 1))
        val add = r.nextBoolean()
        val answer = if (add) a + b else a - b
        val op = if (add) "+" else "-"
        return mission(seed, MathTopic.Fractions, MathSkill.FractionAddSubtract, grade, difficulty, QuestionType.SimplifiedFractionAnswer,
            "Calculate and simplify: $a $op $b", AnswerDefinition.FractionAnswer(answer, requireSimplified = true),
            listOf("Find a common denominator.", "Combine numerators.", "Simplify the result."), listOf("Common error: adding denominators."))
    }

    private fun fractionMultiplyDivide(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val maxD = difficulty.denominator()
        val a = Rational(r.nextInt(1, maxD), r.nextInt(2, maxD + 1))
        val b = Rational(r.nextInt(1, maxD), r.nextInt(2, maxD + 1))
        val divide = difficulty.ordinal >= DifficultyLevel.Medium.ordinal && r.nextBoolean()
        val answer = if (divide) a / b else a * b
        val op = if (divide) "÷" else "×"
        return mission(seed, MathTopic.Fractions, MathSkill.FractionMultiplyDivide, grade, difficulty, QuestionType.SimplifiedFractionAnswer,
            "Calculate and simplify: $a $op $b", AnswerDefinition.FractionAnswer(answer, true),
            listOf(if (divide) "Multiply by the reciprocal." else "Multiply numerators and denominators.", "Simplify."), listOf("Common error: not flipping the divisor."))
    }

    private fun decimalOperations(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val scale = if (difficulty.ordinal >= DifficultyLevel.Hard.ordinal) 100.0 else 10.0
        val a = r.nextInt(10, 250) / scale
        val b = r.nextInt(5, 120) / scale
        val multiply = difficulty.ordinal >= DifficultyLevel.Medium.ordinal && r.nextBoolean()
        val answer = if (multiply) a * b else a + b
        val op = if (multiply) "×" else "+"
        return mission(seed, MathTopic.Decimals, MathSkill.DecimalOperations, grade, difficulty, QuestionType.DecimalAnswer,
            "Compute: ${a.clean()} $op ${b.clean()}", AnswerDefinition.DecimalAnswer(answer, tolerance = 0.01),
            listOf("Line up decimal places for addition.", "Estimate to check reasonableness."), listOf("Common error: misplaced decimal point."))
    }

    private fun percentage(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val percent = listOf(5, 10, 12, 15, 20, 25, 30, 40, 50).random(r) + if (difficulty.ordinal >= DifficultyLevel.Hard.ordinal) r.nextInt(0, 6) else 0
        val base = r.nextInt(8, difficulty.range() * 6).coerceAtLeast(10)
        val answer = base * percent / 100.0
        return mission(seed, MathTopic.RatiosPercentages, MathSkill.PercentageCalculation, grade, difficulty, QuestionType.DecimalAnswer,
            "Find $percent% of $base.", AnswerDefinition.DecimalAnswer(answer, tolerance = 0.01),
            listOf("Convert percent to a decimal.", "Multiply by the whole."), listOf("Common error: using percent as a whole number."))
    }

    private fun ratio(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val a = r.nextInt(2, difficulty.range())
        val b = r.nextInt(2, difficulty.range())
        val factor = r.nextInt(2, 9)
        val x = a * factor
        val y = b * factor
        return mission(seed, MathTopic.RatiosPercentages, MathSkill.RatioSimplification, grade, difficulty, QuestionType.FractionAnswer,
            "Simplify the ratio $x:$y. Enter as a fraction x/y.", AnswerDefinition.FractionAnswer(Rational(a, b), true),
            listOf("Find the greatest common factor.", "Divide both parts by the same number."), listOf("Common error: dividing only one side."))
    }

    private fun oneStepEquation(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val x = r.nextInt(-difficulty.range(), difficulty.range()).avoidZero()
        val add = r.nextInt(-12, 13).avoidZero()
        val rhs = x + add
        return mission(seed, MathTopic.LinearEquations, MathSkill.OneStepEquation, grade, difficulty, QuestionType.EquationSolution,
            "Solve for x: x ${if (add >= 0) "+" else "-"} ${abs(add)} = $rhs", AnswerDefinition.EquationAnswer("x", Rational(x, 1)),
            listOf("Undo addition or subtraction.", "Keep the equation balanced."), listOf("Common error: applying the inverse to only one side."))
    }

    private fun twoStepEquation(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val x = r.nextInt(-difficulty.range(), difficulty.range()).avoidZero()
        val a = r.nextInt(2, 8)
        val b = r.nextInt(-12, 13).avoidZero()
        val rhs = a * x + b
        return mission(seed, MathTopic.LinearEquations, MathSkill.TwoStepEquation, grade, difficulty, QuestionType.EquationSolution,
            "Solve for x: ${a}x ${if (b >= 0) "+" else "-"} ${abs(b)} = $rhs", AnswerDefinition.EquationAnswer("x", Rational(x, 1)),
            listOf("Undo addition/subtraction first.", "Then divide by the coefficient."), listOf("Common error: dividing before undoing the constant."))
    }

    private fun substitution(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val x = r.nextInt(-8, 9).avoidZero()
        val a = r.nextInt(2, 8)
        val b = r.nextInt(-10, 11)
        val answer = a * x + b
        return mission(seed, MathTopic.Algebra, MathSkill.Substitution, grade, difficulty, QuestionType.IntegerAnswer,
            "Evaluate ${a}x ${if (b >= 0) "+" else "-"} ${abs(b)} when x = $x.", AnswerDefinition.IntegerAnswer(answer),
            listOf("Replace x with the given value.", "Multiply before adding."), listOf("Common error: forgetting parentheses around a negative value."))
    }

    private fun angleRelationship(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val supplementary = r.nextBoolean()
        val angle = if (supplementary) r.nextInt(25, 155) else r.nextInt(10, 80)
        val answer = if (supplementary) 180 - angle else 90 - angle.coerceAtMost(80)
        val text = if (supplementary) "supplementary" else "complementary"
        return mission(seed, MathTopic.Angles, MathSkill.AngleRelationships, grade, difficulty, QuestionType.AngleAnswer,
            "An angle is ${abs(angle)}°. Find its $text angle.", AnswerDefinition.AngleAnswer(answer.toDouble()),
            listOf(if (supplementary) "Supplementary angles sum to 180°." else "Complementary angles sum to 90°."), listOf("Common error: using the wrong total."))
    }

    private fun triangleAngle(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val a = r.nextInt(30, 80)
        val b = r.nextInt(35, 95).coerceAtMost(140 - a)
        val c = 180 - a - b
        return mission(seed, MathTopic.Angles, MathSkill.TriangleAngleSum, grade, difficulty, QuestionType.AngleAnswer,
            "A triangle has angles $a° and $b°. Find the third angle.", AnswerDefinition.AngleAnswer(c.toDouble()),
            listOf("Triangle angles sum to 180°.", "Subtract the two known angles."), listOf("Common error: using 360°."))
    }

    private fun area(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val rectangle = r.nextBoolean()
        val w = r.nextInt(3, difficulty.range() + 8)
        val h = r.nextInt(3, difficulty.range() + 8)
        val answer = if (rectangle) (w * h).toDouble() else (w * h / 2.0)
        val shape = if (rectangle) "rectangle" else "triangle"
        return mission(seed, MathTopic.GeometryMeasurement, MathSkill.Area, grade, difficulty, QuestionType.MeasurementAnswer,
            "Find the area of a $shape with base $w m and height $h m.", AnswerDefinition.MeasurementAnswer(answer, UnitKind.SquareMetres, tolerance = 0.01),
            listOf(if (rectangle) "Area = length × width." else "Area = base × height ÷ 2."), listOf("Common error: forgetting square units."))
    }

    private fun perimeter(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val l = r.nextInt(3, difficulty.range() + 10)
        val w = r.nextInt(2, difficulty.range() + 8)
        return mission(seed, MathTopic.GeometryMeasurement, MathSkill.Perimeter, grade, difficulty, QuestionType.MeasurementAnswer,
            "Find the perimeter of a rectangle with length $l m and width $w m.", AnswerDefinition.MeasurementAnswer((2 * (l + w)).toDouble(), UnitKind.Metres),
            listOf("Perimeter is the distance around.", "Add all four sides."), listOf("Common error: calculating area instead."))
    }

    private fun coordinate(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val bound = if (difficulty.ordinal >= DifficultyLevel.Medium.ordinal) 10 else 5
        val x = r.nextInt(-bound, bound + 1)
        val y = r.nextInt(-bound, bound + 1)
        return mission(seed, MathTopic.CoordinateGeometry, MathSkill.CoordinateIdentification, grade, difficulty, QuestionType.OrderedPair,
            "Identify the ordered pair for the point at x = $x and y = $y.", AnswerDefinition.CoordinateAnswer(Rational(x, 1), Rational(y, 1)),
            listOf("Ordered pairs are written (x, y)."), listOf("Common error: reversing x and y."))
    }

    private fun gridDistance(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val x1 = r.nextInt(-6, 7)
        val y1 = r.nextInt(-6, 7)
        val horizontal = difficulty.ordinal < DifficultyLevel.Hard.ordinal || r.nextBoolean()
        val x2 = if (horizontal) r.nextInt(-6, 7).takeIf { it != x1 } ?: x1 + 3 else x1 + r.nextInt(1, 6)
        val y2 = if (horizontal) y1 else y1 + r.nextInt(1, 6)
        val answer = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
        return mission(seed, MathTopic.CoordinateGeometry, MathSkill.GridDistance, grade, difficulty, QuestionType.DecimalAnswer,
            "Find the distance between ($x1,$y1) and ($x2,$y2).", AnswerDefinition.DecimalAnswer(answer, tolerance = 0.02),
            listOf("Use horizontal/vertical distance or the distance formula.", "Square, add, then square root when needed."), listOf("Common error: subtracting coordinates in the wrong direction changes signs, but distance stays positive."))
    }

    private fun slope(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val x1 = r.nextInt(-6, 3)
        val x2 = x1 + r.nextInt(1, 7)
        val y1 = r.nextInt(-6, 7)
        val y2 = y1 + r.nextInt(-6, 7).avoidZero()
        val slope = Rational(y2 - y1, x2 - x1).normalized
        return mission(seed, MathTopic.CoordinateGeometry, MathSkill.SlopeBasics, grade, difficulty, QuestionType.FractionAnswer,
            "Find the slope between ($x1,$y1) and ($x2,$y2).", AnswerDefinition.FractionAnswer(slope, requireSimplified = true),
            listOf("Slope = rise ÷ run.", "Compute change in y over change in x."), listOf("Common error: using run over rise."))
    }

    private fun trigonometry(seed: Long, grade: GradeBand, difficulty: DifficultyLevel): MathMission {
        val r = Random(seed)
        val opposite = r.nextInt(3, 13)
        val adjacent = r.nextInt(3, 13)
        val angle = Math.toDegrees(atan(opposite.toDouble() / adjacent)).roundToInt().toDouble()
        return mission(seed, MathTopic.Trigonometry, MathSkill.RightTriangleTrigonometry, grade, difficulty, QuestionType.AngleAnswer,
            "In a right triangle, opposite = $opposite and adjacent = $adjacent. Estimate θ using tan(θ)=opposite/adjacent.", AnswerDefinition.AngleAnswer(angle, tolerance = 1.0),
            listOf("Use tangent because opposite and adjacent are known.", "θ = arctan(opposite/adjacent)."), listOf("Common error: using sine or cosine with the wrong sides."))
    }

    private fun mission(
        seed: Long,
        topic: MathTopic,
        skill: MathSkill,
        grade: GradeBand,
        difficulty: DifficultyLevel,
        questionType: QuestionType,
        text: String,
        answer: AnswerDefinition,
        steps: List<String>,
        misconceptions: List<String>
    ): MathMission {
        val time = difficulty.timeLimit()
        return MathMission(
            id = "mission-${skill.name.lowercase()}-$seed",
            topic = topic,
            skill = skill,
            gradeBand = grade,
            difficulty = difficulty,
            questionType = questionType,
            prompt = MathPrompt(
                studentText = text,
                analystClue = "Formula or pattern: ${steps.first()}",
                navigatorClue = "Find the AR marker for ${topic.name}.",
                builderClue = "Prepare the construction piece after the solver submits.",
                commanderClue = "Correct answer earns base resources."
            ),
            expectedAnswer = answer,
            solutionSteps = steps.mapIndexed { i, s -> SolutionStep(i + 1, s) },
            hints = steps.mapIndexed { i, s -> Hint(i + 1, s, 15 + i * 15) },
            misconceptions = misconceptions.map { CommonMisconception("", it) },
            timeLimitSeconds = time,
            interactionType = if (topic in setOf(MathTopic.GeometryMeasurement, MathTopic.CoordinateGeometry, MathTopic.Trigonometry)) InteractionType.TeamMultiDevice else InteractionType.NonArCalculation,
            seed = seed
        )
    }

    private fun DifficultyLevel.range(): Int = when (this) {
        DifficultyLevel.Beginner -> 8
        DifficultyLevel.Easy -> 12
        DifficultyLevel.Medium -> 20
        DifficultyLevel.Hard -> 40
        DifficultyLevel.Expert -> 80
        DifficultyLevel.Adaptive -> 25
    }
    private fun DifficultyLevel.denominator(): Int = when (this) {
        DifficultyLevel.Beginner -> 6
        DifficultyLevel.Easy -> 8
        DifficultyLevel.Medium -> 12
        DifficultyLevel.Hard -> 16
        DifficultyLevel.Expert -> 24
        DifficultyLevel.Adaptive -> 12
    }
    private fun DifficultyLevel.timeLimit(): Int = when (this) {
        DifficultyLevel.Beginner -> 90
        DifficultyLevel.Easy -> 75
        DifficultyLevel.Medium -> 60
        DifficultyLevel.Hard -> 45
        DifficultyLevel.Expert -> 35
        DifficultyLevel.Adaptive -> 60
    }
    private fun Int.avoidZero(): Int = if (this == 0) 1 else this
    private fun Double.clean(): String = "%.2f".format(this).trimEnd('0').trimEnd('.')
    private fun MathSkill.topic(): MathTopic = when (this) {
        MathSkill.IntegerOperations -> MathTopic.Integers
        MathSkill.FractionAddSubtract, MathSkill.FractionMultiplyDivide -> MathTopic.Fractions
        MathSkill.DecimalOperations -> MathTopic.Decimals
        MathSkill.PercentageCalculation, MathSkill.RatioSimplification -> MathTopic.RatiosPercentages
        MathSkill.Substitution -> MathTopic.Algebra
        MathSkill.OneStepEquation, MathSkill.TwoStepEquation -> MathTopic.LinearEquations
        MathSkill.AngleRelationships, MathSkill.TriangleAngleSum -> MathTopic.Angles
        MathSkill.Area, MathSkill.Perimeter -> MathTopic.GeometryMeasurement
        MathSkill.CoordinateIdentification, MathSkill.GridDistance, MathSkill.SlopeBasics -> MathTopic.CoordinateGeometry
        MathSkill.RightTriangleTrigonometry -> MathTopic.Trigonometry
    }
}
