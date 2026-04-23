package com.example.cicompanion.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

object MessageNotificationSender {

    private const val TAG = "MessageNotifier"

    //keep aligned with your current emulator setup
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

    fun sendDirectMessageNotification(
        targetUserId: String,
        senderUserId: String,
        senderDisplayName: String,
        conversationId: String,
        messagePreview: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.sendDirectMessagePush(
                    mapOf(
                        "targetUserId" to targetUserId,
                        "senderUserId" to senderUserId,
                        "senderDisplayName" to senderDisplayName,
                        "conversationId" to conversationId,
                        "messagePreview" to messagePreview
                    )
                )
                Log.d(TAG, "Direct message push request sent to backend.")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to request direct message push.", exception)
            }
        }
    }
}