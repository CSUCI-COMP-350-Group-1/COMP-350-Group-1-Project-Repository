package com.example.cicompanion.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

object EventInviteNotificationSender {

    private const val TAG = "EventInviteNotifier"

    // EVENT INVITE NOTIFICATION:
    // Keep this aligned with your other notification senders.
    //
    // Emulator:
    // private const val KTOR_BASE_URL = "http://10.0.2.2:8080/"
    //
    // Physical phone:
    // private const val KTOR_BASE_URL = "http://YOUR_COMPUTER_LAN_IP:8080/"
    private const val KTOR_BASE_URL = "http://10.0.2.2:8080/"

    private val api: FcmApi = Retrofit.Builder()
        .baseUrl(KTOR_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()

    // EVENT INVITE NOTIFICATION:
    // Requests the Ktor backend to send an FCM push to the invited user.
    fun sendEventInviteNotification(
        targetUserId: String,
        inviterDisplayName: String,
        eventTitle: String,
        eventId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.sendEventInvitePush(
                    mapOf(
                        "targetUserId" to targetUserId,
                        "inviterDisplayName" to inviterDisplayName,
                        "eventTitle" to eventTitle,
                        "eventId" to eventId
                    )
                )

                Log.d(TAG, "Event invite push request sent to backend.")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to request event invite push.", exception)
            }
        }
    }
}