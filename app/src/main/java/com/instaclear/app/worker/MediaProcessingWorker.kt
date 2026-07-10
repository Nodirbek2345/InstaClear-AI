package com.instaclear.app.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.instaclear.domain.model.ProcessingPlan
import com.instaclear.domain.repository.ProcessingResult
import com.instaclear.domain.usecase.ProcessMediaUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import com.instaclear.domain.model.MediaType

@HiltWorker
class MediaProcessingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val processMediaUseCase: ProcessMediaUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val targetTypeStr = inputData.getString(KEY_TARGET_TYPE) ?: return Result.failure()
        val targetType = MediaType.valueOf(targetTypeStr)
        val targetWidth = inputData.getInt(KEY_TARGET_WIDTH, 1080)
        val targetHeight = inputData.getInt(KEY_TARGET_HEIGHT, 1080)
        
        val plan = ProcessingPlan(
            uriString = uriString,
            targetType = targetType,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            skipProcessing = inputData.getBoolean(KEY_SKIP, false),
            transcodeVideo = inputData.getBoolean(KEY_TRANSCODE, false),
            toneMapHdrToSdr = inputData.getBoolean(KEY_TONE_MAP, false),
            convertToSrgb = inputData.getBoolean(KEY_CONVERT_SRGB, false),
            applySharpen = inputData.getBoolean(KEY_SHARPEN, false)
        )

        setForeground(createForegroundInfo(0))

        var outputUri: String? = null
        var isError = false

        processMediaUseCase(plan).collectLatest { result ->
            when (result) {
                is ProcessingResult.Progress -> {
                    setForeground(createForegroundInfo(result.percentage))
                    setProgress(workDataOf(KEY_PROGRESS to result.percentage))
                }
                is ProcessingResult.Success -> {
                    outputUri = result.outputUriString
                    setProgress(workDataOf(KEY_PROGRESS to 100))
                }
                is ProcessingResult.Error -> {
                    isError = true
                }
            }
        }

        return if (isError) {
            Result.failure()
        } else {
            Result.success(workDataOf(KEY_OUTPUT_URI to outputUri))
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val channelId = "InstaClearProcessing"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Media Processing",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("✨ InstaClear AI")
            .setTicker("Media optimallashtirilmoqda...")
            .setContentText(if (progress < 100) "Qayta ishlanmoqda: $progress%" else "Yakunlanmoqda...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setColor(0xFF2F80ED.toInt()) // Primary color
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        
        const val KEY_URI = "URI"
        const val KEY_TARGET_TYPE = "TARGET_TYPE"
        const val KEY_TARGET_WIDTH = "TARGET_WIDTH"
        const val KEY_TARGET_HEIGHT = "TARGET_HEIGHT"
        const val KEY_SKIP = "SKIP"
        const val KEY_TRANSCODE = "TRANSCODE"
        const val KEY_TONE_MAP = "TONE_MAP"
        const val KEY_CONVERT_SRGB = "CONVERT_SRGB"
        const val KEY_SHARPEN = "SHARPEN"
        
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_OUTPUT_URI = "OUTPUT_URI"
    }
}
