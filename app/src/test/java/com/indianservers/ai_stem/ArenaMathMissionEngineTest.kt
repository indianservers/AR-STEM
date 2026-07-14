package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.mission.AnswerDefinition
import com.indianservers.ai_stem.feature.games.mission.AnswerValidationEngine
import com.indianservers.ai_stem.feature.games.mission.AntiDominationEngine
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.MathMissionGenerator
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MissionAttempt
import com.indianservers.ai_stem.feature.games.mission.MissionEvent
import com.indianservers.ai_stem.feature.games.mission.MissionProgress
import com.indianservers.ai_stem.feature.games.mission.MissionScoringEngine
import com.indianservers.ai_stem.feature.games.mission.MissionState
import com.indianservers.ai_stem.feature.games.mission.MissionStateMachine
import com.indianservers.ai_stem.feature.games.mission.MissionTemplateMetadataCodec
import com.indianservers.ai_stem.feature.games.mission.PlayerParticipation
import com.indianservers.ai_stem.feature.games.mission.Rational
import com.indianservers.ai_stem.feature.games.mission.RoleClueDistributor
import com.indianservers.ai_stem.feature.games.mission.UnitKind
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaMathMissionEngineTest {
    private val validator = AnswerValidationEngine()

    @Test
    fun everyGeneratorIsDeterministicAndValidAcrossOneHundredSeeds() {
        MathSkill.entries.forEach { skill ->
            repeat(100) { index ->
                val seed = 10_000L + index
                val first = MathMissionGenerator.generate(skill, seed, GradeBand.Mixed, DifficultyLevel.Medium)
                val second = MathMissionGenerator.generate(skill, seed, GradeBand.Mixed, DifficultyLevel.Medium)
                assertEquals(first, second)
                assertEquals(skill, first.skill)
                assertTrue(first.solutionSteps.isNotEmpty())
                assertTrue(first.hints.isNotEmpty())
                assertTrue(first.timeLimitSeconds > 0)
                val correct = validator.validate(answerString(first.expectedAnswer), first.expectedAnswer)
                assertTrue("Expected generated answer to validate for $skill seed $seed but was ${correct.status}", correct.status == ValidationStatus.Correct || correct.status == ValidationStatus.CorrectButNeedsSimplification)
                assertNoInvalidDenominator(first.expectedAnswer)
            }
        }
    }

    @Test
    fun validationHandlesFractionsSimplificationDecimalsCoordinatesAnglesAndUnits() {
        val half = AnswerDefinition.FractionAnswer(Rational(1, 2), requireSimplified = false)
        assertEquals(ValidationStatus.Correct, validator.validate("2/4", half).status)

        val simplifiedHalf = AnswerDefinition.FractionAnswer(Rational(1, 2), requireSimplified = true)
        assertEquals(ValidationStatus.CorrectButNeedsSimplification, validator.validate("2/4", simplifiedHalf).status)

        assertEquals(ValidationStatus.Correct, validator.validate("3.141", AnswerDefinition.DecimalAnswer(3.14, tolerance = 0.01)).status)
        assertEquals(ValidationStatus.WrongCoordinateOrder, validator.validate("(4, 2)", AnswerDefinition.CoordinateAnswer(Rational(2, 1), Rational(4, 1))).status)
        assertEquals(ValidationStatus.Correct, validator.validate("60 degrees", AnswerDefinition.AngleAnswer(60.0)).status)
        assertEquals(ValidationStatus.MissingUnit, validator.validate("24", AnswerDefinition.MeasurementAnswer(24.0, UnitKind.SquareMetres)).status)
        assertEquals(ValidationStatus.Correct, validator.validate("24 m2", AnswerDefinition.MeasurementAnswer(24.0, UnitKind.SquareMetres)).status)
    }

    @Test
    fun difficultyChangesQuestionShape() {
        val easy = MathMissionGenerator.generate(MathSkill.IntegerOperations, 42, GradeBand.Grade6, DifficultyLevel.Easy)
        val expert = MathMissionGenerator.generate(MathSkill.IntegerOperations, 42, GradeBand.Grade6, DifficultyLevel.Expert)
        assertNotEquals(easy.prompt.studentText, expert.prompt.studentText)
        assertTrue(expert.timeLimitSeconds < easy.timeLimitSeconds)
    }

    @Test
    fun roleCluesHideInformationByRole() {
        val mission = MathMissionGenerator.generate(MathSkill.TwoStepEquation, 55, GradeBand.Grade8, DifficultyLevel.Medium)
        val clues = RoleClueDistributor().cluesFor(mission)

        assertTrue(clues.first { it.role == ArenaRole.Solver }.canSubmit)
        assertFalse(clues.first { it.role == ArenaRole.Navigator }.canSubmit)
        assertNotEquals(clues.first { it.role == ArenaRole.Solver }.clue, clues.first { it.role == ArenaRole.Commander }.clue)
    }

    @Test
    fun antiDominationBlocksRepeatSubmitterAndScoresParticipation() {
        val mission = MathMissionGenerator.generate(MathSkill.Area, 7, GradeBand.Mixed, DifficultyLevel.Medium)
        val progress = MissionProgress(mission, lastSubmitterId = "p1")
        val engine = AntiDominationEngine()

        assertFalse(engine.canAct(progress, PlayerParticipation("p1", ArenaRole.Solver)).allowed)
        assertTrue(engine.canAct(progress, PlayerParticipation("p2", ArenaRole.Analyst)).allowed)
        assertTrue(engine.participationBonus(progress, listOf(
            PlayerParticipation("p1", ArenaRole.Solver, actionsTaken = 1),
            PlayerParticipation("p2", ArenaRole.Analyst, actionsTaken = 1),
            PlayerParticipation("p3", ArenaRole.Builder, actionsTaken = 1)
        )) > 0)
    }

    @Test
    fun missionStateMachineRequiresHostAndCalculatesScore() {
        val mission = MathMissionGenerator.generate(MathSkill.OneStepEquation, 99, GradeBand.Grade7, DifficultyLevel.Easy)
        val machine = MissionStateMachine()
        val progress = MissionProgress(mission)
        assertTrue(runCatching { machine.transition(progress, MissionEvent.StartMission, hostAuthorized = false) }.isFailure)

        val active = machine.transition(progress, MissionEvent.StartMission, hostAuthorized = true)
        assertEquals(MissionState.Active, active.state)
        val answered = machine.transition(active, MissionEvent.SubmitAnswer(MissionAttempt("p1", ArenaRole.Solver, answerString(mission.expectedAnswer), 1, 10)), hostAuthorized = true)
        assertEquals(MissionState.Correct, answered.state)
        val score = MissionScoringEngine().score("team", answered, listOf(PlayerParticipation("p1", ArenaRole.Solver, actionsTaken = 1, submissions = 1)), arAccurate = true, explanationCorrect = true)
        assertTrue(score.total >= 100)
    }

    @Test
    fun localTemplateMetadataIsVersionedForOfflineStorage() {
        val mission = MathMissionGenerator.generate(MathSkill.SlopeBasics, 101, GradeBand.Grade8, DifficultyLevel.Medium)
        val encoded = MissionTemplateMetadataCodec.encode(mission)
        assertTrue(MissionTemplateMetadataCodec.isValid(encoded))
        assertTrue(encoded.contains(mission.skill.name))
    }

    @Test
    fun missionProtocolMessagesSerialize() {
        val message = GameMessage(
            roomId = "room",
            senderPlayerId = "host",
            sequence = 1,
            payload = GamePayload.MissionCreated("m1", MathSkill.FractionAddSubtract, 123)
        )
        val decoded = ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(message)).getOrThrow()
        assertEquals(GamePayload.MissionCreated::class, decoded.payload::class)

        val validated = GameMessage(
            roomId = "room",
            senderPlayerId = "host",
            sequence = 2,
            payload = GamePayload.AnswerValidated("m1", "p1", ValidationStatus.Correct)
        )
        assertEquals(GamePayload.AnswerValidated::class, ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(validated)).getOrThrow().payload::class)
    }

    private fun answerString(answer: AnswerDefinition): String = when (answer) {
        is AnswerDefinition.IntegerAnswer -> answer.value.toString()
        is AnswerDefinition.DecimalAnswer -> if (answer.unit == UnitKind.None) answer.value.toString() else "${answer.value} ${unitText(answer.unit)}"
        is AnswerDefinition.FractionAnswer -> answer.value.toString()
        is AnswerDefinition.MultipleChoiceAnswer -> answer.correctChoiceIds.joinToString(",")
        is AnswerDefinition.ExpressionAnswer -> "${answer.expression.coefficient}${answer.expression.variable}+${answer.expression.constant}"
        is AnswerDefinition.EquationAnswer -> "${answer.variable}=${answer.value}"
        is AnswerDefinition.CoordinateAnswer -> "(${answer.x}, ${answer.y})"
        is AnswerDefinition.AngleAnswer -> "${answer.degrees} degrees"
        is AnswerDefinition.MeasurementAnswer -> "${answer.value} ${unitText(answer.unit)}"
        is AnswerDefinition.SequenceAnswer -> answer.orderedIds.joinToString(",")
        is AnswerDefinition.MultiStepAnswer -> answer.parts.joinToString("|") { answerString(it) }
    }

    private fun assertNoInvalidDenominator(answer: AnswerDefinition) {
        when (answer) {
            is AnswerDefinition.FractionAnswer -> assertNotEquals(0, answer.value.denominator)
            is AnswerDefinition.EquationAnswer -> assertNotEquals(0, answer.value.denominator)
            is AnswerDefinition.CoordinateAnswer -> {
                assertNotEquals(0, answer.x.denominator)
                assertNotEquals(0, answer.y.denominator)
            }
            is AnswerDefinition.MultiStepAnswer -> answer.parts.forEach(::assertNoInvalidDenominator)
            else -> Unit
        }
    }

    private fun unitText(unit: UnitKind): String = when (unit) {
        UnitKind.None -> ""
        UnitKind.Degrees -> "degrees"
        UnitKind.Metres -> "m"
        UnitKind.SquareMetres -> "m2"
        UnitKind.Centimetres -> "cm"
        UnitKind.Percent -> "%"
    }
}
