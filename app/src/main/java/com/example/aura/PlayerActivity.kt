package com.example.aura

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.aura.databinding.ActivityPlayerBinding
import com.squareup.picasso.Picasso

class PlayerActivity : BaseActivity() {
    companion object {
        var mediaPlayer: MediaPlayer? = null
        var staticSongList: ArrayList<Data>? = null
        var currentPosition: Int = 0
    }

    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler(Looper.getMainLooper())
    private var lastClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300
    private var isPreparing = false
    private lateinit var runnable: Runnable

    // 1. ADDED: Receiver to listen to Notification Buttons
    private val songChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "NEXT_SONG" -> {
                    if (!isPreparing && currentPosition < (staticSongList?.size ?: 0) - 1) {
                        playSong(currentPosition + 1)
                    }
                }
                "PREV_SONG" -> {
                    if (!isPreparing && currentPosition > 0) {
                        playSong(currentPosition - 1)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }


        val filter = IntentFilter().apply {
            addAction("NEXT_SONG")
            addAction("PREV_SONG")
        }

        ContextCompat.registerReceiver(
            this,
            songChangeReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED // This is the flag the error is asking for
        )


        val incomingList = intent.getSerializableExtra("songList") as? ArrayList<Data>
        val pos = intent.getIntExtra("position", 0)

        if (incomingList != null) {
            staticSongList = incomingList
            currentPosition = pos
        }

        if (staticSongList.isNullOrEmpty()) {
            showCustomToast("Error: Song list is empty!")
        } else {
            playSong(currentPosition)
        }

        // --- YOUR ORIGINAL LOGIC PRESERVED BELOW ---

        binding.btnLikeSong.setOnClickListener {
            val currentSong = staticSongList?.get(currentPosition) ?: return@setOnClickListener
            val sharedPrefs = getSharedPreferences("AuraLibrary", MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            val likedIds = sharedPrefs.getStringSet("liked_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val songId = currentSong.id.toString()

            if (likedIds.contains(songId)) {
                likedIds.remove(songId)
                binding.btnLikeSong.setImageResource(R.drawable.outlinelike)
                showCustomToast("Removed from Library")
            } else {
                likedIds.add(songId)
                binding.btnLikeSong.setImageResource(R.drawable.like)
                showCustomToast("Added to Library")
            }
            editor.putStringSet("liked_ids", likedIds).apply()
        }

        binding.btnPlayPause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.play)
                } else {
                    it.start()
                    binding.btnPlayPause.setImageResource(R.drawable.pause)
                }
            }
        }

        binding.btnForward.setOnClickListener {
            if (isPreparing) { showCustomToast("Slow down! Loading song..."); return@setOnClickListener }
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                if (currentPosition < (staticSongList?.size ?: 1) - 1) {
                    playSong(currentPosition + 1)
                }
            } else {
                mediaPlayer?.let { mp ->
                    val newPos = mp.currentPosition + 10000
                    mp.seekTo(if (newPos <= mp.duration) newPos else mp.duration)
                }
            }
            lastClickTime = currentTime
        }

        binding.btnBackward.setOnClickListener {
            if (isPreparing) { showCustomToast("Slow down! Loading song..."); return@setOnClickListener }
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                if (currentPosition > 0) {
                    playSong(currentPosition - 1)
                } else {
                    mediaPlayer?.seekTo(0)
                    showCustomToast("First Song!")
                }
            } else {
                mediaPlayer?.let { mp ->
                    try {
                        val newPos = mp.currentPosition - 10000
                        mp.seekTo(if (newPos >= 0) newPos else 0)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            lastClickTime = currentTime
        }

        binding.playerBackBtn.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
        }
    }

    private fun playSong(position: Int) {
        val listSize = staticSongList?.size ?: 0

        // 1. Safety Check
        if (listSize == 0 || position >= listSize) {
            isPreparing = false
            binding.btnPlayPause.setImageResource(R.drawable.play)
            showCustomToast("End of Playlist")
            return
        }

        // 2. FORCE RESET: Stop the old timer and clear the "preparing" lock
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        isPreparing = false

        currentPosition = position
        val currentSong = staticSongList!![currentPosition]

        // Update UI
        binding.playerTitle.text = currentSong.title
        binding.playerArtist.text = currentSong.artist.name
        Picasso.get().load(currentSong.album.cover_xl).into(binding.playerImage)

        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()

            mediaPlayer?.apply {
                stop()
                reset() // Put the player back to IDLE state

                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())

                setDataSource(currentSong.preview)

                // Mark as preparing BEFORE calling prepareAsync
                isPreparing = true
                prepareAsync()

                setOnPreparedListener {
                    isPreparing = false // Success! Unlock now
                    it.start()

                    // Update Service Notification
                    val serviceIntent = Intent(this@PlayerActivity, MusicService::class.java).apply {
                        putExtra("title", currentSong.title)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
                    else startService(serviceIntent)

                    binding.btnPlayPause.setImageResource(R.drawable.pause)
                    setupSeekBar()
                }

                // --- THE AUTO-PLAY LOGIC ---
                setOnCompletionListener {
                    android.util.Log.d("AuraMusic", "Song Finished. Current Pos: $currentPosition")

                    isPreparing = false // Unlock immediately

                    val nextPos = currentPosition + 1
                    if (nextPos < (staticSongList?.size ?: 0)) {
                        // Give the system 300ms to clear the audio hardware
                        handler.postDelayed({
                            android.util.Log.d("AuraMusic", "Starting next song: $nextPos")
                            playSong(nextPos)
                        }, 300)
                    } else {
                        binding.btnPlayPause.setImageResource(R.drawable.play)
                        showCustomToast("Feature is Coming Autochange")
                    }
                }

                setOnErrorListener { mp, _, _ ->
                    isPreparing = false
                    mp.reset()
                    false
                }
            }
        } catch (e: Exception) {
            isPreparing = false
            android.util.Log.e("AuraMusic", "Error: ${e.message}")
        }
    }

    private fun setupSeekBar() {
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.let { mp ->
            binding.seekBar.max = mp.duration
            runnable = Runnable {
                if (mediaPlayer != null) {
                    binding.seekBar.progress = mp.currentPosition
                    handler.postDelayed(runnable, 1000)
                }
            }
            handler.post(runnable)
        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        try { unregisterReceiver(songChangeReceiver) } catch (e: Exception) {}
    }

    private fun showCustomToast(message: String) {
        val layout = layoutInflater.inflate(R.layout.layout_custom_toast, findViewById(R.id.custom_toast_container))
        layout.findViewById<TextView>(R.id.toast_text).text = message
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}