package com.example.ice4

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ice4.databinding.ActivityFeedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val adapter = PostAdapter()
    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Signed in as ${auth.currentUser?.displayName ?: auth.currentUser?.email}"

        binding.recyclerPosts.layoutManager = LinearLayoutManager(this)
        binding.recyclerPosts.adapter = adapter

        binding.btnNewPost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) return

        // Live listener: the feed updates automatically when anyone posts.
        postsListener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Couldn't load posts: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                adapter.submit(posts)
                binding.tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onStop() {
        super.onStop()
        postsListener?.remove() // stop listening when the screen isn't visible
    }
}
