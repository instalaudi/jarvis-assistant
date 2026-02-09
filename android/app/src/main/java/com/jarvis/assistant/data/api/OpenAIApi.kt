package com.jarvis.assistant.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApi {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): OpenAIResponse

    @retrofit2.http.Streaming
    @POST("v1/chat/completions")
    suspend fun createChatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): okhttp3.ResponseBody
}
