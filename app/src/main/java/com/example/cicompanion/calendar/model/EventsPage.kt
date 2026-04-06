package com.example.cicompanion.calendar.model

data class EventsPage(
    val items: List<CalendarEvent>,
    val nextSyncToken: String?
)
