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
    
    var filterCsuci by mutableStateOf(true)
        private set
    var filterCustom by mutableStateOf(true)
        private set
    var filterPinned by mutableStateOf(true)
        private set

    var highlightedEventId: String? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    init {
        loadOnlineCalendar()
        loadCustomEvents()
    }

    val events: List<CalendarEvent>
        get() = (customEvents + csuciEvents).filter { event ->
            if (event.isPinned) {
                filterPinned
            } else if (event.calendarId == "custom") {
                filterCustom
            } else {
                filterCsuci
            }
        }

    fun updateMode(newMode: CalendarMode) {
        mode = newMode
    }
    
    fun toggleFilterCsuci() {
        filterCsuci = !filterCsuci
    }

    fun toggleFilterCustom() {
        filterCustom = !filterCustom
    }

    fun toggleFilterPinned() {
        filterPinned = !filterPinned
    }

    fun resetFilters() {
        filterCsuci = true
        filterCustom = true
        filterPinned = true
    }

    fun setHighlightedEvent(eventId: String?) {
        highlightedEventId = eventId
    }

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

    fun togglePinEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val targetStatus = !event.isPinned
            FirestoreManager.updateEventPinStatus(event.id, targetStatus)
            loadCustomEvents()
        }
    }

    fun onDateSelected(date: LocalDate) {
        selectedDate = date
        visibleMonth = YearMonth.from(date)
    }

    fun previousDay() {
        onDateSelected(selectedDate.minusDays(1))
    }

    fun nextDay() {
        onDateSelected(selectedDate.plusDays(1))
    }

    fun previousWeek() {
        onDateSelected(selectedDate.minusWeeks(1))
    }

    fun nextWeek() {
        onDateSelected(selectedDate.plusWeeks(1))
    }

    fun previousMonth() {
        visibleMonth = visibleMonth.minusMonths(1)
        selectedDate = clampSelectedDateToVisibleMonth(selectedDate, visibleMonth)
    }

    fun nextMonth() {
        visibleMonth = visibleMonth.plusMonths(1)
        selectedDate = clampSelectedDateToVisibleMonth(selectedDate, visibleMonth)
    }

    fun clearError() {
        errorMessage = null
    }

    private fun clampSelectedDateToVisibleMonth(date: LocalDate, month: YearMonth): LocalDate {
        val safeDay = date.dayOfMonth.coerceAtMost(month.lengthOfMonth())
        return month.atDay(safeDay)
    }

    private companion object {
        const val CSUCI_CALENDAR_SUBSCRIBE_URL: String =
            "webcal://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    }
}