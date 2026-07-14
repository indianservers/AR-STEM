package com.indianservers.ai_stem.feature.games.basedefense

import com.indianservers.ai_stem.feature.games.mission.AnswerValidationEngine
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.MathMissionGenerator
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

sealed interface BaseDefenseCommand {
    data class PlaceBase(val hostPlayerId: String, val candidate: BasePlacementCandidate, val teamName: String) : BaseDefenseCommand
    data class AdvanceStage(val hostPlayerId: String) : BaseDefenseCommand
    data class SpawnResources(val hostPlayerId: String, val seed: Long, val nowMs: Long) : BaseDefenseCommand
    data class SubmitResourceAnswer(val hostPlayerId: String, val resourceId: String, val playerId: String, val answer: String) : BaseDefenseCommand
    data class BuildDefense(val hostPlayerId: String, val teamId: String, val slotId: String, val type: DefenseType, val answer: String) : BaseDefenseCommand
    data class SpawnWave(val hostPlayerId: String, val wave: Int, val seed: Long) : BaseDefenseCommand
    data class Tick(val hostPlayerId: String, val deltaSeconds: Float) : BaseDefenseCommand
    data class ActivateDefense(val hostPlayerId: String, val defenseId: String, val enemyId: String, val role: ArenaRole, val answer: String) : BaseDefenseCommand
    data class StartBoss(val hostPlayerId: String, val seed: Long) : BaseDefenseCommand
    data class SubmitBossAction(val hostPlayerId: String, val role: ArenaRole, val answer: String) : BaseDefenseCommand
    data class Pause(val hostPlayerId: String) : BaseDefenseCommand
    data class Resume(val hostPlayerId: String) : BaseDefenseCommand
    data class ReconnectPlayer(val hostPlayerId: String, val playerId: String) : BaseDefenseCommand
    data class EndMatch(val hostPlayerId: String) : BaseDefenseCommand
}

data class BaseDefenseResult(val state: BaseDefenseMatchState, val accepted: Boolean, val reason: String? = null)

object BasePlacementPolicy {
    fun validate(candidate: BasePlacementCandidate, existing: List<TeamBase>, config: BaseDefenseConfig): Result<Unit> = runCatching {
        val position = candidate.transform.positionMetres
        val distanceFromMarker = hypot(position.x.toDouble(), position.z.toDouble()).toFloat()
        require(distanceFromMarker >= config.minBaseDistanceFromMarkerMetres) { "Base is too close to the shared marker." }
        require(distanceFromMarker <= config.maxClassroomRadiusMetres) { "Base is outside classroom radius." }
        require(!candidate.insideWall) { "Base cannot be inside a detected wall." }
        require(candidate.planeWidthMetres >= 0.9f && candidate.planeDepthMetres >= 0.9f) { "Plane is too small for a base." }
        existing.forEach {
            val other = it.transform.positionMetres
            require(hypot((position.x - other.x).toDouble(), (position.z - other.z).toDouble()) >= config.minBaseSeparationMetres) {
                "Team bases are too close."
            }
        }
    }
}

class ResourceSpawner {
    fun spawn(state: BaseDefenseMatchState, seed: Long, nowMs: Long): List<ResourceSpawn> {
        val random = Random(seed)
        val bases = state.bases
        val count = state.config.resourceLimit.coerceAtMost(if (state.config.pace == MatchPace.Short) 4 else 8)
        val resources = mutableListOf<ResourceSpawn>()
        var attempts = 0
        while (resources.size < count && attempts < 80) {
            attempts++
            val type = ResourceType.entries[resources.size % ResourceType.entries.size]
            val position = Vector3Dto(random.nextDouble(-3.5, 3.5).toFloat(), 0.05f, random.nextDouble(-3.5, 3.5).toFloat())
            val tooCloseToBase = bases.any { distance2d(position, it.transform.positionMetres) < 0.9f }
            val clustered = resources.any { distance2d(position, it.transform.positionMetres) < 0.75f }
            if (tooCloseToBase || clustered || kotlin.math.abs(position.y) > 1.5f) continue
            val mission = MathMissionGenerator.generate(type.skill(), seed + resources.size * 97L, GradeBand.Mixed, DifficultyLevel.Medium)
            resources += ResourceSpawn(
                resourceId = "res-${state.sequence}-${resources.size}",
                type = type,
                transform = SharedTransform(originVersion = state.bases.firstOrNull()?.transform?.originVersion ?: 0, positionMetres = position),
                mission = mission,
                owningTeamId = bases.getOrNull(resources.size % bases.size.coerceAtLeast(1))?.teamId,
                spawnedAtMs = nowMs,
                expiresAtMs = nowMs + 180_000L
            )
        }
        return resources
    }
}

class EnemyWaveSystem {
    fun spawnWave(state: BaseDefenseMatchState, wave: Int, seed: Long): List<EnemyState> {
        val random = Random(seed + wave)
        val count = if (wave == 1) 4 else 6
        return (0 until count).map { index ->
            val targetBase = state.bases[index % state.bases.size]
            val angle = random.nextDouble(0.0, Math.PI * 2.0)
            val spawn = Vector3Dto((kotlin.math.cos(angle) * 4.5).toFloat(), 0.25f, (kotlin.math.sin(angle) * 4.5).toFloat())
            EnemyState(
                enemyId = "wave-$wave-enemy-$index",
                type = EnemyType.entries[index % EnemyType.entries.size],
                targetTeamId = targetBase.teamId,
                spawn = spawn,
                target = targetBase.transform.positionMetres,
                progress = 0f,
                speedMetresPerSecond = 0.08f + wave * 0.025f,
                health = 80 + wave * 35,
                damage = 20 + wave * 10,
                wave = wave
            )
        }
    }
}

class BaseDefenseReducer(
    private val validator: AnswerValidationEngine = AnswerValidationEngine(),
    private val spawner: ResourceSpawner = ResourceSpawner(),
    private val waveSystem: EnemyWaveSystem = EnemyWaveSystem()
) {
    fun reduce(state: BaseDefenseMatchState, command: BaseDefenseCommand): BaseDefenseResult =
        runCatching {
            when (command) {
                is BaseDefenseCommand.PlaceBase -> {
                    host(state, command.hostPlayerId)
                    BasePlacementPolicy.validate(command.candidate, state.bases, state.config).getOrThrow()
                    val base = TeamBase(command.candidate.teamId, command.teamName, command.candidate.transform)
                    state.bump().copy(bases = state.bases.filterNot { it.teamId == base.teamId } + base).ok()
                }
                is BaseDefenseCommand.AdvanceStage -> {
                    host(state, command.hostPlayerId)
                    state.bump().copy(stage = state.stage.next(state.config), paused = false).ok()
                }
                is BaseDefenseCommand.SpawnResources -> {
                    host(state, command.hostPlayerId)
                    require(state.bases.isNotEmpty()) { "Place bases before spawning resources." }
                    state.bump().copy(resources = state.resources + spawner.spawn(state, command.seed, command.nowMs)).ok()
                }
                is BaseDefenseCommand.SubmitResourceAnswer -> collectResource(state, command)
                is BaseDefenseCommand.BuildDefense -> buildDefense(state, command)
                is BaseDefenseCommand.SpawnWave -> {
                    host(state, command.hostPlayerId)
                    require(state.bases.isNotEmpty()) { "Bases required." }
                    state.bump().copy(enemies = state.enemies + waveSystem.spawnWave(state, command.wave, command.seed), currentWave = command.wave).ok()
                }
                is BaseDefenseCommand.Tick -> tick(state, command)
                is BaseDefenseCommand.ActivateDefense -> activateDefense(state, command)
                is BaseDefenseCommand.StartBoss -> {
                    host(state, command.hostPlayerId)
                    val mission = MathMissionGenerator.generate(com.indianservers.ai_stem.feature.games.mission.MathSkill.CoordinateIdentification, command.seed, GradeBand.Mixed, DifficultyLevel.Hard)
                    state.bump().copy(stage = BaseDefenseMatchStage.BossBattle, boss = BossState(activeMission = mission)).ok()
                }
                is BaseDefenseCommand.SubmitBossAction -> bossAction(state, command)
                is BaseDefenseCommand.Pause -> { host(state, command.hostPlayerId); state.bump().copy(paused = true).ok() }
                is BaseDefenseCommand.Resume -> { host(state, command.hostPlayerId); state.bump().copy(paused = false).ok() }
                is BaseDefenseCommand.ReconnectPlayer -> { host(state, command.hostPlayerId); state.bump().copy(lastError = "Restored state for ${command.playerId}").ok() }
                is BaseDefenseCommand.EndMatch -> { host(state, command.hostPlayerId); state.bump().copy(stage = BaseDefenseMatchStage.ResultsLearningSummary, paused = false).ok() }
            }
        }.getOrElse { state.copy(lastError = it.message).let { BaseDefenseResult(it, false, it.lastError) } }

    private fun collectResource(state: BaseDefenseMatchState, command: BaseDefenseCommand.SubmitResourceAnswer): BaseDefenseResult {
        host(state, command.hostPlayerId)
        val resource = state.resources.firstOrNull { it.resourceId == command.resourceId && it.lifecycle == ObjectLifecycle.Active } ?: error("Resource is unavailable.")
        val result = validator.validate(command.answer, resource.mission.expectedAnswer)
        val teamId = resource.owningTeamId ?: state.bases.first().teamId
        val reward = if (result.status == ValidationStatus.Correct || result.status == ValidationStatus.CorrectButNeedsSimplification) resource.rewardEnergy else 8
        return state.bump().copy(
            resources = state.resources.map { if (it.resourceId == resource.resourceId) it.copy(lifecycle = if (reward >= resource.rewardEnergy) ObjectLifecycle.Collected else ObjectLifecycle.Active, rewardEnergy = reward) else it },
            bases = state.bases.map { if (it.teamId == teamId) it.copy(energy = it.energy + reward, score = it.score + reward * 2) else it },
            activeMissions = state.activeMissions + ActiveMissionBinding(resource.mission.id, teamId, ArenaRole.Solver, resource.resourceId, resource.type.name, resource.mission, result)
        ).ok()
    }

    private fun buildDefense(state: BaseDefenseMatchState, command: BaseDefenseCommand.BuildDefense): BaseDefenseResult {
        host(state, command.hostPlayerId)
        val base = state.bases.firstOrNull { it.teamId == command.teamId } ?: error("Unknown team.")
        val slot = base.defenseSlots.firstOrNull { it.slotId == command.slotId } ?: error("Unknown slot.")
        require(slot.occupiedDefenseId == null) { "Slot occupied." }
        require(base.energy >= command.type.energyCost()) { "Not enough energy." }
        val mission = MathMissionGenerator.generate(command.type.skill(), state.sequence + command.type.ordinal, GradeBand.Mixed, DifficultyLevel.Medium)
        val validation = validator.validate(command.answer, mission.expectedAnswer)
        require(validation.status == ValidationStatus.Correct || validation.status == ValidationStatus.CorrectButNeedsSimplification) { "Math required before construction." }
        val defense = DefenseBuild("def-${state.sequence}-${command.slotId}", command.type, base.teamId, slot.slotId, base.transform.copy(positionMetres = base.transform.positionMetres + slot.localOffsetMetres), health = 180, charge = 100)
        return state.bump().copy(
            defenses = state.defenses + defense,
            bases = state.bases.map {
                if (it.teamId == base.teamId) it.copy(
                    energy = it.energy - command.type.energyCost(),
                    defenseSlots = it.defenseSlots.map { s -> if (s.slotId == slot.slotId) s.copy(occupiedDefenseId = defense.defenseId) else s },
                    score = it.score + 60
                ) else it
            },
            activeMissions = state.activeMissions + ActiveMissionBinding(mission.id, base.teamId, ArenaRole.Builder, defense.defenseId, command.type.name, mission, validation)
        ).ok()
    }

    private fun tick(state: BaseDefenseMatchState, command: BaseDefenseCommand.Tick): BaseDefenseResult {
        host(state, command.hostPlayerId)
        if (state.paused) return state.ok()
        val moved = state.enemies.map { enemy ->
            if (enemy.lifecycle != ObjectLifecycle.Active) enemy else enemy.copy(progress = (enemy.progress + command.deltaSeconds * enemy.speedMetresPerSecond).coerceAtMost(1f))
        }
        val damagedBases = state.bases.map { base ->
            val attackers = moved.filter { it.lifecycle == ObjectLifecycle.Active && it.targetTeamId == base.teamId && it.progress >= 1f }
            if (attackers.isEmpty()) base else base.copy(health = max(0, base.health - attackers.sumOf { it.damage }))
        }
        return state.bump().copy(enemies = moved.map { if (it.progress >= 1f) it.copy(lifecycle = ObjectLifecycle.Destroyed) else it }, bases = damagedBases, matchClockSeconds = state.matchClockSeconds + command.deltaSeconds.toInt()).ok()
    }

    private fun activateDefense(state: BaseDefenseMatchState, command: BaseDefenseCommand.ActivateDefense): BaseDefenseResult {
        host(state, command.hostPlayerId)
        val defense = state.defenses.firstOrNull { it.defenseId == command.defenseId && it.active } ?: error("Defense unavailable.")
        val enemy = state.enemies.firstOrNull { it.enemyId == command.enemyId && it.lifecycle == ObjectLifecycle.Active } ?: error("Enemy unavailable.")
        val skill = when (defense.type) {
            DefenseType.NumberCannon -> com.indianservers.ai_stem.feature.games.mission.MathSkill.AngleRelationships
            DefenseType.AlgebraLaser -> com.indianservers.ai_stem.feature.games.mission.MathSkill.TwoStepEquation
            DefenseType.CoordinateRadar -> com.indianservers.ai_stem.feature.games.mission.MathSkill.GridDistance
            DefenseType.ShieldWall -> com.indianservers.ai_stem.feature.games.mission.MathSkill.FractionAddSubtract
            DefenseType.GeometryTower -> com.indianservers.ai_stem.feature.games.mission.MathSkill.TriangleAngleSum
            DefenseType.FormulaBooster -> com.indianservers.ai_stem.feature.games.mission.MathSkill.Substitution
        }
        val mission = MathMissionGenerator.generate(skill, state.sequence + defense.defenseId.hashCode(), GradeBand.Mixed, DifficultyLevel.Medium)
        val validation = validator.validate(command.answer, mission.expectedAnswer)
        require(validation.status == ValidationStatus.Correct || validation.status == ValidationStatus.CorrectButNeedsSimplification) { "Wrong answers do not activate attacks." }
        val damage = 90 + defense.charge / 2
        return state.bump().copy(
            enemies = state.enemies.map { if (it.enemyId == enemy.enemyId) it.copy(health = it.health - damage, lifecycle = if (it.health - damage <= 0) ObjectLifecycle.Destroyed else it.lifecycle) else it },
            defenses = state.defenses.map { if (it.defenseId == defense.defenseId) it.copy(charge = (it.charge - 25).coerceAtLeast(0)) else it },
            bases = state.bases.map { if (it.teamId == defense.teamId) it.copy(score = it.score + damage) else it },
            activeMissions = state.activeMissions + ActiveMissionBinding(mission.id, defense.teamId, command.role, enemy.enemyId, defense.type.name, mission, validation)
        ).ok()
    }

    private fun bossAction(state: BaseDefenseMatchState, command: BaseDefenseCommand.SubmitBossAction): BaseDefenseResult {
        host(state, command.hostPlayerId)
        val boss = state.boss ?: error("Boss not active.")
        require(command.role in boss.requiredRoles) { "This role is not part of the boss action." }
        val mission = boss.activeMission ?: MathMissionGenerator.generate(com.indianservers.ai_stem.feature.games.mission.MathSkill.CoordinateIdentification, state.sequence, GradeBand.Mixed, DifficultyLevel.Hard)
        val validation = validator.validate(command.answer, mission.expectedAnswer)
        require(validation.status == ValidationStatus.Correct || validation.status == ValidationStatus.CorrectButNeedsSimplification) { "Boss action failed. Try with team clues." }
        val completedRoles = boss.completedRoles + command.role
        val nextPhase = if (completedRoles.containsAll(boss.requiredRoles)) boss.phase.next() else boss.phase
        val nextHealth = if (nextPhase != boss.phase) boss.health - 240 else boss.health - 80
        val defeated = nextHealth <= 0 || nextPhase == BossPhase.Defeated
        return state.bump().copy(
            boss = boss.copy(phase = if (defeated) BossPhase.Defeated else nextPhase, health = nextHealth.coerceAtLeast(0), completedRoles = if (nextPhase != boss.phase) emptySet() else completedRoles),
            bases = state.bases.map { it.copy(score = it.score + 120) },
            stage = if (defeated) BaseDefenseMatchStage.ResultsLearningSummary else state.stage
        ).ok()
    }

    private fun host(state: BaseDefenseMatchState, playerId: String) {
        require(playerId == state.hostPlayerId) { "Host authority required." }
    }

    private fun BaseDefenseMatchState.bump(): BaseDefenseMatchState = copy(sequence = sequence + 1, lastError = null)
    private fun BaseDefenseMatchState.ok(): BaseDefenseResult = BaseDefenseResult(this, true)
    private fun BaseDefenseMatchStage.next(config: BaseDefenseConfig): BaseDefenseMatchStage = when (this) {
        BaseDefenseMatchStage.SharedArCalibration -> BaseDefenseMatchStage.TeamBasePlacement
        BaseDefenseMatchStage.TeamBasePlacement -> BaseDefenseMatchStage.TutorialMission
        BaseDefenseMatchStage.TutorialMission -> BaseDefenseMatchStage.ResourceCollectionRound
        BaseDefenseMatchStage.ResourceCollectionRound -> BaseDefenseMatchStage.BaseConstructionRound
        BaseDefenseMatchStage.BaseConstructionRound -> BaseDefenseMatchStage.EnemyWaveOne
        BaseDefenseMatchStage.EnemyWaveOne -> if (config.pace == MatchPace.Short) BaseDefenseMatchStage.BossBattle else BaseDefenseMatchStage.TeamRoleRotation
        BaseDefenseMatchStage.TeamRoleRotation -> BaseDefenseMatchStage.EnemyWaveTwo
        BaseDefenseMatchStage.EnemyWaveTwo -> BaseDefenseMatchStage.StrategicUpgradePhase
        BaseDefenseMatchStage.StrategicUpgradePhase -> BaseDefenseMatchStage.BossBattle
        BaseDefenseMatchStage.BossBattle -> BaseDefenseMatchStage.ResultsLearningSummary
        BaseDefenseMatchStage.ResultsLearningSummary -> BaseDefenseMatchStage.Completed
        BaseDefenseMatchStage.Completed, BaseDefenseMatchStage.Cancelled -> this
    }
    private fun BossPhase.next(): BossPhase = when (this) {
        BossPhase.CoordinateWeakPoint -> BossPhase.AngleTrajectory
        BossPhase.AngleTrajectory -> BossPhase.AlgebraicLock
        BossPhase.AlgebraicLock -> BossPhase.ShieldShape
        BossPhase.ShieldShape -> BossPhase.SynchronizedAction
        BossPhase.SynchronizedAction, BossPhase.Defeated -> BossPhase.Defeated
    }
}

private fun distance2d(a: Vector3Dto, b: Vector3Dto): Float = hypot((a.x - b.x).toDouble(), (a.z - b.z).toDouble()).toFloat()
