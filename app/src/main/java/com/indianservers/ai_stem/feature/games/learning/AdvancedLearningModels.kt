package com.indianservers.ai_stem.feature.games.learning

import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

const val ADVANCED_LEARNING_SCHEMA_VERSION = 1
const val PROCEDURAL_GENERATOR_VERSION = 1

enum class LearningSubject { Mathematics }
enum class LearningDomain { NumberSystems, ArithmeticCommercial, Algebra, Geometry, Trigonometry, StatisticsProbability, PreCalculusCalculus }
enum class CognitiveLevel { Recall, Understand, Apply, Analyse, Evaluate, Construct }
enum class MasteryState { NotAttempted, Introduced, Developing, Secure, Mastered, NeedsReview }
enum class Misconception {
    AddDenominators,
    ReverseCoordinates,
    AreaPerimeterConfusion,
    EquationSignError,
    SlopeRiseRunSwap,
    SineTangentConfusion,
    PercentAsWholeNumber,
    RadiusDiameterConfusion
}

data class CurriculumSkill(
    val skillId: String,
    val subject: LearningSubject,
    val domain: LearningDomain,
    val topic: String,
    val subtopic: String,
    val skill: String,
    val prerequisiteSkillIds: Set<String>,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val cognitiveLevel: CognitiveLevel,
    val interactionType: String,
    val suitableGameIds: Set<String>,
    val masteryThreshold: MasteryThreshold
)

data class MasteryThreshold(
    val minAttempts: Int,
    val minIndependentCorrect: Int,
    val minRecentAccuracy: Double,
    val maxHintRate: Double,
    val retentionDays: Int
)

data class ProceduralMission(
    val missionId: String,
    val gameId: String,
    val skillId: String,
    val seed: Long,
    val generatorVersion: Int,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val cognitiveLevel: CognitiveLevel,
    val prompt: String,
    val expectedAnswer: String,
    val unit: String?,
    val hints: List<String>,
    val workedSolution: List<String>,
    val distractors: List<String>,
    val misconceptions: Set<Misconception>,
    val parameters: Map<String, String>
)

data class ContentQualityIssue(val code: String, val message: String, val severity: Int)
data class ContentQualityResult(val valid: Boolean, val issues: List<ContentQualityIssue>)
data class ContentAuditSummary(
    val generatedCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int,
    val generatorVersion: Int,
    val failureRate: Double
)

data class SkillEvidence(
    val skillId: String,
    val gameId: String,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val correct: Boolean,
    val independent: Boolean,
    val hintCount: Int,
    val responseSeconds: Int,
    val misconception: Misconception?,
    val completedAtDay: Int
)

data class SkillMasteryRecord(
    val playerId: String,
    val skillId: String,
    val gameId: String,
    val gradeBand: GradeBand,
    val difficulty: DifficultyLevel,
    val attempts: Int,
    val independentCorrect: Int,
    val recentAccuracy: Double?,
    val hintRate: Double?,
    val averageResponseSeconds: Double?,
    val repeatedMisconceptions: Set<Misconception>,
    val lastAttemptDay: Int?,
    val state: MasteryState,
    val schemaVersion: Int = ADVANCED_LEARNING_SCHEMA_VERSION
)

data class TeacherMasterySettings(
    val masteryThresholds: Map<String, MasteryThreshold>,
    val enforcePrerequisites: Boolean,
    val allowTeacherOverride: Boolean,
    val dailyChallengesEnabled: Boolean,
    val maxGradeBand: GradeBand,
    val advancedTopicAccess: Boolean,
    val workedSolutionsEnabled: Boolean,
    val crossGameRecommendationsEnabled: Boolean
)

data class AdaptiveRecommendation(
    val recommendationId: String,
    val playerId: String,
    val gameId: String,
    val skillId: String,
    val reason: String,
    val mission: ProceduralMission,
    val remediation: RemediationMission?,
    val blockedByPrerequisiteIds: Set<String>
)

data class RemediationMission(
    val remediationId: String,
    val misconception: Misconception,
    val explanation: String,
    val visualCorrection: String,
    val guidedMission: ProceduralMission,
    val independentRecheck: ProceduralMission
)

data class CampaignNode(
    val nodeId: String,
    val title: String,
    val gameId: String,
    val skillId: String,
    val missionSeed: Long,
    val unlockSkillIds: Set<String>,
    val optional: Boolean,
    val finalChallenge: Boolean,
    val difficulty: DifficultyLevel
)

data class AdvancedCampaign(
    val campaignId: String,
    val gameId: String,
    val title: String,
    val chapters: List<CampaignChapter>,
    val unlockAllTeacherOverride: Boolean = false
)

data class CampaignChapter(
    val chapterId: String,
    val title: String,
    val nodes: List<CampaignNode>
)

data class CampaignProgress(
    val playerId: String,
    val campaignId: String,
    val completedNodeIds: Set<String>,
    val restoredFromVersion: Int,
    val schemaVersion: Int = ADVANCED_LEARNING_SCHEMA_VERSION
)

data class LearningJourneyStep(val gameId: String, val skillId: String, val explanation: String)
data class DailyChallenge(val challengeId: String, val dateKey: Int, val gameId: String, val mission: ProceduralMission, val irreversibleReward: Boolean = false)
data class WeeklyChallenge(val challengeId: String, val weekKey: Int, val missions: List<ProceduralMission>, val irreversibleReward: Boolean = false)
data class MigrationResult(val migrated: Boolean, val schemaVersion: Int, val fallbackSkillTags: Set<String>, val warnings: List<String>)

object AdvancedCurriculumCatalog {
    val defaultThreshold = MasteryThreshold(4, 3, 0.72, 0.35, 14)

    val skills: List<CurriculumSkill> = buildList {
        add(skill("scientific-notation-error-bounds", LearningDomain.NumberSystems, "Real numbers", "Scientific notation", "Use scientific notation and error bounds for measurement", emptySet(), GradeBand.Grade8, DifficultyLevel.Hard, CognitiveLevel.Evaluate, setOf("math_expedition_ar", "ar-math-arena")))
        add(skill("fraction-equivalence", LearningDomain.ArithmeticCommercial, "Fractions", "Equivalence", "Recognize and generate equivalent fractions", emptySet(), GradeBand.Grade6, DifficultyLevel.Easy, CognitiveLevel.Understand, setOf("fraction_factory_ar", "ar-math-arena")))
        add(skill("fraction-ratio-scale", LearningDomain.ArithmeticCommercial, "Ratio", "Scale", "Apply ratio as scale factor", setOf("fraction-equivalence"), GradeBand.Grade7, DifficultyLevel.Medium, CognitiveLevel.Apply, setOf("fraction_factory_ar", "geometry_architect_ar", "math_expedition_ar")))
        add(skill("linear-equation-solving", LearningDomain.Algebra, "Equations", "Linear equations", "Solve one and two step linear equations", emptySet(), GradeBand.Grade7, DifficultyLevel.Medium, CognitiveLevel.Apply, setOf("equation_escape_ar", "ar-math-arena")))
        add(skill("simultaneous-equations", LearningDomain.Algebra, "Equations", "Systems", "Solve simultaneous linear equations", setOf("linear-equation-solving"), GradeBand.Grade9, DifficultyLevel.Hard, CognitiveLevel.Analyse, setOf("equation_escape_ar", "coordinate_conquest_ar", "ar-math-arena")))
        add(skill("quadratic-targeting", LearningDomain.Algebra, "Quadratics", "Roots and vertex", "Solve quadratic targeting problems", setOf("linear-equation-solving"), GradeBand.Grade10, DifficultyLevel.Expert, CognitiveLevel.Analyse, setOf("ar-math-arena", "equation_escape_ar", "coordinate_conquest_ar")))
        add(skill("coordinate-slope", LearningDomain.Geometry, "Coordinate geometry", "Slope", "Calculate slope and interpret rise over run", emptySet(), GradeBand.Grade8, DifficultyLevel.Medium, CognitiveLevel.Apply, setOf("coordinate_conquest_ar", "math_expedition_ar")))
        add(skill("line-intersection", LearningDomain.Geometry, "Coordinate geometry", "Intersections", "Find where two lines intersect", setOf("coordinate-slope", "linear-equation-solving"), GradeBand.Grade9, DifficultyLevel.Hard, CognitiveLevel.Analyse, setOf("coordinate_conquest_ar", "equation_escape_ar")))
        add(skill("similarity-scale-structures", LearningDomain.Geometry, "Similarity", "Scale models", "Use similar figures for structures", setOf("fraction-ratio-scale"), GradeBand.Grade8, DifficultyLevel.Medium, CognitiveLevel.Construct, setOf("geometry_architect_ar")))
        add(skill("surface-volume-optimization", LearningDomain.Geometry, "Mensuration", "Optimization", "Compare surface area and volume tradeoffs", setOf("similarity-scale-structures"), GradeBand.Grade10, DifficultyLevel.Expert, CognitiveLevel.Evaluate, setOf("geometry_architect_ar", "ar-math-arena")))
        add(skill("trig-elevation", LearningDomain.Trigonometry, "Right triangles", "Elevation", "Use tangent for elevation and depression", setOf("similarity-scale-structures"), GradeBand.Grade10, DifficultyLevel.Hard, CognitiveLevel.Apply, setOf("geometry_architect_ar", "math_expedition_ar", "ar-math-arena")))
        add(skill("bearing-route", LearningDomain.Trigonometry, "Bearings", "Route bearing", "Calculate and compare bearings", setOf("coordinate-slope"), GradeBand.Grade9, DifficultyLevel.Hard, CognitiveLevel.Apply, setOf("math_expedition_ar")))
        add(skill("mean-range-field-data", LearningDomain.StatisticsProbability, "Statistics", "Field data", "Summarize collected data using mean and range", emptySet(), GradeBand.Grade7, DifficultyLevel.Medium, CognitiveLevel.Analyse, setOf("math_expedition_ar", "ar-math-arena")))
        add(skill("probability-shield", LearningDomain.StatisticsProbability, "Probability", "Defence probability", "Calculate combined event probability", setOf("fraction-equivalence"), GradeBand.Grade9, DifficultyLevel.Hard, CognitiveLevel.Evaluate, setOf("ar-math-arena", "equation_escape_ar")))
        add(skill("rate-of-change", LearningDomain.PreCalculusCalculus, "Calculus intuition", "Gradient", "Interpret rate of change from a graph", setOf("coordinate-slope"), GradeBand.Grade10, DifficultyLevel.Expert, CognitiveLevel.Analyse, setOf("ar-math-arena", "coordinate_conquest_ar")))
        add(skill("area-under-curve", LearningDomain.PreCalculusCalculus, "Calculus intuition", "Accumulation", "Estimate area under a curve", setOf("rate-of-change"), GradeBand.Grade10, DifficultyLevel.Expert, CognitiveLevel.Evaluate, setOf("coordinate_conquest_ar", "ar-math-arena")))
    }

    val prerequisiteEdges: Map<String, Set<String>> = skills.associate { it.skillId to it.prerequisiteSkillIds }

    private fun skill(
        id: String,
        domain: LearningDomain,
        topic: String,
        subtopic: String,
        description: String,
        prerequisites: Set<String>,
        gradeBand: GradeBand,
        difficulty: DifficultyLevel,
        cognitiveLevel: CognitiveLevel,
        gameIds: Set<String>
    ): CurriculumSkill = CurriculumSkill(
        skillId = id,
        subject = LearningSubject.Mathematics,
        domain = domain,
        topic = topic,
        subtopic = subtopic,
        skill = description,
        prerequisiteSkillIds = prerequisites,
        gradeBand = gradeBand,
        difficulty = difficulty,
        cognitiveLevel = cognitiveLevel,
        interactionType = if (gameIds.any { it.contains("ar") }) "AR gameplay" else "Math practice",
        suitableGameIds = gameIds,
        masteryThreshold = defaultThreshold
    )
}

class ProceduralMissionGenerator(
    private val curriculum: List<CurriculumSkill> = AdvancedCurriculumCatalog.skills
) {
    fun generate(gameId: String, skillId: String, seed: Long, gradeBand: GradeBand, difficulty: DifficultyLevel): ProceduralMission {
        val skill = curriculum.first { it.skillId == skillId }
        require(gameId in skill.suitableGameIds) { "Skill $skillId is not suitable for $gameId." }
        require(gradeBand.ordinal >= skill.gradeBand.ordinal || gradeBand == GradeBand.Mixed) { "Selected grade is below skill prerequisite grade." }
        val random = Random(seed xor skillId.hashCode().toLong() xor gameId.hashCode().toLong())
        repeat(40) { attempt ->
            val mission = candidate(gameId, skill, seed + attempt, random, gradeBand, difficulty)
            val quality = ContentQualityValidator().validate(mission, skill)
            if (quality.valid) return mission
        }
        error("Unable to generate valid mission for $skillId after constraint checks.")
    }

    private fun candidate(gameId: String, skill: CurriculumSkill, seed: Long, random: Random, gradeBand: GradeBand, difficulty: DifficultyLevel): ProceduralMission {
        val seedBase = if (seed < 0) -seed else seed
        val a = 2 + ((seedBase * 31 + random.nextInt(0, 97)) % (90 + difficulty.ordinal * 12)).toInt()
        val b = 2 + ((seedBase * 17 + random.nextInt(0, 89)) % (82 + difficulty.ordinal * 11)).toInt()
        val c = 1 + ((seedBase * 13 + random.nextInt(0, 47)) % (36 + difficulty.ordinal * 7)).toInt()
        val (prompt, answer, unit, solution, misconception) = when (skill.skillId) {
            "scientific-notation-error-bounds" -> {
                val exponent = random.nextInt(3, 7)
                val value = a * 10.0.pow(exponent)
                tuple("Write $value in scientific notation coefficient.", "$a", null, listOf("Scientific notation is coefficient times a power of 10.", "$value = $a x 10^$exponent."), Misconception.PercentAsWholeNumber)
            }
            "fraction-equivalence" -> {
                val multiplier = random.nextInt(2, 6)
                tuple("Create a fraction equivalent to $a/$b using multiplier $multiplier.", "${a * multiplier}/${b * multiplier}", null, listOf("Multiply numerator and denominator by the same number.", "$a/$b = ${a * multiplier}/${b * multiplier}."), Misconception.AddDenominators)
            }
            "fraction-ratio-scale" -> tuple("A model uses scale 1:$a. Real length is ${a * b} m. Find model length.", "$b", "m", listOf("Divide real length by scale.", "${a * b} / $a = $b m."), Misconception.PercentAsWholeNumber)
            "linear-equation-solving" -> tuple("Solve ${a}x + $b = ${a * c + b}.", "$c", null, listOf("Subtract $b from both sides.", "Divide by $a, x = $c."), Misconception.EquationSignError)
            "simultaneous-equations" -> tuple("Solve x + y = ${a + b}, x - y = ${a - b}. Give x.", "$a", null, listOf("Add equations to get 2x = ${2 * a}.", "x = $a."), Misconception.EquationSignError)
            "quadratic-targeting" -> tuple("Find a positive root of x^2 - ${a + b}x + ${a * b} = 0.", "${maxOf(a, b)}", null, listOf("Factor as (x-$a)(x-$b)=0.", "Positive roots are $a and $b."), Misconception.EquationSignError)
            "coordinate-slope" -> tuple("Find slope from ($a,$b) to (${a + c},${b + c * 2}).", "2", null, listOf("Slope = rise/run.", "Rise ${c * 2}, run $c, slope 2."), Misconception.SlopeRiseRunSwap)
            "line-intersection" -> tuple("Lines y=x+$a and y=-x+${a + b * 2} intersect at x = ?", "$b", null, listOf("Set x+$a = -x+${a + b * 2}.", "2x = ${b * 2}, x = $b."), Misconception.ReverseCoordinates)
            "similarity-scale-structures" -> tuple("Two similar towers have scale $a:$b. Small height ${a * c} m. Large height?", "${b * c}", "m", listOf("Scale factor from small to large is $b/$a.", "${a * c} * $b / $a = ${b * c} m."), Misconception.RadiusDiameterConfusion)
            "surface-volume-optimization" -> tuple("Cube side $a m. Find surface area.", "${6 * a * a}", "m^2", listOf("Cube has 6 equal square faces.", "6 * $a^2 = ${6 * a * a} m^2."), Misconception.AreaPerimeterConfusion)
            "trig-elevation" -> tuple("A roof rise is ${a * c} m over run $a m. Find tan(theta).", "$c", null, listOf("tan(theta)=opposite/adjacent.", "${a * c}/$a = $c."), Misconception.SineTangentConfusion)
            "bearing-route" -> tuple("A route goes east $a units then north $a units. Bearing from start is about?", "45", "degrees", listOf("Equal east and north components form a 45 degree direction.", "Bearing is 045 degrees."), Misconception.SlopeRiseRunSwap)
            "mean-range-field-data" -> tuple("Data: $a, $b, $c. Find range.", "${maxOf(a, b, c) - minOf(a, b, c)}", null, listOf("Range = maximum - minimum.", "${maxOf(a, b, c)} - ${minOf(a, b, c)}."), Misconception.AreaPerimeterConfusion)
            "probability-shield" -> tuple("Two independent events have probabilities 1/$a and 1/$b. Probability both occur?", "1/${a * b}", null, listOf("For independent events, multiply probabilities.", "1/$a * 1/$b = 1/${a * b}."), Misconception.PercentAsWholeNumber)
            "rate-of-change" -> tuple("Position changes from $a m to ${a + b} m in $c s. Average rate?", "${format((b).toDouble() / c)}", "m/s", listOf("Rate = change/time.", "$b / $c = ${format((b).toDouble() / c)} m/s."), Misconception.SlopeRiseRunSwap)
            "area-under-curve" -> tuple("Estimate area under constant graph y=$a from x=0 to x=$b.", "${a * b}", "units^2", listOf("Area is rectangle height times width.", "$a * $b = ${a * b}."), Misconception.AreaPerimeterConfusion)
            else -> tuple("Solve $a + $b.", "${a + b}", null, listOf("Add the two values.", "Answer ${a + b}."), Misconception.EquationSignError)
        }
        return ProceduralMission(
            missionId = "proc-${gameId}-${skill.skillId}-$seed",
            gameId = gameId,
            skillId = skill.skillId,
            seed = seed,
            generatorVersion = PROCEDURAL_GENERATOR_VERSION,
            gradeBand = gradeBand,
            difficulty = difficulty,
            cognitiveLevel = skill.cognitiveLevel,
            prompt = prompt,
            expectedAnswer = answer,
            unit = unit,
            hints = listOf("Identify what the question asks.", "Use ${skill.subtopic.lowercase()} carefully."),
            workedSolution = solution,
            distractors = distractors(answer, random),
            misconceptions = setOf(misconception),
            parameters = mapOf("a" to "$a", "b" to "$b", "c" to "$c")
        )
    }

    private fun tuple(prompt: String, answer: String, unit: String?, solution: List<String>, misconception: Misconception): GeneratedTuple =
        GeneratedTuple(prompt, answer, unit, solution, misconception)

    private fun distractors(answer: String, random: Random): List<String> {
        val numeric = answer.toDoubleOrNull()
        return if (numeric != null) {
            listOf(numeric + 1, (numeric - 1).coerceAtLeast(0.0), numeric * 2).map { format(it) }.distinct().filterNot { it == answer }
        } else {
            listOf("${random.nextInt(1, 9)}/${random.nextInt(2, 12)}", "0", "1").distinct().filterNot { it == answer }
        }
    }

    private fun format(value: Double): String =
        if (abs(value - value.toInt()) < 0.0001) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

    private data class GeneratedTuple(val prompt: String, val answer: String, val unit: String?, val solution: List<String>, val misconception: Misconception)
}

class ContentQualityValidator {
    fun validate(mission: ProceduralMission, skill: CurriculumSkill): ContentQualityResult {
        val issues = mutableListOf<ContentQualityIssue>()
        if (mission.prompt.isBlank()) issues += issue("prompt", "Prompt is missing.", 3)
        if (mission.expectedAnswer.isBlank()) issues += issue("answer", "Expected answer is missing.", 3)
        if (mission.hints.size < 2) issues += issue("hints", "At least two progressive hints are required.", 2)
        if (mission.workedSolution.size < 2) issues += issue("solution", "Worked solution needs at least two steps.", 3)
        if (mission.skillId != skill.skillId) issues += issue("skill", "Mission skill tag does not match taxonomy.", 3)
        if (mission.gameId !in skill.suitableGameIds) issues += issue("game", "Mission assigned to unsuitable game.", 3)
        if (mission.gradeBand != GradeBand.Mixed && mission.gradeBand.ordinal < skill.gradeBand.ordinal) issues += issue("grade", "Mission grade is below skill grade.", 3)
        if (mission.expectedAnswer.contains("/0")) issues += issue("domain", "Answer contains undefined division.", 3)
        if (mission.distractors.contains(mission.expectedAnswer)) issues += issue("distractor", "Distractor duplicates the expected answer.", 2)
        if (mission.parameters.values.count { it == "0" } > 2) issues += issue("repetition", "Generated values are excessively repetitive.", 1)
        return ContentQualityResult(issues.none { it.severity >= 3 }, issues)
    }

    fun audit(missions: List<ProceduralMission>, skills: List<CurriculumSkill>): ContentAuditSummary {
        val seen = mutableSetOf<String>()
        var valid = 0
        var duplicates = 0
        missions.forEach { mission ->
            val skill = skills.first { it.skillId == mission.skillId }
            if (validate(mission, skill).valid) valid += 1
            val signature = "${mission.gameId}|${mission.skillId}|${mission.prompt}|${mission.expectedAnswer}|${mission.parameters.toSortedMap()}"
            if (!seen.add(signature)) duplicates += 1
        }
        val invalid = missions.size - valid
        return ContentAuditSummary(missions.size, valid, invalid, duplicates, PROCEDURAL_GENERATOR_VERSION, if (missions.isEmpty()) 0.0 else invalid.toDouble() / missions.size)
    }

    private fun issue(code: String, message: String, severity: Int): ContentQualityIssue = ContentQualityIssue(code, message, severity)
}

class MasteryEngine {
    fun update(playerId: String, skill: CurriculumSkill, evidence: List<SkillEvidence>, today: Int): SkillMasteryRecord {
        val attempts = evidence.size
        val independentCorrect = evidence.count { it.correct && it.independent }
        val correct = evidence.count { it.correct }
        val hintRate = evidence.takeIf { it.isNotEmpty() }?.count { it.hintCount > 0 }?.toDouble()?.div(attempts)
        val accuracy = evidence.takeLast(8).takeIf { it.isNotEmpty() }?.count { it.correct }?.toDouble()?.div(evidence.takeLast(8).size)
        val misconceptions = evidence.mapNotNull { it.misconception }.groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
        val state = when {
            attempts == 0 -> MasteryState.NotAttempted
            misconceptions.isNotEmpty() -> MasteryState.NeedsReview
            attempts < skill.masteryThreshold.minAttempts -> MasteryState.Introduced
            independentCorrect >= skill.masteryThreshold.minIndependentCorrect &&
                (accuracy ?: 0.0) >= skill.masteryThreshold.minRecentAccuracy &&
                (hintRate ?: 1.0) <= skill.masteryThreshold.maxHintRate &&
                evidence.maxOfOrNull { it.completedAtDay }?.let { today - it <= skill.masteryThreshold.retentionDays } == true -> MasteryState.Mastered
            (accuracy ?: 0.0) >= 0.6 -> MasteryState.Secure
            else -> MasteryState.Developing
        }
        return SkillMasteryRecord(playerId, skill.skillId, evidence.lastOrNull()?.gameId ?: skill.suitableGameIds.first(), skill.gradeBand, skill.difficulty, attempts, independentCorrect, accuracy, hintRate, evidence.map { it.responseSeconds }.average().takeIf { !it.isNaN() }, misconceptions, evidence.maxOfOrNull { it.completedAtDay }, state)
    }
}

class PrerequisiteGraph(private val skills: List<CurriculumSkill> = AdvancedCurriculumCatalog.skills) {
    fun missingPrerequisites(skillId: String, records: Map<String, SkillMasteryRecord>): Set<String> =
        skills.first { it.skillId == skillId }.prerequisiteSkillIds.filter { records[it]?.state !in setOf(MasteryState.Secure, MasteryState.Mastered) }.toSet()

    fun explain(skillId: String, missing: Set<String>): String =
        if (missing.isEmpty()) "Prerequisites are ready for $skillId." else "Recommended because $skillId builds on ${missing.joinToString()}."
}

class RemediationEngine(private val generator: ProceduralMissionGenerator = ProceduralMissionGenerator()) {
    fun forMisconception(playerId: String, gameId: String, skill: CurriculumSkill, misconception: Misconception, seed: Long): RemediationMission {
        val guided = generator.generate(gameId, skill.skillId, seed, skill.gradeBand, DifficultyLevel.Easy)
        val recheck = generator.generate(gameId, skill.skillId, seed + 97, skill.gradeBand, skill.difficulty)
        return RemediationMission(
            remediationId = "rem-$playerId-${skill.skillId}-${misconception.name}",
            misconception = misconception,
            explanation = explanation(misconception),
            visualCorrection = visual(misconception),
            guidedMission = guided,
            independentRecheck = recheck
        )
    }

    private fun explanation(m: Misconception): String = when (m) {
        Misconception.AddDenominators -> "Equivalent fractions keep value by scaling numerator and denominator together."
        Misconception.ReverseCoordinates -> "Coordinates are ordered as x first, then y."
        Misconception.AreaPerimeterConfusion -> "Perimeter measures boundary length; area measures covered surface."
        Misconception.EquationSignError -> "Perform inverse operations on both sides and keep signs attached to terms."
        Misconception.SlopeRiseRunSwap -> "Slope is rise divided by run, y-change over x-change."
        Misconception.SineTangentConfusion -> "Use tangent when comparing opposite and adjacent sides."
        Misconception.PercentAsWholeNumber -> "A percent is out of 100, so 25% is 0.25."
        Misconception.RadiusDiameterConfusion -> "Diameter is twice the radius."
    }

    private fun visual(m: Misconception): String = "Show AR correction card: ${explanation(m)}"
}

class AdaptiveLearningEngine(
    private val generator: ProceduralMissionGenerator = ProceduralMissionGenerator(),
    private val graph: PrerequisiteGraph = PrerequisiteGraph(),
    private val remediation: RemediationEngine = RemediationEngine(generator)
) {
    fun recommend(playerId: String, targetSkillId: String, gameId: String, records: Map<String, SkillMasteryRecord>, settings: TeacherMasterySettings, seed: Long): AdaptiveRecommendation {
        val skill = AdvancedCurriculumCatalog.skills.first { it.skillId == targetSkillId }
        val missing = if (settings.enforcePrerequisites) graph.missingPrerequisites(targetSkillId, records) else emptySet()
        val missionSkill = missing.firstOrNull() ?: targetSkillId
        val selectedSkill = AdvancedCurriculumCatalog.skills.first { it.skillId == missionSkill }
        val misconception = records[missionSkill]?.repeatedMisconceptions?.firstOrNull() ?: records[targetSkillId]?.repeatedMisconceptions?.firstOrNull()
        val selectedGame = selectedSkill.suitableGameIds.firstOrNull { it == gameId } ?: selectedSkill.suitableGameIds.first()
        val mission = generator.generate(selectedGame, selectedSkill.skillId, seed, selectedSkill.gradeBand, selectedSkill.difficulty)
        return AdaptiveRecommendation(
            recommendationId = "rec-$playerId-$targetSkillId-$seed",
            playerId = playerId,
            gameId = selectedGame,
            skillId = selectedSkill.skillId,
            reason = if (missing.isNotEmpty()) graph.explain(targetSkillId, missing) else "Recommended from recent performance, hints, mastery and teacher settings.",
            mission = mission,
            remediation = misconception?.let { remediation.forMisconception(playerId, selectedGame, selectedSkill, it, seed + 11) },
            blockedByPrerequisiteIds = missing
        )
    }
}

object AdvancedCampaignCatalog {
    fun campaigns(): List<AdvancedCampaign> = GamesCatalog.games.map { game ->
        val suitable = AdvancedCurriculumCatalog.skills.filter { game.id in it.suitableGameIds }
        AdvancedCampaign(
            campaignId = "advanced-${game.id}",
            gameId = game.id,
            title = "Advanced ${game.title}",
            chapters = listOf(
                CampaignChapter("foundation-${game.id}", "Foundation Route", suitable.take(3).mapIndexed { index, skill -> node(game, skill, index, optional = false) }),
                CampaignChapter("branch-${game.id}", "Adaptive Branch", suitable.drop(1).take(3).mapIndexed { index, skill -> node(game, skill, index + 10, optional = true) }),
                CampaignChapter("final-${game.id}", "Final Challenge", suitable.takeLast(1).mapIndexed { index, skill -> node(game, skill, index + 20, optional = false, final = true) })
            )
        )
    }

    fun unlockedNodes(campaign: AdvancedCampaign, progress: CampaignProgress, records: Map<String, SkillMasteryRecord>, teacherUnlockAll: Boolean): List<CampaignNode> {
        if (teacherUnlockAll || campaign.unlockAllTeacherOverride) return campaign.chapters.flatMap { it.nodes }
        val completed = progress.completedNodeIds
        return campaign.chapters.flatMap { it.nodes }.filter { node ->
            node.optional || node.unlockSkillIds.all { records[it]?.state in setOf(MasteryState.Secure, MasteryState.Mastered) } || node.nodeId in completed
        }
    }

    private fun node(game: GameDefinition, skill: CurriculumSkill, index: Int, optional: Boolean, final: Boolean = false): CampaignNode =
        CampaignNode(
            nodeId = "${game.id}-${skill.skillId}-$index",
            title = skill.skill,
            gameId = game.id,
            skillId = skill.skillId,
            missionSeed = (game.id.hashCode() * 31L + skill.skillId.hashCode() + index).let { if (it < 0) -it else it },
            unlockSkillIds = skill.prerequisiteSkillIds,
            optional = optional,
            finalChallenge = final,
            difficulty = if (final) DifficultyLevel.Expert else skill.difficulty
        )
}

class CrossGameProgressionEngine {
    fun journeyFor(skillId: String): List<LearningJourneyStep> {
        val skill = AdvancedCurriculumCatalog.skills.first { it.skillId == skillId }
        return skill.suitableGameIds.map { gameId ->
            LearningJourneyStep(gameId, skillId, "Practice ${skill.skill} in ${GamesCatalog.requireGame(gameId).title}.")
        }
    }
}

class LocalChallengeEngine(private val generator: ProceduralMissionGenerator = ProceduralMissionGenerator()) {
    fun daily(dateKey: Int, installationSeed: Long, gameId: String): DailyChallenge {
        val skill = AdvancedCurriculumCatalog.skills.first { gameId in it.suitableGameIds }
        val seed = installationSeed xor dateKey.toLong() xor gameId.hashCode().toLong()
        return DailyChallenge("daily-$dateKey-$gameId", dateKey, gameId, generator.generate(gameId, skill.skillId, seed, skill.gradeBand, skill.difficulty))
    }

    fun weekly(weekKey: Int, installationSeed: Long): WeeklyChallenge {
        val missions = GamesCatalog.games.mapIndexed { index, game ->
            val skill = AdvancedCurriculumCatalog.skills.first { game.id in it.suitableGameIds }
            generator.generate(game.id, skill.skillId, installationSeed xor weekKey.toLong() xor index.toLong(), skill.gradeBand, skill.difficulty)
        }
        return WeeklyChallenge("weekly-$weekKey", weekKey, missions)
    }
}

class LearningDataMigration {
    fun migrateMastery(rawSkillId: String?, gameId: String, oldAccuracy: Double?): MigrationResult {
        val fallback = rawSkillId ?: AdvancedCurriculumCatalog.skills.firstOrNull { gameId in it.suitableGameIds }?.skillId ?: "unknown-skill"
        val warnings = buildList {
            if (rawSkillId == null) add("Old record lacked skill tag; fallback skill assigned.")
            if (oldAccuracy == null) add("Old record lacked accuracy; mastery evidence remains introduced.")
        }
        return MigrationResult(true, ADVANCED_LEARNING_SCHEMA_VERSION, setOf(fallback), warnings)
    }
}
