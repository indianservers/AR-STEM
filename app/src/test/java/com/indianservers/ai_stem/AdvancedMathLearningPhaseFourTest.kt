package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.learning.AdvancedCampaignCatalog
import com.indianservers.ai_stem.feature.games.learning.AdvancedCurriculumCatalog
import com.indianservers.ai_stem.feature.games.learning.AdaptiveLearningEngine
import com.indianservers.ai_stem.feature.games.learning.CognitiveLevel
import com.indianservers.ai_stem.feature.games.learning.ContentQualityValidator
import com.indianservers.ai_stem.feature.games.learning.CrossGameProgressionEngine
import com.indianservers.ai_stem.feature.games.learning.LearningDataMigration
import com.indianservers.ai_stem.feature.games.learning.LearningDomain
import com.indianservers.ai_stem.feature.games.learning.LocalChallengeEngine
import com.indianservers.ai_stem.feature.games.learning.MasteryEngine
import com.indianservers.ai_stem.feature.games.learning.MasteryState
import com.indianservers.ai_stem.feature.games.learning.Misconception
import com.indianservers.ai_stem.feature.games.learning.PrerequisiteGraph
import com.indianservers.ai_stem.feature.games.learning.ProceduralMissionGenerator
import com.indianservers.ai_stem.feature.games.learning.RemediationEngine
import com.indianservers.ai_stem.feature.games.learning.SkillEvidence
import com.indianservers.ai_stem.feature.games.learning.TeacherMasterySettings
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMathLearningPhaseFourTest {
    private val generator = ProceduralMissionGenerator()
    private val validator = ContentQualityValidator()

    @Test
    fun curriculumCoversAdvancedDomainsCognitiveLevelsAndAllGames() {
        val domains = AdvancedCurriculumCatalog.skills.map { it.domain }.toSet()
        val cognitive = AdvancedCurriculumCatalog.skills.map { it.cognitiveLevel }.toSet()
        val games = AdvancedCurriculumCatalog.skills.flatMap { it.suitableGameIds }.toSet()

        assertTrue(domains.containsAll(LearningDomain.entries))
        assertTrue(cognitive.contains(CognitiveLevel.Analyse))
        assertTrue(cognitive.contains(CognitiveLevel.Evaluate))
        assertTrue(cognitive.contains(CognitiveLevel.Construct))
        assertTrue(GamesCatalog.games.all { it.id in games })
        assertTrue(AdvancedCurriculumCatalog.skills.all { it.skillId.isNotBlank() && it.masteryThreshold.minAttempts > 0 })
    }

    @Test
    fun prerequisiteGraphReportsMissingSkillsAndAllowsReadiness() {
        val graph = PrerequisiteGraph()
        val missing = graph.missingPrerequisites("simultaneous-equations", emptyMap())

        assertTrue("linear-equation-solving" in missing)
        assertTrue(graph.explain("simultaneous-equations", missing).contains("linear-equation-solving"))
    }

    @Test
    fun proceduralGeneratorValidatesOneThousandDeterministicSeeds() {
        val missions = mutableListOf<com.indianservers.ai_stem.feature.games.learning.ProceduralMission>()
        val skills = AdvancedCurriculumCatalog.skills
        repeat(1000) { index ->
            val skill = skills[index % skills.size]
            val gameId = skill.suitableGameIds.elementAt(index % skill.suitableGameIds.size)
            val mission = generator.generate(gameId, skill.skillId, 10_000L + index, GradeBand.Mixed, skill.difficulty)
            val quality = validator.validate(mission, skill)
            assertTrue("Invalid seed $index: ${quality.issues}", quality.valid)
            missions += mission
        }
        val audit = validator.audit(missions, skills)
        assertEquals(1000, audit.generatedCount)
        assertEquals(1000, audit.validCount)
        assertEquals(0, audit.invalidCount)
        assertTrue(audit.duplicateCount < 30)
    }

    @Test
    fun masteryUsesIndependentAccuracyHintsRecencyAndMisconceptions() {
        val skill = AdvancedCurriculumCatalog.skills.first { it.skillId == "fraction-equivalence" }
        val evidence = (1..4).map {
            SkillEvidence(skill.skillId, "fraction_factory_ar", skill.gradeBand, skill.difficulty, correct = true, independent = true, hintCount = 0, responseSeconds = 20, misconception = null, completedAtDay = 20 + it)
        }
        val mastered = MasteryEngine().update("player", skill, evidence, today = 25)
        val review = MasteryEngine().update(
            "player",
            skill,
            evidence.take(2) + listOf(
                SkillEvidence(skill.skillId, "fraction_factory_ar", skill.gradeBand, skill.difficulty, false, false, 2, 45, Misconception.AddDenominators, 24),
                SkillEvidence(skill.skillId, "fraction_factory_ar", skill.gradeBand, skill.difficulty, false, false, 2, 42, Misconception.AddDenominators, 25)
            ),
            today = 25
        )

        assertEquals(MasteryState.Mastered, mastered.state)
        assertEquals(MasteryState.NeedsReview, review.state)
    }

    @Test
    fun adaptiveRecommendationUsesPrerequisitesAndMisconceptionRemediation() {
        val skill = AdvancedCurriculumCatalog.skills.first { it.skillId == "fraction-equivalence" }
        val needsReview = MasteryEngine().update(
            "player",
            skill,
            listOf(
                SkillEvidence(skill.skillId, "fraction_factory_ar", skill.gradeBand, skill.difficulty, false, false, 2, 40, Misconception.AddDenominators, 1),
                SkillEvidence(skill.skillId, "fraction_factory_ar", skill.gradeBand, skill.difficulty, false, false, 2, 42, Misconception.AddDenominators, 2)
            ),
            today = 3
        )
        val settings = TeacherMasterySettings(emptyMap(), enforcePrerequisites = true, allowTeacherOverride = true, dailyChallengesEnabled = true, maxGradeBand = GradeBand.Grade10, advancedTopicAccess = true, workedSolutionsEnabled = true, crossGameRecommendationsEnabled = true)
        val rec = AdaptiveLearningEngine().recommend("player", "fraction-ratio-scale", "geometry_architect_ar", mapOf(skill.skillId to needsReview), settings, 99)

        assertEquals("fraction-equivalence", rec.skillId)
        assertTrue(rec.reason.contains("fraction-equivalence"))
        assertTrue(rec.remediation?.misconception == Misconception.AddDenominators)
    }

    @Test
    fun remediationIsMisconceptionSpecificAndIncludesRecheck() {
        val skill = AdvancedCurriculumCatalog.skills.first { it.skillId == "coordinate-slope" }
        val remediation = RemediationEngine().forMisconception("player", "coordinate_conquest_ar", skill, Misconception.SlopeRiseRunSwap, 7)

        assertTrue(remediation.explanation.contains("Slope"))
        assertEquals("coordinate-slope", remediation.guidedMission.skillId)
        assertEquals("coordinate-slope", remediation.independentRecheck.skillId)
    }

    @Test
    fun campaignsExistForAllSixGamesAndUnlockByMastery() {
        val campaigns = AdvancedCampaignCatalog.campaigns()
        assertEquals(GamesCatalog.games.map { it.id }.toSet(), campaigns.map { it.gameId }.toSet())
        assertTrue(campaigns.all { campaign -> campaign.chapters.size >= 3 && campaign.chapters.any { chapter -> chapter.nodes.any { it.finalChallenge } } })
        val first = campaigns.first()
        val all = AdvancedCampaignCatalog.unlockedNodes(first, com.indianservers.ai_stem.feature.games.learning.CampaignProgress("p", first.campaignId, emptySet(), 0), emptyMap(), teacherUnlockAll = true)
        assertTrue(all.size >= 3)
    }

    @Test
    fun crossGameProgressionDailyWeeklyChallengesAndMigrationAreLocal() {
        val journey = CrossGameProgressionEngine().journeyFor("fraction-ratio-scale")
        val dailyA = LocalChallengeEngine().daily(20260714, 1234, "fraction_factory_ar")
        val dailyB = LocalChallengeEngine().daily(20260714, 1234, "fraction_factory_ar")
        val weekly = LocalChallengeEngine().weekly(202629, 1234)
        val migration = LearningDataMigration().migrateMastery(null, "math_expedition_ar", null)

        assertTrue(journey.size >= 3)
        assertEquals(dailyA.mission.expectedAnswer, dailyB.mission.expectedAnswer)
        assertFalse(dailyA.irreversibleReward)
        assertEquals(6, weekly.missions.size)
        assertFalse(weekly.irreversibleReward)
        assertTrue(migration.migrated)
        assertTrue(migration.warnings.isNotEmpty())
    }
}
