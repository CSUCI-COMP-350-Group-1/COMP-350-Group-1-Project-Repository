package com.example.cicompanion.calendar.model

// EVENT NOTIFICATION
// Stores one user's opt-in preference for either:
// - a recurring class as a whole
// - or a single one-time event occurrence
data class EventNotificationPreference(
    val preferenceId: String = "",
    val targetType: String = "",   // "recurring_class" or "single_event"
    val targetId: String = "",     // selectedClass.id or event.id
    val title: String = "",
    val calendarId: String = "",
    val start: String = "",
    val enabled: Boolean = true,
    val updatedAt: Long = 0L
)