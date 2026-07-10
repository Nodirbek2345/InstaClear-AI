package com.instaclear.data.processing.repository

import android.net.Uri
import com.instaclear.data.processing.export.ExportManagerImpl
import com.instaclear.data.processing.image.ImageProcessorImpl
import com.instaclear.data.processing.video.VideoProcessorImpl
import com.instaclear.domain.model.MediaType
import com.instaclear.domain.model.ProcessingPlan
import com.instaclear.domain.repository.ProcessingRepository
import com.instaclear.domain.repository.ProcessingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProcessingRepositoryImpl @Inject constructor(
    private val imageProcessor: ImageProcessorImpl,
    private val videoProcessor: VideoProcessorImpl,
    private val exportManager: ExportManagerImpl
) : ProcessingRepository {

    override fun processMedia(plan: ProcessingPlan): Flow<ProcessingResult> = flow {
        try {
            emit(ProcessingResult.Progress(0))
            
            val extension = if (plan.targetType == MediaType.VIDEO) "mp4" else "jpg"
            val outputFile = exportManager.getOutputFile(extension)

            if (plan.targetType == MediaType.IMAGE) {
                val result = imageProcessor.processImage(plan, outputFile)
                if (result.isSuccess) {
                    emit(ProcessingResult.Progress(100))
                    emit(ProcessingResult.Success(Uri.fromFile(outputFile).toString()))
                } else {
                    emit(ProcessingResult.Error(result.exceptionOrNull() ?: Exception("Unknown error")))
                }
            } else {
                videoProcessor.processVideo(plan, outputFile).collect { progress ->
                    emit(ProcessingResult.Progress(progress))
                    if (progress == 100) {
                        emit(ProcessingResult.Success(Uri.fromFile(outputFile).toString()))
                    }
                }
            }
        } catch (e: Exception) {
            emit(ProcessingResult.Error(e))
        }
    }
}
