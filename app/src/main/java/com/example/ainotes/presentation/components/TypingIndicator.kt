package com.example.ainotes.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    bubbleColor: Color,
    contentColor: Color
) {
    val dotCount = 3
    val durationPerDot = 300
    val pauseDuration = 400 // пауза после последней точки
    val totalDuration = dotCount * durationPerDot + pauseDuration

    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "typingProgress"
    )

    Row(
        modifier = modifier
            .background(bubbleColor, shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            val phaseOffset = i * durationPerDot.toFloat() / totalDuration
            val phase = (animatedProgress - phaseOffset + 1f) % 1f

            // Считаем длительность активной фазы, остальное — "пауза"
            val activeDurationFraction = (dotCount * durationPerDot).toFloat() / totalDuration

            val alpha = if (phase < activeDurationFraction) {
                val localPhase = phase / activeDurationFraction
                0.3f + 0.7f * kotlin.math.sin(localPhase * 2 * Math.PI).toFloat().coerceAtLeast(0f)
            } else {
                0.3f // во время паузы все точки тусклые
            }

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(alpha)
                    .background(contentColor, shape = CircleShape)
            )
        }
    }
}