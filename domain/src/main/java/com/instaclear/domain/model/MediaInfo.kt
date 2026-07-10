package com.instaclear.domain.model

data class MediaInfo(
    val uriString: String,
    val type: MediaType,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val hasHdr: Boolean = false,
    val colorSpace: String? = null,
    val bitrate: Int? = null,
    val fps: Float? = null,
    val codec: String? = null
)

enum class MediaType {
    IMAGE, VIDEO, UNKNOWN
}
