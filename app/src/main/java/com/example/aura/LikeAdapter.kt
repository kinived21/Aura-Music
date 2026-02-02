package com.example.aura

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class LikeAdapter(val context: Context, val songList: List<Data>) :
    RecyclerView.Adapter<LikeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.likeMusicImage)
        val title: TextView = view.findViewById(R.id.likeSongName)
        val artist: TextView = view.findViewById(R.id.likeArtistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // This connects your "eachitem.xml" to the RecyclerView
        val view = LayoutInflater.from(context).inflate(R.layout.eachitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]
        holder.title.text = song.title
        holder.artist.text = song.artist.name
        Picasso.get().load(song.album.cover_xl).into(holder.image)

        // When a user clicks a liked song, open the Player!
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("songList", ArrayList(songList))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = songList.size
}