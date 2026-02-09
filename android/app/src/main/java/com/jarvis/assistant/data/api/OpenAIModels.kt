package com.jarvis.assistant.data.api

import com.google.gson.annotations.SerializedName

// OpenAI API Request/Response models

data class OpenAIRequest(
    val model: String = "gpt-4o",
    val messages: List<OpenAIMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val temperature: Double = 0.7,
    @SerializedName("presence_penalty")
    val presencePenalty: Double = 0.6,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double = 0.3,

    val stream: Boolean = false,
    val tools: List<Tool>? = null,
    @SerializedName("tool_choice")
    val toolChoice: Any? = null // "auto", "none", or specific tool
)

data class OpenAIMessage(
    val role: String,
    val content: String?,
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id")
    val toolCallId: String? = null
)

data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: OpenAIMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

data class OpenAIError(
    val error: ErrorDetail?
)

data class ErrorDetail(
    val message: String?,
    val type: String?,
    val code: String?
)

// Tools Models
data class Tool(
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: Any // Map or JsonObject
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ToolOutput(
    val toolCallId: String,
    val output: String
)

// Streaming Models
data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>
)

data class ChunkChoice(
    val index: Int,
    val delta: ChunkDelta,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ChunkDelta(
    val content: String?,
    @SerializedName("tool_calls")
    val toolCalls: List<ChunkToolCall>? = null
)

data class ChunkToolCall(
    val index: Int,
    val id: String?,
    val function: ChunkFunction?
)

data class ChunkFunction(
    val name: String?,
    val arguments: String?
)
