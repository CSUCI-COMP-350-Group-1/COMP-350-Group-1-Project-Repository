package com.example.sprint1homeui.calendar.model

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val htmlLink: String?,
    val start: ZonedDateTime,
    val endExclusive: ZonedDateTime,
    val isAllDay: Boolean
) {
    fun lastDateInclusive(): LocalDate {
        return if (isAllDay) {
            endExclusive.toLocalDate().minusDays(1)
        } else {
            endExclusive.minusNanos(1).toLocalDate()
        }
    }

    fun occursOn(date: LocalDate): Boolean {
        return !date.isBefore(start.toLocalDate()) && !date.isAfter(lastDateInclusive())
    }

    fun timeLabel(): String {
        if (isAllDay) return "All day"

        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return "${start.format(formatter)} - ${endExclusive.format(formatter)}"
    }
}