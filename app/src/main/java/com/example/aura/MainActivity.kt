package com.example.aura

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aura.databinding.ActivityMainBinding
import com.example.aura.databinding.ActivityMenuBinding
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMenuBinding
    lateinit var rvMusic: RecyclerView
    lateinit var myApadter: MyApadter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        // This makes the area behind the phone's back/home buttons transparent
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            // Get the current layout parameters of the miniPlayer
            val params = binding.miniPlayer.layoutParams as RelativeLayout.LayoutParams

            // Force the margin to be exactly the height of the navigation bar
            params.bottomMargin = navigationBarHeight
            binding.miniPlayer.layoutParams = params

            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 1. Access the Header View through the Navigation View
        val headerView = binding.menuNavigation.getHeaderView(0)
        val tvName = headerView.findViewById<android.widget.TextView>(R.id.headerName)
        val tvEmail = headerView.findViewById<android.widget.TextView>(R.id.headerEmail)
        val ivProfile = headerView.findViewById<android.widget.ImageView>(R.id.userImage)

       // 2. Get current user from Firebase Auth
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            // Set the email (comes directly from Auth)
            tvEmail.text = user.email
            // 3. Fetch the Name from Realtime Database
            val uid = user.uid
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")

            database.child(uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // "name" must match the key you used in AuraSignUpActivity
                    val name = snapshot.child("name").value.toString()
                    tvName.text = name
                    // FETCH SAVED AVATAR: Look for the saved avatar ID
                    val savedAvatar = snapshot.child("avatar").value.toString().toIntOrNull()
                    if (savedAvatar != null) {
                        ivProfile.setImageResource(savedAvatar)
                    }
                }
            }
            // CLICK LISTENER: When user clicks the profile image
            ivProfile.setOnClickListener {
                showAvatarSelectionSheet(ivProfile, uid)
            }
        }

        binding.mainActivity.ivMenu.setOnClickListener {
            binding.drawLayout.openDrawer(androidx.core.view.GravityCompat.END)
        }

        binding.menuNavigation.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navLogout -> {
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setTitle("Logout")
                    builder.setMessage("Are you sure you want to logout?")
                    builder.setIcon(R.drawable.logout) // Use your logout icon


                    builder.setPositiveButton("Yes") { dialog, _ ->

                        AudioManager.stopSong()
                        // Sign out from Firebase
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

                        val intent = android.content.Intent(this, AuraSignInActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        finish()
                        dialog.dismiss()
                    }

                    builder.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }

                    val alertDialog = builder.create()
                    alertDialog.show()
                }

                R.id.navSupport -> {
                    // Opens the Email app with a pre-filled subject
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@auraapp.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Support Request - Aura App")
                    }
                    startActivity(Intent.createChooser(intent, "Send Email via..."))
                }

                R.id.navContact -> {
                    // Opens the Phone Dialer with your number
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:+1234567890")
                    startActivity(intent)
                }

                R.id.navLike -> {
                    val intent = Intent(this, LikeActivity::class.java)
                    startActivity(intent)
                }
            }
            binding.drawLayout.closeDrawer(androidx.core.view.GravityCompat.END)
            true
        }


        rvMusic= binding.mainActivity.rvMusic


        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = retrofitBuilder.getData("eminem")

        retrofitData.enqueue(object : retrofit2.Callback<MyData?> { // Changed to MyData wrapper
            override fun onResponse(call: Call<MyData?>, response: Response<MyData?>) {
                val responseBody = response.body()
                val dataList = responseBody?.data // Extract the list of songs

                if (dataList != null) {

                    PlayerActivity.staticSongList = ArrayList(dataList)
                    // Initialize the Adapter with the context and the data list
                    myApadter = MyApadter(this@MainActivity, dataList)

                    // Connect Adapter to RecyclerView
                    rvMusic.adapter = myApadter

                    // Set Layout Manager (Crucial for the list to show)
                    rvMusic.layoutManager = LinearLayoutManager(this@MainActivity)
                }
            }

            override fun onFailure(call: Call<MyData?>, t: Throwable) {
                android.util.Log.d("API_ERROR", "Error: " + t.message)
            }
        })


    }

    private fun showAvatarSelectionSheet(headerIv: android.widget.ImageView, uid: String) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        bottomSheet.setContentView(view)

        // IDs of the 6 avatar ImageViews in your dialog_avatar_picker.xml
        val avatars = listOf(
            view.findViewById<android.widget.ImageView>(R.id.avatar1),
            view.findViewById<android.widget.ImageView>(R.id.avatar2),
            view.findViewById<android.widget.ImageView>(R.id.avatar3),
            view.findViewById<android.widget.ImageView>(R.id.avatar4),
            view.findViewById<android.widget.ImageView>(R.id.avatar5),
            view.findViewById<android.widget.ImageView>(R.id.avatar6)
        )

        // Map each view to a drawable resource
        val drawableResources = listOf(
            R.drawable.pro1, R.drawable.pro2, R.drawable.pro3,
            R.drawable.pro4, R.drawable.pro5, R.drawable.pro6
        )

        for (i in avatars.indices) {
            avatars[i].setOnClickListener {
                val selectedRes = drawableResources[i]

                // 1. Update the UI Header immediately
                headerIv.setImageResource(selectedRes)

                // 2. Save the choice to Firebase Realtime Database
                val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                database.child(uid).child("avatar").setValue(selectedRes)

                showCustomToast("Avatar Updated!")
                bottomSheet.dismiss()
            }
        }
        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        setupMiniPlayer()
    }

    private fun setupMiniPlayer() {
        val player = PlayerActivity.mediaPlayer
        val list = PlayerActivity.staticSongList
        val pos = PlayerActivity.currentPosition

        if (player != null && !list.isNullOrEmpty()) {
            // 1. COOL ANIMATION: Slide up and Fade in if it was hidden
            if (binding.miniPlayer.visibility == android.view.View.GONE) {
                binding.miniPlayer.visibility = android.view.View.VISIBLE
                binding.miniPlayer.alpha = 0f
                binding.miniPlayer.translationY = 50f
                binding.miniPlayer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .start()
            }

            val currentSong = list[pos]
            binding.miniSongTitle.text = currentSong.title
            binding.miniArtistName.text = currentSong.artist.name

            // 2. STYLISH TEXT: Enable Marquee (Scrolling text)
            binding.miniSongTitle.isSelected = true

            Picasso.get().load(currentSong.album.cover_small).into(binding.miniAlbumArt)

            // 3. SYNC ICON: Set correct Play/Pause icon
            binding.miniPlayPause.setImageResource(if (player.isPlaying) R.drawable.pause else R.drawable.play)

            // 4. NAVIGATION: Go to full player
            binding.miniPlayer.setOnClickListener {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("position", pos)
                startActivity(intent)
                // Slide transition
                overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
            }

            // 5. MINI CONTROLS: Toggle Play/Pause without leaving screen
            binding.miniPlayPause.setOnClickListener {
                if (player.isPlaying) {
                    player.pause()
                    binding.miniPlayPause.setImageResource(R.drawable.play)
                } else {
                    player.start()
                    binding.miniPlayPause.setImageResource(R.drawable.pause)
                }
            }
        } else {
            // Smoothly hide if no music is playing
            binding.miniPlayer.visibility = android.view.View.GONE
        }
    }

    private fun showCustomToast(message: String) {
        val layout = layoutInflater.inflate(R.layout.layout_custom_toast, findViewById(R.id.custom_toast_container))

        // Set the text
        val textView: TextView = layout.findViewById(R.id.toast_text)
        textView.text = message

        // Create and show the toast
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}