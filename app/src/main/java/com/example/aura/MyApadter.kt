package com.example.aura

import android.app.Activity
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class MyApadter (val context: Activity, val musicList: List<Data>) :
RecyclerView.Adapter<MyApadter.MyViewHolder>(){

    // Keep track of the currently playing player and position
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_music,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = musicList[position]
        holder.title.text = currentItem.title
        holder.artist.text = currentItem.artist.name
        Picasso.get().load(currentItem.album.cover).into(holder.image)

        // --- NEW: CLICK LOGIC FOR THE WHOLE ROW ---
        holder.itemView.setOnClickListener {
            // Stop any current preview playing before switching screens
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            val oldPos = currentPlayingPosition
            currentPlayingPosition = -1
            notifyItemChanged(oldPos)

            // Launch PlayerActivity with Data
            val intent = android.content.Intent(context, PlayerActivity::class.java)
            intent.putExtra("songTitle", currentItem.title)
            intent.putExtra("songArtist", currentItem.artist.name)
            intent.putExtra("songImage", currentItem.album.cover_xl) // XL for the big screen
            intent.putExtra("songLink", currentItem.preview)
            val songArrayList = ArrayList(musicList)
            intent.putExtra("songList", songArrayList)
            intent.putExtra("position", position)
            context.startActivity(intent)
        }


        // Use absoluteAdapterPosition to check the icon state
        if (currentPlayingPosition == holder.bindingAdapterPosition) {
            holder.play.setImageResource(R.drawable.pause)
        } else {
            holder.play.setImageResource(R.drawable.play)
        }

        holder.play.setOnClickListener {
            // 1. GET THE FRESH POSITION
            val actualPosition = holder.bindingAdapterPosition

            // FIX: Use the @ label to tell Kotlin where to return from
            if (actualPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            // 2. CHECK IF THIS SONG IS ALREADY PLAYING
            if (currentPlayingPosition == actualPosition && AudioManager.isPlaying()) {
                // ACTION: STOP
                AudioManager.stopSong()

                val oldPos = currentPlayingPosition
                currentPlayingPosition = -1

                // Refresh this row to show "Play" icon
                notifyItemChanged(actualPosition)
            } else {
                // ACTION: PLAY NEW SONG
                val oldPos = currentPlayingPosition
                currentPlayingPosition = actualPosition

                // Use the Global Manager (Stops previous audio automatically)
                AudioManager.playSong(currentItem.preview)

                // Only refresh oldPos if it was actually a valid position
                if (oldPos != -1) {
                    notifyItemChanged(oldPos)
                }
                notifyItemChanged(currentPlayingPosition)

                // 3. AUTO-RESET ICON WHEN SONG FINISHES
                PlayerActivity.mediaPlayer?.setOnCompletionListener {
                    val completedPos = actualPosition
                    currentPlayingPosition = -1
                    notifyItemChanged(completedPos)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList.size

    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val image : ImageView
        val title : TextView
        val artist : TextView
        val play : ImageView

        init{
            image = itemView.findViewById(R.id.ivAlbumArt)
            title = itemView.findViewById(R.id.tvSongTitle)
            artist = itemView.findViewById(R.id.tvArtistName)
            play = itemView.findViewById(R.id.ivPlayBtn)
        }
    }
}