package com.example.ice4

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ice4.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { register() }
        binding.tvGoLogin.setOnClickListener { finish() } // back to login
    }

    private fun register() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        when {
            username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                toast("Fill in all fields"); return
            }
            password.length < 6 -> {
                toast("Password must be at least 6 characters"); return
            }
            password != confirm -> {
                toast("Passwords don't match"); return
            }
        }

        binding.btnRegister.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // Store the username on the Firebase Auth profile as the display name.
                // Posts read it from here ("username from auth").
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()

                result.user?.updateProfile(profile)?.addOnCompleteListener {
                    val intent = Intent(this, FeedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                binding.btnRegister.isEnabled = true
                toast(e.message ?: "Registration failed")
            }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
