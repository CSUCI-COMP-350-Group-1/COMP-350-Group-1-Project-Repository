package com.example.cicompanion.social

// Firestore message model
data class DirectMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val sentAt: Long = 0L,
    val type: String = "text", // "text", "location", "pin", "event_invite"
    val metadata: Map<String, String> = emptyMap()
)
