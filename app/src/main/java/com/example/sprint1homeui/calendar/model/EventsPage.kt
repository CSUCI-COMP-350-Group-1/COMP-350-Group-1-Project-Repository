package com.example.sprint1homeui.calendar.model

data class EventsPage(
    val items: List<CalendarEvent>,
    val nextSyncToken: String?
)
