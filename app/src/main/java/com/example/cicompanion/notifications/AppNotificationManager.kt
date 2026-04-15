package com.example.cicompanion.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cicompanion.MainActivity
import com.example.cicompanion.social.FriendRequest
import com.example.cicompanion.notifications.NotificationRepository

object AppNotificationManager {

    //Single channel for friend request notifications
    const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_channel"

    private const val FRIEND_REQUEST_CHANNEL_NAME = "Friend Requests"
    private const val FRIEND_REQUEST_CHANNEL_DESCRIPTION =
        "Notifications for incoming friend requests"

    //Calendar reminder notification channel
    const val CALENDAR_REMINDER_CHANNEL_ID = "calendar_reminder_channel"

    private const val CALENDAR_REMINDER_CHANNEL_NAME = "Calendar Reminders"
    private const val CALENDAR_REMINDER_CHANNEL_DESCRIPTION =
        "Notifications for upcoming calendar events"

    //Create Android notification channel once at app startup
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        //Friend request channel
        val friendRequestChannel = NotificationChannel(
            FRIEND_REQUEST_CHANNEL_ID,
            FRIEND_REQUEST_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = FRIEND_REQUEST_CHANNEL_DESCRIPTION
        }

        //Calendar reminder channel
        val calendarReminderChannel = NotificationChannel(
            CALENDAR_REMINDER_CHANNEL_ID,
            CALENDAR_REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CALENDAR_REMINDER_CHANNEL_DESCRIPTION
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        //Register both channels
        notificationManager.createNotificationChannel(friendRequestChannel)
        notificationManager.createNotificationChannel(calendarReminderChannel)
    }

    //Friend request specific wrapper
    /*fun showFriendRequestNotification(context: Context, request: FriendRequest) {
        val senderName = request.fromDisplayName.ifBlank { request.fromEmail }

        showSimpleNotification(
            context = context,
            notificationId = request.id.hashCode(),
            title = "New friend request",
            body = "$senderName sent you a friend request."
        )
    }*/
    fun showFriendRequestNotification(context: Context, request: FriendRequest) {
        val senderName = request.fromDisplayName.ifBlank { request.fromEmail }

        showNotification(
            context = context,
            channelId = FRIEND_REQUEST_CHANNEL_ID,
            notificationId = request.id.hashCode(),
            title = "New friend request",
            body = "$senderName sent you a friend request.",
            type = "Friend Request"
        )
    }

    // NEW: Wrapper used by CalendarReminderWorker.
    /*fun showCalendarReminderNotification(
        context: Context,
        title: String,
        location: String,
        timeLabel: String
    ) {
        val body = if (location.isBlank()) {
            timeLabel
        } else {
            "$timeLabel\n$location"
        }

        showNotification(
            context = context,
            channelId = CALENDAR_REMINDER_CHANNEL_ID,
            notificationId = ("calendar_" + title + timeLabel).hashCode(),
            title = "Event tomorrow",
            body = "$title\n$body"
        )
    }*/
    fun showCalendarReminderNotification(
        context: Context,
        title: String,
        location: String,
        timeLabel: String
    ) {
        val body = if (location.isBlank()) {
            timeLabel
        } else {
            "$timeLabel\n$location"
        }

        showNotification(
            context = context,
            channelId = CALENDAR_REMINDER_CHANNEL_ID,
            notificationId = ("calendar_" + title + timeLabel).hashCode(),
            title = "Event tomorrow",
            body = "$title\n$body",
            type = "Calendar Reminder"
        )
    }

    //Reusable notification function for local notifications and future FCM handling.
    /*fun showSimpleNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        showNotification(
            context = context,
            channelId = FRIEND_REQUEST_CHANNEL_ID,
            notificationId = notificationId,
            title = title,
            body = body
        )
    }*/
    fun showSimpleNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        showNotification(
            context = context,
            channelId = FRIEND_REQUEST_CHANNEL_ID,
            notificationId = notificationId,
            title = title,
            body = body,
            type = "General"
        )
    }
    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        type: String
    ) {
        //Save every posted notification so it also appears in NotificationScreen
        NotificationRepository.addNotification(
            context = context,
            title = title,
            body = body,
            type = type
        )

        if (!canPostNotifications(context)) return

        val contentIntent = buildMainActivityPendingIntent(
            context = context,
            requestCode = notificationId
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun buildMainActivityPendingIntent(
        context: Context,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    //Create the calendar reminder channel.
    private fun createCalendarReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CALENDAR_REMINDER_CHANNEL_ID,
            CALENDAR_REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CALENDAR_REMINDER_CHANNEL_DESCRIPTION
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}