package com.instaclear.domain.repository

import com.instaclear.domain.model.ProcessingPlan
import kotlinx.coroutines.flow.Flow

interface ProcessingRepository {
    /**
     * Returns a Flow of progress percentage (0 to 100) and the final output URI string.
     */
    fun processMedia(plan: ProcessingPlan): Flow<ProcessingResult>
}

sealed class ProcessingResult {
    data class Progress(val percentage: Int) : ProcessingResult()
    data class Success(val outputUriString: String) : ProcessingResult()
    data class Error(val exception: Throwable) : ProcessingResult()
}
