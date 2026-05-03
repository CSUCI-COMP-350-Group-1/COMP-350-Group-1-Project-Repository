package com.example.cicompanion.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
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

    // CALENDAR SCHEDULE CHANGE:
    // Store the user's saved classes separately from custom events.
    // These are converted to CalendarEvent items only when building the UI list.
    var selectedClasses: List<SelectedClass> by mutableStateOf(emptyList())
        private set

    // EVENT NOTIFICATION CHANGE:
    // Stores the enabled preference IDs for the current signed-in user.
    var enabledEventNotificationPreferenceIds: Set<String> by mutableStateOf(emptySet())
        private set

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

        // EVENT NOTIFICATION CHANGE:
        // Reload both custom events and event notification preferences on auth changes.
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadCustomEvents()
                loadEventNotificationPreferences()
            } else {
                customEvents = emptyList()
                enabledEventNotificationPreferenceIds = emptySet()
            }
        }
    }

    val events: List<CalendarEvent>
        get() = (customEvents + buildSelectedClassEvents() + csuciEvents).filter { event ->
            if (event.isPinned) {
                filterPinned
            } else if (event.calendarId == "custom" || event.calendarId == "schedule") {
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

    // CALENDAR SCHEDULE CHANGE:
    // Load the user's saved class schedule entries from Firestore.
    fun loadSelectedClasses() {
        viewModelScope.launch {
            selectedClasses = FirestoreManager.fetchSelectedClasses()
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

    // CALENDAR SCHEDULE CHANGE:
    // Save one class entry, then refresh the schedule list.
    fun saveSelectedClass(
        selectedClass: SelectedClass,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = FirestoreManager.saveSelectedClass(selectedClass)
            if (success) {
                loadSelectedClasses()
            }
            onComplete(success)
        }
    }

    // CALENDAR SCHEDULE CHANGE:
    // Delete one class entry, then refresh the schedule list.
    fun deleteSelectedClass(
        selectedClassId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = FirestoreManager.deleteSelectedClass(selectedClassId)
            if (success) {
                loadSelectedClasses()
            }
            onComplete(success)
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

    // EVENT NOTIFICATION
    fun loadEventNotificationPreferences() {
        viewModelScope.launch {
            enabledEventNotificationPreferenceIds =
                FirestoreManager.fetchEnabledEventNotificationPreferenceIds()
        }
    }

    // EVENT NOTIFICATION:
    // Class events check the recurring class preference ID.
    // One-time events check the exact occurrence preference ID.
    fun isEventNotificationEnabled(event: CalendarEvent): Boolean {
        val preferenceId = FirestoreManager.buildEventNotificationPreferenceIdForEvent(event)
        return preferenceId in enabledEventNotificationPreferenceIds
    }

    fun setEventNotificationEnabled(
        event: CalendarEvent,
        enabled: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = FirestoreManager.saveEventNotificationPreference(event, enabled)
            if (success) {
                loadEventNotificationPreferences()
            }
            onComplete(success)
        }
    }

    // CALENDAR SCHEDULE CHANGE:
    // Convert saved classes into normal calendar events so the existing calendar
    // UI can display them without needing a separate event renderer.
    private fun buildSelectedClassEvents(): List<CalendarEvent> {
        return selectedClasses.flatMap { selectedClass ->
            selectedClass.toCalendarEvents()
        }
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