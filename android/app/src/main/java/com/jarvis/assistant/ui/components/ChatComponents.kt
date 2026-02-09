package com.jarvis.assistant.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.data.model.Message
import com.jarvis.assistant.data.model.MessageRole
import com.jarvis.assistant.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal

@Composable
fun ChatBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val isLoading = message.isLoading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            colors = listOf(JarvisCyan.copy(alpha = 0.3f), JarvisCyan.copy(alpha = 0.1f))
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(JarvisSurfaceVariant, JarvisCard)
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisCyan.copy(alpha = 0.5f) else JarvisNavy,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (isLoading && message.content.isEmpty()) {
                TypingIndicator()
            } else {
                Column {
                    if (!isUser) {
                        Text(
                            text = "J.A.R.V.I.S.",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    val segments = remember(message.content) { parseMessageSegments(message.content) }
                    
                    segments.forEach { segment ->
                        when (segment) {
                            is MessageSegment.Text -> {
                                if (segment.content.isNotBlank()) {
                                    Text(
                                        text = segment.content.trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = JarvisTextPrimary
                                    )
                                }
                            }
                            is MessageSegment.Code -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                CodeBlock(code = segment.content, language = segment.language)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val content: String, val language: String?) : MessageSegment()
}

fun parseMessageSegments(content: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val regex = Regex("```(?:(\\w+)?\\n)?([\\s\\S]*?)```")
    var lastIndex = 0
    
    regex.findAll(content).forEach { matchResult ->
        // Add preceding text
        val textBefore = content.substring(lastIndex, matchResult.range.first)
        if (textBefore.isNotBlank()) {
            segments.add(MessageSegment.Text(textBefore))
        }
        
        // Add code block
        val language = matchResult.groups[1]?.value
        val code = matchResult.groups[2]?.value ?: ""
        segments.add(MessageSegment.Code(code.trim(), language))
        
        lastIndex = matchResult.range.last + 1
    }
    
    // Add remaining text
    if (lastIndex < content.length) {
        val remainingText = content.substring(lastIndex)
        if (remainingText.isNotBlank()) {
            segments.add(MessageSegment.Text(remainingText))
        }
    }
    
    return if (segments.isEmpty()) listOf(MessageSegment.Text(content)) else segments
}

@Composable
fun CodeBlock(
    code: String,
    language: String? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisBlack.copy(alpha = 0.6f))
            .border(1.dp, JarvisNavy, RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisNavy.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = language?.uppercase() ?: "CÓDIGO",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }
            
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan.copy(alpha = 0.9f)
                )
            )
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = JarvisCyan.copy(alpha = alpha),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun ChatList(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut()
            ) {
                ChatBubble(message = message)
            }
        }
    }
}

@Composable
fun TranscriptDisplay(
    transcript: String,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isListening && transcript.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(12.dp),
            color = JarvisSurface.copy(alpha = 0.9f)
        ) {
            Text(
                text = transcript,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = JarvisSuccess,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateMessage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "J.A.R.V.I.S.",
            style = MaterialTheme.typography.headlineLarge,
            color = JarvisCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Just A Rather Very Intelligent System",
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Toque el micrófono o escriba un mensaje para comenzar",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextMuted,
            textAlign = TextAlign.Center
        )
    }
}
