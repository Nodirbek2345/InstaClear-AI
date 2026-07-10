package com.instaclear.data.processing.export

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ExportManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getOutputFile(extension: String): File {
        val dir = File(context.cacheDir, "processed_media")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "InstaClear_${UUID.randomUUID()}.$extension")
    }
}
