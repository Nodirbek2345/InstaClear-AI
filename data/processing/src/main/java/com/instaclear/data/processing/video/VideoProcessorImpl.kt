package com.instaclear.data.processing.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.instaclear.domain.model.ProcessingPlan
import androidx.media3.common.Effect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.DefaultMuxer
import androidx.media3.transformer.TransformationRequest
import com.instaclear.data.processing.gl.SharpenGlEffect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject

class VideoProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun processVideo(plan: ProcessingPlan, outputFile: File): Flow<Int> = callbackFlow {
        // Calculate an optimal bitrate based on resolution (Instagram prefers max 5-8 Mbps for 1080p)
        val optimalBitrate = if (plan.targetWidth >= 1080) 8_000_000 else 5_000_000
        
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                androidx.media3.transformer.VideoEncoderSettings.Builder()
                    .setBitrate(optimalBitrate)
                    .build()
            )
            .setEnableFallback(true) // Ensure it works on all devices
            .build()
            
        // MP4 FastStart is default in modern Media3's DefaultMuxer for better social media uploading

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    trySend(100)
                    close()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    close(exportException)
                }
            })
            .build()

        val mediaItem = MediaItem.fromUri(Uri.parse(plan.uriString))
        
        // Video effektlarini yig'ish
        val videoEffects = mutableListOf<Effect>()
        
        // Rezolyutsiyani moslashtirish (agar kerak bo'lsa)
        if (plan.targetWidth > 0 && plan.targetHeight > 0) {
            videoEffects.add(
                Presentation.createForWidthAndHeight(
                    plan.targetWidth, plan.targetHeight, Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }
        
        // GPU Sharpen effekti (agar yoqilgan bo'lsa)
        if (plan.applySharpen) {
            videoEffects.add(SharpenGlEffect(strength = 0.4f))
        }
        
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(androidx.media3.transformer.Effects(
                /* audioProcessors= */ listOf(),
                /* videoEffects= */ videoEffects
            ))
            .build()
        
        transformer.start(editedMediaItem, outputFile.absolutePath)

        // Polling progress
        val progressHolder = androidx.media3.transformer.ProgressHolder()
        val thread = Thread {
            try {
                while (!isClosedForSend) {
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                        trySend(progressHolder.progress)
                    }
                    Thread.sleep(500)
                }
            } catch (e: Exception) {
                // Ignore interrupted
            }
        }
        thread.start()

        awaitClose {
            transformer.cancel()
            thread.interrupt()
        }
    }
}
