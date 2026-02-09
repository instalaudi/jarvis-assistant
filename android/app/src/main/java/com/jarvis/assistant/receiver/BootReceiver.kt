package com.jarvis.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.MainActivity

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.miui.permcenter.autostart") {
            
            // Start the app on boot (optional - can be disabled in settings)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Only launch if autostart is enabled
            val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("autostart_enabled", false)) {
                context.startActivity(launchIntent)
            }
        }
    }
}
