package com.indianservers.ai_stem.feature.mathematics

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.indianservers.ai_stem.feature.subjects.SimpleHeader

@Composable
fun MathematicsHomeScreen(
    onBack: () -> Unit,
    onOpenAr: () -> Unit
) {
    Scaffold(topBar = { SimpleHeader("Mathematics", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.ViewInAr, contentDescription = null)
                    Text("AR Mathematics Playground", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Place and manipulate mathematical objects on a real surface using the camera and AR sensors.")
                    Button(onClick = onOpenAr, modifier = Modifier.fillMaxWidth()) {
                        Text("Open AR Playground")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "This Mathematics section is now focused only on AR. Use Android logcat tag AiStemAR while testing crashes.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
