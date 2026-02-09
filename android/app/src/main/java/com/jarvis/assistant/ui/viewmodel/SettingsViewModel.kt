package com.jarvis.assistant.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Named

data class SettingsUiState(
    val voiceEnabled: Boolean = true,
    val ttsEnabled: Boolean = true,
    val whatsappEnabled: Boolean = false,
    val mediaControlEnabled: Boolean = false,
    val gmailConnected: Boolean = false,
    val selectedProvider: com.jarvis.assistant.data.model.AIProvider = com.jarvis.assistant.data.model.AIProvider.OPENAI,
    val selectedModelId: String = "gpt-4o",
    val openaiKey: String = "",
    val geminiKey: String = "",
    val copilotKey: String = "",
    val wakeWordEnabled: Boolean = false,
    val appVersion: String = "2.1.0",
    val currentModelName: String = "GPT-4o"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @Named("encrypted") private val prefs: SharedPreferences,
    private val chatRepository: com.jarvis.assistant.data.repository.ChatRepository,
    private val textToSpeechService: com.jarvis.assistant.service.TextToSpeechService,
    private val gmailService: com.jarvis.assistant.service.GmailService
) : ViewModel() {


    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkGmailConnection()
    }

    private fun loadSettings(): SettingsUiState {
        return SettingsUiState(
            voiceEnabled = chatRepository.isVoiceEnabled(),
            ttsEnabled = chatRepository.isTtsEnabled(),
            whatsappEnabled = chatRepository.isWhatsappEnabled(),
            mediaControlEnabled = chatRepository.isMediaControlEnabled(),
            gmailConnected = prefs.getBoolean("gmail_connected", false),
            selectedProvider = chatRepository.getSelectedProvider(),
            selectedModelId = chatRepository.getSelectedModel(),
            openaiKey = chatRepository.getApiKey(com.jarvis.assistant.data.model.AIProvider.OPENAI) ?: "",
            geminiKey = chatRepository.getApiKey(com.jarvis.assistant.data.model.AIProvider.GEMINI) ?: "",
            copilotKey = chatRepository.getApiKey(com.jarvis.assistant.data.model.AIProvider.COPILOT) ?: "",
            wakeWordEnabled = chatRepository.isWakeWordEnabled(),
            appVersion = "2.1.0",
            currentModelName = com.jarvis.assistant.data.model.ModelDefinitions.getModelById(chatRepository.getSelectedModel()).name
        )
    }

    fun setSelectedProvider(provider: com.jarvis.assistant.data.model.AIProvider) {
        chatRepository.setSelectedProvider(provider)
        // Reset model if current one doesn't belong to provider
        val models = com.jarvis.assistant.data.model.ModelDefinitions.getModelsForProvider(provider)
        if (models.none { it.id == uiState.value.selectedModelId }) {
            models.firstOrNull()?.let { setSelectedModel(it.id) }
        }
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun setSelectedModel(modelId: String) {
        chatRepository.setSelectedModel(modelId)
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun saveApiKey(provider: com.jarvis.assistant.data.model.AIProvider, key: String) {
        chatRepository.saveApiKey(provider, key)
        _uiState.update {
            when (provider) {
                com.jarvis.assistant.data.model.AIProvider.OPENAI -> it.copy(openaiKey = key)
                com.jarvis.assistant.data.model.AIProvider.GEMINI -> it.copy(geminiKey = key)
                com.jarvis.assistant.data.model.AIProvider.COPILOT -> it.copy(copilotKey = key)
            }
        }
    }

    // ... existing toggle methods ...

    fun setVoiceEnabled(enabled: Boolean) {
        chatRepository.setVoiceEnabled(enabled)
        _uiState.update { it.copy(voiceEnabled = enabled) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        chatRepository.setTtsEnabled(enabled)
        _uiState.update { it.copy(ttsEnabled = enabled) }
    }

    fun setWhatsappEnabled(enabled: Boolean) {
        chatRepository.setWhatsappEnabled(enabled)
        _uiState.update { it.copy(whatsappEnabled = enabled) }
    }

    fun setMediaControlEnabled(enabled: Boolean) {
        chatRepository.setMediaControlEnabled(enabled)
        _uiState.update { it.copy(mediaControlEnabled = enabled) }
    }

    fun setWakeWordEnabled(context: android.content.Context, enabled: Boolean) {
        chatRepository.setWakeWordEnabled(enabled)
        _uiState.update { it.copy(wakeWordEnabled = enabled) }
        
        if (enabled) {
            com.jarvis.assistant.service.WakeWordService.start(context)
        } else {
            com.jarvis.assistant.service.WakeWordService.stop(context)
        }
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        _uiState.value = SettingsUiState()
    }
    
    // Voice Selection Logic
    
    fun getAvailableVoices(): List<android.speech.tts.Voice> {
        return textToSpeechService.getAvailableVoices()
    }
    
    fun setPreferredVoice(voiceName: String) {
        textToSpeechService.setVoice(voiceName)
        prefs.edit().putString("preferred_voice", voiceName).apply()
    }
    
    fun getPreferredVoice(): String? {
        return prefs.getString("preferred_voice", null)
    }

    fun previewVoice(voiceName: String) {
        textToSpeechService.setVoice(voiceName)
        textToSpeechService.speak("Hola, soy JARVIS. Esta es una prueba de mi voz.")
    }

    // Gmail Logic

    fun getGmailSignInIntent(): android.content.Intent {
        return gmailService.getSignInIntent()
    }

    fun checkGmailConnection() {
        val account = gmailService.getLastSignedInAccount()
        _uiState.update { it.copy(gmailConnected = account != null) }
        prefs.edit().putBoolean("gmail_connected", account != null).apply()
    }

    fun handleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                _uiState.update { it.copy(gmailConnected = true) }
                prefs.edit().putBoolean("gmail_connected", true).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(gmailConnected = false) }
        }
    }

    fun disconnectGmail() {
        gmailService.signOut {
            _uiState.update { it.copy(gmailConnected = false) }
            prefs.edit().putBoolean("gmail_connected", false).apply()
        }
    }
}
