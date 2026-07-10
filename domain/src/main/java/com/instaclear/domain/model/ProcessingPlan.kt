package com.instaclear.domain.model

data class ProcessingPlan(
    val uriString: String,
    val skipProcessing: Boolean = false, // If already optimal
    val targetType: MediaType,
    val targetWidth: Int,
    val targetHeight: Int,
    // Video specific
    val transcodeVideo: Boolean = false,
    val toneMapHdrToSdr: Boolean = false,
    val targetVideoBitrate: Int? = null,
    val targetFps: Int? = null,
    // Image specific
    val convertToSrgb: Boolean = false,
    val applySharpen: Boolean = false
)
