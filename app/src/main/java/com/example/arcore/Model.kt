package com.example.arcore


import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Model : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var uid: String? = null // The user's UID

    // Firestore instance
    private val db = Firebase.firestore

    init {
        // Retrieve the current user's UID when the ViewModel is created
        val currentUser = auth.currentUser
        uid = currentUser?.uid

        // If the user is not authenticated, handle it (e.g., force login)
        if (uid == null) {
            Log.e("GameViewModel", "User not authenticated")
            // Handle authentication here (e.g., redirect to login screen)
        }
    }

    val userID: String
        get() = uid.orEmpty()

    // Reference to the user's document in Firestore
    val userDoc: DocumentReference? = uid?.let { db.document("Users/$it/") }

}