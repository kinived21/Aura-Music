package com.example.aura

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.aura.databinding.ActivityAuraSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuraSignUpActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().getReference("Users")
    lateinit var binding: ActivityAuraSignUpBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuraSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showCustomToast("Please fill all fields")
                return@setOnClickListener
            }
            if (!binding.cbTerms.isChecked) {
                binding.cbTerms.buttonTintList = ColorStateList.valueOf(Color.RED)
                showCustomToast("Please accept the terms and conditions")
                return@setOnClickListener
            }

            // Create User in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userMap = mapOf(
                        "name" to name,
                        "email" to email,
                        "password" to password
                    )
                    // Store extra details in Realtime Database
                    userId?.let {
                        database.child(it).setValue(userMap).addOnSuccessListener {
                            showCustomToast("Account Created Successful")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    showCustomToast("Account Creation Failed")
                }
            }
        }
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, AuraSignInActivity::class.java))
            finish()
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