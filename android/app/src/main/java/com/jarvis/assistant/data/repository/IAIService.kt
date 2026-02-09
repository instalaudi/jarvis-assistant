package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.model.ChatStreamEvent
import kotlinx.coroutines.flow.Flow

interface IAIService {
    fun sendMessageStream(
        apiKey: String,
        modelId: String,
        messages: List<com.jarvis.assistant.data.api.OpenAIMessage>
    ): Flow<ChatStreamEvent>
    
    suspend fun sendMessage(
        apiKey: String,
        modelId: String,
        messages: List<com.jarvis.assistant.data.api.OpenAIMessage>
    ): com.jarvis.assistant.data.api.OpenAIMessage
}
