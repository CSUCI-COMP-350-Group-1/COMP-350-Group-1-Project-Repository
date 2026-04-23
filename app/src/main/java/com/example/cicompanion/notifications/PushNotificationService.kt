package com.example.cicompanion.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cicompanion.MainActivity
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.ui.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // PUSH NOTIFICATIONS save refreshed token
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirestoreManager.saveFcmToken(currentUser.uid, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // MESSAGING allow routing + channel selection from FCM data
        val destinationRoute = message.data["destination_route"] ?: Routes.FRIENDS_AND_REQUESTS
        val channelId = message.data["channel_id"] ?: FRIEND_REQUEST_CHANNEL_ID

        showNotification(
            title = message.notification?.title ?: "New notification",
            body = message.notification?.body ?: "You have a new update.",
            destinationRoute = destinationRoute,
            channelId = channelId
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        destinationRoute: String,
        channelId: String
    ) {
        createNotificationChannelsIfNeeded()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // MESSAGING notification tap route is now dynamic
            putExtra(EXTRA_DESTINATION_ROUTE, destinationRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannelsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val friendRequestChannel = NotificationChannel(
            FRIEND_REQUEST_CHANNEL_ID,
            FRIEND_REQUEST_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Friend request notifications"
        }

        // MESSAGING new high-importance direct message channel
        val directMessageChannel = NotificationChannel(
            DIRECT_MESSAGE_CHANNEL_ID,
            DIRECT_MESSAGE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Direct message notifications"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(friendRequestChannel)
        manager.createNotificationChannel(directMessageChannel)
    }

    companion object {
        private const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_notifications"
        private const val FRIEND_REQUEST_CHANNEL_NAME = "Friend Requests"

        // MESSAGING CHANGE
        const val DIRECT_MESSAGE_CHANNEL_ID = "direct_message_notifications"
        private const val DIRECT_MESSAGE_CHANNEL_NAME = "Direct Messages"

        const val EXTRA_DESTINATION_ROUTE = "destination_route"
    }
}