package com.jarvis.assistant.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisGold
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    amplitude: Float = 1f,
    color: Color = JarvisCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val waveAmplitude by animateFloatAsState(
        targetValue = if (isActive) amplitude else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "amplitude"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerY = size.height / 2
        val waveHeight = size.height * 0.3f * waveAmplitude

        // Draw multiple waves with different frequencies
        listOf(
            Triple(1f, 0f, 1f),      // frequency, phase offset, alpha
            Triple(1.5f, 45f, 0.6f),
            Triple(2f, 90f, 0.3f)
        ).forEach { (freq, phaseOffset, alpha) ->
            val path = Path()
            var started = false

            for (x in 0..size.width.toInt() step 2) {
                val xFloat = x.toFloat()
                val waveY = centerY + sin(
                    Math.toRadians((xFloat * freq + phase + phaseOffset).toDouble())
                ).toFloat() * waveHeight

                if (!started) {
                    path.moveTo(xFloat, waveY)
                    started = true
                } else {
                    path.lineTo(xFloat, waveY)
                }
            }

            drawPath(
                path = path,
                color = color.copy(alpha = alpha * waveAmplitude.coerceIn(0f, 1f)),
                style = Stroke(width = 3f - (1 - alpha) * 2)
            )
        }

        // Center line glow
        if (isActive) {
            drawLine(
                color = color.copy(alpha = 0.2f),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun SpeakingIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking")
    
    val bars = remember { listOf(0.3f, 0.6f, 1f, 0.8f, 0.5f, 0.9f, 0.4f) }
    
    val animatedBars = bars.mapIndexed { index, _ ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 300 + index * 50,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }

    val targetAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    Canvas(modifier = modifier) {
        if (targetAlpha <= 0) return@Canvas

        val barWidth = size.width / (bars.size * 2 - 1)
        val maxHeight = size.height * 0.8f

        animatedBars.forEachIndexed { index, animatedValue ->
            val height = if (isActive) {
                maxHeight * animatedValue.value
            } else {
                maxHeight * 0.1f
            }

            val x = index * barWidth * 2
            val y = (size.height - height) / 2

            drawRoundRect(
                color = JarvisGold.copy(alpha = targetAlpha),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
fun ListeningRipple(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val ripples = remember { mutableStateListOf<Float>() }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                ripples.add(0f)
                if (ripples.size > 4) {
                    ripples.removeAt(0)
                }
                kotlinx.coroutines.delay(500)
            }
        } else {
            ripples.clear()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2

        for (i in 0..3) {
            val rippleProgress = ((progress + i * 0.25f) % 1f)
            val radius = maxRadius * rippleProgress
            val alpha = (1f - rippleProgress) * 0.5f

            if (isActive || rippleProgress < 0.5f) {
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
