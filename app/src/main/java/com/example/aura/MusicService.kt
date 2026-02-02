package com.example.aura

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private val binder = MusicBinder()
    private val CHANNEL_ID = "aura_music_v5"
    private val NOTIFICATION_ID = 111
    private lateinit var mediaSession: MediaSessionCompat
    private var lastTitle: String = "Aura Music"

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updatePlaybackState()
            updateHandler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "AuraMusicSession")

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                PlayerActivity.mediaPlayer?.seekTo(pos.toInt())
                updatePlaybackState()
            }

            // Handle Next/Prev from Lock Screen/System UI
            override fun onSkipToNext() {
                sendBroadcast(Intent("NEXT_SONG"))
            }
            override fun onSkipToPrevious() {
                sendBroadcast(Intent("PREV_SONG"))
            }
            override fun onPlay() {
                PlayerActivity.mediaPlayer?.start()
                showNotification(lastTitle)
            }
            override fun onPause() {
                PlayerActivity.mediaPlayer?.pause()
                showNotification(lastTitle)
            }
        })

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songTitle = intent?.getStringExtra("title") ?: lastTitle
        val action = intent?.action

        when (action) {
            "PLAY_PAUSE" -> {
                if (PlayerActivity.mediaPlayer?.isPlaying == true) {
                    PlayerActivity.mediaPlayer?.pause()
                    updateHandler.removeCallbacks(updateRunnable)
                } else {
                    PlayerActivity.mediaPlayer?.start()
                    updateHandler.post(updateRunnable)
                }
            }
            "NEXT" -> {
                val intent = Intent("NEXT_SONG")
                intent.setPackage(packageName) // This tells Android to only send it to YOUR app
                sendBroadcast(intent)
            }
            "PREV" -> {
                val intent = Intent("PREV_SONG")
                intent.setPackage(packageName)
                sendBroadcast(intent)
            }
        }

        showNotification(songTitle)
        return START_NOT_STICKY
    }

    private fun updatePlaybackState() {
        val player = PlayerActivity.mediaPlayer ?: return

        // Check if the player is actually playing
        val state = if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE
            )
            // IMPORTANT: currentPosition and 1.0f speed are what make the seek bar move
            .setState(state, player.currentPosition.toLong(), 1.0f)

        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun showNotification(title: String) {
        lastTitle = title
        val player = PlayerActivity.mediaPlayer
        val duration = player?.duration?.toLong() ?: 0L

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Aura Music")
            // THIS LINE IS REQUIRED FOR THE SEEK BAR TO HAVE A MAX LENGTH
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()

        mediaSession.setMetadata(metadata)

        val notificationIntent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // RemoteIntents for buttons
        val prevPending = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply { action = "PREV" }, PendingIntent.FLAG_IMMUTABLE)
        val playPausePending = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply { action = "PLAY_PAUSE" }, PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).apply { action = "NEXT" }, PendingIntent.FLAG_IMMUTABLE)

        val icon = if (player?.isPlaying == true) R.drawable.pause else R.drawable.play

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(title)
            .setContentText("Aura Music")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .addAction(R.drawable.baseline_arrow_back_24, "Previous", prevPending)
            .addAction(icon, "Play/Pause", playPausePending)
            .addAction(R.drawable.outline_arrow_forward_24, "Next", nextPending)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(null, null)
            manager.createNotificationChannel(channel)
        }
    }

    // Inside your MusicService class
    override fun onTaskRemoved(rootIntent: Intent?) {
        // 1. Stop the music
        PlayerActivity.mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
        }

        // 2. Remove the notification and kill the service
        // STOP_FOREGROUND_REMOVE is the correct flag for API 12+
        stopForeground(STOP_FOREGROUND_REMOVE)

        // 3. Stop the service entirely
        stopSelf()

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        updateHandler.removeCallbacks(updateRunnable)

        // Remove notification on destroy
        stopForeground(STOP_FOREGROUND_REMOVE)

        mediaSession.isActive = false
        mediaSession.release()

        super.onDestroy()
    }
}