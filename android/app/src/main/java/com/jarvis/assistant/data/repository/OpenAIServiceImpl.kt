package com.jarvis.assistant.data.repository

import android.util.Log
import com.google.gson.Gson
import com.jarvis.assistant.data.api.*
import com.jarvis.assistant.data.model.ChatStreamEvent
import com.jarvis.assistant.data.model.ToolDefinitions
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class OpenAIServiceImpl @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val gson: Gson
) : IAIService {

    override suspend fun sendMessage(
        apiKey: String,
        modelId: String,
        messages: List<OpenAIMessage>
    ): OpenAIMessage {
        val request = OpenAIRequest(
            model = modelId,
            messages = messages,
            maxTokens = 1024,
            temperature = 0.8
        )

        val response = openAIApi.createChatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        val choice = response.choices.firstOrNull()
        return choice?.message ?: OpenAIMessage("assistant", "Lo siento, señor. Hubo un error.")
    }

    override fun sendMessageStream(
        apiKey: String,
        modelId: String,
        messages: List<OpenAIMessage>
    ): Flow<ChatStreamEvent> = flow {
        val request = OpenAIRequest(
            model = modelId,
            messages = messages,
            maxTokens = 1024,
            temperature = 0.8,
            stream = true,
            tools = ToolDefinitions.allTools,
            toolChoice = "auto"
        )

        try {
            val responseBody = openAIApi.createChatCompletionStream(
                authorization = "Bearer $apiKey",
                request = request
            )

            responseBody.use { body ->
                val inputStream = body.byteStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val toolCallsMap = mutableMapOf<Int, ChunkToolCall>()

                var line: String? = reader.readLine()
                while (line != null && currentCoroutineContext().isActive) {
                    if (line.startsWith("data: ") && line != "data: [DONE]") {
                        val json = line.removePrefix("data: ")
                        try {
                            val chunk = gson.fromJson(json, ChatCompletionChunk::class.java)
                            val choice = chunk.choices.firstOrNull()
                            val delta = choice?.delta
                            val finishReason = choice?.finishReason

                            if (!delta?.content.isNullOrEmpty()) {
                                emit(ChatStreamEvent.Content(delta.content!!))
                            }

                            if (delta?.toolCalls != null) {
                                delta.toolCalls.forEach { toolCallChunk ->
                                    val index = toolCallChunk.index
                                    val existing = toolCallsMap[index]
                                    val mergedId = existing?.id ?: toolCallChunk.id
                                    val mergedName = (existing?.function?.name ?: "") + (toolCallChunk.function?.name ?: "")
                                    val mergedArgs = (existing?.function?.arguments ?: "") + (toolCallChunk.function?.arguments ?: "")
                                    toolCallsMap[index] = toolCallChunk.copy(
                                        id = mergedId,
                                        function = ChunkFunction(name = mergedName, arguments = mergedArgs)
                                    )
                                }
                            }

                            if (finishReason == "tool_calls" || (finishReason == "stop" && toolCallsMap.isNotEmpty())) {
                                 val finalToolCalls = toolCallsMap.values.map { accumulated ->
                                     ToolCall(
                                         id = accumulated.id ?: "",
                                         type = "function",
                                         function = ToolCallFunction(
                                             name = accumulated.function?.name ?: "",
                                             arguments = accumulated.function?.arguments ?: ""
                                         )
                                     )
                                 }
                                 emit(ChatStreamEvent.ToolCall(finalToolCalls))
                            }
                        } catch (e: Exception) {
                            Log.e("OpenAIService", "Error parsing chunk: $json", e)
                        }
                    }
                    line = reader.readLine()
                }
            } // Auto-closes responseBody and underlying stream
            
            emit(ChatStreamEvent.Done)
        } catch (e: Exception) {
            throw e
        }
    }
}
