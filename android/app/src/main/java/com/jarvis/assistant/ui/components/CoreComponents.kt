package com.jarvis.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.ui.effects.ListeningRipple
import com.jarvis.assistant.ui.effects.PulsingOrb
import com.jarvis.assistant.ui.theme.*

@Composable
fun VoiceButton(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isListening -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val glowColor = when {
        isListening -> JarvisSuccess
        isSpeaking -> JarvisGold
        else -> JarvisCyan
    }

    Box(
        modifier = modifier
            .size(120.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Ripple effect when listening
        AnimatedVisibility(
            visible = isListening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ListeningRipple(
                modifier = Modifier.size(200.dp),
                isActive = isListening
            )
        }

        // Outer glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = if (isListening || isSpeaking) 20.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
        )

        // Main button
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JarvisSurfaceVariant,
                            JarvisSurface,
                            JarvisBlack
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(glowColor, glowColor.copy(alpha = 0.3f))
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isEnabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing orb inside
            PulsingOrb(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                isListening = isListening,
                isSpeaking = isSpeaking
            )

            // Icon overlay
            Icon(
                imageVector = when {
                    isListening -> Icons.Default.Stop
                    !isEnabled -> Icons.Default.MicOff
                    else -> Icons.Default.Mic
                },
                contentDescription = if (isListening) "Stop listening" else "Start listening",
                tint = if (isEnabled) Color.White else JarvisTextMuted,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        JarvisCyan.copy(alpha = 0.15f)
    } else {
        JarvisTextMuted.copy(alpha = 0.1f)
    }

    val borderColor = if (isActive) JarvisCyan else JarvisTextMuted
    val textColor = if (isActive) JarvisCyan else JarvisTextMuted

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) JarvisSuccess else JarvisTextMuted,
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color = JarvisCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = glowColor.copy(alpha = 0.3f),
                spotColor = glowColor.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = glowColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = JarvisCard.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
