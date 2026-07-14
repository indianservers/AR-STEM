package com.indianservers.ai_stem.feature.games.mission

import kotlin.math.abs

class AnswerValidationEngine {
    fun validate(answer: String, expected: AnswerDefinition, alreadySubmitted: Boolean = false, timedOut: Boolean = false): ValidationResult {
        if (alreadySubmitted) return ValidationResult(ValidationStatus.AlreadySubmitted, "This answer was already submitted.", 0.0)
        if (timedOut) return ValidationResult(ValidationStatus.TimedOut, "Time is up.", 0.0)
        val raw = answer.trim()
        if (raw.isBlank()) return ValidationResult(ValidationStatus.InvalidFormat, "Enter an answer.", 0.0)
        return when (expected) {
            is AnswerDefinition.IntegerAnswer -> validateInteger(raw, expected.value)
            is AnswerDefinition.DecimalAnswer -> validateDecimal(raw, expected.value, expected.tolerance, expected.unit)
            is AnswerDefinition.FractionAnswer -> validateFraction(raw, expected.value, expected.requireSimplified)
            is AnswerDefinition.MultipleChoiceAnswer -> validateSelection(raw, expected.correctChoiceIds, expected.allowPartial)
            is AnswerDefinition.ExpressionAnswer -> validateExpression(raw, expected.expression)
            is AnswerDefinition.EquationAnswer -> validateEquation(raw, expected.variable, expected.value)
            is AnswerDefinition.CoordinateAnswer -> validateCoordinate(raw, expected.x, expected.y)
            is AnswerDefinition.AngleAnswer -> validateAngle(raw, expected.degrees, expected.tolerance)
            is AnswerDefinition.MeasurementAnswer -> validateMeasurement(raw, expected.value, expected.unit, expected.tolerance)
            is AnswerDefinition.SequenceAnswer -> validateSequence(raw, expected.orderedIds)
            is AnswerDefinition.MultiStepAnswer -> validateMultiStep(raw, expected)
        }
    }

    private fun validateInteger(raw: String, expected: Int): ValidationResult {
        val value = raw.toIntOrNull() ?: return invalid("Use an integer.")
        return if (value == expected) correct("Correct integer.") else incorrect("Check the sign and operation.")
    }

    private fun validateDecimal(raw: String, expected: Double, tolerance: Double, unit: UnitKind): ValidationResult {
        val parsed = parseNumberAndUnit(raw)
        val value = parsed.first ?: return invalid("Use a decimal number.")
        if (unit != UnitKind.None && parsed.second == UnitKind.None) return ValidationResult(ValidationStatus.MissingUnit, "Include the unit.", 0.4)
        if (unit != UnitKind.None && parsed.second != unit) return incorrect("Use ${unit.label()}.")
        return if (abs(value - expected) <= tolerance) correct("Correct within tolerance.") else incorrect("Your decimal is outside the tolerance.")
    }

    private fun validateFraction(raw: String, expected: Rational, requireSimplified: Boolean): ValidationResult {
        val value = parseRational(raw) ?: return invalid("Use a fraction like 3/4.")
        if (value.denominator == 0) return invalid("Denominator cannot be zero.")
        val equivalent = value.normalized == expected.normalized
        if (!equivalent) return incorrect("The fraction value is not equivalent.")
        return if (requireSimplified && !value.simplified) {
            ValidationResult(ValidationStatus.CorrectButNeedsSimplification, "Correct value, but simplify the fraction.", 0.75)
        } else {
            correct("Correct fraction.")
        }
    }

    private fun validateSelection(raw: String, expected: Set<String>, allowPartial: Boolean): ValidationResult {
        val selected = raw.split(",", " ", ";").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
        if (selected.isEmpty()) return invalid("Choose one or more options.")
        if (selected == expected.map { it.uppercase() }.toSet()) return correct("Correct choice.")
        val correctCount = selected.count { it in expected.map(String::uppercase).toSet() }
        return if (allowPartial && correctCount > 0) {
            ValidationResult(ValidationStatus.PartiallyCorrect, "Some selections are correct.", correctCount.toDouble() / expected.size, correctCount, expected.size)
        } else incorrect("Review the choices.")
    }

    private fun validateExpression(raw: String, expected: LinearExpression): ValidationResult {
        val expression = parseLinearExpression(raw, expected.variable) ?: return invalid("Use a linear expression such as 2x + 3.")
        return if (expression.equivalentTo(expected)) correct("Equivalent expression.") else incorrect("The expression is not equivalent.")
    }

    private fun validateEquation(raw: String, variable: String, expected: Rational): ValidationResult {
        val cleaned = raw.replace(" ", "")
        val value = when {
            cleaned.startsWith("$variable=") -> parseRational(cleaned.substringAfter("="))
            cleaned.startsWith("${variable.uppercase()}=") -> parseRational(cleaned.substringAfter("="))
            else -> parseRational(cleaned)
        } ?: return invalid("Use a solution such as $variable=3.")
        return if (value.normalized == expected.normalized) correct("Correct solution.") else incorrect("Substitute your value back into the equation.")
    }

    private fun validateCoordinate(raw: String, expectedX: Rational, expectedY: Rational): ValidationResult {
        val pair = parseCoordinate(raw) ?: return invalid("Use an ordered pair like (2, -3).")
        if (
            expectedX.normalized != expectedY.normalized &&
            pair.first.normalized == expectedY.normalized &&
            pair.second.normalized == expectedX.normalized
        ) {
            return ValidationResult(ValidationStatus.WrongCoordinateOrder, "Coordinates are reversed. Use (x, y).", 0.5)
        }
        return if (pair.first.normalized == expectedX.normalized && pair.second.normalized == expectedY.normalized) {
            correct("Correct coordinate.")
        } else incorrect("Check x first, then y.")
    }

    private fun validateAngle(raw: String, expected: Double, tolerance: Double): ValidationResult {
        val value = raw.lowercase().replace("degrees", "").replace("degree", "").replace("deg", "").replace("°", "").trim().toDoubleOrNull()
            ?: return invalid("Use an angle in degrees.")
        return if (abs(value - expected) <= tolerance) correct("Correct angle.") else incorrect("Use the angle relationship carefully.")
    }

    private fun validateMeasurement(raw: String, expected: Double, unit: UnitKind, tolerance: Double): ValidationResult =
        validateDecimal(raw, expected, tolerance, unit)

    private fun validateSequence(raw: String, expected: List<String>): ValidationResult {
        val sequence = raw.split(",", ">", " ").map { it.trim() }.filter { it.isNotBlank() }
        return if (sequence == expected) correct("Correct order.") else incorrect("The order is not correct.")
    }

    private fun validateMultiStep(raw: String, expected: AnswerDefinition.MultiStepAnswer): ValidationResult {
        val parts = raw.split("|").map { it.trim() }
        val results = expected.parts.mapIndexed { index, answerDefinition -> validate(parts.getOrElse(index) { "" }, answerDefinition) }
        val correctParts = results.count { it.status == ValidationStatus.Correct || it.status == ValidationStatus.CorrectButNeedsSimplification }
        return when {
            correctParts >= expected.requiredCorrectParts -> ValidationResult(ValidationStatus.Correct, "Team answer complete.", 1.0, correctParts, expected.parts.size)
            correctParts > 0 -> ValidationResult(ValidationStatus.PartiallyCorrect, "Some parts are correct.", correctParts.toDouble() / expected.parts.size, correctParts, expected.parts.size)
            else -> incorrect("Work through each part with your team.")
        }
    }

    fun parseRational(raw: String): Rational? {
        val clean = raw.trim().replace(" ", "")
        return when {
            "/" in clean -> {
                val n = clean.substringBefore("/").toIntOrNull()
                val d = clean.substringAfter("/").toIntOrNull()
                if (n == null || d == null || d == 0) null else Rational(n, d)
            }
            clean.toIntOrNull() != null -> Rational(clean.toInt(), 1)
            clean.toDoubleOrNull() != null -> decimalToRational(clean.toDouble())
            else -> null
        }
    }

    private fun parseCoordinate(raw: String): Pair<Rational, Rational>? {
        val clean = raw.trim().removePrefix("(").removeSuffix(")")
        val parts = clean.split(",").map { it.trim() }
        if (parts.size != 2) return null
        val x = parseRational(parts[0]) ?: return null
        val y = parseRational(parts[1]) ?: return null
        return x to y
    }

    private fun parseLinearExpression(raw: String, variable: String): LinearExpression? {
        val clean = raw.lowercase().replace(" ", "").replace("-", "+-")
        var coefficient = Rational(0, 1)
        var constant = Rational(0, 1)
        clean.split("+").filter { it.isNotBlank() }.forEach { term ->
            if (variable in term) {
                val c = term.replace(variable, "")
                coefficient += when (c) {
                    "", "+" -> Rational(1, 1)
                    "-" -> Rational(-1, 1)
                    else -> parseRational(c) ?: return null
                }
            } else {
                constant += parseRational(term) ?: return null
            }
        }
        return LinearExpression(coefficient.normalized, constant.normalized, variable)
    }

    private fun parseNumberAndUnit(raw: String): Pair<Double?, UnitKind> {
        val lowered = raw.lowercase().replace("²", "2")
        val number = Regex("-?\\d+(\\.\\d+)?").find(lowered)?.value?.toDoubleOrNull()
        val unit = when {
            "%" in lowered || "percent" in lowered -> UnitKind.Percent
            "m2" in lowered || "sq m" in lowered || "square metre" in lowered || "square meter" in lowered -> UnitKind.SquareMetres
            "cm" in lowered -> UnitKind.Centimetres
            Regex("\\bm\\b").containsMatchIn(lowered) || "metre" in lowered || "meter" in lowered -> UnitKind.Metres
            "deg" in lowered || "degree" in lowered || "°" in lowered -> UnitKind.Degrees
            else -> UnitKind.None
        }
        return number to unit
    }

    private fun decimalToRational(value: Double): Rational {
        val scale = 10_000
        return Rational((value * scale).toInt(), scale).normalized
    }

    private fun correct(message: String) = ValidationResult(ValidationStatus.Correct, message, 1.0)
    private fun incorrect(message: String) = ValidationResult(ValidationStatus.Incorrect, message, 0.0)
    private fun invalid(message: String) = ValidationResult(ValidationStatus.InvalidFormat, message, 0.0)
    private fun UnitKind.label(): String = when (this) {
        UnitKind.None -> ""
        UnitKind.Degrees -> "degrees"
        UnitKind.Metres -> "m"
        UnitKind.SquareMetres -> "m2"
        UnitKind.Centimetres -> "cm"
        UnitKind.Percent -> "%"
    }
}
