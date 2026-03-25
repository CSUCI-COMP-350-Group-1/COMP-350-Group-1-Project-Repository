package com.example.sprint1homeui.model

data class EventsPage(
    val items: List<CalendarEvent>,
    val nextSyncToken: String?
)
