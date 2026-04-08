package com.example.cicompanion.calendar.data.repository

import com.example.cicompanion.calendar.data.api.CalendarApiService
import com.example.cicompanion.calendar.model.CalendarEvent

class CalendarRepository(
    private val api: CalendarApiService = CalendarApiService()
) {

    /** Loads all events from the subscribed ICS feed and sorts them by start time. */
    suspend fun loadEvents(sourceUrl: String): List<CalendarEvent> {
        return api.fetchEvents(sourceUrl).sortedBy { it.start }
    }
}
