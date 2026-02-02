package com.example.aura


import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

// This is an "Extension Function" that any Activity can now use
fun Activity.showAuraToast(message: String) {
    val inflater = layoutInflater
    val layout = inflater.inflate(R.layout.layout_custom_toast, null)

    // Set the text
    val text: TextView = layout.findViewById(R.id.toast_text)
    text.text = message

    // Set the logo (Optional: you can change the image based on the message)
    val image: ImageView = layout.findViewById(R.id.toast_icon)
    image.setImageResource(R.mipmap.ic_launcher)

    with (Toast(applicationContext)) {
        setGravity(Gravity.BOTTOM, 0, 100) // Position it near the bottom
        duration = Toast.LENGTH_SHORT
        view = layout
        show()
    }
}