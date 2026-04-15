package com.example.cicompanion.notifications

data class AppNotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)