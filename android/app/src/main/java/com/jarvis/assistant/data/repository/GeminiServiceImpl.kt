package com.jarvis.assistant.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import com.jarvis.assistant.data.api.OpenAIMessage
import com.jarvis.assistant.data.api.ToolCall
import com.jarvis.assistant.data.api.ToolCallFunction
import com.jarvis.assistant.data.model.ChatStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GeminiServiceImpl @Inject constructor() : IAIService {

    override suspend fun sendMessage(
        apiKey: String,
        modelId: String,
        messages: List<OpenAIMessage>
    ): OpenAIMessage {
        val generativeModel = GenerativeModel(
            modelName = modelId,
            apiKey = apiKey
        )

        val history = messages.map { msg ->
            content(role = if (msg.role == "assistant") "model" else "user") {
                text(msg.content ?: "")
            }
        }

        val response = generativeModel.generateContent(*history.toTypedArray())
        return OpenAIMessage("assistant", response.text ?: "No recibí respuesta de Gemini.")
    }

    override fun sendMessageStream(
        apiKey: String,
        modelId: String,
        messages: List<OpenAIMessage>
    ): Flow<ChatStreamEvent> = flow {
        val generativeModel = GenerativeModel(
            modelName = modelId,
            apiKey = apiKey
        )

        // Only take the last few for context in Gemini stream to avoid too many parts for now
        // and avoid system prompt issues if not handled by SDK systemInstruction
        val history = messages.map { msg ->
            content(role = if (msg.role == "assistant") "model" else "user") {
                text(msg.content ?: "")
            }
        }

        try {
            generativeModel.generateContentStream(*history.toTypedArray()).collect { response ->
                response.text?.let { 
                    emit(ChatStreamEvent.Content(it))
                }
            }
        } catch (e: Exception) {
            // Prevent crash on network error
            android.util.Log.e("GeminiService", "Error streaming content", e)
            emit(ChatStreamEvent.Content(" [Error de conexión con Gemini]"))
        }
        emit(ChatStreamEvent.Done)
    }
}
