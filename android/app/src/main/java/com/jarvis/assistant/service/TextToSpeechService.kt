package com.jarvis.assistant.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onCompleteCallback: (() -> Unit)? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val pendingMessages = java.util.LinkedList<String>()

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
        while (pendingMessages.isNotEmpty()) {
            val text = pendingMessages.poll()
            if (text != null) {
                speak(text)
            }
        }
    }

    private fun configureTTS() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback = null
            }
        })
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            pendingMessages.add(text)
            return
        }
        
        onCompleteCallback = onComplete

        // Process text for more natural speech
        val processedText = processTextForSpeech(text)

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun speakQueued(text: String) {
        if (!isInitialized) {
            pendingMessages.add(text)
            return
        }
        
        val processedText = processTextForSpeech(text)
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(processedText, TextToSpeech.QUEUE_ADD, null, utteranceId)
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
        pendingMessages.clear()
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingMessages.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
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
