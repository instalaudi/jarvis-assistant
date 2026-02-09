package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

data class VoiceRecognitionState(
    val isListening: Boolean = false,
    val partialResults: String = "",
    val finalResult: String = "",
    val error: String? = null
)

@Singleton
class VoiceRecognitionService @Inject constructor() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var context: Context? = null
    
    private val _state = MutableStateFlow(VoiceRecognitionState())
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    // Callbacks
    private var onResultCallback: ((String) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onCompleteCallback: ((String) -> Unit)? = null

    val isAvailable: Boolean
        get() = context?.let { SpeechRecognizer.isRecognitionAvailable(it) } ?: false

    fun initialize(ctx: Context) {
        context = ctx
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
            speechRecognizer?.setRecognitionListener(createListener())
        }
    }

    /**
     * Start listening with multiple callbacks for different events
     */
    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: (String) -> Unit
    ) {
        onResultCallback = onResult
        onPartialResultCallback = onPartialResult
        onErrorCallback = onError
        onCompleteCallback = onComplete
        
        val ctx = context ?: run {
            val error = "Context not initialized for VoiceRecognitionService"
            _state.value = _state.value.copy(error = error)
            onErrorCallback?.invoke(error)
            return
        }
        
        if (!isAvailable) {
            val error = "Reconocimiento de voz no disponible"
            _state.value = _state.value.copy(error = error)
            onErrorCallback?.invoke(error)
            return
        }

        // Use MainScope to launch on Main thread without blocking
        CoroutineScope(Dispatchers.Main).launch {
            // Siempre limpiar la instancia anterior antes de una nueva escucha para evitar ERROR_CLIENT (5)
            destroy()
            delay(50) // Pequeño buffer para que el sistema procese la destrucción

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
                speechRecognizer?.setRecognitionListener(createListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
                    
                    // --- Ajustes de Tiempo para evitar cortes prematuros ---
                    // Tiempo de silencio total para considerar que se terminó de hablar (5 segundos)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L) 
                    // Tiempo de silencio parcial (posible final, 3.5 segundos)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                    // Longitud mínima de la entrada de voz (2 segundos)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                }

                speechRecognizer?.startListening(intent)
                _state.value = _state.value.copy(isListening = true, error = null, partialResults = "")
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = "Error al iniciar escucha: ${e.message}"
                _state.value = _state.value.copy(error = errorMsg)
                onErrorCallback?.invoke(errorMsg)
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore if already stopped
        }
        _state.value = _state.value.copy(isListening = false)
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) { e.printStackTrace() }
        _state.value = _state.value.copy(isListening = false, partialResults = "")
    }

    fun destroy() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) { e.printStackTrace() }
        speechRecognizer = null
        _state.value = _state.value.copy(isListening = false)
    }

    private fun clearCallbacks() {
        onResultCallback = null
        onPartialResultCallback = null
        onErrorCallback = null
        onCompleteCallback = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false)
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red"
                SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el habla"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó habla"
                else -> "Error desconocido"
            }
            val fullError = "$errorMessage ($error)"
            _state.value = _state.value.copy(isListening = false, error = fullError)
            onErrorCallback?.invoke(fullError)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val result = matches?.firstOrNull() ?: ""
            _state.value = _state.value.copy(
                isListening = false,
                finalResult = result,
                partialResults = ""
            )
            if (result.isNotBlank()) {
                onResultCallback?.invoke(result)
                onCompleteCallback?.invoke(result)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            _state.value = _state.value.copy(partialResults = partial)
            onPartialResultCallback?.invoke(partial)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
