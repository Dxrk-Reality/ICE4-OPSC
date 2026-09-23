package com.example.ice4

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ice4.databinding.ActivityCreatePostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var selectedImage: Uri? = null

    // Firestore documents are limited to 1 MB, so images are shrunk
    // to at most 800px on the longest side before saving.
    private val maxImageSize = 800

    // Android's built-in photo picker: no storage permissions needed
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImage = uri
                binding.ivPreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnPost.setOnClickListener { savePost() }
    }

    private fun savePost() {
        val user = auth.currentUser
        if (user == null) {
            toast("You need to be signed in"); finish(); return
        }
        val caption = binding.etCaption.text.toString().trim()
        val imageUri = selectedImage

        if (imageUri == null) { toast("Pick an image first"); return }
        if (caption.isEmpty()) { toast("Write a caption"); return }

        setLoading(true)

        // Step 1: shrink the image and turn it into a data URL string
        val imageDataUrl = encodeImage(imageUri)
        if (imageDataUrl == null) {
            setLoading(false)
            toast("Couldn't read that image, try another one")
            return
        }

        // Step 2: save the post (username, caption, image) to Firestore
        val post = hashMapOf(
            "username" to (user.displayName ?: user.email ?: "Unknown"),
            "caption" to caption,
            "imageUrl" to imageDataUrl,
            "userId" to user.uid,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("posts").add(post)
            .addOnSuccessListener {
                toast("Posted!")
                finish() // back to the feed, which updates live
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Saving post failed: ${e.message}")
            }
    }

    /**
     * Loads the picked image at a reduced size, compresses it to JPEG,
     * and returns it as "data:image/jpeg;base64,..." so Glide can display it
     * exactly like a normal image URL.
     */
    private fun encodeImage(uri: Uri): String? {
        // First pass: read only the image dimensions
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Load a smaller version to save memory
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxImageSize ||
            bounds.outHeight / (sampleSize * 2) >= maxImageSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // Scale down to the max size if it's still too big
        val scale = maxImageSize.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt(),
                (decoded.height * scale).toInt(),
                true
            )
        } else decoded

        // Compress to JPEG and encode as Base64 text
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !loading
        binding.btnPickImage.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}