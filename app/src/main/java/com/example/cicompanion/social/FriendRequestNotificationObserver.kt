package com.example.cicompanion.social

import android.content.Context
import android.util.Log
import com.example.cicompanion.notifications.AppNotificationManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration

class FriendRequestNotificationObserver(
    private val context: Context
) {

    private var registration: ListenerRegistration? = null
    private var hasConsumedInitialSnapshot = false

    // Start listening for newly added pending friend requests
    fun start(currentUserId: String) {
        stop()

        //The first snapshot contains existing docs already in Firestore
        // skip first load so only brand new friend requests show a notification
        hasConsumedInitialSnapshot = false

        registration = SocialRepository.observePendingIncomingFriendRequests(
            currentUserId = currentUserId,
            onSnapshot = { snapshot ->
                if (!hasConsumedInitialSnapshot) {
                    hasConsumedInitialSnapshot = true
                    return@observePendingIncomingFriendRequests
                }

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .mapNotNull { it.document.toObject(FriendRequest::class.java) }
                    .forEach { request ->
                        AppNotificationManager.showFriendRequestNotification(
                            context = context,
                            request = request
                        )
                    }
            },
            onError = { message ->
                Log.e(TAG, message)
            }
        )
    }

    fun stop() {
        registration?.remove()
        registration = null
        hasConsumedInitialSnapshot = false
    }

    private companion object {
        const val TAG = "FriendRequestObserver"
    }
}