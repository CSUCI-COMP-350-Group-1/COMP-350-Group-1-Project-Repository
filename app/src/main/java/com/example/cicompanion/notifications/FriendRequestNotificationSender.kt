package com.example.cicompanion.firebase

import android.util.Log
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


object FriendRequestNotificationSender {

    private const val TAG = "FriendRequestNotifier"

    // Emulator:
    // private const val KTOR_BASE_URL = "http://10.0.2.2:8080/"
    //
    // Physical phone:
    // private const val KTOR_BASE_URL = "http://YOUR_COMPUTER_LAN_IP:8080/"
    private const val KTOR_BASE_URL = "http://192.168.1.146:8080/"


    //For push notif
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: FcmApi = Retrofit.Builder()
        .baseUrl(KTOR_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()

    fun syncCurrentUserFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    FirestoreManager.saveFcmToken(currentUser.uid, token)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch current FCM token.", exception)
            }
    }

    fun sendFriendRequestNotification(
        targetUserId: String,
        senderDisplayName: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.sendFriendRequestPush(
                    mapOf(
                        "targetUserId" to targetUserId,
                        "senderDisplayName" to senderDisplayName
                    )
                )
                Log.d(TAG, "Friend request push request sent to backend.")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to request friend request push.", exception)
            }
        }
    }
}