package com.example.cicompanion.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.FirestoreManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class EventFilter { ALL, CSUCI, CUSTOM }

class CalendarViewModel(
    private val repository: CalendarRepository = CalendarRepository()
) : ViewModel() {

    var mode: CalendarMode by mutableStateOf(CalendarMode.MONTH)
        private set

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var visibleMonth: YearMonth by mutableStateOf(YearMonth.now())
        private set

    var sourceUrl: String by mutableStateOf(CSUCI_CALENDAR_SUBSCRIBE_URL)
        private set

    private var csuciEvents: List<CalendarEvent> by mutableStateOf(emptyList())
    private var customEvents: List<CalendarEvent> by mutableStateOf(emptyList())
    
    var filter: EventFilter by mutableStateOf(EventFilter.ALL)
        private set

    val events: List<CalendarEvent>
        get() = when (filter) {
            EventFilter.ALL -> customEvents + csuciEvents // Custom events on top
            EventFilter.CSUCI -> csuciEvents
            EventFilter.CUSTOM -> customEvents
        }

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    init {
        loadOnlineCalendar()
        loadCustomEvents()
    }

    /** Updates the active calendar view mode. */
    fun updateMode(newMode: CalendarMode) {
        mode = newMode
    }
    
    fun updateFilter(newFilter: EventFilter) {
        filter = newFilter
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
                csuciEvents = loadedEvents
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to load online calendar."
            }

            isLoading = false
        }
    }

    fun loadCustomEvents() {
        viewModelScope.launch {
            customEvents = FirestoreManager.fetchCustomEvents()
        }
    }

    fun addCustomEvent(event: CalendarEvent) {
        viewModelScope.launch {
            FirestoreManager.saveCustomEvent(event)
            loadCustomEvents()
        }
    }

    fun deleteCustomEvent(eventId: String) {
        viewModelScope.launch {
            FirestoreManager.deleteCustomEvent(eventId)
            loadCustomEvents()
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

    private companion object {
        /** Holds the CSUCI events feed used by the app. */
        const val CSUCI_CALENDAR_SUBSCRIBE_URL: String =
            "webcal://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    }
}