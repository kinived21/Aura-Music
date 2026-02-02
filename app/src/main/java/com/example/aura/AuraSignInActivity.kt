package com.example.aura

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.aura.databinding.ActivityAuraSignInBinding
import com.google.firebase.auth.FirebaseAuth

class AuraSignInActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var binding: ActivityAuraSignInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuraSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        AudioManager.stopSong()
        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE // Show loading
            binding.btnLogin.isEnabled = false // Disable button

            val email = binding.etEmailLogin.text.toString()
            val password = binding.etPasswordLogin.text.toString()

            // 1. Reset errors at the start of every click
            binding.etEmailLogin.error = null
            binding.etPasswordLogin.error = null

            // 2. Validation Checks
            if (email.isEmpty()) {
                binding.etEmailLogin.error = "Email is required"
                binding.etEmailLogin.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPasswordLogin.error = "Password is required"
                binding.etPasswordLogin.requestFocus()
                return@setOnClickListener
            }

            if(email.isNotEmpty() && password.isNotEmpty()){
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.GONE // Hide loading
                    binding.btnLogin.isEnabled = true // Re-enable button
                    if(task.isSuccessful){
                        showCustomToast("Login Successful")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    else {
                        // 4. Handle Specific Firebase Errors
                        val errorMessage= task.exception?.message ?: "Login Failed"
                        if (errorMessage.contains("email", ignoreCase = true)) {
                            binding.etEmailLogin.error = "No account found with this email"
                            binding.etEmailLogin.requestFocus()
                        } else if (errorMessage.contains("password", ignoreCase = true)) {
                            binding.etPasswordLogin.error = "Incorrect password"
                            binding.etPasswordLogin.requestFocus()
                        } else {
                            showCustomToast(errorMessage)
                        }
                    }
                }
            }
            else {
                showCustomToast("Login Failed")
            }

        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmailLogin.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmailLogin.error = "Enter your email to reset password"
                binding.etEmailLogin.requestFocus()
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showCustomToast("Password reset email sent")
                    } else {
                        showCustomToast("Failed to send password reset email")
                    }
                }
            }
        }


        binding.tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, AuraSignUpActivity::class.java))
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