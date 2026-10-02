package com.jarvis.assistant.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val pendingMessages = ConcurrentLinkedQueue<String>()

    // Callbacks por utterance: permite hablar por frases mientras llega el stream del LLM
    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()
    // Acción a ejecutar cuando la cola de habla termina de drenar por completo
    private val queueDrainedCallback = AtomicReference<(() -> Unit)?>(null)
    // Número de utterances encoladas en el motor TTS (los eventos llegan en un hilo binder)
    private val pendingUtterances = AtomicInteger(0)

    // ===== Audio Focus: la música baja/bpausa mientras JARVIS habla =====
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        // Pérdida permanente del foco (otra app pidió audio excluyente): cortamos la habla
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stop()
        }
    }

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }

    private var hasAudioFocus = false

    fun initialize(onInit: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureTTS()
                onInit()
                processPendingMessages()
            }
        }
    }

    private fun processPendingMessages() {
        while (true) {
            val text = pendingMessages.poll() ?: break
            speak(text)
        }
    }

    private fun configureTTS() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                onUtteranceFinished(utteranceId)
            }

            override fun onError(utteranceId: String?) {
                onUtteranceFinished(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                onUtteranceFinished(utteranceId)
            }
        })
    }

    /**
     * Se llama cuando una utterance concreta termina (o se interrumpe).
     * Si era la última de la cola: libera el audio focus y dispara el callback de drenado.
     */
    private fun onUtteranceFinished(utteranceId: String?) {
        val cb = utteranceId?.let { utteranceCallbacks.remove(it) }
        val remaining = pendingUtterances.updateAndGet { (it - 1).coerceAtLeast(0) }
        if (remaining == 0) {
            _isSpeaking.value = false
            abandonAudioFocus()
            queueDrainedCallback.getAndSet(null)?.invoke()
        }
        cb?.invoke()
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        hasAudioFocus = audioManager.requestAudioFocus(audioFocusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            hasAudioFocus = false
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            pendingMessages.add(text)
            return
        }

        val utteranceId = registerUtterance(onComplete)

        // Process text for more natural speech
        val processedText = processTextForSpeech(text)

        tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Encola texto sin cortar lo que ya se está hablando (usado por el TTS en streaming).
     * Si se pasa [onComplete], se invoca justo cuando este fragmento termina de hablarse.
     */
    fun speakQueued(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) {
            pendingMessages.add(text)
            return
        }

        val utteranceId = registerUtterance(onComplete)
        val processedText = processTextForSpeech(text)

        tts?.speak(processedText, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    /**
     * Ejecuta [action] cuando la cola de habla termina de drenar por completo
     * (o inmediatamente si no queda nada pendiente de hablar).
     */
    fun whenQueueDrained(action: () -> Unit) {
        if (pendingUtterances.get() == 0 && tts?.isSpeaking != true) {
            action()
        } else {
            queueDrainedCallback.set(action)
        }
    }

    private fun registerUtterance(onComplete: (() -> Unit)?): String {
        val utteranceId = UUID.randomUUID().toString()
        if (onComplete != null) {
            utteranceCallbacks[utteranceId] = onComplete
        }
        pendingUtterances.incrementAndGet()
        requestAudioFocus()
        return utteranceId
    }

    private fun processTextForSpeech(text: String): String {
        // Remove code blocks (content between ``` )
        val codeBlockRegex = Regex("```[\\s\\S]*?```")
        val textWithoutCode = text.replace(codeBlockRegex, " [Código omitido para lectura] ")

        return textWithoutCode
            // Add pauses at natural points
            .replace("...", ", , ,")
            .replace(". ", ". , ")
            .replace("? ", "? , ")
            .replace("! ", "! , ")
            // Handle common abbreviations
            .replace("etc.", "etcétera")
            .replace("Sr.", "señor")
            .replace("Sra.", "señora")
            .replace("Dr.", "doctor")
            // Clean up emojis (TTS doesn't handle them well)
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
    }

    fun stop() {
        // Limpiar el estado ANTES de parar el motor para que onStop no dispare callbacks viejos
        pendingMessages.clear()
        utteranceCallbacks.clear()
        queueDrainedCallback.set(null)
        pendingUtterances.set(0)
        abandonAudioFocus()
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingMessages.clear()
        utteranceCallbacks.clear()
        queueDrainedCallback.set(null)
        pendingUtterances.set(0)
        abandonAudioFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }

    fun isAvailable(): Boolean = isInitialized

    fun getAvailableVoices(): List<Voice> {
        return tts?.voices?.filter { it.locale.language == "es" }?.toList() ?: emptyList()
    }

    fun setVoice(voiceName: String) {
        val voice = tts?.voices?.find { it.name == voiceName }
        voice?.let {
            tts?.voice = it
            // Keep natural values
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
        }
    }
}
