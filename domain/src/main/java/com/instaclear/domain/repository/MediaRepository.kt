package com.instaclear.domain.repository

import com.instaclear.domain.model.MediaInfo

interface MediaRepository {
    suspend fun getMediaInfo(uriString: String): Result<MediaInfo>
}
