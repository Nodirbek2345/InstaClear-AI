package com.instaclear.data.processing.gl

import android.opengl.GLES20
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.SingleFrameGlShaderProgram
import android.content.Context

/**
 * Media3 GlEffect — GPU orqali video kadrlarini o'tkirlash (sharpen).
 *
 * Ishlash prinsipi:
 * Har bir kadr uchun fragment shader'da 3x3 Laplacian kernel yordamida
 * o'tkirlash (unsharp-mask uslubida) amalga oshiriladi.
 * Strength parametri orqali o'tkirlash darajasi sozlanadi (0.0 = hech narsa, 1.0 = to'liq).
 */
class SharpenGlEffect(
    private val strength: Float = 0.4f
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return SharpenShaderProgram(strength, useHdr)
    }

    companion object {
        // Vertex shader — standart fullscreen quad
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoords;
            varying vec2 vTexCoords;
            void main() {
                gl_Position = aPosition;
                vTexCoords = aTexCoords.xy;
            }
        """

        // Fragment shader — 3x3 convolution kernel orqali sharpen
        // Kernel:
        //   0  -1   0
        //  -1   5  -1
        //   0  -1   0
        // Strength parametri bilan original va sharpened o'rtasida mix qilinadi
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoords;
            uniform sampler2D uTexSampler;
            uniform float uStrength;
            uniform vec2 uTexelSize;

            void main() {
                vec4 center = texture2D(uTexSampler, vTexCoords);

                vec4 top    = texture2D(uTexSampler, vTexCoords + vec2(0.0, -uTexelSize.y));
                vec4 bottom = texture2D(uTexSampler, vTexCoords + vec2(0.0,  uTexelSize.y));
                vec4 left   = texture2D(uTexSampler, vTexCoords + vec2(-uTexelSize.x, 0.0));
                vec4 right  = texture2D(uTexSampler, vTexCoords + vec2( uTexelSize.x, 0.0));

                // Laplacian sharpen: center * 5 - neighbors
                vec4 sharpened = center * 5.0 - top - bottom - left - right;

                // Mix between original and sharpened based on strength
                gl_FragColor = mix(center, sharpened, uStrength);
            }
        """
    }

    /**
     * SingleFrameGlShaderProgram — har bir kadr uchun alohida GPU shader ishga tushiradi.
     */
    private class SharpenShaderProgram(
        private val strength: Float,
        useHdr: Boolean
    ) : SingleFrameGlShaderProgram(useHdr) {

        private var glProgram: GlProgram? = null

        override fun configure(inputWidth: Int, inputHeight: Int): android.util.Size {
            if (glProgram == null) {
                glProgram = GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            }
            glProgram?.let { program ->
                program.setFloatUniform("uStrength", strength)
                program.setFloatsUniform(
                    "uTexelSize",
                    floatArrayOf(1.0f / inputWidth, 1.0f / inputHeight)
                )
            }
            return android.util.Size(inputWidth, inputHeight)
        }

        override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
            glProgram?.let { program ->
                program.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
                program.bindAttributesAndUniforms()
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4)
                GlUtil.checkGlError()
            }
        }

        override fun release() {
            glProgram?.delete()
            glProgram = null
        }
    }
}
