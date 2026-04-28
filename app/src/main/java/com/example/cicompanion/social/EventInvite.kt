package com.example.cicompanion.social

/**
 * Represents an invitation to a calendar event.
 */
data class EventInvite(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromDisplayName: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventDescription: String? = "",
    val eventLocation: String? = "",
    val eventStart: String = "", // ISO_ZONED_DATE_TIME
    val eventEnd: String = "",   // ISO_ZONED_DATE_TIME
    val isAllDay: Boolean = false,
    val isPinnedByLeader: Boolean = false,
    val status: String = "pending", // pending, accepted, declined
    val sentAt: Long = System.currentTimeMillis()
)
