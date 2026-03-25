package com.example.googlecalendarviewer.model

data class EventsPage(
    val items: List<CalendarEvent>,
    val nextSyncToken: String?
)