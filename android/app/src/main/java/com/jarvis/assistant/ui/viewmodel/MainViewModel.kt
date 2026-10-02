package com.jarvis.assistant.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.jarvis.assistant.data.api.ToolCall
import com.jarvis.assistant.data.api.ToolOutput
import com.jarvis.assistant.data.error.JarvisException
import com.jarvis.assistant.data.model.ChatState
import com.jarvis.assistant.data.model.ChatStreamEvent
import com.jarvis.assistant.data.model.Message
import com.jarvis.assistant.data.model.MessageRole
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.service.NotificationListenerService
import com.jarvis.assistant.service.SystemControlService
import com.jarvis.assistant.service.TextToSpeechService
import com.jarvis.assistant.service.VoiceRecognitionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceRecognitionService: VoiceRecognitionService,
    private val textToSpeechService: TextToSpeechService,
    private val systemControlService: SystemControlService,
    private val gmailService: com.jarvis.assistant.service.GmailService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    // State Machine
    private val _jarvisState = MutableStateFlow(com.jarvis.assistant.data.model.JarvisState.IDLE)
    val jarvisState: StateFlow<com.jarvis.assistant.data.model.JarvisState> = _jarvisState.asStateFlow()

    private var ttsEnabled = true
    private var conversationWindowJob: kotlinx.coroutines.Job? = null

    // Frases completas (terminan en . ! ? … seguido de espacio) para el TTS en streaming
    private val sentenceEndRegex = Regex("(?<=[.!?…])\\s+")

    init {
        checkSetupStatus()
        initializeServices()
        loadStoredMessages()
        
        // Initial State
        transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
    }

    private fun transitionTo(newState: com.jarvis.assistant.data.model.JarvisState) {
        viewModelScope.launch {
            val oldState = _jarvisState.value
            android.util.Log.d("MainViewModel", "State Transition: $oldState -> $newState")
            
            _jarvisState.value = newState

            when (newState) {
                com.jarvis.assistant.data.model.JarvisState.IDLE -> {
                    _chatState.update { it.copy(isListening = false, isProcessing = false, isSpeaking = false) }
                    stopVoiceRecognitionInternal()
                    // Aumentado a 500ms para asegurar la liberación del micrófono
                    kotlinx.coroutines.delay(500)
                    resumeWakeWordListening() // Enable Wake Word
                }
                com.jarvis.assistant.data.model.JarvisState.LISTENING -> {
                    conversationWindowJob?.cancel() // Cancel timeout if any
                    _chatState.update { it.copy(isListening = true, isProcessing = false, isSpeaking = false) }
                    pauseWakeWordListening() // Disable Wake Word
                    // Aumentado a 500ms para asegurar que WakeWord libere el micrófono
                    kotlinx.coroutines.delay(500)
                    startVoiceRecognitionInternal()
                }
                com.jarvis.assistant.data.model.JarvisState.PROCESSING -> {
                    _chatState.update { it.copy(isListening = false, isProcessing = true, isSpeaking = false) }
                    stopVoiceRecognitionInternal()
                    pauseWakeWordListening() // Ensure Wake Word is OFF
                }
                com.jarvis.assistant.data.model.JarvisState.SPEAKING -> {
                    _chatState.update { it.copy(isListening = false, isProcessing = false, isSpeaking = true) }
                    stopVoiceRecognitionInternal()
                    pauseWakeWordListening() // Ensure Wake Word is OFF
                }
                com.jarvis.assistant.data.model.JarvisState.COOLDOWN -> {
                    _chatState.update { it.copy(isListening = true, isProcessing = false, isSpeaking = false) }
                    pauseWakeWordListening() // Disable Wake Word
                    // Small delay not strictly necessary here as coming from Speaking, but good for safety
                    startVoiceRecognitionInternal()
                    
                    // Start Conversation Window Timer
                    conversationWindowJob?.cancel()
                    conversationWindowJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(5000) // 5 seconds window
                        if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.COOLDOWN) {
                            transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
                        }
                    }
                }
            }
        }
    }

    private fun checkSetupStatus() {
        _isSetupComplete.value = chatRepository.isSetupComplete()
    }

    private fun initializeServices() {
        // Inicializar TTS inmediatamente ya que no requiere FGS de micro
        textToSpeechService.initialize {
            val preferredVoice = chatRepository.getPreferredVoice()
            if (preferredVoice != null) {
                textToSpeechService.setVoice(preferredVoice)
            }
        }
        
        ttsEnabled = chatRepository.isTtsEnabled()
        voiceRecognitionService.initialize(context)

        // Iniciar observación de WhatsApp
        observeNotifications()

        // Iniciar WakeWordService con un pequeño retraso para asegurar "estado elegible"
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            if (chatRepository.isWakeWordEnabled()) {
                android.util.Log.d("MainViewModel", "Iniciando WakeWordService desde el primer plano comprobado")
                com.jarvis.assistant.service.WakeWordService.start(context)
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            NotificationListenerService.notifications.collect { notification ->
                if (chatRepository.isWhatsappEnabled() && 
                    _jarvisState.value == com.jarvis.assistant.data.model.JarvisState.IDLE) {
                    
                    val announcement = "Señor, tiene un mensaje de WhatsApp de ${notification.title} que dice: ${notification.text}"
                    android.util.Log.d("MainViewModel", "Avisando notificación de WhatsApp: $announcement")
                    textToSpeechService.speak(announcement)
                }
            }
        }
    }

    private fun loadStoredMessages() {
        viewModelScope.launch {
            val storedMessages = chatRepository.loadStoredMessages()
            _chatState.update { it.copy(messages = storedMessages) }
            
            if (storedMessages.isEmpty() && chatRepository.isSetupComplete()) {
                addWelcomeMessage()
            }
        }
    }

    private fun addWelcomeMessage() {
        val userName = chatRepository.getUserName()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Buenos días"
            hour < 19 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        val welcomeMessage = Message(
            content = "$greeting, $userName. Todos los sistemas están en línea y operativos. ¿En qué puedo asistirle hoy?",
            role = MessageRole.ASSISTANT
        )

        _chatState.update { it.copy(messages = listOf(welcomeMessage)) }
        
        if (ttsEnabled) {
            transitionTo(com.jarvis.assistant.data.model.JarvisState.SPEAKING)
            textToSpeechService.speak(welcomeMessage.content) {
                transitionTo(com.jarvis.assistant.data.model.JarvisState.COOLDOWN) // Open mic after welcome
            }
        }
    }

    private fun pauseWakeWordListening() {
        val intent = android.content.Intent(com.jarvis.assistant.service.WakeWordService.ACTION_PAUSE_LISTENING).apply {
            `package` = context.packageName
        }
        context.sendBroadcast(intent)
    }

    private fun resumeWakeWordListening() {
        val intent = android.content.Intent(com.jarvis.assistant.service.WakeWordService.ACTION_RESUME_LISTENING).apply {
            `package` = context.packageName
        }
        context.sendBroadcast(intent)
    }

    // ============ Public Triggers ============

    // Called from MainActivity specific broadcast receiver or UI
    fun onWakeWordDetected(command: String? = null) {
        if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.IDLE || 
            _jarvisState.value == com.jarvis.assistant.data.model.JarvisState.COOLDOWN) {
            
            if (!command.isNullOrBlank()) {
                android.util.Log.d("MainViewModel", "Procesando comando directo de WakeWord: $command")
                sendMessage(command)
            } else {
                transitionTo(com.jarvis.assistant.data.model.JarvisState.LISTENING)
            }
        }
    }
    
    // Called from UI Mic Button
    fun onMicButtonClicked() {
        if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.LISTENING || 
            _jarvisState.value == com.jarvis.assistant.data.model.JarvisState.COOLDOWN) {
            transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
        } else {
            transitionTo(com.jarvis.assistant.data.model.JarvisState.LISTENING)
        }
    }

    // ============ Internal Logic ============

    private fun startVoiceRecognitionInternal() {
        _chatState.update { it.copy(currentTranscript = "") }
        
        voiceRecognitionService.startListening(
            onResult = { transcript ->
                _chatState.update { it.copy(currentTranscript = transcript) }
            },
            onPartialResult = { partial ->
                _chatState.update { it.copy(currentTranscript = partial) }
            },
            onError = { error ->
                android.util.Log.e("MainViewModel", "ASR Error: $error")
                // Only show error if explicitly listening, otherwise silence in cooldown
                if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.LISTENING) {
                     _chatState.update { it.copy(error = "Error: $error") }
                     transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE) // Reset
                } else if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.COOLDOWN) {
                    // Silently fail back to IDLE in cooldown
                    transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
                }
            },
            onComplete = { finalTranscript ->
                if (finalTranscript.isNotBlank()) {
                    sendMessage(finalTranscript)
                } else {
                    transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
                }
            }
        )
    }

    private fun stopVoiceRecognitionInternal() {
        voiceRecognitionService.stopListening()
    }

    // ============ Chat & Processing ============

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Stop TTS if speaking (Interruption)
        if (_jarvisState.value == com.jarvis.assistant.data.model.JarvisState.SPEAKING) {
            textToSpeechService.stop()
        }
        
        transitionTo(com.jarvis.assistant.data.model.JarvisState.PROCESSING)

        viewModelScope.launch {
            val userMessage = Message(content = content, role = MessageRole.USER)
            _chatState.update { state -> state.copy(messages = state.messages + userMessage, error = null) }

            try {
                handleChatStream(chatRepository.sendMessageStream(content))
            } catch (e: Exception) {
                val errorText = e.localizedMessage ?: "Error sending message"
                _chatState.update { state -> state.copy(error = errorText) }
                transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
            }
        }
    }

    private suspend fun handleChatStream(flow: Flow<ChatStreamEvent>) {
        val assistantMessageId = UUID.randomUUID().toString()
        val initialMessage = Message(id = assistantMessageId, content = "", role = MessageRole.ASSISTANT, isLoading = true)
        
        _chatState.update { state -> state.copy(messages = state.messages + initialMessage) }

        val fullResponseBuilder = StringBuilder()
        // Texto recibido pero aún sin hablar (esperando el final de la frase)
        val speechBuffer = StringBuilder()
        var speechStarted = false
        var lastUpdate = 0L
        val batchInterval = 50L

        try {
            flow.collect { event ->
                when (event) {
                    is ChatStreamEvent.Content -> {
                        fullResponseBuilder.append(event.content)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= batchInterval) {
                            val currentText = fullResponseBuilder.toString()
                            _chatState.update { state ->
                                val updatedMessages = state.messages.map { msg ->
                                    if (msg.id == assistantMessageId) msg.copy(content = currentText) else msg
                                }
                                state.copy(messages = updatedMessages)
                            }
                            lastUpdate = now
                        }

                        // TTS en streaming: encolar frases completas mientras el LLM sigue generando
                        if (ttsEnabled) {
                            speechBuffer.append(event.content)
                            val sentences = sentenceEndRegex.split(speechBuffer.toString())
                            if (sentences.size > 1) {
                                val toSpeak = sentences.subList(0, sentences.size - 1).joinToString(" ")
                                if (!speechStarted) {
                                    speechStarted = true
                                    transitionTo(com.jarvis.assistant.data.model.JarvisState.SPEAKING)
                                }
                                textToSpeechService.speakQueued(toSpeak)
                                speechBuffer.clear()
                                speechBuffer.append(sentences.last())
                            }
                        }
                    }
                    is ChatStreamEvent.ToolCall -> {
                         // Tool logic remains similar, simplified for brevity but essential to keep
                        chatRepository.saveAssistantToolCall(fullResponseBuilder.toString(), event.toolCalls)
                        
                        val toolNames = event.toolCalls.joinToString { it.function.name }
                         _chatState.update { state ->
                            val updatedMessages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(content = fullResponseBuilder.toString() + "\n(Ejecutando: $toolNames...)", isLoading = false)
                                } else msg
                            }
                            state.copy(messages = updatedMessages)
                        }
                        
                        val outputs = executeTools(event.toolCalls)
                        // Recursive call
                        handleChatStream(chatRepository.sendToolOutputs(outputs))
                    }
                    is ChatStreamEvent.Done -> {
                       val currentText = fullResponseBuilder.toString()
                       if (currentText.isNotEmpty()) {
                           _chatState.update { state ->
                                val updatedMessages = state.messages.map { msg ->
                                    if (msg.id == assistantMessageId) {
                                        if (msg.isLoading) msg.copy(content = currentText, isLoading = false) else msg
                                    } else msg
                                }
                                state.copy(messages = updatedMessages)
                           }
                           
                           if (ttsEnabled) {
                               if (speechStarted) {
                                   // Ya venimos hablando por frases: encolar el resto y pasar a
                                   // COOLDOWN cuando la cola termine de drenar
                                   val remainder = speechBuffer.toString().trim()
                                   if (remainder.isNotEmpty()) {
                                       textToSpeechService.speakQueued(remainder) {
                                           transitionTo(com.jarvis.assistant.data.model.JarvisState.COOLDOWN) // Trigger Conversation Window
                                       }
                                   } else {
                                       textToSpeechService.whenQueueDrained {
                                           transitionTo(com.jarvis.assistant.data.model.JarvisState.COOLDOWN) // Trigger Conversation Window
                                       }
                                   }
                               } else {
                                   transitionTo(com.jarvis.assistant.data.model.JarvisState.SPEAKING)
                                   textToSpeechService.speak(currentText) {
                                       transitionTo(com.jarvis.assistant.data.model.JarvisState.COOLDOWN) // Trigger Conversation Window
                                   }
                               }
                           } else {
                               transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
                           }
                       } else {
                           transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
                       }
                    }
                }
            }
        } catch (e: Exception) {
            transitionTo(com.jarvis.assistant.data.model.JarvisState.IDLE)
             // Error handling logic
             // ... (Keep existing detailed error handling if needed, or simplified)
             _chatState.update { state -> state.copy(error = "Error streaming: ${e.message}") }
        }
    }
    
    private suspend fun executeTools(toolCalls: List<ToolCall>): List<ToolOutput> {
         // Re-using existing tool execution logic (extracted for cleaner code)
         return toolCalls.map { toolCall ->
            val functionName = toolCall.function.name
            val args = toolCall.function.arguments
            val outputContent = try {
                val jsonArgs = JsonParser.parseString(args).asJsonObject
                when (functionName) {
                    "toggle_flashlight" -> {
                        val state = jsonArgs.get("state").asBoolean
                        systemControlService.toggleFlashlight(state)
                    }
                     "set_brightness" -> {
                         val level = jsonArgs.get("level").asInt
                         systemControlService.setBrightness(level)
                     }
                     "set_volume" -> {
                         val level = jsonArgs.get("level").asInt
                         systemControlService.setVolume(level)
                     }
                    "open_app" -> {
                        val appName = jsonArgs.get("app_name").asString
                        systemControlService.openApp(appName)
                    }
                    "play_music" -> {
                        val query = jsonArgs.get("query").asString
                        val appName = if (jsonArgs.has("app_name")) jsonArgs.get("app_name").asString else null
                        systemControlService.playMusic(query, appName)
                    }
                    "send_whatsapp" -> {
                        val phoneNumber = jsonArgs.get("phone_number").asString
                        val message = jsonArgs.get("message").asString
                        systemControlService.sendWhatsApp(phoneNumber, message)
                    }
                     "read_emails" -> {
                         val limit = if (jsonArgs.has("limit")) jsonArgs.get("limit").asInt else 5
                         val query = if (jsonArgs.has("query")) jsonArgs.get("query").asString else "is:unread"
                         gmailService.readEmails(limit, query)
                     }
                     "send_email" -> {
                         val recipient = jsonArgs.get("recipient").asString
                         val subject = jsonArgs.get("subject").asString
                         val body = jsonArgs.get("body").asString
                         gmailService.sendEmail(recipient, subject, body)
                     }
                     "create_email_draft" -> {
                         val recipient = jsonArgs.get("recipient").asString
                         val subject = jsonArgs.get("subject").asString
                         val body = jsonArgs.get("body").asString
                         gmailService.createDraft(recipient, subject, body)
                     }
                    else -> "Función $functionName no encontrada."
                }
            } catch (e: Exception) {
                "Error ejecutando $functionName: ${e.message}"
            }
            ToolOutput(toolCallId = toolCall.id, output = outputContent)
        }
    }

    // ============ Standard UI Methods ============

    fun setTtsEnabled(enabled: Boolean) {
        ttsEnabled = enabled
    }

    fun updateTranscript(transcript: String) {
        _chatState.update { it.copy(currentTranscript = transcript) }
    }

    fun clearTranscript() {
        _chatState.update { it.copy(currentTranscript = "") }
    }

    fun clearError() {
        _chatState.update { it.copy(error = null) }
    }
    
    fun setListening(isListening: Boolean) {
        // Compatibility method - redirected to state machine
         if (isListening) onMicButtonClicked()
    }
    
    fun setSpeaking(isSpeaking: Boolean) {
        // Compatibility method - largely managed by state machine now
        _chatState.update { it.copy(isSpeaking = isSpeaking) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _chatState.update { it.copy(messages = emptyList()) }
            addWelcomeMessage()
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechService.shutdown()
        voiceRecognitionService.destroy()
    }
}
