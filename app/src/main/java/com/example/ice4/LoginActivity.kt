package com.example.ice4

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ice4.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already signed in from a previous session? Go straight to the feed.
        if (auth.currentUser != null) {
            goToFeed()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Enter your email and password")
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { goToFeed() }
                .addOnFailureListener { e ->
                    binding.btnLogin.isEnabled = true
                    toast(e.message ?: "Login failed")
                }
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToFeed() {
        startActivity(Intent(this, FeedActivity::class.java))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
