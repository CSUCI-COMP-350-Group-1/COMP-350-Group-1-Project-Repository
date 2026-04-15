package com.example.cicompanion.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val repository: CalendarRepository = CalendarRepository()
) : ViewModel() {

    var mode: CalendarMode by mutableStateOf(CalendarMode.MONTH)
        private set

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var visibleMonth: YearMonth by mutableStateOf(YearMonth.now())
        private set

    //Uses shared config so the background worker and UI stay in sync
    var sourceUrl: String by mutableStateOf(CalendarFeedConfig.CSUCI_CALENDAR_SUBSCRIBE_URL)

    var events: List<CalendarEvent> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    init {
        loadOnlineCalendar()
    }

    /** Updates the active calendar view mode. */
    fun updateMode(newMode: CalendarMode) {
        mode = newMode
    }

    /** Loads all events from the CSUCI calendar subscribe link. */
    fun loadOnlineCalendar() {
        val trimmedUrl = sourceUrl.trim()
        if (trimmedUrl.isBlank()) {
            errorMessage = "Calendar feed URL is missing."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                repository.loadEvents(sourceUrl = trimmedUrl)
            }.onSuccess { loadedEvents ->
                events = loadedEvents
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to load online calendar."
            }

            isLoading = false
        }
    }

    /** Selects a new date and updates the visible month when needed. */
    fun onDateSelected(date: LocalDate) {
        selectedDate = date
        visibleMonth = YearMonth.from(date)
    }

    /** Moves the selected date back by one day. */
    fun previousDay() {
        onDateSelected(selectedDate.minusDays(1))
    }

    /** Moves the selected date forward by one day. */
    fun nextDay() {
        onDateSelected(selectedDate.plusDays(1))
    }

    /** Moves the selected date back by one week. */
    fun previousWeek() {
        onDateSelected(selectedDate.minusWeeks(1))
    }

    /** Moves the selected date forward by one week. */
    fun nextWeek() {
        onDateSelected(selectedDate.plusWeeks(1))
    }

    /** Shows the previous month and keeps the selected day inside it. */
    fun previousMonth() {
        visibleMonth = visibleMonth.minusMonths(1)
        selectedDate = clampSelectedDateToVisibleMonth(selectedDate, visibleMonth)
    }

    /** Shows the next month and keeps the selected day inside it. */
    fun nextMonth() {
        visibleMonth = visibleMonth.plusMonths(1)
        selectedDate = clampSelectedDateToVisibleMonth(selectedDate, visibleMonth)
    }

    /** Clears the current error message. */
    fun clearError() {
        errorMessage = null
    }

    /** Keeps the selected day inside the month being shown. */
    private fun clampSelectedDateToVisibleMonth(date: LocalDate, month: YearMonth): LocalDate {
        val safeDay = date.dayOfMonth.coerceAtMost(month.lengthOfMonth())
        return month.atDay(safeDay)
    }
}
