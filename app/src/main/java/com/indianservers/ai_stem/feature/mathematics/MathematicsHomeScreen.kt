package com.indianservers.ai_stem.feature.mathematics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.ai_stem.domain.workspace.WorkspaceEnvironmentMode
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

private data class MathematicsModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val enabled: Boolean = true,
    val action: () -> Unit
)

@Composable
fun MathematicsHomeScreen(
    onBack: () -> Unit,
    onOpenAr: () -> Unit,
    onOpenGraphingStudio: () -> Unit,
    onOpenGeometry2D: () -> Unit,
    onOpenGeometry3D: () -> Unit,
    onOpenAlgebra: () -> Unit,
    onOpenCalculus: () -> Unit,
    onOpenProjects: () -> Unit,
    viewModel: MathematicsHomeViewModel = viewModel()
) {
    val environment by viewModel.environmentMode.collectAsStateWithLifecycle()
    val dark = environment == WorkspaceEnvironmentMode.Black
    val background = if (dark) Color(0xFF080B12) else Color(0xFFF6F8FC)
    val panel = if (dark) Color(0xFF121722) else Color.White
    val foreground = if (dark) Color(0xFFF5F7FF) else Color(0xFF121722)

    val modules = listOf(
        MathematicsModule("Graphs", "Functions, analysis and parameters", Icons.Outlined.AutoGraph, Color(0xFF00BCD4), action = onOpenGraphingStudio),
        MathematicsModule("2D Geometry", "Dynamic constructions and measurement", Icons.Outlined.ChangeHistory, Color(0xFF7C4DFF), action = onOpenGeometry2D),
        MathematicsModule("3D Geometry", "Solids, sections and volume", Icons.Outlined.ViewInAr, Color(0xFF00A884), action = onOpenGeometry3D),
        MathematicsModule("Trigonometry", "Triangles, circles and identities", Icons.Outlined.Explore, Color(0xFFFFA000), action = onOpenCalculus),
        MathematicsModule("Coordinate Geometry", "Points, lines and transformations", Icons.Outlined.GridOn, Color(0xFF2196F3), action = onOpenGeometry2D),
        MathematicsModule("Algebra / CAS", "Expressions and equation solving", Icons.Outlined.Functions, Color(0xFFE64A6A), action = onOpenAlgebra),
        MathematicsModule("Activities", "Guided mathematical investigations", Icons.Outlined.School, Color(0xFF5C6BC0), action = onOpenProjects),
        MathematicsModule("AR Mode", "Open the preserved marker AR laboratory", Icons.Outlined.Calculate, Color(0xFF9C27B0), action = onOpenAr)
    )

    Scaffold(topBar = { SimpleHeader("Mathematics Lab", onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Workspace", color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkspaceEnvironmentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = environment == mode,
                            onClick = {
                                viewModel.selectEnvironment(mode)
                                if (mode == WorkspaceEnvironmentMode.AR) onOpenAr()
                            },
                            label = { Text(mode.displayName.removeSuffix(" Workspace")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(164.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(modules, key = { it.title }) { module ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = module.enabled, onClick = module.action)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(module.icon, contentDescription = null, tint = module.accent)
                            Text(module.title, color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(module.subtitle, color = foreground.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
