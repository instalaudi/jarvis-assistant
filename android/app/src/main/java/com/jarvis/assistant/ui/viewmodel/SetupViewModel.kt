package com.jarvis.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSetupComplete: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        // Check if already setup
        if (chatRepository.isSetupComplete()) {
            _uiState.update { it.copy(isSetupComplete = true) }
        }
    }

    fun completeSetup(apiKey: String, userName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Validate API key format
                if (!apiKey.startsWith("sk-") || apiKey.length < 20) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Formato de API key inválido"
                        )
                    }
                    return@launch
                }

                // Save credentials
                chatRepository.saveApiKey(com.jarvis.assistant.data.model.AIProvider.OPENAI, apiKey)
                chatRepository.saveUserName(userName)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSetupComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al guardar configuración: ${e.message}"
                    )
                }
            }
        }
    }
}
