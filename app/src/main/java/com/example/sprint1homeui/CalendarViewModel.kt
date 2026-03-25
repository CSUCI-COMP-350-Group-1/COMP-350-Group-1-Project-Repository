package com.example.sprint1homeui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint1homeui.data.repository.CalendarRepository
import com.example.sprint1homeui.model.CalendarEvent
import com.example.sprint1homeui.model.CalendarSummary
import com.example.sprint1homeui.model.EventDraft
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val repository: CalendarRepository = CalendarRepository()
) : ViewModel() {

    var accessToken: String? by mutableStateOf(null)
        private set

    var calendars: List<CalendarSummary> by mutableStateOf(emptyList())
        private set

    var activeCalendarId: String by mutableStateOf("primary")
        private set

    var activeCalendarTitle: String by mutableStateOf("Primary")
        private set

    var mode: CalendarMode by mutableStateOf(CalendarMode.MONTH)
        private set

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var visibleMonth: YearMonth by mutableStateOf(YearMonth.now())
        private set

    var events: List<CalendarEvent> by mutableStateOf(emptyList())
        private set

    var latestSyncToken: String? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var showCreateDialog: Boolean by mutableStateOf(false)
        private set

    fun onAuthorized(token: String) {
        accessToken = token
        refreshAll()
    }

    fun updateMode(newMode: CalendarMode) {
        mode = newMode
    }

    fun onDateSelected(date: LocalDate) {
        selectedDate = date
        val newMonth = YearMonth.from(date)
        if (newMonth != visibleMonth) {
            visibleMonth = newMonth
            refreshEvents()
        }
    }

    fun previousDay() = onDateSelected(selectedDate.minusDays(1))
    fun nextDay() = onDateSelected(selectedDate.plusDays(1))
    fun previousWeek() = onDateSelected(selectedDate.minusWeeks(1))
    fun nextWeek() = onDateSelected(selectedDate.plusWeeks(1))

    fun previousMonth() {
        visibleMonth = visibleMonth.minusMonths(1)
        selectedDate = visibleMonth.atDay(1)
        refreshEvents()
    }

    fun nextMonth() {
        visibleMonth = visibleMonth.plusMonths(1)
        selectedDate = visibleMonth.atDay(1)
        refreshEvents()
    }

    fun refresh() {
        refreshAll()
    }

    fun openCreateDialog() {
        showCreateDialog = true
    }

    fun closeCreateDialog() {
        showCreateDialog = false
    }

    fun setError(message: String) {
        errorMessage = message
    }

    fun clearError() {
        errorMessage = null
    }

    fun createEvent(draft: EventDraft) {
        val token = accessToken ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                repository.createEvent(
                    accessToken = token,
                    calendarId = activeCalendarId,
                    draft = draft
                )
                repository.loadEventsForWindow(
                    accessToken = token,
                    calendarId = activeCalendarId,
                    visibleMonth = YearMonth.from(draft.date)
                )
            }.onSuccess { page ->
                selectedDate = draft.date
                visibleMonth = YearMonth.from(draft.date)
                events = page.items
                latestSyncToken = page.nextSyncToken
                showCreateDialog = false
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to create event."
            }

            isLoading = false
        }
    }

    fun selectCalendar(calendarId: String) {
        val selected = calendars.firstOrNull { it.id == calendarId } ?: return
        activeCalendarId = selected.id
        activeCalendarTitle = selected.title
        refreshEvents()
    }

    private fun refreshAll() {
        val token = accessToken ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                val availableCalendars = repository.loadCalendars(token)
                val selected = availableCalendars.firstOrNull { it.id == activeCalendarId }
                    ?: repository.selectInitialCalendar(availableCalendars)
                val page = repository.loadEventsForWindow(
                    accessToken = token,
                    calendarId = selected.id,
                    visibleMonth = visibleMonth
                )
                LoadedData(
                    calendars = availableCalendars,
                    selectedCalendar = selected,
                    page = page
                )
            }.onSuccess { loaded ->
                calendars = loaded.calendars
                activeCalendarId = loaded.selectedCalendar.id
                activeCalendarTitle = loaded.selectedCalendar.title
                events = loaded.page.items
                latestSyncToken = loaded.page.nextSyncToken
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to load calendar data."
            }

            isLoading = false
        }
    }

    private fun refreshEvents() {
        val token = accessToken ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                repository.loadEventsForWindow(
                    accessToken = token,
                    calendarId = activeCalendarId,
                    visibleMonth = visibleMonth
                )
            }.onSuccess { page ->
                events = page.items
                latestSyncToken = page.nextSyncToken
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to refresh events."
            }

            isLoading = false
        }
    }

    private data class LoadedData(
        val calendars: List<CalendarSummary>,
        val selectedCalendar: CalendarSummary,
        val page: com.example.sprint1homeui.model.EventsPage
    )
}
