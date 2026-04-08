package com.example.cicompanion.social

/**
 * Represents a friend request stored in Firestore.
 */
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromDisplayName: String = "",
    val fromEmail: String = "",
    val fromPhotoUrl: String = "",
    val toDisplayName: String = "",
    val toEmail: String = "",
    val status: String = "pending",
    val sentAt: Long = System.currentTimeMillis()
)
