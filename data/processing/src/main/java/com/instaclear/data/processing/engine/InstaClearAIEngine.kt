package com.instaclear.data.processing.engine

import android.os.Build
import android.net.Uri
import android.content.Context
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

data class ProcessingPlan(
    val useGpuSharpen: Boolean,
    val useAiDenoise: Boolean,
    val targetBitrateMbps: Int,
    val forceH264: Boolean,
    val convertColorSpaceToSrgb: Boolean,
    val resizeTo1080p: Boolean
)

class InstaClearAIEngine(private val context: Context) {

    suspend fun analyzeAndCreatePlan(uri: Uri, isVideo: Boolean): ProcessingPlan = withContext(Dispatchers.IO) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()

        Log.d("InstaClearAI", "Tahlil qilinmoqda... Qurilma: \$manufacturer \$model")

        // Default safe parameters for Instagram
        var useGpuSharpen = false
        var useAiDenoise = false
        var targetBitrate = 8 // 8 Mbps is a sweet spot for IG H.264 1080p
        var convertToSrgb = false

        // 1. Device-specific intelligent decisions
        when {
            manufacturer.contains("samsung") -> {
                // Samsung native camera tends to over-process or compress heavily for socials. 
                // We use GPU sharpen to restore details lost before IG compression.
                useGpuSharpen = true
                targetBitrate = 12
                // High-end Samsungs might use HDR10+, which looks washed out on IG. Force sRGB.
                if (model.contains("s2") || model.contains("fold") || model.contains("flip")) {
                    convertToSrgb = true
                }
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                // Xiaomi devices often have noise in low-light shadows.
                useAiDenoise = true
                useGpuSharpen = true
                targetBitrate = 10
            }
            manufacturer.contains("pixel") -> {
                // Pixels have excellent native processing, we just need to ensure standard bitrate 
                // and color space to prevent IG from ruining it.
                useGpuSharpen = false
                useAiDenoise = false
                targetBitrate = 8
            }
            manufacturer.contains("honor") || manufacturer.contains("huawei") -> {
                useGpuSharpen = true
                targetBitrate = 10
            }
            else -> {
                // Generic Android fallback
                useGpuSharpen = true
                targetBitrate = 8
            }
        }

        // 2. Media specific analysis (Extract metadata)
        if (isVideo) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
                
                // If the video is 4K, IG will compress it brutally. 1080p is safer.
                // However, Media3 handles scaling. We just need to set the right bitrate.
                if (bitrate > 20_000_000) { // If > 20Mbps, it's very high quality
                    targetBitrate = 15 
                }
                retriever.release()
            } catch (e: Exception) {
                Log.e("InstaClearAI", "Metadata o'qishda xatolik", e)
            }
        }

        val plan = ProcessingPlan(
            useGpuSharpen = useGpuSharpen,
            useAiDenoise = useAiDenoise,
            targetBitrateMbps = targetBitrate,
            forceH264 = true, // IG always prefers H.264
            convertColorSpaceToSrgb = convertToSrgb,
            resizeTo1080p = true 
        )

        Log.d("InstaClearAI", "AI Plan tayyor: \$plan")
        return@withContext plan
    }
}
