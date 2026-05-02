package com.example.cicompanion.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.calendar.data.repository.CalendarRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.EventInvite
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
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
    
    var incomingInvites by mutableStateOf<List<EventInvite>>(emptyList())
        private set

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

    init {
        loadOnlineCalendar()
        
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            customEventsListener?.remove()
            incomingInvitesListener?.remove()
            if (user != null) {
                // Real-time listener for custom events (includes shared ones and bookmarked CSUCI ones)
                customEventsListener = SocialRepository.listenToCustomEvents(user.uid,
                    onEventsChanged = { customEvents = it },
                    onError = { errorMessage = it }
                )
                
                // Real-time listener for incoming invites
                incomingInvitesListener = SocialRepository.listenToIncomingEventInvites(user.uid,
                    onInvitesChanged = { incomingInvites = it },
                    onError = { errorMessage = it }
                )
            } else {
                customEvents = emptyList()
                incomingInvites = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        customEventsListener?.remove()
        incomingInvitesListener?.remove()
    }

    val events: List<CalendarEvent>
        get() {
            val user = FirebaseAuth.getInstance().currentUser
            
            // Merge csuciEvents and customEvents, prioritizing the one from customEvents if it's the same event ID
            // (this handles bookmarked/pinned CSUCI events that are saved to Firestore)
            val merged = (customEvents + csuciEvents)
            val distinctEvents = merged.groupBy { it.id }.map { (_, events) ->
                if (events.size > 1) {
                    // Prefer the version from Firestore which has isBookmarked/isPinned status
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
                    if (isShared) filterShared else filterCustom
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

    fun addCustomEvent(event: CalendarEvent) {
        viewModelScope.launch {
            FirestoreManager.saveCustomEvent(event)
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
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
                // If it's a CSUCI event, save a copy to Firestore with the pin status
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
                // If it's a CSUCI event, save a copy to Firestore with the bookmark status
                FirestoreManager.saveCustomEvent(event.copy(isBookmarked = targetStatus))
            } else {
                FirestoreManager.updateEventBookmarkStatus(event.id, targetStatus)
            }
        }
    }

    fun acceptInvite(invite: EventInvite) {
        SocialRepository.acceptEventInvite(invite,
            onSuccess = {
                // Listener will update incomingInvites automatically
            },
            onError = { errorMessage = it }
        )
    }

    fun declineInvite(invite: EventInvite) {
        SocialRepository.declineEventInvite(invite,
            onSuccess = {
                // Listener will update incomingInvites automatically
            },
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
