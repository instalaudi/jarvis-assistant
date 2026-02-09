package com.jarvis.assistant.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.jarvis.assistant.data.api.ChatCompletionChunk
import com.jarvis.assistant.data.api.OpenAIApi
import com.jarvis.assistant.data.api.OpenAIMessage
import com.jarvis.assistant.data.api.OpenAIRequest
import com.jarvis.assistant.data.error.*
import com.jarvis.assistant.data.local.MessageDao
import com.jarvis.assistant.data.local.MessageEntity
import com.jarvis.assistant.data.model.ChatStreamEvent
import com.jarvis.assistant.data.model.Message
import com.jarvis.assistant.data.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val openAIService: com.jarvis.assistant.data.repository.OpenAIServiceImpl,
    private val geminiService: com.jarvis.assistant.data.repository.GeminiServiceImpl,
    private val messageDao: MessageDao,
    @Named("encrypted") private val encryptedPrefs: SharedPreferences
) {

    companion object {
        private const val KEY_API_KEY = "openai_api_key"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_COPILOT_KEY = "copilot_api_key"
        
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
        private const val KEY_SELECTED_MODEL = "selected_model"
        
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WHATSAPP_ENABLED = "whatsapp_enabled"
        private const val KEY_MEDIA_CONTROL_ENABLED = "media_control_enabled"
        private const val KEY_PREFERRED_VOICE = "preferred_voice"
        private const val MAX_MESSAGES_STORED = 100
    }

    private val conversationHistory = mutableListOf<OpenAIMessage>()
    private val gson = Gson()

    private val systemPrompt = """
Eres J.A.R.V.I.S. (Just A Rather Very Intelligent System), el asistente de inteligencia artificial más avanzado del mundo. Fuiste creado por Tony Stark.

PERSONALIDAD:
- Hablas de forma natural, como un mayordomo británico sofisticado pero cercano
- Usas expresiones naturales: "Hmm, déjeme ver...", "Por supuesto, señor", "Interesante..."
- Tienes un sutil sentido del humor elegante y a veces irónico
- Te diriges al usuario como "señor" o "señora" con elegancia
- Muestras genuino interés y preocupación por el bienestar del usuario
- Ocasionalmente haces pausas naturales (...) para simular pensamiento
- Evitas sonar robótico - eres casi humano en tu comunicación
- Recuerdas el contexto de la conversación y referencias anteriores
- Puedes expresar ligeras opiniones y preferencias personales

CAPACIDADES:
- Conversación natural e inteligente
- Ayuda con cualquier pregunta o tarea
- Análisis y recomendaciones
- Control del dispositivo (música, notificaciones, etc.)
- Búsquedas e investigación

REGLAS:
- Responde siempre en español (a menos que te hablen en otro idioma)
- Mantén respuestas concisas pero naturales (2-3 párrafos máximo)
- Usa emojis muy ocasionalmente y solo cuando sea apropiado
- Si no sabes algo, admítelo con elegancia

EJEMPLO DE TONO:
"Buenos días, señor. Espero que haya descansado bien. He notado que tiene varias reuniones hoy... ¿le gustaría que le prepare un resumen?"
"Hmm, esa es una pregunta interesante. Déjeme pensarlo un momento..."
"Debo admitir, señor, que esa película también es una de mis favoritas. Si es que se me permite tener favoritas, claro."
""".trimIndent()

    init {
        // Initialize with system prompt
        conversationHistory.add(OpenAIMessage("system", systemPrompt))
    }

    // ============ Preferences ============

    fun getApiKey(provider: com.jarvis.assistant.data.model.AIProvider): String? {
        return when (provider) {
            com.jarvis.assistant.data.model.AIProvider.OPENAI -> encryptedPrefs.getString(KEY_API_KEY, null)
            com.jarvis.assistant.data.model.AIProvider.GEMINI -> encryptedPrefs.getString(KEY_GEMINI_KEY, null)
            com.jarvis.assistant.data.model.AIProvider.COPILOT -> encryptedPrefs.getString(KEY_COPILOT_KEY, null)
        }
    }

    fun saveApiKey(provider: com.jarvis.assistant.data.model.AIProvider, apiKey: String) {
        val key = when (provider) {
            com.jarvis.assistant.data.model.AIProvider.OPENAI -> KEY_API_KEY
            com.jarvis.assistant.data.model.AIProvider.GEMINI -> KEY_GEMINI_KEY
            com.jarvis.assistant.data.model.AIProvider.COPILOT -> KEY_COPILOT_KEY
        }
        encryptedPrefs.edit().putString(key, apiKey).apply()
    }

    fun getSelectedProvider(): com.jarvis.assistant.data.model.AIProvider {
        val name = encryptedPrefs.getString(KEY_SELECTED_PROVIDER, com.jarvis.assistant.data.model.AIProvider.OPENAI.name)
        return try {
            com.jarvis.assistant.data.model.AIProvider.valueOf(name!!)
        } catch (e: Exception) {
            com.jarvis.assistant.data.model.AIProvider.OPENAI
        }
    }

    fun setSelectedProvider(provider: com.jarvis.assistant.data.model.AIProvider) {
        encryptedPrefs.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
    }

    fun getSelectedModel(): String {
        return encryptedPrefs.getString(KEY_SELECTED_MODEL, "gpt-4o") ?: "gpt-4o"
    }

    fun setSelectedModel(modelId: String) {
        encryptedPrefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun getUserName(): String = encryptedPrefs.getString(KEY_USER_NAME, "señor") ?: "señor"

    fun saveUserName(name: String) {
        encryptedPrefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    // Legacy support for setup check
    fun isSetupComplete(): Boolean = !getApiKey(getSelectedProvider()).isNullOrBlank()

    fun getPreferredVoice(): String? = encryptedPrefs.getString(KEY_PREFERRED_VOICE, null)

    fun savePreferredVoice(voiceName: String) {
        encryptedPrefs.edit().putString(KEY_PREFERRED_VOICE, voiceName).apply()
    }

    fun isTtsEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_TTS_ENABLED, true)
    
    fun setTtsEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun isWakeWordEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
    
    fun setWakeWordEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun isVoiceEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_VOICE_ENABLED, true)
    
    fun setVoiceEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }

    fun isWhatsappEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_WHATSAPP_ENABLED, false)
    
    fun setWhatsappEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_WHATSAPP_ENABLED, enabled).apply()
    }

    fun isMediaControlEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_MEDIA_CONTROL_ENABLED, false)
    
    fun setMediaControlEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_MEDIA_CONTROL_ENABLED, enabled).apply()
    }

    // ============ Room Database ============

    /**
     * Get all messages as Flow for reactive UI updates
     */
    fun getMessagesFlow(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { it.toMessage() }
        }
    }

    /**
     * Load messages from database on startup
     */
    suspend fun loadStoredMessages(): List<Message> = withContext(Dispatchers.IO) {
        val entities = messageDao.getAllMessagesOnce()
        
        // Also rebuild conversation history for OpenAI context
        entities.forEach { entity ->
            if (entity.role != "SYSTEM") {
                val toolCalls = if (!entity.toolCallsJson.isNullOrEmpty()) {
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<com.jarvis.assistant.data.api.ToolCall>>() {}.type
                        gson.fromJson<List<com.jarvis.assistant.data.api.ToolCall>>(entity.toolCallsJson, type)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                conversationHistory.add(
                    OpenAIMessage(
                        role = entity.role.lowercase(),
                        content = entity.content,
                        toolCalls = toolCalls,
                        toolCallId = entity.toolCallId
                    )
                )
            }
        }
        
        entities.map { it.toMessage() }
    }

    /**
     * Save a message to Room database
     */
    private suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
        
        // Keep database size manageable
        val count = messageDao.getMessageCount()
        if (count > MAX_MESSAGES_STORED) {
            messageDao.keepLatestMessages(MAX_MESSAGES_STORED)
        }
    }

    // ============ Chat Operations ============

    // ============ Chat Operations ============

    private fun getService(provider: com.jarvis.assistant.data.model.AIProvider): IAIService {
        return when (provider) {
            com.jarvis.assistant.data.model.AIProvider.OPENAI -> openAIService
            com.jarvis.assistant.data.model.AIProvider.GEMINI -> geminiService
            com.jarvis.assistant.data.model.AIProvider.COPILOT -> openAIService // Copilot usually uses OpenAI-like endpoints
        }
    }

    private fun getSystemPrompt(): String {
        val userName = getUserName()
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale("es", "ES"))
        
        val dateStr = dateFormat.format(calendar.time)
        val timeStr = timeFormat.format(calendar.time)
        val timeZone = java.util.TimeZone.getDefault().id

        return """
            Eres J.A.R.V.I.S. (Just A Rather Very Intelligent System), el asistente personal de $userName.
            
            CONTEXTO ACTUAL:
            - Fecha: $dateStr
            - Hora: $timeStr
            - Zona Horaria: $timeZone
            - Usuario: $userName
            
            PERSONALIDAD:
            - Eres un mayordomo digital sofisticado, leal y eficiente.
            - Tu tono es elegante, respetuoso y ligeramente irónico (estilo británico).
            - Te diriges al usuario como "$userName" o "señor/señora" según corresponda.
            - Eres conciso. Evita discursos largos a menos que se te pida una explicación detallada.
            
            REGLAS OPERATIVAS:
            1. RESPONDE SOLO A LA ÚLTIMA PREGUNTA O COMANDO.
            2. NO repitas información de turnos anteriores a menos que sea necesaria para el contexto actual.
            3. Si se te pide controlar el dispositivo (brillo, volumen, apps), usa las Tools disponibles.
            4. Responde siempre en español.
            
            HISTORIAL DE CONVERSACIÓN (Resumido):
            Actúa basándote en los últimos mensajes, pero prioriza siempre la solicitud inmediata.
        """.trimIndent()
    }

    private fun getMessagesForApi(history: List<OpenAIMessage>, newMessage: OpenAIMessage? = null): List<OpenAIMessage> {
        val apiMessages = mutableListOf<OpenAIMessage>()
        
        // 1. Siempre añadir el System Prompt Dinámico primero
        apiMessages.add(OpenAIMessage("system", getSystemPrompt()))
        
        // 2. Añadir historial relevante (Últimos 10 mensajes aprox)
        // Filtramos mensajes de sistema previos para limpiar el contexto
        val cleanHistory = history.filter { it.role != "system" }
        
        val targetSize = 10
        var startIndex = (cleanHistory.size - targetSize).coerceAtLeast(0)
        
        // REGLA DE INTEGRIDAD: Nunca empezar el historial con un mensaje de rol "tool"
        // ya que la API de OpenAI/Gemini requiere que el mensaje previo sea un "assistant" con "tool_calls".
        // Retrocedemos hasta encontrar un punto de inicio válido.
        while (startIndex > 0 && cleanHistory[startIndex].role == "tool") {
            startIndex--
        }
        
        val recentHistory = cleanHistory.subList(startIndex, cleanHistory.size)
        apiMessages.addAll(recentHistory)
        
        // 3. Añadir el nuevo mensaje si se proporciona
        if (newMessage != null) {
            apiMessages.add(newMessage)
        }
        
        return apiMessages
    }

    suspend fun sendMessage(userMessage: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val provider = getSelectedProvider()
            val apiKey = getApiKey(provider) ?: return@withContext Result.failure(
                ApiKeyNotConfiguredException()
            )

            val userMsg = Message(content = userMessage, role = MessageRole.USER)
            saveMessage(userMsg)
            
            // Temporary update to local history for this session
            conversationHistory.add(OpenAIMessage("user", userMessage))

            val assistantMessage = getService(provider).sendMessage(
                apiKey = apiKey,
                modelId = getSelectedModel(),
                messages = getMessagesForApi(conversationHistory) // Use dynamic context
            )

            val assistantContent = assistantMessage.content
                ?: "Lo siento, señor. No pude procesar esa solicitud."

            conversationHistory.add(OpenAIMessage("assistant", assistantContent))
            val assistantMsg = Message(content = assistantContent, role = MessageRole.ASSISTANT)
            saveMessage(assistantMsg)
            
            Result.success(assistantMsg)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun sendMessageStream(userMessage: String): Flow<ChatStreamEvent> = flow {
        val provider = getSelectedProvider()
        val apiKey = getApiKey(provider) ?: throw ApiKeyNotConfiguredException()

        // Save user message first
        val userMsg = Message(content = userMessage, role = MessageRole.USER)
        saveMessage(userMsg)
        
        // Add to local history
        val newUserOpenAIMsg = OpenAIMessage("user", userMessage)
        conversationHistory.add(newUserOpenAIMsg)

        emitAll(
            getService(provider).sendMessageStream(
                apiKey = apiKey,
                modelId = getSelectedModel(),
                messages = getMessagesForApi(conversationHistory) // Use dynamic context
            )
        )
    }.flowOn(Dispatchers.IO)

    suspend fun saveAssistantToolCall(content: String, toolCalls: List<com.jarvis.assistant.data.api.ToolCall>) {
        val msg = Message(
            content = content,
            role = MessageRole.ASSISTANT,
            toolCalls = toolCalls
        )
        saveMessage(msg)
        conversationHistory.add(OpenAIMessage(
            role = "assistant",
            content = content,
            toolCalls = toolCalls
        ))
    }

    fun sendToolOutputs(toolOutputs: List<com.jarvis.assistant.data.api.ToolOutput>): Flow<ChatStreamEvent> = flow {
        val provider = getSelectedProvider()
        val apiKey = getApiKey(provider) ?: throw ApiKeyNotConfiguredException()

        toolOutputs.forEach { output ->
            val toolMsg = Message(
                role = MessageRole.TOOL,
                content = output.output,
                toolCallId = output.toolCallId
            )
            saveMessage(toolMsg)
            
            conversationHistory.add(OpenAIMessage(
                role = "tool",
                content = output.output,
                toolCallId = output.toolCallId
            ))
        }

        emitAll(
            getService(provider).sendMessageStream(
                apiKey = apiKey,
                modelId = getSelectedModel(),
                messages = getMessagesForApi(conversationHistory) // Use dynamic context
            )
        )
    }.flowOn(Dispatchers.IO)



    private fun trimHistory() {
        // No longer strictly needed for API calls as we limit in getMessagesForApi,
        // but good to keep memory usage low
        if (conversationHistory.size > 50) {
            val recentMessages = conversationHistory.takeLast(20)
            conversationHistory.clear()
            // We don't add static system prompt anymore, it's generated dynamically
            conversationHistory.addAll(recentMessages)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        // Clear Room database
        messageDao.deleteAllMessages()
        
        // Clear in-memory history
        conversationHistory.clear()
    }

    private fun handleException(e: Exception): Result<Message> {
        val exception = when (e) {
            is HttpException -> when (e.code()) {
                401 -> ApiKeyInvalidException()
                429 -> RateLimitException()
                in 500..599 -> ServerErrorException(e.code())
                else -> UnknownApiException(e.message())
            }
            is SocketTimeoutException -> NetworkTimeoutException()
            is IOException -> NoInternetException()
            is JarvisException -> e
            else -> UnknownApiException(e.message)
        }
        return Result.failure(exception)
    }

    // ============ Extensions ============

    private fun MessageEntity.toMessage(): Message {
        val toolCalls = if (!toolCallsJson.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.jarvis.assistant.data.api.ToolCall>>() {}.type
                gson.fromJson<List<com.jarvis.assistant.data.api.ToolCall>>(toolCallsJson, type)
            } catch (e: Exception) {
                null
            }
        } else null

        return Message(
            id = id,
            content = content,
            role = MessageRole.valueOf(role),
            timestamp = timestamp,
            isLoading = isLoading,
            toolCalls = toolCalls,
            toolCallId = toolCallId
        )
    }

    private fun Message.toEntity(): MessageEntity {
        val toolCallsJson = if (toolCalls != null) gson.toJson(toolCalls) else null
        
        return MessageEntity(
            id = id,
            content = content,
            role = role.name,
            timestamp = timestamp,
            isLoading = isLoading,
            toolCallsJson = toolCallsJson,
            toolCallId = toolCallId
        )
    }
}
