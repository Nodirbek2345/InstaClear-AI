package com.instaclear.data.processing.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.instaclear.domain.model.ProcessingPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

class ImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun processImage(plan: ProcessingPlan, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(plan.uriString)
            
            var bitmap: Bitmap? = null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    // Set target color space to sRGB if requested
                    if (plan.convertToSrgb && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                    }
                    
                    // Basic resize during decode to save memory
                    if (plan.targetWidth > 0 && plan.targetWidth < info.size.width) {
                        decoder.setTargetSize(plan.targetWidth, plan.targetHeight)
                    }
                }
            } else {
                // Fallback for API 26-27
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = android.graphics.BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                    
                    var inSampleSize = 1
                    if (plan.targetWidth > 0 && options.outWidth > plan.targetWidth) {
                        inSampleSize = Math.round(options.outWidth.toFloat() / plan.targetWidth.toFloat())
                    }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = inSampleSize
                    
                    context.contentResolver.openInputStream(uri)?.use { stream2 ->
                        val decoded = android.graphics.BitmapFactory.decodeStream(stream2, null, options)
                        
                        // Handle EXIF Orientation for older APIs
                        context.contentResolver.openInputStream(uri)?.use { exifStream ->
                            val exif = ExifInterface(exifStream)
                            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
                            }
                            
                            if (decoded != null) {
                                bitmap = if (!matrix.isIdentity) {
                                    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                                    if (rotated != decoded) decoded.recycle()
                                    rotated
                                } else {
                                    decoded
                                }
                            }
                        }
                    }
                }
            }

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image"))
            }

            // Algorithmic Denoise before sharpening (if needed for noisy sensors)
            if (plan.applySharpen) { // We tie Denoise logic with Sharpen for this MVP or use a dedicated flag
                try {
                    bitmap?.let { currentBitmap ->
                        // Subtle blur to reduce color noise before sharpening
                        val denoisedBitmap = com.google.android.renderscript.Toolkit.blur(currentBitmap, 1)
                        if (denoisedBitmap !== currentBitmap) {
                            currentBitmap.recycle()
                            bitmap = denoisedBitmap
                        }
                    }
                } catch (e: Exception) {}
            }

            if (plan.applySharpen) {
                try {
                    val sharpenKernel = floatArrayOf(
                        0.0f, -0.5f, 0.0f,
                        -0.5f, 3.0f, -0.5f,
                        0.0f, -0.5f, 0.0f
                    )
                    
                    bitmap?.let { currentBitmap ->
                        val sharpenedBitmap = com.google.android.renderscript.Toolkit.convolve3x3(currentBitmap, sharpenKernel)
                        currentBitmap.recycle()
                        bitmap = sharpenedBitmap
                    }
                } catch (e: Exception) {
                    // Fallback: ignore sharpen if toolkit fails
                }
            }

            // EXIF Cleanup happens automatically because we only write the Bitmap to the output file
            // without copying the original Exif data. This ensures privacy and smaller file sizes.
            
            FileOutputStream(outputFile).use { out ->
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            
            bitmap.recycle()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
