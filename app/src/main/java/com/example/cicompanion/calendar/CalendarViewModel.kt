package com.example.cicompanion.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.EventInvite
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.social.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
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

    var sourceUrl: String by mutableStateOf(CSUCI_CALENDAR_SUBSCRIBE_URL)
        private set

    private var csuciEvents: List<CalendarEvent> by mutableStateOf(emptyList())
    private var customEvents: List<CalendarEvent> by mutableStateOf(emptyList())

    var selectedClasses: List<SelectedClass> by mutableStateOf(emptyList())
        private set

    // EVENT NOTIFICATION CHANGE:
    // Stores the enabled preference IDs for the current signed-in user.
    var enabledEventNotificationPreferenceIds: Set<String> by mutableStateOf(emptySet())
        private set

    var incomingInvites by mutableStateOf<List<EventInvite>>(emptyList())
        private set

    private var acceptedInvites by mutableStateOf<List<EventInvite>>(emptyList())

    var filterCsuci by mutableStateOf(true)
        private set
    var filterCustom by mutableStateOf(true)
        private set
    var filterPinned by mutableStateOf(true)
        private set
    var filterBookmarked by mutableStateOf(true)
        private set
    var filterShared by mutableStateOf(true)
        private set

    var highlightedEventId: String? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    private var customEventsListener: ListenerRegistration? = null
    private var incomingInvitesListener: ListenerRegistration? = null
    private var acceptedInvitesListener: ListenerRegistration? = null

    init {
        loadOnlineCalendar()

        // EVENT NOTIFICATION CHANGE:
        // Reload both custom events and event notification preferences on auth changes.
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            customEventsListener?.remove()
            incomingInvitesListener?.remove()
            acceptedInvitesListener?.remove()

            if (user != null) {
                customEventsListener = SocialRepository.listenToCustomEvents(user.uid,
                    onEventsChanged = { customEvents = it },
                    onError = { errorMessage = it }
                )

                incomingInvitesListener = SocialRepository.listenToIncomingEventInvites(user.uid,
                    onInvitesChanged = { incomingInvites = it },
                    onError = { errorMessage = it }
                )

                acceptedInvitesListener = SocialRepository.listenToAcceptedEventInvites(user.uid,
                    onInvitesChanged = { acceptedInvites = it },
                    onError = { errorMessage = it }
                )
                loadCustomEvents()
                loadEventNotificationPreferences()
            } else {
                customEvents = emptyList()
                enabledEventNotificationPreferenceIds = emptySet()
                selectedClasses = emptyList()
                incomingInvites = emptyList()
                acceptedInvites = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        customEventsListener?.remove()
        incomingInvitesListener?.remove()
        acceptedInvitesListener?.remove()
    }

    val events: List<CalendarEvent>
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            val merged = (customEvents + buildSelectedClassEvents() + csuciEvents)
            val distinctEvents = merged.groupBy { it.id }.map { (_, events) ->
                if (events.size > 1) {
                    events.find { it.isBookmarked || it.isPinned }
                        ?: events.find { it.calendarId == "custom" }
                        ?: events.first()
                } else {
                    events.first()
                }
            }

            return distinctEvents.filter { event ->
                if (event.isPinned) {
                    filterPinned
                } else if (event.isBookmarked) {
                    filterBookmarked
                } else if (event.calendarId == "custom") {
                    val isShared = event.ownerId != null && event.ownerId != user?.uid
                    if (isShared) {
                        val hasActiveInvite = acceptedInvites.any { it.eventId == event.id }
                        filterShared && hasActiveInvite
                    } else {
                        filterCustom
                    }
                } else if (event.calendarId == "schedule") {
                    filterCustom
                } else {
                    filterCsuci
                }
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

    fun toggleFilterBookmarked() {
        filterBookmarked = !filterBookmarked
    }

    fun toggleFilterShared() {
        filterShared = !filterShared
    }

    fun resetFilters() {
        filterCsuci = true
        filterCustom = true
        filterPinned = true
        filterBookmarked = true
        filterShared = true
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

    fun loadSelectedClasses() {
        viewModelScope.launch {
            selectedClasses = FirestoreManager.fetchSelectedClasses()
        }
    }

    fun addCustomEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val success = FirestoreManager.saveCustomEvent(event)
            if (success) {
                customEvents = FirestoreManager.fetchCustomEvents()
            }
        }
    }

    // EVENT NOTIFICATION FIX:
    // Editing a custom event creates a new notification version.
    // The old opt-in is removed, so the user must opt into the edited event again.
    fun updateCustomEvent(
        originalEvent: CalendarEvent,
        updatedEvent: CalendarEvent
    ) {
        viewModelScope.launch {
            FirestoreManager.saveEventNotificationPreference(
                event = originalEvent,
                enabled = false
            )

            val success = FirestoreManager.saveCustomEvent(updatedEvent)
            if (success) {
                customEvents = FirestoreManager.fetchCustomEvents()
                enabledEventNotificationPreferenceIds =
                    FirestoreManager.fetchEnabledEventNotificationPreferenceIds()
            }
            FirestoreManager.saveCustomEvent(event)
        }
    }

    // EVENT NOTIFICATION FIX:
    // Deleting a custom event also deletes its notification preference.
    fun deleteCustomEvent(event: CalendarEvent) {
    fun deleteEvent(event: CalendarEvent) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            FirestoreManager.saveEventNotificationPreference(
                event = event,
                enabled = false
            )

            val success = FirestoreManager.deleteCustomEvent(event.id)
            if (success) {
                customEvents = FirestoreManager.fetchCustomEvents()
                enabledEventNotificationPreferenceIds =
                    FirestoreManager.fetchEnabledEventNotificationPreferenceIds()
            }
            if (event.ownerId == user.uid) {
                SocialRepository.deleteEventForAll(user.uid, event.id,
                    onSuccess = { },
                    onError = { errorMessage = it }
                )
            } else {
                SocialRepository.leaveEvent(user.uid, event.id,
                    onSuccess = { },
                    onError = { errorMessage = it }
                )
            }
        }
    }

    fun togglePinEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val targetStatus = !event.isPinned
            if (event.calendarId != "custom") {
                FirestoreManager.saveCustomEvent(event.copy(isPinned = targetStatus))
            } else {
                FirestoreManager.updateEventPinStatus(event.id, targetStatus)
            }
        }
    }

    fun toggleBookmarkEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val targetStatus = !event.isBookmarked
            if (event.calendarId != "custom") {
                FirestoreManager.saveCustomEvent(event.copy(isBookmarked = targetStatus))
            } else {
                FirestoreManager.updateEventBookmarkStatus(event.id, targetStatus)
            }
        }
    }

    fun acceptInvite(invite: EventInvite) {
        SocialRepository.acceptEventInvite(invite,
            onSuccess = { },
            onError = { errorMessage = it }
        )
    }

    fun declineInvite(invite: EventInvite) {
        SocialRepository.declineEventInvite(invite,
            onSuccess = { },
            onError = { errorMessage = it }
        )
    }

    fun kickUser(eventId: String, targetUserId: String, onSuccess: () -> Unit = {}) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        SocialRepository.kickFromEvent(ownerId, targetUserId, eventId,
            onSuccess = onSuccess,
            onError = { errorMessage = it }
        )
    }

    fun kickUsers(eventId: String, targetUserIds: List<String>, onSuccess: () -> Unit = {}) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        SocialRepository.kickMultipleFromEvent(ownerId, targetUserIds, eventId,
            onSuccess = onSuccess,
            onError = { errorMessage = it }
        )
    }

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
        return selectedClasses.flatMap { it.toCalendarEvents() }
    }

    private fun clampSelectedDateToVisibleMonth(date: LocalDate, month: YearMonth): LocalDate {
        val safeDay = date.dayOfMonth.coerceAtMost(month.lengthOfMonth())
        return month.atDay(safeDay)
    }

    companion object {
        const val CSUCI_CALENDAR_SUBSCRIBE_URL: String =
            "webcal://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    }
}
