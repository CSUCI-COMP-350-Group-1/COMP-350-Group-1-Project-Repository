package com.example.cicompanion.calendar.data.repository

import com.example.cicompanion.calendar.model.CalendarSummary
import com.example.cicompanion.calendar.model.EventDraft
import com.example.cicompanion.calendar.model.EventsPage
import com.example.cicompanion.calendar.data.api.CalendarApiService
import java.time.YearMonth
import java.time.ZoneId

class CalendarRepository(
    private val api: CalendarApiService = CalendarApiService()
) {
    suspend fun loadCalendars(accessToken: String): List<CalendarSummary> {
        return api.listCalendars(accessToken)
            .sortedBy { it.title.lowercase() }
    }

    suspend fun loadEventsForWindow(
        accessToken: String,
        calendarId: String,
        visibleMonth: YearMonth
    ): EventsPage {
        val zone = ZoneId.systemDefault()
        val from = visibleMonth.minusMonths(1).atDay(1).atStartOfDay(zone)
        val until = visibleMonth.plusMonths(2).atDay(1).atStartOfDay(zone)

        return api.listEvents(
            accessToken = accessToken,
            calendarId = calendarId,
            from = from,
            until = until
        )
    }

    suspend fun createEvent(
        accessToken: String,
        calendarId: String,
        draft: EventDraft
    ) = api.insertEvent(
        accessToken = accessToken,
        calendarId = calendarId,
        draft = draft
    )

    fun selectInitialCalendar(calendars: List<CalendarSummary>): CalendarSummary {
        return calendars.firstOrNull { it.isPrimary }
            ?: calendars.firstOrNull()
            ?: CalendarSummary(
                id = "primary",
                title = "Primary",
                isPrimary = true
            )
    }
}
