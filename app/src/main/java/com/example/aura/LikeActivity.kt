package com.example.aura

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.example.aura.databinding.ActivityLikeBinding

class LikeActivity : BaseActivity() {
    lateinit var binding: ActivityLikeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

    }
    override fun onResume() {
        super.onResume()
        loadLikedSongs()
    }

    private fun loadLikedSongs() {
        val sharedPrefs = getSharedPreferences("AuraLibrary", MODE_PRIVATE)
        val likedIds = sharedPrefs.getStringSet("liked_ids", emptySet()) ?: emptySet()

        // Filter the static list (which MainActivity has now filled)
        val favoriteSongs = PlayerActivity.staticSongList?.filter { song ->
            likedIds.contains(song.id.toString())
        } ?: emptyList()

        if (favoriteSongs.isEmpty()) {
            binding.emptyLibraryLayout.visibility = View.VISIBLE
            binding.rvLibrary.visibility = View.GONE
        } else {
            binding.emptyLibraryLayout.visibility = View.GONE
            binding.rvLibrary.visibility = View.VISIBLE
            binding.rvLibrary.layoutManager = GridLayoutManager(this, 2)
            binding.rvLibrary.adapter = LikeAdapter(this, favoriteSongs)
        }
    }
}