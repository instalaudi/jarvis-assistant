package com.jarvis.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JarvisApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "jarvis_main_channel"
        const val NOTIFICATION_CHANNEL_NAME = "J.A.R.V.I.S. Notifications"
        const val VOICE_CHANNEL_ID = "jarvis_voice_channel"
        const val VOICE_CHANNEL_NAME = "Voice Recognition"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Main notification channel
        val mainChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones principales de J.A.R.V.I.S."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 100, 250)
        }

        // Voice service channel
        val voiceChannel = NotificationChannel(
            VOICE_CHANNEL_ID,
            VOICE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servicio de reconocimiento de voz activo"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(listOf(mainChannel, voiceChannel))
    }
}
