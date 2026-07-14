package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseCommand
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseConfig
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchStage
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseReducer
import com.indianservers.ai_stem.feature.games.basedefense.BasePlacementCandidate
import com.indianservers.ai_stem.feature.games.basedefense.DefenseType
import com.indianservers.ai_stem.feature.games.basedefense.ObjectLifecycle
import com.indianservers.ai_stem.feature.games.basedefense.PerformanceProfile
import com.indianservers.ai_stem.feature.games.basedefense.skill
import com.indianservers.ai_stem.feature.games.mission.AnswerDefinition
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.MathMissionGenerator
import com.indianservers.ai_stem.feature.games.mission.UnitKind
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaBaseDefenseEngineTest {
    private val reducer = BaseDefenseReducer()

    @Test
    fun basePlacementRejectsUnsafeAndAcceptsSeparatedBases() {
        val state = state()
        val tooClose = reducer.reduce(state, BaseDefenseCommand.PlaceBase("host", candidate("red", 0.1f, z = 0.1f), "Red"))
        assertFalse(tooClose.accepted)

        val red = reducer.reduce(state, BaseDefenseCommand.PlaceBase("host", candidate("red", -1.6f), "Red")).state
        val blue = reducer.reduce(red, BaseDefenseCommand.PlaceBase("host", candidate("blue", 1.6f), "Blue"))
        assertTrue(blue.accepted)
        assertEquals(2, blue.state.bases.size)
    }

    @Test
    fun resourcesAreMissionBoundAndCorrectAnswerRewardsEnergy() {
        val placed = twoBases()
        val spawned = reducer.reduce(placed, BaseDefenseCommand.SpawnResources("host", 77, 1000)).state
        val resource = spawned.resources.first()
        val before = spawned.bases.first { it.teamId == resource.owningTeamId }.energy

        val collected = reducer.reduce(spawned, BaseDefenseCommand.SubmitResourceAnswer("host", resource.resourceId, "p1", resource.mission.expectedAnswer.answerString()))

        assertTrue(collected.accepted)
        assertTrue(collected.state.bases.first { it.teamId == resource.owningTeamId }.energy > before)
        assertEquals(ObjectLifecycle.Collected, collected.state.resources.first { it.resourceId == resource.resourceId }.lifecycle)
        assertTrue(collected.state.activeMissions.any { it.sourceObjectId == resource.resourceId })
    }

    @Test
    fun wrongResourceAnswerAppliesLimitedPenaltyNotSuccessfulCollection() {
        val spawned = reducer.reduce(twoBases(), BaseDefenseCommand.SpawnResources("host", 88, 1000)).state
        val resource = spawned.resources.first()

        val result = reducer.reduce(spawned, BaseDefenseCommand.SubmitResourceAnswer("host", resource.resourceId, "p1", "wrong"))

        assertTrue(result.accepted)
        assertEquals(ObjectLifecycle.Active, result.state.resources.first { it.resourceId == resource.resourceId }.lifecycle)
        assertTrue(result.state.resources.first { it.resourceId == resource.resourceId }.rewardEnergy < 25)
    }

    @Test
    fun defenseConstructionRequiresMathAndConsumesEnergy() {
        val placed = twoBases()
        val base = placed.bases.first()
        val type = DefenseType.NumberCannon
        val mission = MathMissionGenerator.generate(type.skill(), placed.sequence + type.ordinal, GradeBand.Mixed, DifficultyLevel.Medium)

        val rejected = reducer.reduce(placed, BaseDefenseCommand.BuildDefense("host", base.teamId, "front", type, "bad"))
        assertFalse(rejected.accepted)

        val built = reducer.reduce(placed, BaseDefenseCommand.BuildDefense("host", base.teamId, "front", type, mission.expectedAnswer.answerString()))
        assertTrue(built.accepted)
        assertEquals(1, built.state.defenses.size)
        assertTrue(built.state.bases.first { it.teamId == base.teamId }.energy < base.energy)
    }

    @Test
    fun waveEnemiesMoveDamageBaseAndPauseStopsTicks() {
        val wave = reducer.reduce(twoBases(), BaseDefenseCommand.SpawnWave("host", 1, 44)).state
        val ticked = reducer.reduce(wave, BaseDefenseCommand.Tick("host", 100f)).state
        assertTrue(ticked.enemies.any { it.progress > 0f })

        val paused = reducer.reduce(ticked, BaseDefenseCommand.Pause("host")).state
        val afterPause = reducer.reduce(paused, BaseDefenseCommand.Tick("host", 100f)).state
        assertEquals(paused.enemies.first().progress, afterPause.enemies.first().progress, 0.0001f)
    }

    @Test
    fun bossRequiresRoleActionsAndAdvancesPhases() {
        val bossState = reducer.reduce(twoBases(), BaseDefenseCommand.StartBoss("host", 99)).state
        val missionAnswer = bossState.boss!!.activeMission!!.expectedAnswer.answerString()
        val afterNavigator = reducer.reduce(bossState, BaseDefenseCommand.SubmitBossAction("host", ArenaRole.Navigator, missionAnswer))
        assertTrue(afterNavigator.accepted)
        assertTrue(afterNavigator.state.boss!!.completedRoles.contains(ArenaRole.Navigator))
    }

    @Test
    fun stagesSupportShortMatchVariantAndHostAuthority() {
        val state = state().copy(config = BaseDefenseConfig(pace = com.indianservers.ai_stem.feature.games.basedefense.MatchPace.Short))
        assertFalse(reducer.reduce(state, BaseDefenseCommand.AdvanceStage("client")).accepted)
        var current = state
        repeat(6) { current = reducer.reduce(current, BaseDefenseCommand.AdvanceStage("host")).state }
        assertEquals(BaseDefenseMatchStage.BossBattle, current.stage)
    }

    @Test
    fun protocolAndPerformanceProfileAreRepresented() {
        val message = GameMessage(roomId = "room", senderPlayerId = "host", sequence = 1, payload = GamePayload.EnemyWaveStarted("m", 2, 6))
        assertEquals(GamePayload.EnemyWaveStarted::class, ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(message)).getOrThrow().payload::class)
        val profile = PerformanceProfile(24, 12, 21.5, reducedQuality = true, particleBudget = 16, maxTextureSize = 512, depthOcclusion = false)
        assertTrue(profile.reducedQuality)
        assertFalse(profile.depthOcclusion)
    }

    private fun state(): BaseDefenseMatchState = BaseDefenseMatchState("match", "host")

    private fun twoBases(): BaseDefenseMatchState {
        val red = reducer.reduce(state(), BaseDefenseCommand.PlaceBase("host", candidate("red", -1.6f), "Red")).state
        return reducer.reduce(red, BaseDefenseCommand.PlaceBase("host", candidate("blue", 1.6f), "Blue")).state
    }

    private fun candidate(teamId: String, x: Float, z: Float = 1.2f): BasePlacementCandidate =
        BasePlacementCandidate(
            teamId = teamId,
            transform = SharedTransform(originVersion = 1, positionMetres = Vector3Dto(x, 0f, z)),
            planeWidthMetres = 2f,
            planeDepthMetres = 2f
        )

    private fun AnswerDefinition.answerString(): String = when (this) {
        is AnswerDefinition.IntegerAnswer -> value.toString()
        is AnswerDefinition.DecimalAnswer -> value.toString()
        is AnswerDefinition.FractionAnswer -> value.toString()
        is AnswerDefinition.MultipleChoiceAnswer -> correctChoiceIds.joinToString(",")
        is AnswerDefinition.ExpressionAnswer -> "${expression.coefficient}${expression.variable}+${expression.constant}"
        is AnswerDefinition.EquationAnswer -> "$variable=$value"
        is AnswerDefinition.CoordinateAnswer -> "($x,$y)"
        is AnswerDefinition.AngleAnswer -> "$degrees degrees"
        is AnswerDefinition.MeasurementAnswer -> "$value ${if (unit == UnitKind.SquareMetres) "m2" else unit.name}"
        is AnswerDefinition.SequenceAnswer -> orderedIds.joinToString(",")
        is AnswerDefinition.MultiStepAnswer -> parts.joinToString("|") { it.answerString() }
    }
}
