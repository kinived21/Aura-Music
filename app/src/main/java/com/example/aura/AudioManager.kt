package com.example.aura

import android.media.MediaPlayer

object AudioManager {
    // We use the player from PlayerActivity to ensure only ONE instance exists
    fun playSong(path: String?) {
        if (path.isNullOrEmpty()) return

        try {
            // Initialize if null
            if (PlayerActivity.mediaPlayer == null) {
                PlayerActivity.mediaPlayer = MediaPlayer()
            }

            PlayerActivity.mediaPlayer?.apply {
                reset() // This KILLS the previous song immediately
                setDataSource(path)
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSong() {
        PlayerActivity.mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.reset()
        }
    }

    fun isPlaying(): Boolean = PlayerActivity.mediaPlayer?.isPlaying ?: false
}