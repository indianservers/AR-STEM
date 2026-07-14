package com.indianservers.ai_stem.feature.games.performance

enum class ArenaPerformanceTier { Low, Medium, High }

data class ArenaRenderBudget(
    val tier: ArenaPerformanceTier,
    val maxActiveEnemiesVisible: Int,
    val maxActiveResourcesVisible: Int,
    val maxParticles: Int,
    val meshLodStep: Int,
    val enableDepthOcclusion: Boolean,
    val enableShadows: Boolean,
    val enableHaptics: Boolean,
    val simulationTickHz: Int = 20
)

object ArenaPerformancePolicy {
    fun budgetFor(tier: ArenaPerformanceTier): ArenaRenderBudget = when (tier) {
        ArenaPerformanceTier.Low -> ArenaRenderBudget(
            tier = tier,
            maxActiveEnemiesVisible = 12,
            maxActiveResourcesVisible = 10,
            maxParticles = 80,
            meshLodStep = 3,
            enableDepthOcclusion = false,
            enableShadows = false,
            enableHaptics = true
        )
        ArenaPerformanceTier.Medium -> ArenaRenderBudget(
            tier = tier,
            maxActiveEnemiesVisible = 24,
            maxActiveResourcesVisible = 16,
            maxParticles = 180,
            meshLodStep = 2,
            enableDepthOcclusion = true,
            enableShadows = false,
            enableHaptics = true
        )
        ArenaPerformanceTier.High -> ArenaRenderBudget(
            tier = tier,
            maxActiveEnemiesVisible = 40,
            maxActiveResourcesVisible = 24,
            maxParticles = 360,
            meshLodStep = 1,
            enableDepthOcclusion = true,
            enableShadows = true,
            enableHaptics = true
        )
    }

    fun simulationFairnessInvariant(budgets: Collection<ArenaRenderBudget>): Boolean =
        budgets.map { it.simulationTickHz }.distinct().size == 1
}
