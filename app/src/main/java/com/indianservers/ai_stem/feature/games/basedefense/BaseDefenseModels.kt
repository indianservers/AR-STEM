package com.indianservers.ai_stem.feature.games.basedefense

import com.indianservers.ai_stem.feature.games.mission.MathMission
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MathTopic
import com.indianservers.ai_stem.feature.games.mission.ValidationResult
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto

enum class BaseDefenseMatchStage {
    SharedArCalibration,
    TeamBasePlacement,
    TutorialMission,
    ResourceCollectionRound,
    BaseConstructionRound,
    EnemyWaveOne,
    TeamRoleRotation,
    EnemyWaveTwo,
    StrategicUpgradePhase,
    BossBattle,
    ResultsLearningSummary,
    Completed,
    Cancelled
}

enum class BaseDefenseMode { Cooperative, Competitive }
enum class MatchPace { Short, Standard }
enum class ResourceType { NumberCrystal, FractionOrb, AlgebraKey, GeometryShield, CoordinateBeacon, FormulaCore }
enum class DefenseType { ShieldWall, NumberCannon, GeometryTower, AlgebraLaser, CoordinateRadar, FormulaBooster }
enum class EnemyType { ErrorDrone, FractionBug, EquationBot, AngleRaider, CoordinateGlitch }
enum class BossPhase { CoordinateWeakPoint, AngleTrajectory, AlgebraicLock, ShieldShape, SynchronizedAction, Defeated }
enum class ObjectLifecycle { Preview, Active, Collected, Expired, Destroyed }

data class BaseDefenseConfig(
    val mode: BaseDefenseMode = BaseDefenseMode.Competitive,
    val pace: MatchPace = MatchPace.Standard,
    val maxClassroomRadiusMetres: Float = 5.0f,
    val minBaseDistanceFromMarkerMetres: Float = 0.75f,
    val minBaseSeparationMetres: Float = 1.25f,
    val resourceLimit: Int = 8,
    val lowPerformanceMode: Boolean = false,
    val depthOcclusionEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

data class TeamBase(
    val teamId: String,
    val teamName: String,
    val transform: SharedTransform,
    val health: Int = 500,
    val maxHealth: Int = 500,
    val energy: Int = 100,
    val score: Int = 0,
    val defenseSlots: List<DefenseSlot> = defaultDefenseSlots(),
    val upgrades: Set<String> = emptySet()
)

data class DefenseSlot(
    val slotId: String,
    val localOffsetMetres: Vector3Dto,
    val occupiedDefenseId: String? = null
)

data class ResourceSpawn(
    val resourceId: String,
    val type: ResourceType,
    val transform: SharedTransform,
    val mission: MathMission,
    val owningTeamId: String?,
    val spawnedAtMs: Long,
    val expiresAtMs: Long,
    val lifecycle: ObjectLifecycle = ObjectLifecycle.Active,
    val rewardEnergy: Int = 25
)

data class DefenseBuild(
    val defenseId: String,
    val type: DefenseType,
    val teamId: String,
    val slotId: String,
    val transform: SharedTransform,
    val health: Int,
    val charge: Int,
    val scale: Float = 1f,
    val active: Boolean = true
)

data class EnemyState(
    val enemyId: String,
    val type: EnemyType,
    val targetTeamId: String,
    val spawn: Vector3Dto,
    val target: Vector3Dto,
    val progress: Float,
    val speedMetresPerSecond: Float,
    val health: Int,
    val damage: Int,
    val wave: Int,
    val lifecycle: ObjectLifecycle = ObjectLifecycle.Active
) {
    val position: Vector3Dto
        get() = spawn + (target - spawn) * progress.coerceIn(0f, 1f)
}

data class BossState(
    val bossId: String = "geometry-titan",
    val phase: BossPhase = BossPhase.CoordinateWeakPoint,
    val health: Int = 1200,
    val requiredRoles: Set<ArenaRole> = setOf(ArenaRole.Navigator, ArenaRole.Analyst, ArenaRole.Solver, ArenaRole.Builder, ArenaRole.Commander),
    val completedRoles: Set<ArenaRole> = emptySet(),
    val activeMission: MathMission? = null
)

data class ActiveMissionBinding(
    val missionId: String,
    val teamId: String,
    val roleRequired: ArenaRole,
    val sourceObjectId: String,
    val sourceType: String,
    val mission: MathMission,
    val validation: ValidationResult? = null
)

data class BaseDefenseMatchState(
    val matchId: String,
    val hostPlayerId: String,
    val config: BaseDefenseConfig = BaseDefenseConfig(),
    val stage: BaseDefenseMatchStage = BaseDefenseMatchStage.SharedArCalibration,
    val bases: List<TeamBase> = emptyList(),
    val resources: List<ResourceSpawn> = emptyList(),
    val defenses: List<DefenseBuild> = emptyList(),
    val enemies: List<EnemyState> = emptyList(),
    val boss: BossState? = null,
    val activeMissions: List<ActiveMissionBinding> = emptyList(),
    val currentWave: Int = 0,
    val paused: Boolean = false,
    val roleRotationIndex: Int = 0,
    val matchClockSeconds: Int = 0,
    val sequence: Long = 0,
    val lastError: String? = null
)

data class BasePlacementCandidate(
    val teamId: String,
    val transform: SharedTransform,
    val planeWidthMetres: Float,
    val planeDepthMetres: Float,
    val insideWall: Boolean = false
)

data class PerformanceProfile(
    val pooledObjects: Int,
    val activeObjects: Int,
    val frameTimeMs: Double,
    val reducedQuality: Boolean,
    val particleBudget: Int,
    val maxTextureSize: Int,
    val depthOcclusion: Boolean
)

data class AudioHapticsSettings(
    val musicEnabled: Boolean,
    val soundEffectsEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val volumeAware: Boolean = true,
    val accessibilityVisualCues: Boolean = true
)

fun defaultDefenseSlots(): List<DefenseSlot> = listOf(
    DefenseSlot("front", Vector3Dto(0f, 0f, 0.55f)),
    DefenseSlot("left", Vector3Dto(-0.55f, 0f, 0f)),
    DefenseSlot("right", Vector3Dto(0.55f, 0f, 0f)),
    DefenseSlot("rear", Vector3Dto(0f, 0f, -0.55f))
)

fun ResourceType.skill(): MathSkill = when (this) {
    ResourceType.NumberCrystal -> MathSkill.IntegerOperations
    ResourceType.FractionOrb -> MathSkill.FractionAddSubtract
    ResourceType.AlgebraKey -> MathSkill.TwoStepEquation
    ResourceType.GeometryShield -> MathSkill.Area
    ResourceType.CoordinateBeacon -> MathSkill.SlopeBasics
    ResourceType.FormulaCore -> MathSkill.Substitution
}

fun DefenseType.skill(): MathSkill = when (this) {
    DefenseType.ShieldWall -> MathSkill.Area
    DefenseType.NumberCannon -> MathSkill.FractionMultiplyDivide
    DefenseType.GeometryTower -> MathSkill.AngleRelationships
    DefenseType.AlgebraLaser -> MathSkill.TwoStepEquation
    DefenseType.CoordinateRadar -> MathSkill.CoordinateIdentification
    DefenseType.FormulaBooster -> MathSkill.Substitution
}

fun DefenseType.energyCost(): Int = when (this) {
    DefenseType.ShieldWall -> 35
    DefenseType.NumberCannon -> 45
    DefenseType.GeometryTower -> 50
    DefenseType.AlgebraLaser -> 60
    DefenseType.CoordinateRadar -> 40
    DefenseType.FormulaBooster -> 55
}
