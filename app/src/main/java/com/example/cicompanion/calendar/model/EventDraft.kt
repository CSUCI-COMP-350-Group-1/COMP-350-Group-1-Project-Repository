package com.example.cicompanion.calendar.model

import java.time.LocalDate
import java.time.LocalTime

data class EventDraft(
    val title: String,
    val description: String,
    val location: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAllDay: Boolean
)
