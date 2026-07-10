package com.instaclear.feature.processing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.instaclear.domain.model.ProcessingPlan
import com.instaclear.domain.usecase.AnalyzeMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import java.util.UUID

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val analyzeMediaUseCase: AnalyzeMediaUseCase,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Loading)
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private val plans = mutableListOf<ProcessingPlan>()
    private var currentProcessingIndex = 0

    fun analyzeBatch(uriStrings: List<String>) {
        viewModelScope.launch {
            _uiState.value = ProcessingUiState.Loading
            plans.clear()
            val errors = mutableListOf<String>()

            for (uri in uriStrings) {
                val result = analyzeMediaUseCase(uri)
                if (result.isSuccess) {
                    plans.add(result.getOrNull()!!)
                } else {
                    errors.add("Faylni tahlil qilib bo'lmadi: $uri")
                }
            }

            if (plans.isEmpty()) {
                _uiState.value = ProcessingUiState.Error(errors.joinToString("\n"))
            } else {
                _uiState.value = ProcessingUiState.Ready(plans.toList())
            }
        }
    }

    fun startBatchProcessing() {
        if (plans.isEmpty()) return
        
        currentProcessingIndex = 0
        processNext()
    }

    private fun processNext() {
        if (currentProcessingIndex >= plans.size) {
            _uiState.value = ProcessingUiState.AllDone
            return
        }

        val plan = plans[currentProcessingIndex]
        
        val inputData = Data.Builder()
            .putString("URI", plan.uriString)
            .putString("TARGET_TYPE", plan.targetType.name)
            .putInt("TARGET_WIDTH", plan.targetWidth)
            .putInt("TARGET_HEIGHT", plan.targetHeight)
            .putBoolean("SKIP", plan.skipProcessing)
            .putBoolean("TRANSCODE", plan.transcodeVideo)
            .putBoolean("TONE_MAP", plan.toneMapHdrToSdr)
            .putBoolean("CONVERT_SRGB", plan.convertToSrgb)
            .putBoolean("SHARPEN", plan.applySharpen)
            .build()

        try {
            val workerClass = Class.forName("com.instaclear.app.worker.MediaProcessingWorker") as Class<out ListenableWorker>
            val workRequest = OneTimeWorkRequest.Builder(workerClass)
                .setInputData(inputData)
                .build()

            workManager.enqueueUniqueWork(
                "process_media_${currentProcessingIndex}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            _uiState.value = ProcessingUiState.Processing(
                currentIndex = currentProcessingIndex,
                totalCount = plans.size,
                currentWorkId = workRequest.id
            )
            
            // Note: To automatically process next, we would observe WorkInfo from WorkManager here.
            // For MVP simplicity, we might just rely on UI triggering or basic chaining if needed.
        } catch (e: Exception) {
            _uiState.value = ProcessingUiState.Error(e.message ?: "Xato")
        }
    }
    
    fun onCurrentItemFinished() {
        currentProcessingIndex++
        processNext()
    }
}

sealed class ProcessingUiState {
    object Loading : ProcessingUiState()
    data class Ready(val plans: List<ProcessingPlan>) : ProcessingUiState()
    data class Processing(val currentIndex: Int, val totalCount: Int, val currentWorkId: UUID) : ProcessingUiState()
    data class Error(val message: String) : ProcessingUiState()
    object AllDone : ProcessingUiState()
}
