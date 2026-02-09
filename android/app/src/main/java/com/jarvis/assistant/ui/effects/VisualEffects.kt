package com.jarvis.assistant.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jarvis.assistant.ui.theme.GlowCyan
import com.jarvis.assistant.ui.theme.HudGridColor
import com.jarvis.assistant.ui.theme.HudLineColor
import com.jarvis.assistant.ui.theme.HudScanColor
import com.jarvis.assistant.ui.theme.JarvisCyan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Float,
    var size: Float,
    var life: Float = 1f
)

@Composable
fun ParticleSystem(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    color: Color = JarvisCyan
) {
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (particles.isEmpty()) {
            particles = List(particleCount) {
                Particle(
                    x = Random.nextFloat() * size.width,
                    y = Random.nextFloat() * size.height,
                    velocityX = (Random.nextFloat() - 0.5f) * 2f,
                    velocityY = (Random.nextFloat() - 0.5f) * 2f,
                    alpha = Random.nextFloat() * 0.5f + 0.2f,
                    size = Random.nextFloat() * 3f + 1f
                )
            }
        }

        particles = particles.map { particle ->
            particle.copy(
                x = (particle.x + particle.velocityX + size.width) % size.width,
                y = (particle.y + particle.velocityY + size.height) % size.height,
                alpha = (sin(time * 6.28f + particle.x * 0.01f) * 0.3f + 0.4f).coerceIn(0.1f, 0.7f)
            )
        }

        particles.forEach { particle ->
            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}

@Composable
fun HudGridOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 40f
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = HudGridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSpacing
        }

        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = HudGridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSpacing
        }

        // Scanning line
        val scanY = scanPosition * size.height
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    HudScanColor,
                    HudScanColor,
                    Color.Transparent
                )
            ),
            start = Offset(0f, scanY),
            end = Offset(size.width, scanY),
            strokeWidth = 2f
        )

        // Glow effect below scan line
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(HudScanColor.copy(alpha = 0.3f), Color.Transparent),
                startY = scanY,
                endY = scanY + 50f
            ),
            topLeft = Offset(0f, scanY),
            size = androidx.compose.ui.geometry.Size(size.width, 50f)
        )
    }
}

@Composable
fun CircuitAnimation(
    modifier: Modifier = Modifier,
    color: Color = JarvisCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circuit")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val circuitPaths = listOf(
            listOf(Offset(0f, size.height * 0.3f), Offset(size.width * 0.2f, size.height * 0.3f), 
                   Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.4f, size.height * 0.5f)),
            listOf(Offset(size.width, size.height * 0.7f), Offset(size.width * 0.7f, size.height * 0.7f),
                   Offset(size.width * 0.65f, size.height * 0.4f), Offset(size.width * 0.5f, size.height * 0.4f))
        )

        circuitPaths.forEach { path ->
            for (i in 0 until path.size - 1) {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = path[i],
                    end = path[i + 1],
                    strokeWidth = 2f
                )
            }

            // Animated dot
            val pathIndex = (progress * (path.size - 1)).toInt().coerceIn(0, path.size - 2)
            val localProgress = (progress * (path.size - 1)) - pathIndex
            val dotPos = Offset(
                path[pathIndex].x + (path[pathIndex + 1].x - path[pathIndex].x) * localProgress,
                path[pathIndex].y + (path[pathIndex + 1].y - path[pathIndex].y) * localProgress
            )
            
            drawCircle(
                color = color,
                radius = 5f,
                center = dotPos
            )
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 10f,
                center = dotPos
            )
        }
    }
}

@Composable  
fun PulsingOrb(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isListening: Boolean = false,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    val baseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val listeningPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listening"
    )

    val color = when {
        isListening -> Color(0xFF00E676)  // Green when listening
        isSpeaking -> Color(0xFFFFD700)   // Gold when speaking  
        else -> JarvisCyan
    }

    val scale = if (isListening) listeningPulse else baseScale

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) / 3

        // Outer glow rings
        for (i in 3 downTo 1) {
            drawCircle(
                color = color.copy(alpha = pulseAlpha * 0.2f / i),
                radius = baseRadius * scale * (1 + i * 0.15f),
                center = center
            )
        }

        // Main orb gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.9f),
                    color.copy(alpha = 0.5f),
                    color.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * scale
            ),
            radius = baseRadius * scale,
            center = center
        )

        // Inner bright core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    color.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 0.3f
            ),
            radius = baseRadius * 0.3f * scale,
            center = center
        )

        // Rotating ring
        drawCircle(
            color = color.copy(alpha = pulseAlpha),
            radius = baseRadius * scale * 0.8f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}
