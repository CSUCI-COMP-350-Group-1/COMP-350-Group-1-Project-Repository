package com.example.cicompanion.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    //pull the current FCM token and save it under the signed-in user.
    fun syncCurrentTokenForSignedInUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveToken(userId = userId, token = token)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch FCM token.", exception)
            }
    }

    //keep token persistence in one place.
    fun saveToken(userId: String, token: String) {
        tokenCollection(userId)
            .document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save FCM token.", exception)
            }
    }

    //Remove the current device token from the old user on sign out/account switch.
    fun removeCurrentTokenFromUser(userId: String) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                tokenCollection(userId)
                    .document(token)
                    .delete()
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to delete FCM token.", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch FCM token for deletion.", exception)
            }
    }

    private fun tokenCollection(userId: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("fcmTokens")
}