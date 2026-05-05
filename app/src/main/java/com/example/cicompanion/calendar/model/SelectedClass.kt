package com.example.cicompanion.calendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime


// Stores one user-selected class schedule entry and converts it to calendar events
data class SelectedClass(
    val id: String = "",
    val majorCode: String = "",
    val majorName: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val typicallyOffered: String = "",
    val daysOfWeek: List<Int> = emptyList(), // 1 = Monday ... 7 = Sunday
    val startTime: String = "",              // "09:00"
    val endTime: String = "",                // "10:15"
    val startDate: String = "",              // "2026-08-24"
    val endDate: String = "",                // "2026-12-18"
    val location: String = "",
    val notes: String = "",
    val termLabel: String = "",
    val colorArgb: Int = 0xFFEF3347.toInt(),

    // Reserved for future reminder implementation
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toCalendarEvents(zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        return runCatching {
            if (daysOfWeek.isEmpty()) return emptyList()

            val firstDate = LocalDate.parse(startDate)
            val lastDate = LocalDate.parse(endDate)
            val classStartTime = LocalTime.parse(startTime)
            val classEndTime = LocalTime.parse(endTime)

            if (lastDate.isBefore(firstDate)) return emptyList()
            if (!classEndTime.isAfter(classStartTime)) return emptyList()

            val meetingDays = daysOfWeek.filter { it in 1..7 }.toSet()
            if (meetingDays.isEmpty()) return emptyList()

            val events = mutableListOf<CalendarEvent>()
            var cursor = firstDate

            while (!cursor.isAfter(lastDate)) {
                if (cursor.dayOfWeek.value in meetingDays) {
                    val startDateTime = ZonedDateTime.of(cursor, classStartTime, zoneId)
                    val endDateTime = ZonedDateTime.of(cursor, classEndTime, zoneId)

                    val descriptionText = buildString {
                        append("Class Schedule")
                        if (termLabel.isNotBlank()) {
                            append("\nTerm: ").append(termLabel)
                        }
                        if (notes.isNotBlank()) {
                            append("\nNotes: ").append(notes)
                        }

                    }

                    events += CalendarEvent(
                        id = "$id-${cursor}",
                        calendarId = "schedule",
                        title = "$courseCode - $courseTitle",
                        description = descriptionText,
                        location = location.ifBlank { null },
                        htmlLink = null,
                        start = startDateTime,
                        endExclusive = endDateTime,
                        isAllDay = false,
                        isPinned = false
                    )
                }

                cursor = cursor.plusDays(1)
            }

            events
        }.getOrDefault(emptyList())
    }

    fun meetingPatternLabel(): String {
        return daysOfWeek
            .sorted()
            .joinToString(" ") { dayNumber ->
                when (dayNumber) {
                    1 -> "Mon"
                    2 -> "Tue"
                    3 -> "Wed"
                    4 -> "Thu"
                    5 -> "Fri"
                    6 -> "Sat"
                    7 -> "Sun"
                    else -> "?"
                }
            }
    }
}