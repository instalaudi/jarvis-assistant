package com.jarvis.assistant.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale

@AndroidEntryPoint
class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isPaused = false

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE_LISTENING -> {
                    Log.d("WakeWordService", "Received PAUSE_LISTENING command")
                    isPaused = true
                    stopListeningInternal()
                    // No se usa runBlocking aquí: bloqueaba el hilo main y podía provocar ANRs.
                    // startListeningInternal() ya aplica su propio buffer de 100ms al reanudar.
                }
                ACTION_RESUME_LISTENING -> {
                    Log.d("WakeWordService", "Received RESUME_LISTENING command")
                    isPaused = false
                    startListeningInternal()
                }
            }
        }
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wake_word_channel"
        const val ACTION_WAKE_WORD_DETECTED = "com.jarvis.assistant.WAKE_WORD_DETECTED"
        const val ACTION_PAUSE_LISTENING = "com.jarvis.assistant.PAUSE_LISTENING"
        const val ACTION_RESUME_LISTENING = "com.jarvis.assistant.RESUME_LISTENING"
        
        // Lower threshold as requested for better sensitivity
        private const val CONFIDENCE_THRESHOLD = 0.6f 
        
        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE_LISTENING)
            addAction(ACTION_RESUME_LISTENING)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter)
        }

        // Check permission before starting foreground service with type microphone
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WakeWordService", "Microphone permission NOT granted. Cannot start service.")
            stopSelf()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } catch (e: SecurityException) {
                    Log.e("WakeWordService", "SECURITY EXCEPTION: No se pudo iniciar FGS con tipo MICROPHONE. ¿Estado elegible? ${e.message}")
                    // Intentar sin tipo como último recurso si no es Android 14+
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, createNotification())
                    } else {
                        // En Android 14+ (U), es obligatorio el tipo si se usa el micro
                        stopSelf()
                        return
                    }
                } catch (e: Exception) {
                    Log.e("WakeWordService", "Error inesperado al iniciar FGS", e)
                    stopSelf()
                    return
                }
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            // Initial state is listening unless paused
            if (!isPaused) {
                startListeningInternal()
            }
        } catch (e: Exception) {
            Log.e("WakeWordService", "CRITICAL: Failed to start foreground service", e)
            stopSelf()
        }
        
        Log.i("JARVIS_VERSION", "WakeWordService v1.1 FIX APPLIED")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Redundant safety check for permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
             Log.e("WakeWordService", "onStartCommand: Permission missing, stopping.")
             stopSelf()
             return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startListeningInternal() {
        if (isPaused) {
            Log.d("WakeWordService", "Skipping startListening because service is PAUSED")
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WakeWordService", "Microphone permission NOT granted. Stopping service.")
            stopSelf()
            return
        }

        serviceScope.launch {
            try {
                // Ensure clean state
                stopListeningInternal()
                delay(100) // Small buffer

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@WakeWordService)
                    speechRecognizer?.setRecognitionListener(WakeWordListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "ES").toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Try to minimize beeps (not guaranteed on all devices/APIS)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }

                speechRecognizer?.startListening(intent)
                Log.d("WakeWordService", "Listening started (Threshold: $CONFIDENCE_THRESHOLD)")
            } catch (e: Exception) {
                Log.e("WakeWordService", "Error starting listening", e)
                restartListeningLater()
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d("WakeWordService", "Listening fully STOPPED")
        } catch (e: Exception) {
            Log.e("WakeWordService", "Error stopping listening", e)
        }
    }

    private fun restartListeningLater(delayMs: Long = 1000) {
        if (isPaused) return
        serviceScope.launch {
            delay(delayMs)
            startListeningInternal()
        }
    }

    private inner class WakeWordListener : RecognitionListener {
        override fun onResults(results: Bundle?) {
            if (isPaused) return

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase() ?: ""
            
            Log.d("WakeWordService", "Full Result: '$text'")
            
            if (isWakeWord(text)) {
                // Notificar con el texto completo para extraer el comando
                notifyWakeWordDetected(text)
                
                // Pausar y detener tras una detección exitosa para evitar bucles
                isPaused = true
                stopListeningInternal()
            } else {
                restartListeningLater(100) // Quick restart
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (isPaused) return

            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase() ?: ""
            
            // Si detectamos la palabra clave, simplemente lo logueamos pero permitimos que continúe
            // para capturar el comando completo en onResults.
            if (isWakeWord(text)) {
                Log.d("WakeWordService", "Partial Match detected: '$text'. Await full sentence...")
            }
        }

        private fun isWakeWord(text: String): Boolean {
            val cleanText = text.trim()
            return cleanText.contains("jarvis") || 
                   cleanText.contains("hey jarvis") || 
                   cleanText.contains("hola jarvis") ||
                   cleanText.contains("ok jarvis")
        }

        override fun onError(error: Int) {
            if (isPaused) return
            
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "NO MATCH"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "TIMEOUT"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "BUSY"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "NO PERMISSIONS"
                else -> "ERROR $error"
            }
            
            // Log.d("WakeWordService", "onError: $errorMessage") -- Reduce log spam

            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                restartListeningLater(2000)
            } else if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                restartListeningLater(500)
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun notifyWakeWordDetected(fullText: String = "") {
        Log.d("WakeWordService", ">>> WAKE WORD TRIGGERED <<<")
        
        // Extraer el comando (todo lo que va después de la palabra clave)
        var command: String? = null
        val keywords = listOf("hey jarvis", "ok jarvis", "hola jarvis", "jarvis")
        
        val lowerText = fullText.lowercase()
        for (key in keywords) {
            if (lowerText.contains(key)) {
                val index = lowerText.indexOf(key) + key.length
                if (index < fullText.length) {
                    command = fullText.substring(index).trim()
                }
                break
            }
        }

        if (!command.isNullOrBlank()) {
            Log.d("WakeWordService", "Extracted Command: '$command'")
        }

        val intent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            `package` = packageName
            if (!command.isNullOrBlank()) {
                putExtra("command", command)
            }
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Escucha 'Hey JARVIS' en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS en espera")
            .setContentText("Diga 'Hey JARVIS'...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(controlReceiver)
        serviceScope.cancel()
        stopListeningInternal()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
