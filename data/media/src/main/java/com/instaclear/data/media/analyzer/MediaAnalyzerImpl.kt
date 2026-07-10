package com.instaclear.data.media.analyzer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.instaclear.domain.model.MediaInfo
import com.instaclear.domain.model.MediaType
import com.instaclear.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaAnalyzerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    override suspend fun getMediaInfo(uriString: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""

            val type = when {
                mimeType.startsWith("image/") -> MediaType.IMAGE
                mimeType.startsWith("video/") -> MediaType.VIDEO
                else -> MediaType.UNKNOWN
            }

            if (type == MediaType.UNKNOWN) {
                return@withContext Result.failure(IllegalArgumentException("Unknown media type"))
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            
            val actualWidth = if (rotation == 90 || rotation == 270) height else width
            val actualHeight = if (rotation == 90 || rotation == 270) width else height

            var bitrate: Int? = null
            var fps: Float? = null
            var hasHdr = false
            var colorSpace: String? = null
            val codec = mimeType

            if (type == MediaType.VIDEO) {
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                // Hdr detection is complex on old APIs, but we can check color standard
                val colorStandard = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toIntOrNull()
                hasHdr = colorStandard == MediaMetadataRetriever.COLOR_STANDARD_BT2020
            } else if (type == MediaType.IMAGE) {
                // Heic and P3 detection
                // For simplicity in MVP we rely on MimeType. In a real app we'd use ExifInterface or ImageDecoder to read ColorSpace
                colorSpace = if (mimeType.contains("heic") || mimeType.contains("heif")) "Display P3" else "sRGB"
                hasHdr = false // Gainmap check would go here in Android 14+
            }

            retriever.release()

            val sizeBytes = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: 0L

            Result.success(
                MediaInfo(
                    uriString = uriString,
                    type = type,
                    width = actualWidth,
                    height = actualHeight,
                    sizeBytes = sizeBytes,
                    hasHdr = hasHdr,
                    colorSpace = colorSpace,
                    bitrate = bitrate,
                    fps = fps,
                    codec = codec
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
