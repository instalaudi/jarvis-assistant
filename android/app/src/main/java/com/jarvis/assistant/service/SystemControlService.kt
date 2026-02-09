package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.provider.ContactsContract
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemControlService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun toggleFlashlight(enable: Boolean): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList[0] // Usually back camera
            cameraManager.setTorchMode(cameraId, enable)
            if (enable) "Linterna encendida" else "Linterna apagada"
        } catch (e: Exception) {
            "Error al controlar linterna: ${e.message}"
        }
    }

    fun setBrightness(level: Int): String {
        if (!Settings.System.canWrite(context)) {
            return "Permiso denegado. Se requiere permiso para modificar la configuración del sistema."
        }
        return try {
            // value is 0-255
            val brightnessValue = (level / 100f * 255).toInt().coerceIn(0, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
            "Brillo ajustado al $level%"
        } catch (e: Exception) {
            "Error al ajustar brillo: ${e.message}"
        }
    }

    fun setVolume(level: Int): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val volumeValue = (level / 100f * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                volumeValue,
                android.media.AudioManager.FLAG_SHOW_UI
            )
            "Volumen multimedia ajustado al $level%"
        } catch (e: Exception) {
            "Error al ajustar volumen: ${e.message}"
        }
    }

    private val aliases = mapOf(
        "musica" to listOf("spotify", "youtube music", "apple music", "amazon music"),
        "music" to listOf("spotify", "youtube music", "apple music"),
        "mapas" to listOf("maps", "google maps", "waze"),
        "maps" to listOf("maps", "google maps", "waze"),
        "insta" to listOf("instagram"),
        "face" to listOf("facebook"),
        "whats" to listOf("whatsapp")
    )

    private fun findPackage(appName: String): android.content.pm.PackageInfo? {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)
        val query = appName.trim().lowercase()

        fun score(label: String, pkg: String): Int {
            val l = label.lowercase()
            val p = pkg.lowercase()
            
            if (l == query) return 100
            if (p == query) return 95
            
            val aliasTargets = aliases[query]
            if (aliasTargets != null) {
                 if (aliasTargets.any { l.contains(it) }) return 90
            }

            if (l.startsWith(query)) return 80
            if (l.contains(query)) return 70
            
            return 0
        }

        return packages
            .asSequence()
            .map { 
                val appInfo = it.applicationInfo
                val label = if (appInfo != null) packageManager.getApplicationLabel(appInfo).toString() else it.packageName
                val score = score(label, it.packageName)
                Triple(it, label, score)
            }
            .filter { it.third > 0 }
            .maxByOrNull { it.third }
            ?.first
    }

    fun openApp(appName: String): String {
        val pkgInfo = findPackage(appName)
        val packageManager = context.packageManager

        return if (pkgInfo != null) {
            val intent = packageManager.getLaunchIntentForPackage(pkgInfo.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val appInfo = pkgInfo.applicationInfo
                val label = if (appInfo != null) packageManager.getApplicationLabel(appInfo).toString() else pkgInfo.packageName
                "Abriendo $label"
            } else {
                "No se pudo iniciar ${pkgInfo.packageName}"
            }
        } else {
            "Aplicación '$appName' no encontrada"
        }
    }

    fun playMusic(query: String, preferredApp: String? = null): String {
        val appToUse = preferredApp ?: "spotify"
        val pkgInfo = findPackage(appToUse)
        
        if (pkgInfo == null) {
            return "No encontré la aplicación de música '$appToUse'."
        }

        return try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                setPackage(pkgInfo.packageName)
                putExtra(android.app.SearchManager.QUERY, query)
                
                // Parámetros críticos para forzar la reproducción automática
                putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra("android.intent.extra.focus", "vnd.android.cursor.item/*")
                
                // Añadir artista/título como pista adicional para el buscador interno
                putExtra(android.provider.MediaStore.EXTRA_MEDIA_ARTIST, query)
                putExtra(android.provider.MediaStore.EXTRA_MEDIA_TITLE, query)
                
                // Flags para asegurar que la app tome el foco correctamente
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            android.util.Log.d("SystemControlService", "Enviando comando de reproducción: $query en ${pkgInfo.packageName}")
            context.startActivity(intent)
            
            val appInfo = pkgInfo.applicationInfo
            val label = if (appInfo != null) context.packageManager.getApplicationLabel(appInfo).toString() else pkgInfo.packageName
            "Reproduciendo '$query' en $label"
        } catch (e: Exception) {
            // Fallback: Just open the app if search fails
            openApp(appToUse)
            "Abriendo aplicación de música (no pude iniciar la reproducción directa): ${e.message}"
        }
    }

    private fun resolveContactNumber(name: String): String? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val cr = context.contentResolver
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        // Case-insensitive flexible search
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")
        
        try {
            cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun sendWhatsApp(phoneNumber: String, message: String): String {
        var finalNumber = phoneNumber
        var contactName = ""

        // Check if input is likely a name (contains letters)
        if (phoneNumber.any { it.isLetter() }) {
            val resolved = resolveContactNumber(phoneNumber)
            if (resolved != null) {
                // Remove everything except digits
                finalNumber = resolved.replace(Regex("[^0-9]"), "")
                contactName = " ($phoneNumber)" 
            } else {
                return "No encontré ningún contacto con el nombre '$phoneNumber' en tu agenda. Asegúrate de darme permisos de Contactos."
            }
        } else {
            // It's a raw number, clean it just in case
             finalNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        }

        return try {
            val url = "https://api.whatsapp.com/send?phone=$finalNumber&text=${java.net.URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Abriendo WhatsApp para enviar mensaje a $finalNumber$contactName"
        } catch (e: Exception) {
            "Error al abrir WhatsApp: ${e.message}"
        }
    }
}
