package com.example.cicompanion.social

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreManager {
    fun saveUserToFirestore(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        val userProfile = hashMapOf(
            "uid" to user.uid,
            "username" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "profileImageUrl" to (user.photoUrl?.toString() ?: ""),
            "lastSignInAt" to System.currentTimeMillis()
        )

        userRef.set(userProfile, SetOptions.merge())
            .addOnSuccessListener { /* Handle success */ }
            .addOnFailureListener { /* Handle error */ }
    }
}