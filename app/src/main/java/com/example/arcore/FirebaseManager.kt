package com.example.arcore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirebaseManager {

    private val firestore = FirebaseFirestore.getInstance()

    fun saveAnchorId(anchorId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val anchorData = hashMapOf("anchorId" to anchorId, "timestamp" to System.currentTimeMillis())
        firestore.collection("anchors").add(anchorData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onError(error) }
    }

    fun fetchLatestAnchorId(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("anchors")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val anchorId = result.documents.firstOrNull()?.getString("anchorId")
                if (!anchorId.isNullOrEmpty()) {
                    onSuccess(anchorId)
                } else {
                    onError(Exception("No anchor found"))
                }
            }
            .addOnFailureListener { error -> onError(error) }
    }
}