package com.example.sprint1homeui.calendar.data.repository

import com.example.sprint1homeui.calendar.model.CalendarEvent
import com.example.sprint1homeui.calendar.data.api.CalendarApiService
import java.time.YearMonth
import java.time.ZoneId

class CalendarRepository(
    private val api: CalendarApiService = CalendarApiService()
) {

    /** Loads all events from the subscribed ICS feed and sorts them by start time. */
    suspend fun loadEvents(sourceUrl: String): List<CalendarEvent> {
        return api.fetchEvents(sourceUrl).sortedBy { it.start }
    }
}