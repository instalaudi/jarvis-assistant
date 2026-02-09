package com.jarvis.assistant.service

import android.app.Notification
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.service.notification.NotificationListenerService as AndroidNotificationListenerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NotificationInfo(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class NotificationListenerService : AndroidNotificationListenerService() {

    companion object {
        private val _notifications = MutableSharedFlow<NotificationInfo>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
        val notifications = _notifications.asSharedFlow()

        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b"  // WhatsApp Business
        )

        var isEnabled = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isEnabled = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Only process WhatsApp notifications
        if (sbn.packageName !in WHATSAPP_PACKAGES) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "WhatsApp"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Skip empty notifications (but allow empty title if text exists)
        if (text.isEmpty()) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val info = NotificationInfo(
            packageName = sbn.packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        // Log for debugging
        android.util.Log.d("NotificationService", "Detectado WhatsApp de $title: $text")

        // Emit notification to be read by TTS
        _notifications.tryEmit(info)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for our use case
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isEnabled = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isEnabled = false
    }
}
