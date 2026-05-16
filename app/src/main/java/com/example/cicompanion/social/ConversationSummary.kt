package com.example.cicompanion.social

// MESSAGING : one row in the messages list
data class ConversationSummary(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val groupName: String = "",
    val groupNameUpdatedAt: Long = 0L,
    val groupNameUpdatedBy: String = "",
    val lastMessageText: String = "",
    val lastMessageType: String = "text",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L
){
    // GROUP MESSAGING:
    // keeps directmessage logic readable without another Firestore field
    val isGroup: Boolean
        get() = participantIds.size > 2
}