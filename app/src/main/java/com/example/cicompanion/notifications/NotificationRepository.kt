package com.example.cicompanion.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object NotificationRepository {

    private const val PREFS_NAME = "app_notifications"
    private const val KEY_NOTIFICATIONS = "notifications_json"
    private const val MAX_NOTIFICATIONS = 100

    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        _notifications.value = loadNotifications(context)
        isInitialized = true
    }

    fun addNotification(
        context: Context,
        title: String,
        body: String,
        type: String
    ) {
        initialize(context)

        val newItem = AppNotificationItem(
            id = "${type}_${System.currentTimeMillis()}",
            title = title,
            body = body,
            type = type
        )

        val updated = listOf(newItem) + _notifications.value
        _notifications.value = updated.take(MAX_NOTIFICATIONS)
        saveNotifications(context, _notifications.value)
    }

    fun markAllAsRead(context: Context) {
        initialize(context)

        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        saveNotifications(context, updated)
    }

    private fun saveNotifications(
        context: Context,
        notifications: List<AppNotificationItem>
    ) {
        val jsonArray = JSONArray()

        notifications.forEach { item ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("type", item.type)
                    put("createdAt", item.createdAt)
                    put("isRead", item.isRead)
                }
            )
        }

        preferences(context)
            .edit()
            .putString(KEY_NOTIFICATIONS, jsonArray.toString())
            .apply()
    }

    private fun loadNotifications(context: Context): List<AppNotificationItem> {
        val rawJson = preferences(context).getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val jsonArray = JSONArray(rawJson)
        val result = mutableListOf<AppNotificationItem>()

        for (index in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(index)
            result.add(
                AppNotificationItem(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    body = item.getString("body"),
                    type = item.getString("type"),
                    createdAt = item.getLong("createdAt"),
                    isRead = item.optBoolean("isRead", false)
                )
            )
        }

        return result
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}