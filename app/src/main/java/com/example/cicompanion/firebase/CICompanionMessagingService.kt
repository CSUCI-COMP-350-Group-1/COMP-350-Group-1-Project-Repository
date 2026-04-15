package com.example.cicompanion.firebase

import com.example.cicompanion.notifications.AppNotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CICompanionMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        //Save refreshed tokens automatically for future server-driven FCM
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FcmTokenManager.saveToken(userId = currentUserId, token = token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        //supports future FCM messages and manual console tests
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: return

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: return

        AppNotificationManager.showSimpleNotification(
            context = applicationContext,
            notificationId = (title + body).hashCode(),
            title = title,
            body = body
        )
    }
}