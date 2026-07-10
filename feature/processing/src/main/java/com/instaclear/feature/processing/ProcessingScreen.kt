package com.instaclear.feature.processing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instaclear.core.ui.components.glassmorphism
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

@Composable
fun ProcessingScreen(
    urisString: String,
    onFinished: (String) -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(urisString) {
        val uris = urisString.split(",").map { java.net.URLDecoder.decode(it, "UTF-8") }
        viewModel.analyzeBatch(uris)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is ProcessingUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Tahlil qilinmoqda...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is ProcessingUiState.Ready -> {
                    val plans = state.plans
                    Text("Tanlangan fayllar soni: ${plans.size}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { viewModel.startBatchProcessing() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .glassmorphism(cornerRadius = 32f),
                        shape = CircleShape
                    ) {
                        Text("Optimallashtirishni boshlash", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is ProcessingUiState.Processing -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Qayta ishlanmoqda: ${state.currentIndex + 1} / ${state.totalCount}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Instagram qisqartirishidan oldingi\nsifatni maksimal darajada saqlash uchun tayyorlanmoqda...",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(64.dp))
                    // Mock next button for MVP since WorkManager isn't directly observed here
                    androidx.compose.material3.TextButton(onClick = { viewModel.onCurrentItemFinished() }) {
                        Text("Simulyatsiya - Keyingisiga o'tish", color = MaterialTheme.colorScheme.primary)
                    }
                }
                is ProcessingUiState.AllDone -> {
                    Text("Barcha fayllar tayyor! ✨", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { onFinished("content://mock_output_dir") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .glassmorphism(cornerRadius = 32f),
                        shape = CircleShape
                    ) {
                        Text("Natijalarni ko'rish", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is ProcessingUiState.Error -> {
                    Text("Xatolik: ${state.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
