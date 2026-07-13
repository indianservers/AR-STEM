package com.indianservers.ai_stem.feature.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.domain.algebra.AlgebraEngine
import com.indianservers.ai_stem.domain.algebra.ComplexNumber
import com.indianservers.ai_stem.domain.algebra.Matrix
import com.indianservers.ai_stem.domain.algebra.RationalNumber
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun AlgebraLaboratoryScreen(onBack: () -> Unit) {
    val linear = AlgebraEngine.solveLinear(2.0, -8.0)
    val quadratic = AlgebraEngine.solveQuadratic(1.0, -5.0, 6.0)
    val factors = AlgebraEngine.factorMonicQuadratic(-5, 6)
    val determinant = AlgebraEngine.determinant2x2(Matrix(listOf(listOf(2.0, 3.0), listOf(1.0, 4.0))))
    val system = AlgebraEngine.solveTwoByTwo(2.0, 1.0, 7.0, 1.0, -1.0, 1.0)
    val complex = ComplexNumber(3.0, 4.0)
    Scaffold(topBar = { SimpleHeader("Algebra Laboratory", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LabCard("Exact Arithmetic", "1/2 + 1/3 = ${RationalNumber(1, 2) + RationalNumber(1, 3)}") }
            item { LabCard("Linear Equation", linear.steps.joinToString("\n") { "${it.expression}  •  ${it.reason}" }) }
            item { LabCard("Quadratic Equation", quadratic.exact.joinToString("\n")) }
            item { LabCard("Factorization", factors.joinToString("\n") { "${it.expression}  •  ${it.reason}" }) }
            item { LabCard("Matrices", "det [[2,3],[1,4]] = $determinant") }
            item { LabCard("System of Equations", system.exact.joinToString("\n")) }
            item { LabCard("Complex Numbers", "|3 + 4i| = ${complex.magnitude()}, conjugate = ${complex.conjugate()}") }
        }
    }
}

@Composable
internal fun LabCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
