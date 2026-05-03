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
    
    // Optional second time range (e.g. for labs)
    val hasSecondTimeRange: Boolean = false,
    val daysOfWeek2: List<Int> = emptyList(),
    val startTime2: String = "",
    val endTime2: String = "",
    val notes2: String = "",

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
            val events = mutableListOf<CalendarEvent>()
            val firstDate = LocalDate.parse(startDate)
            val lastDate = LocalDate.parse(endDate)

            if (lastDate.isBefore(firstDate)) return emptyList()

            // Process first time range
            if (daysOfWeek.isNotEmpty()) {
                val classStartTime = LocalTime.parse(startTime)
                val classEndTime = LocalTime.parse(endTime)
                if (classEndTime.isAfter(classStartTime)) {
                    val meetingDays = daysOfWeek.filter { it in 1..7 }.toSet()
                    var cursor = firstDate
                    while (!cursor.isAfter(lastDate)) {
                        if (cursor.dayOfWeek.value in meetingDays) {
                            val startDateTime = ZonedDateTime.of(cursor, classStartTime, zoneId)
                            val endDateTime = ZonedDateTime.of(cursor, classEndTime, zoneId)
                            val descriptionText = buildDescription(notes)
                            events += createEvent(cursor, startDateTime, endDateTime, descriptionText)
                        }
                        cursor = cursor.plusDays(1)
                    }
                }
            }

            // Process second time range
            if (hasSecondTimeRange && daysOfWeek2.isNotEmpty()) {
                val classStartTime2 = LocalTime.parse(startTime2)
                val classEndTime2 = LocalTime.parse(endTime2)
                if (classEndTime2.isAfter(classStartTime2)) {
                    val meetingDays2 = daysOfWeek2.filter { it in 1..7 }.toSet()
                    var cursor = firstDate
                    while (!cursor.isAfter(lastDate)) {
                        if (cursor.dayOfWeek.value in meetingDays2) {
                            val startDateTime = ZonedDateTime.of(cursor, classStartTime2, zoneId)
                            val endDateTime = ZonedDateTime.of(cursor, classEndTime2, zoneId)
                            val descriptionText = buildDescription(notes2, isSecondRange = true)
                            events += createEvent(cursor, startDateTime, endDateTime, descriptionText, isSecondRange = true, secondRangeNote = notes2)
                        }
                        cursor = cursor.plusDays(1)
                    }
                }
            }

            events
        }.getOrDefault(emptyList())
    }

    private fun buildDescription(customNotes: String, isSecondRange: Boolean = false): String {
        return buildString {
            if (isSecondRange) {
                append("Secondary Meeting / Lab\n")
            }
            append("Class Schedule")
            if (termLabel.isNotBlank()) {
                append("\nTerm: ").append(termLabel)
            }
            if (customNotes.isNotBlank()) {
                append("\nNotes: ").append(customNotes)
            }
        }
    }

    private fun createEvent(
        date: LocalDate,
        start: ZonedDateTime,
        end: ZonedDateTime,
        description: String,
        isSecondRange: Boolean = false,
        secondRangeNote: String = ""
    ): CalendarEvent {
        val suffix = if (isSecondRange) "-2" else ""
        val titleSuffix = if (isSecondRange) {
            if (secondRangeNote.isNotBlank()) " ($secondRangeNote)" else " (2nd Period)"
        } else ""
        
        return CalendarEvent(
            id = "$id-${date}$suffix",
            calendarId = "schedule",
            title = "$courseCode - $courseTitle$titleSuffix",
            description = description,
            location = location.ifBlank { null },
            htmlLink = null,
            start = start,
            endExclusive = end,
            isAllDay = false,
            isPinned = false
        )
    }

    fun meetingPatternLabel(): String {
        val pattern1 = formatDays(daysOfWeek)
        if (!hasSecondTimeRange || daysOfWeek2.isEmpty()) return pattern1
        val pattern2 = formatDays(daysOfWeek2)
        return "$pattern1 | 2nd: $pattern2"
    }

    private fun formatDays(days: List<Int>): String {
        return days.sorted().joinToString(" ") { dayNumber ->
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