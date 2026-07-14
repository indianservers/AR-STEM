package com.indianservers.ai_stem.feature.games.fractionfactory

import com.indianservers.ai_stem.feature.games.mission.Rational
import kotlin.math.abs

enum class FactoryMode { SingleFactoryOperator, TeamProductionLine, TimedOrderChallenge }
enum class FactoryMachineType { Mixer, Splitter, Converter, RatioBalancer, PercentStation }
enum class QualityInspectionStatus { Pending, Pass, NeedsCorrection }
enum class FactoryArState { TableScanning, FactoryAnchored, WeakTracking, Paused }

data class IngredientQuantity(val ingredientId: String, val amount: Rational, val unit: String)
data class ProductionOrder(val orderId: String, val title: String, val requiredQuantities: List<IngredientQuantity>, val targetRatio: Rational?)
data class ContainerDefinition(val containerId: String, val capacity: Rational, val unit: String)
data class MachineDefinition(val machineId: String, val type: FactoryMachineType, val acceptedUnits: Set<String>)
data class ProductionResult(val orderId: String, val producedQuantities: List<IngredientQuantity>, val machineIdsUsed: List<String>)
data class QualityInspectionResult(val status: QualityInspectionStatus, val feedback: List<String>)

data class FactoryOrderTemplate(
    val id: String,
    val title: String,
    val prompt: String,
    val target: IngredientQuantity,
    val machine: FactoryMachineType,
    val skill: String,
    val points: Int
)

data class FactoryProgress(
    val orderIndex: Int = 0,
    val score: Int = 0,
    val completedOrders: Set<String> = emptySet(),
    val attempts: Int = 0,
    val arState: FactoryArState = FactoryArState.TableScanning
)

interface FractionFactoryValidator {
    fun inspect(order: ProductionOrder, result: ProductionResult): QualityInspectionResult
}

class FractionFactoryEngine : FractionFactoryValidator {
    val orders: List<FactoryOrderTemplate> = buildOrders()

    fun currentOrder(progress: FactoryProgress): FactoryOrderTemplate = orders[progress.orderIndex.coerceIn(0, orders.lastIndex)]

    fun validate(template: FactoryOrderTemplate, rawAmount: String, rawUnit: String): QualityInspectionResult {
        val amount = FractionMath.parseRational(rawAmount)
            ?: return QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, listOf("Enter a fraction, mixed number, decimal or percent."))
        val unitMatches = rawUnit.trim().equals(template.target.unit, ignoreCase = true)
        val amountMatches = amount.normalized == template.target.amount.normalized
        return when {
            amountMatches && unitMatches -> QualityInspectionResult(QualityInspectionStatus.Pass, listOf("Batch approved. Equivalent value and unit both match."))
            amountMatches -> QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, listOf("Quantity is correct, but the unit should be ${template.target.unit}."))
            unitMatches -> QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, listOf("Unit is correct, but the amount is not equivalent to ${template.target.amount}."))
            else -> QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, listOf("Both quantity and unit need correction."))
        }
    }

    fun apply(progress: FactoryProgress, template: FactoryOrderTemplate, result: QualityInspectionResult): FactoryProgress {
        val passed = result.status == QualityInspectionStatus.Pass
        return progress.copy(
            orderIndex = if (passed) (progress.orderIndex + 1).coerceAtMost(orders.lastIndex) else progress.orderIndex,
            score = progress.score + if (passed) template.points else 0,
            completedOrders = if (passed) progress.completedOrders + template.id else progress.completedOrders,
            attempts = progress.attempts + 1,
            arState = if (passed) FactoryArState.FactoryAnchored else progress.arState
        )
    }

    override fun inspect(order: ProductionOrder, result: ProductionResult): QualityInspectionResult {
        if (order.orderId != result.orderId) {
            return QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, listOf("Result belongs to a different order."))
        }
        val feedback = mutableListOf<String>()
        order.requiredQuantities.forEach { required ->
            val produced = result.producedQuantities.firstOrNull { it.ingredientId == required.ingredientId }
            when {
                produced == null -> feedback += "Missing ${required.ingredientId}."
                produced.unit != required.unit -> feedback += "${required.ingredientId} unit should be ${required.unit}."
                produced.amount.normalized != required.amount.normalized -> feedback += "${required.ingredientId} should be ${required.amount} ${required.unit}."
            }
        }
        return if (feedback.isEmpty()) {
            QualityInspectionResult(QualityInspectionStatus.Pass, listOf("Production result passes inspection."))
        } else {
            QualityInspectionResult(QualityInspectionStatus.NeedsCorrection, feedback)
        }
    }

    private fun buildOrders(): List<FactoryOrderTemplate> {
        val base = listOf(
            Triple("milk", Rational(3, 4), "L"),
            Triple("grain", Rational(5, 6), "kg"),
            Triple("paint", Rational(7, 10), "L"),
            Triple("wire", Rational(9, 8), "m"),
            Triple("juice", Rational(2, 3), "L")
        )
        val machines = FactoryMachineType.entries
        return (1..50).map { index ->
            val item = base[(index - 1) % base.size]
            val multiplier = Rational((index % 5) + 1, ((index + 1) % 4) + 1)
            val target = (item.second * multiplier).normalized
            val machine = machines[(index - 1) % machines.size]
            FactoryOrderTemplate(
                id = "order-$index",
                title = "Batch $index ${item.first.replaceFirstChar { it.uppercase() }}",
                prompt = "Use the ${machine.name.lowercase()} to produce ${target} ${item.third} of ${item.first}.",
                target = IngredientQuantity(item.first, target, item.third),
                machine = machine,
                skill = when (machine) {
                    FactoryMachineType.Mixer -> "Add and simplify fractions"
                    FactoryMachineType.Splitter -> "Divide fractions into equal parts"
                    FactoryMachineType.Converter -> "Convert between fraction, decimal and percent"
                    FactoryMachineType.RatioBalancer -> "Maintain equivalent ratios"
                    FactoryMachineType.PercentStation -> "Convert percent to fractional quantity"
                },
                points = 80 + (index % 5) * 10
            )
        }
    }
}

object FractionMath {
    fun parseRational(raw: String): Rational? {
        val value = raw.trim().lowercase().removeSuffix("l").removeSuffix("kg").removeSuffix("m").trim()
        if (value.isEmpty()) return null
        if (value.endsWith("%")) {
            val percent = value.removeSuffix("%").toDoubleOrNull() ?: return null
            return decimalToRational(percent / 100.0)
        }
        val mixed = Regex("""^(-?\d+)\s+(\d+)/(\d+)$""").matchEntire(value)
        if (mixed != null) {
            val whole = mixed.groupValues[1].toInt()
            val numerator = mixed.groupValues[2].toInt()
            val denominator = mixed.groupValues[3].toInt().takeIf { it != 0 } ?: return null
            val sign = if (whole < 0) -1 else 1
            return Rational(whole * denominator + sign * numerator, denominator).normalized
        }
        if ("/" in value) {
            val parts = value.split("/")
            if (parts.size != 2) return null
            val numerator = parts[0].toIntOrNull() ?: return null
            val denominator = parts[1].toIntOrNull()?.takeIf { it != 0 } ?: return null
            return Rational(numerator, denominator).normalized
        }
        return value.toDoubleOrNull()?.let(::decimalToRational)
    }

    fun decimalToRational(decimal: Double): Rational? {
        if (!decimal.isFinite()) return null
        val scale = 10000
        val numerator = (decimal * scale).let {
            if (it >= 0) it + 0.5 else it - 0.5
        }.toInt()
        return Rational(numerator, scale).normalized
    }

    fun equivalent(a: Rational, b: Rational): Boolean = abs(a.toDouble() - b.toDouble()) < 0.0001
}
