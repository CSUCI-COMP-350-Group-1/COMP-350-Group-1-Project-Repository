package com.example.cicompanion.social

// MESSAGING : one row in the messages list
data class ConversationSummary(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L
)