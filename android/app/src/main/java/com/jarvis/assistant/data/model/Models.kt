package com.jarvis.assistant.data.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val toolCalls: List<com.jarvis.assistant.data.api.ToolCall>? = null,
    val toolCallId: String? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val currentTranscript: String = "",
    val error: String? = null
)

data class JarvisStatus(
    val isOnline: Boolean = true,
    val voiceEnabled: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val mediaControlEnabled: Boolean = false,
    val systemStatus: String = "Todos los sistemas operativos"
)

sealed class ChatStreamEvent {
    data class Content(val content: String) : ChatStreamEvent()
    data class ToolCall(val toolCalls: List<com.jarvis.assistant.data.api.ToolCall>) : ChatStreamEvent()
    object Done : ChatStreamEvent()
}
