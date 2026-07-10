package com.instaclear.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.instaclear.core.ui.theme.GlassBackground
import com.instaclear.core.ui.theme.GlassBorder
import androidx.compose.ui.draw.blur

fun Modifier.glassmorphism(
    cornerRadius: Float = 24f,
    blurRadius: Float = 16f,
    backgroundColor: Color = GlassBackground,
    borderColor: Color = GlassBorder
): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        // Note: Real blur is supported in Android 12+ (API 31+) via RenderEffect
        // Jetpack Compose 1.1+ supports Modifier.blur
        // However blur modifier blurs the CONTENT inside. To blur the background behind the composable,
        // it requires complex RenderNode manipulations or Accompanist Blur.
        // For a simple premium look without heavy performance hit, we use a semi-transparent background
        // combined with a subtle border and inner content blur if desired.
        .background(backgroundColor, shape)
        .border(1.dp, borderColor, shape)
        .padding(16.dp)
}
