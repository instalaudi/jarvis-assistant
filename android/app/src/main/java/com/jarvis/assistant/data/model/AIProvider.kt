package com.jarvis.assistant.data.model

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI"),
    GEMINI("Google Gemini")
}

data class AIModel(
    val id: String,
    val name: String,
    val provider: AIProvider
)

object ModelDefinitions {
    val allModels = listOf(
        // OpenAI
        AIModel("gpt-4o", "GPT-4o (Gama Alta)", AIProvider.OPENAI),
        AIModel("gpt-4o-mini", "GPT-4o-mini (Rápido)", AIProvider.OPENAI),
        AIModel("gpt-4-turbo", "GPT-4 Turbo", AIProvider.OPENAI),
        
        // Gemini
        AIModel("gemini-1.5-pro", "Gemini 1.5 Pro", AIProvider.GEMINI),
        AIModel("gemini-1.5-flash", "Gemini 1.5 Flash", AIProvider.GEMINI)
    )

    fun getModelsForProvider(provider: AIProvider) = allModels.filter { it.provider == provider }
    
    fun getModelById(id: String) = allModels.find { it.id == id } ?: allModels.first()
}
