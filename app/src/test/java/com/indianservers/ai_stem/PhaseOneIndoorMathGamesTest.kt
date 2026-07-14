package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.equationescape.EquationEscapeEngine
import com.indianservers.ai_stem.feature.games.equationescape.EscapeProgress
import com.indianservers.ai_stem.feature.games.equationescape.EscapePuzzleGraph
import com.indianservers.ai_stem.feature.games.fractionfactory.FractionFactoryEngine
import com.indianservers.ai_stem.feature.games.fractionfactory.FractionMath
import com.indianservers.ai_stem.feature.games.fractionfactory.QualityInspectionStatus
import com.indianservers.ai_stem.feature.games.geometryarchitect.GeometryArchitectEngine
import com.indianservers.ai_stem.feature.games.geometryarchitect.ShapeConstraintType
import com.indianservers.ai_stem.feature.games.mission.Rational
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseOneIndoorMathGamesTest {
    @Test
    fun equationEscapeHasThreePlayableRoomsAndCompletesFirstRoom() {
        val engine = EquationEscapeEngine()
        assertEquals(3, engine.levels.size)
        assertTrue(engine.levels.all { it.puzzles.size >= 6 })

        val level = engine.levels.first()
        var progress = EscapeProgress(level.id)
        val answers = listOf("15", "7", "15", "48", "(3,4)", "M,A,S")

        level.puzzles.zip(answers).forEach { (puzzle, answer) ->
            val current = engine.currentPuzzle(level, progress)
            assertEquals(puzzle.id, current?.id)
            val result = engine.validate(level, progress, puzzle.id, answer)
            assertTrue(result.message, result.correct)
            progress = engine.apply(progress, puzzle, result)
        }

        assertTrue(engine.isLevelComplete(level, progress))
        assertEquals(680, progress.score)
    }

    @Test
    fun equationEscapeGraphValidatorRejectsCycles() {
        val engine = EquationEscapeEngine()
        val valid = EscapePuzzleGraph("room", listOf("a", "b", "c"), listOf("a" to "b", "b" to "c"))
        val cyclic = EscapePuzzleGraph("room", listOf("a", "b"), listOf("a" to "b", "b" to "a"))

        assertTrue(engine.validateGraph(valid).isSuccess)
        assertTrue(engine.validateGraph(cyclic).isFailure)
    }

    @Test
    fun fractionFactoryHasFiftyOrdersAndValidatesEquivalentFormats() {
        val engine = FractionFactoryEngine()
        assertEquals(50, engine.orders.size)
        assertTrue(engine.orders.map { it.id }.distinct().size == 50)

        assertEquals(Rational(3, 2), FractionMath.parseRational("1 1/2"))
        assertEquals(Rational(3, 4), FractionMath.parseRational("75%"))
        assertEquals(Rational(3, 4), FractionMath.parseRational("0.75"))

        val order = engine.orders.first()
        val pass = engine.validate(order, order.target.amount.toString(), order.target.unit)
        val fail = engine.validate(order, "1/999", order.target.unit)

        assertEquals(QualityInspectionStatus.Pass, pass.status)
        assertEquals(QualityInspectionStatus.NeedsCorrection, fail.status)
    }

    @Test
    fun geometryArchitectHasTwelveBriefsAndValidatesAreaVolumeSurface() {
        val engine = GeometryArchitectEngine()
        assertEquals(12, engine.briefs.size)
        assertTrue(engine.briefs.all { template ->
            template.brief.requiredConstraints.any { it.type == ShapeConstraintType.Area } &&
                template.brief.requiredConstraints.any { it.type == ShapeConstraintType.Volume } &&
                template.brief.requiredConstraints.any { it.type == ShapeConstraintType.SurfaceArea }
        })

        val template = engine.briefs.first()
        val pass = engine.validate(template, template.expectedLength, template.expectedWidth, template.expectedHeight)
        val fail = engine.validate(template, template.expectedLength + 4.0, template.expectedWidth, template.expectedHeight)

        assertTrue(pass.messages.joinToString(), pass.valid)
        assertFalse(fail.valid)
    }

    @Test
    fun geometryArchitectBuildsClosedRectanglesAndMeasuresDistance() {
        val engine = GeometryArchitectEngine()
        val rectangle = engine.rectangleConstruction("site", 3f, 2f)

        assertTrue(engine.isClosedPolygon(rectangle))
        assertEquals(5.0, engine.distanceMetres(Vector3Dto(0f, 0f, 0f), Vector3Dto(3f, 4f, 0f)), 0.0001)
    }
}
