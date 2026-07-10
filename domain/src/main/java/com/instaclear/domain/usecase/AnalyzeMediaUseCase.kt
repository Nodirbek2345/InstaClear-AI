package com.instaclear.domain.usecase

import android.os.Build
import android.util.Log
import com.instaclear.domain.model.MediaInfo
import com.instaclear.domain.model.MediaType
import com.instaclear.domain.model.ProcessingPlan
import com.instaclear.domain.repository.MediaRepository
import javax.inject.Inject

class AnalyzeMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(uriString: String): Result<ProcessingPlan> {
        return mediaRepository.getMediaInfo(uriString).map { info ->
            buildPlan(info)
        }
    }

    private fun buildPlan(info: MediaInfo): ProcessingPlan {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        Log.d("InstaClearAI", "Tahlil qilinmoqda... Qurilma: \$manufacturer \$model")

        val targetWidth = if (info.width > 1080) 1080 else info.width
        val aspectRatio = info.height.toFloat() / info.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()

        if (info.type == MediaType.IMAGE) {
            val isHeic = info.codec?.contains("hevc", ignoreCase = true) == true || 
                         info.codec?.contains("heic", ignoreCase = true) == true
            val isDisplayP3 = info.colorSpace?.contains("P3", ignoreCase = true) == true
            
            // AI logic for Sharpening based on manufacturer
            var applySharpen = false
            when {
                manufacturer.contains("samsung") -> applySharpen = true
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> applySharpen = true
                manufacturer.contains("pixel") -> applySharpen = false // Pixels process perfectly natively
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> applySharpen = true
                else -> applySharpen = true // generic android fallback usually needs it for social media
            }
            
            val skipProcessing = !isHeic && !isDisplayP3 && !info.hasHdr && info.width <= 1080 && !applySharpen
            
            return ProcessingPlan(
                uriString = info.uriString,
                targetType = MediaType.IMAGE,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                convertToSrgb = isDisplayP3 || info.hasHdr || (manufacturer.contains("samsung") && (model.contains("s2") || model.contains("fold"))),
                applySharpen = applySharpen,
                skipProcessing = skipProcessing
            )
        } else {
            val isHevc = info.codec?.contains("hevc", ignoreCase = true) == true
            val needsToneMapping = info.hasHdr
            val needsResize = info.width > 1080
            
            // AI Bitrate logic based on manufacturer and size
            var targetBitrate = 8_000_000
            if (info.width > 1080) {
                targetBitrate = when {
                    manufacturer.contains("samsung") -> 12_000_000
                    manufacturer.contains("xiaomi") -> 10_000_000
                    manufacturer.contains("pixel") -> 8_000_000
                    else -> 10_000_000
                }
            }
            
            val skipProcessing = !isHevc && !needsToneMapping && !needsResize && (info.fps ?: 30f) <= 30f && targetBitrate <= 8_000_000
            
            return ProcessingPlan(
                uriString = info.uriString,
                targetType = MediaType.VIDEO,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                transcodeVideo = !skipProcessing,
                toneMapHdrToSdr = needsToneMapping,
                targetFps = 30, // standard IG
                targetVideoBitrate = targetBitrate,
                skipProcessing = skipProcessing
            )
        }
    }
}
