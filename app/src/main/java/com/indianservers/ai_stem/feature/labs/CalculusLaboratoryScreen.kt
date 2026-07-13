package com.indianservers.ai_stem.feature.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.domain.calculus.CalculusEngine
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun CalculusLaboratoryScreen(onBack: () -> Unit) {
    val engine = CalculusEngine()
    val limit = engine.limit("y = sin(x) / x", 0.0)
    val derivative = engine.derivative("y = x^3", 2.0)
    val secondDerivative = engine.derivative("y = x^3", 2.0, order = 2)
    val integral = engine.definiteIntegral("y = x^2", 0.0, 1.0)
    val riemann = engine.riemannSums("y = x^2", 0.0, 1.0, 20)
    val volume = engine.diskVolumeAroundXAxis("y = x", 0.0, 1.0)
    val partial = engine.partialDerivative("z = x^2 + y^2", 3.0, 4.0, "x")
    Scaffold(topBar = { SimpleHeader("Calculus Laboratory", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LabCard("Limits", "sin(x)/x near 0: left=${limit.left.format()}, right=${limit.right.format()}\n${limit.message}") }
            item { LabCard("Derivatives", "d/dx x³ at x=2 ≈ ${derivative.format()}\nSecond derivative ≈ ${secondDerivative.format()}") }
            item { LabCard("Definite Integral", "∫₀¹ x² dx ≈ ${integral.value.format()} using ${integral.method}") }
            item { LabCard("Riemann Sums", "Left ${riemann.left.format()}, Right ${riemann.right.format()}, Midpoint ${riemann.midpoint.format()}, Trapezoid ${riemann.trapezoid.format()}") }
            item { LabCard("Volume of Revolution", "Disk method for y=x on [0,1] ≈ ${volume.value.format()}") }
            item { LabCard("Multivariable Foundation", "∂/∂x (x² + y²) at (3,4) ≈ ${partial.format()}") }
        }
    }
}

internal fun Double.format(): String = "%.4f".format(this).trimEnd('0').trimEnd('.')
