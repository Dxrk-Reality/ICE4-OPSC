package com.example.ice4

import com.google.firebase.Timestamp

/**
 * One post as stored in the Firestore "posts" collection.
 * Every field needs a default value so Firestore's toObject() can build it.
 */
data class Post(
    var username: String = "",
    var caption: String = "",
    var imageUrl: String = "",
    var userId: String = "",
    var timestamp: Timestamp? = null
)
