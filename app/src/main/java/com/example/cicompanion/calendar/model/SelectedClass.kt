package com.example.cicompanion.calendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// SECOND SECTION NOTIFICATION FIX:
// Primary section event IDs stay as "$id-$date" for backward compatibility.
// Secondary section event IDs become "$id__secondary-$date" so notification opt-ins
// can distinguish the second section from the primary class section.
private const val SECONDARY_SECTION_SUFFIX = "__secondary"

// Stores one user-selected class schedule entry and converts it to calendar events.
data class SelectedClass(
    val id: String = "",
    val majorCode: String = "",
    val majorName: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val typicallyOffered: String = "",

    // Primary meeting section.
    val daysOfWeek: List<Int> = emptyList(), // 1 = Monday ... 7 = Sunday
    val startTime: String = "",              // "09:00"
    val endTime: String = "",                // "10:15"

    // SECOND SECTION NOTIFICATION FIX:
    // Optional second meeting section, such as a lab/discussion.
    val hasSecondTimeRange: Boolean = false,
    val daysOfWeek2: List<Int> = emptyList(),
    val startTime2: String = "",
    val endTime2: String = "",
    val location2: String = "",
    val notes2: String = "",

    val startDate: String = "",              // "2026-08-24"
    val endDate: String = "",                // "2026-12-18"
    val location: String = "",
    val notes: String = "",
    val termLabel: String = "",
    val colorArgb: Int = 0xFFEF3347.toInt(),

    // Reserved for future reminder implementation.
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toCalendarEvents(zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        return runCatching {
            val firstDate = LocalDate.parse(startDate)
            val lastDate = LocalDate.parse(endDate)

            if (lastDate.isBefore(firstDate)) return emptyList()

            val events = mutableListOf<CalendarEvent>()

            // SECOND SECTION NOTIFICATION FIX:
            // Build primary class section events using the original event id format.
            events += buildSectionEvents(
                sectionEventIdPrefix = id,
                sectionLabel = null,
                days = daysOfWeek,
                sectionStartTimeText = startTime,
                sectionEndTimeText = endTime,
                sectionLocation = location,
                sectionNotes = notes,
                firstDate = firstDate,
                lastDate = lastDate,
                zoneId = zoneId
            )

            // SECOND SECTION NOTIFICATION FIX:
            // Build secondary section events with a distinct id prefix.
            // This gives the second section its own notification preference target.
            if (hasSecondTimeRange) {
                events += buildSectionEvents(
                    sectionEventIdPrefix = "$id$SECONDARY_SECTION_SUFFIX",
                    sectionLabel = if (notes2.isNotBlank()) notes2 else "Second Meeting",
                    days = daysOfWeek2,
                    sectionStartTimeText = startTime2,
                    sectionEndTimeText = endTime2,
                    sectionLocation = location2,
                    sectionNotes = notes2,
                    firstDate = firstDate,
                    lastDate = lastDate,
                    zoneId = zoneId
                )
            }

            events
        }.getOrDefault(emptyList())
    }

    // SECOND SECTION NOTIFICATION FIX:
    // Keeps event-building logic in one place so primary and secondary sections behave the same.
    private fun buildSectionEvents(
        sectionEventIdPrefix: String,
        sectionLabel: String?,
        days: List<Int>,
        sectionStartTimeText: String,
        sectionEndTimeText: String,
        sectionLocation: String,
        sectionNotes: String,
        firstDate: LocalDate,
        lastDate: LocalDate,
        zoneId: ZoneId
    ): List<CalendarEvent> {
        if (days.isEmpty()) return emptyList()

        val sectionStartTime = runCatching { LocalTime.parse(sectionStartTimeText) }.getOrNull()
            ?: return emptyList()
        val sectionEndTime = runCatching { LocalTime.parse(sectionEndTimeText) }.getOrNull()
            ?: return emptyList()

        if (!sectionEndTime.isAfter(sectionStartTime)) return emptyList()

        val meetingDays = days.filter { it in 1..7 }.toSet()
        if (meetingDays.isEmpty()) return emptyList()

        val sectionEvents = mutableListOf<CalendarEvent>()
        var cursor = firstDate

        while (!cursor.isAfter(lastDate)) {
            if (cursor.dayOfWeek.value in meetingDays) {
                val startDateTime = ZonedDateTime.of(cursor, sectionStartTime, zoneId)
                val endDateTime = ZonedDateTime.of(cursor, sectionEndTime, zoneId)

                val descriptionText = buildString {
                    append("Class Schedule")

                    if (!sectionLabel.isNullOrBlank()) {
                        append("\nSection: ").append(sectionLabel)
                    }

                    if (termLabel.isNotBlank()) {
                        append("\nTerm: ").append(termLabel)
                    }

                    if (sectionNotes.isNotBlank()) {
                        append("\nNotes: ").append(sectionNotes)
                    }
                }

                sectionEvents += CalendarEvent(
                    id = "$sectionEventIdPrefix-$cursor",
                    calendarId = "schedule",
                    title = if (sectionLabel.isNullOrBlank()) {
                        "$courseCode - $courseTitle"
                    } else {
                        "$courseCode - $courseTitle ($sectionLabel)"
                    },
                    description = descriptionText,
                    location = sectionLocation.ifBlank { null },
                    htmlLink = null,
                    start = startDateTime,
                    endExclusive = endDateTime,
                    isAllDay = false,
                    isPinned = false
                )
            }

            cursor = cursor.plusDays(1)
        }

        return sectionEvents
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