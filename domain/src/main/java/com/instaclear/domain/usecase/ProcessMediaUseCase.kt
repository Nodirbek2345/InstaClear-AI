package com.instaclear.domain.usecase

import com.instaclear.domain.model.ProcessingPlan
import com.instaclear.domain.repository.ProcessingRepository
import com.instaclear.domain.repository.ProcessingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ProcessMediaUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    operator fun invoke(plan: ProcessingPlan): Flow<ProcessingResult> {
        if (plan.skipProcessing) {
            return flowOf(ProcessingResult.Success(plan.uriString))
        }
        return processingRepository.processMedia(plan)
    }
}
