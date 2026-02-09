package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControlService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Play or pause media
     */
    fun playPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    /**
     * Play media
     */
    fun play() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    /**
     * Pause media
     */
    fun pause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    /**
     * Skip to next track
     */
    fun next() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    /**
     * Go to previous track
     */
    fun previous() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /**
     * Stop media
     */
    fun stop() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
    }

    /**
     * Increase volume
     */
    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Decrease volume
     */
    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Set volume to specific level (0-100)
     */
    fun setVolume(percent: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (maxVolume * percent.coerceIn(0, 100)) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Mute/unmute media
     */
    fun toggleMute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_TOGGLE_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    /**
     * Check if music is currently playing
     */
    fun isMusicPlaying(): Boolean {
        return audioManager.isMusicActive
    }

    /**
     * Get current volume level (0-100)
     */
    fun getCurrentVolume(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100) / max else 0
    }

    /**
     * Open default music player
     */
    fun openMusicPlayer() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No music app found
        }
    }

    /**
     * Search and play music/artist
     */
    fun searchAndPlay(query: String) {
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(android.app.SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open music player
            openMusicPlayer()
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}
