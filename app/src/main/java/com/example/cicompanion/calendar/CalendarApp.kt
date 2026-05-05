package com.example.cicompanion.calendar

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.EventInvite
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserProfile
import com.example.cicompanion.social.UserAvatar
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.utils.HtmlUtils
import com.google.firebase.auth.FirebaseAuth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

private val CoralRed = Color(0xFFEF3347)
private val HotPink = Color(0xFFF21F63)
private val SoftText = Color(0xFF6E5555)
private val CardOffWhite = Color(0xFFF6E6D8)
private val DateCellWhite = Color(0xFFF7F4F8)
private val DateCellBorder = Color(0xFFE2BFB7)
private val SharedEventBlue = Color(0xFF2196F3)
private val SharedEventLightBlue = Color(0xFFE3F2FD)
private val CustomEventOrange = Color(0xFFFF9800)
private val PinnedEventPurple = Color(0xFF9C27B0)
private val EventCardGrey = Color(0xFFF2F2F2)

data class DayEventInfo(
    val hasCsuci: Boolean = false, 
    val hasCustom: Boolean = false,
    val hasPinned: Boolean = false,
    val hasShared: Boolean = false
)

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToInvite by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventMembersToShow by remember { mutableStateOf<CalendarEvent?>(null) }
    var inviteToShowDetails by remember { mutableStateOf<EventInvite?>(null) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }


    // EVENT NOTIFICATION
    // Load this user's event notification toggles when the calendar screen is shown.
    LaunchedEffect(Unit) {
        viewModel.loadEventNotificationPreferences()
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    val selectedDateEvents = remember(viewModel.events, viewModel.selectedDate) {
        buildSelectedDateEvents(viewModel.events, viewModel.selectedDate)
    }

    val dayEventInfoMap = remember(viewModel.events) {
        buildDayEventInfoMap(viewModel.events, currentUser?.uid)
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            if (currentUser != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    containerColor = CoralRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(14.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(1.dp)
                                    .background(CoralRed, CircleShape),
                            contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    },
                    text = { Text("Add Event", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // EVENT NOTIFICATION CHANGE:
            // Use the real CalendarScreenBody signature from this file.
            CalendarScreenBody(
                scrollState = scrollState,
                viewModel = viewModel,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                onDismissError = viewModel::clearError,
                onDateSelected = {
                    viewModel.onDateSelected(it)
                    viewModel.setHighlightedEvent(null)
                },
                onRequestDelete = { eventToDelete = it },
                onRequestEdit = { eventToEdit = it },
                onTogglePin = viewModel::togglePinEvent,
                onToggleBookmark = viewModel::toggleBookmarkEvent,
                onRequestInvite = { eventToInvite = it },
                onShowMembers = { eventMembersToShow = it },
                onInviteClick = { inviteToShowDetails = it },

                // EVENT NOTIFICATION MERGE:
                isEventNotificationEnabled = viewModel::isEventNotificationEnabled,
                onToggleEventNotification = { event, enabled ->
                    viewModel.setEventNotificationEnabled(event, enabled)
                }
            )
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            selectedDate = viewModel.selectedDate,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, desc, loc, start, end, invitedFriends ->
                val startZdt = ZonedDateTime.of(viewModel.selectedDate, start, ZonedDateTime.now().zone)
                val endZdt = ZonedDateTime.of(viewModel.selectedDate, end, ZonedDateTime.now().zone)
                val newEvent = CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    calendarId = "custom",
                    title = title,
                    description = desc,
                    location = loc,
                    htmlLink = null,
                    start = startZdt,
                    endExclusive = endZdt,
                    isAllDay = false,
                    ownerId = currentUser?.uid,
                    maxMembers = null,
                    isShared = invitedFriends.isNotEmpty()
                )
                viewModel.addCustomEvent(newEvent)

                currentUser?.let { user ->
                    invitedFriends.forEach { friend ->
                        SocialRepository.sendEventInvite(user, friend.uid, newEvent, {}, {})
                    }
                }

                showAddEventDialog = false
                if (invitedFriends.isNotEmpty()) {
                    Toast.makeText(context, "Event created and ${invitedFriends.size} invites sent!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    eventToEdit?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, description, location, startTime, endTime ->
                val startZdt = ZonedDateTime.of(event.start.toLocalDate(), startTime, event.start.zone)
                val endZdt = ZonedDateTime.of(event.start.toLocalDate(), endTime, event.endExclusive.zone)
                val updatedEvent = event.copy(
                    title = title,
                    description = description,
                    location = location,
                    start = startZdt,
                    endExclusive = endZdt
                )
                // EVENT NOTIFICATION FIX:
                // Editing creates a new single-event notification version.
                viewModel.updateCustomEvent(event, updatedEvent)
                viewModel.addCustomEvent(updatedEvent)
                eventToEdit = null
            }
        )
    }

    eventToDelete?.let { event ->
        val isOwner = event.ownerId == currentUser?.uid
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text(if (isOwner) "Delete Event Globally" else "Leave Event") },
            text = { Text(if (isOwner) "Do you really want to delete this event for everyone?" else "Do you want to remove this event from your calendar?") },
            confirmButton = {
                Button(
                    onClick = {
                        // EVENT NOTIFICATION FIX:
                        // Delete the custom event and its notification opt-in together.
                        viewModel.deleteCustomEvent(event)
                        viewModel.deleteEvent(event)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text(if (isOwner) "Delete All" else "Leave")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null }
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        )
    }

    eventToInvite?.let { event ->
        FriendPickerDialog(
            eventId = event.id,
            onDismiss = { eventToInvite = null },
            onInvite = { friend ->
                currentUser?.let { user ->
                    SocialRepository.sendEventInvite(
                        currentUser = user,
                        targetUserId = friend.uid,
                        event = event,
                        onSuccess = {
                            eventToInvite = null
                            Toast.makeText(context, "Invite sent to ${friend.displayName}!", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }

    eventMembersToShow?.let { event ->
        EventMembersDialog(
            event = event,
            currentUserId = currentUser?.uid ?: "",
            onDismiss = { eventMembersToShow = null },
            onKick = { targetUids ->
                viewModel.kickUsers(event.id, targetUids)
                eventMembersToShow = null
                Toast.makeText(context, "Kicked ${targetUids.size} members", Toast.LENGTH_SHORT).show()
            }
        )
    }

    inviteToShowDetails?.let { invite ->
        InviteDetailDialog(
            invite = invite,
            onDismiss = { inviteToShowDetails = null },
            onAccept = {
                viewModel.acceptInvite(invite)
                inviteToShowDetails = null
            },
            onDecline = {
                viewModel.declineInvite(invite)
                inviteToShowDetails = null
            }
        )
    }
}

@Composable
private fun CalendarScreenBody(
    scrollState: androidx.compose.foundation.ScrollState,
    viewModel: CalendarViewModel,
    selectedDateEvents: List<CalendarEvent>,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    onDismissError: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRequestDelete: (CalendarEvent) -> Unit,
    onRequestEdit: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit,
    onInviteClick: (EventInvite) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(0.dp)
    ) {
        CalendarHeroHeader(
            title = "CI Companion Calendar",
            subtitle = viewModel.selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            trailingContent = { FilterDropdown(viewModel = viewModel) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.incomingInvites.isNotEmpty()) {
                IncomingInvitesSection(
                    invites = viewModel.incomingInvites,
                    onAccept = viewModel::acceptInvite,
                    onDecline = viewModel::declineInvite,
                    onInviteClick = onInviteClick
                )
            }

            viewModel.errorMessage?.let { message ->
                ErrorMessage(
                    message = message,
                    onDismiss = onDismissError
                )
            }

            SectionCard {
                SectionHeading(text = "View")
                Spacer(modifier = Modifier.height(12.dp))
                CalendarModeTabs(
                    selectedMode = viewModel.mode,
                    onModeSelected = viewModel::updateMode
                )
            }

            // EVENT NOTIFICATION CHANGE:
            // Use the real CalendarContent signature from this file.
            CalendarContent(
                mode = viewModel.mode,
                visibleMonth = viewModel.visibleMonth,
                selectedDate = viewModel.selectedDate,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                onDateSelected = onDateSelected,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek = viewModel::nextWeek,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onDeleteEvent = onRequestDelete,
                onEditEvent = onRequestEdit,
                onTogglePin = onTogglePin,
                onToggleBookmark = onToggleBookmark,
                onRequestInvite = onRequestInvite,
                onShowMembers = onShowMembers,
                highlightedEventId = viewModel.highlightedEventId,
                isEventNotificationEnabled = isEventNotificationEnabled,
                onToggleEventNotification = onToggleEventNotification
            )
        }
    }
}

@Composable
fun IncomingInvitesSection(
    invites: List<EventInvite>,
    onAccept: (EventInvite) -> Unit,
    onDecline: (EventInvite) -> Unit,
    onInviteClick: (EventInvite) -> Unit
) {
    SectionCard(modifier = Modifier.border(2.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(24.dp))) {
        SectionHeading(text = "Event Invitations (${invites.size})")
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            invites.forEach { invite ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onInviteClick(invite) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = invite.eventTitle, fontWeight = FontWeight.Bold)
                            Text(text = "from ${invite.fromDisplayName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row {
                            IconButton(onClick = { onAccept(invite) }) {
                                Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                            }
                            IconButton(onClick = { onDecline(invite) }) {
                                Icon(Icons.Default.Close, contentDescription = "Decline", tint = CoralRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(viewModel: CalendarViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filter",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .width(200.dp)
        ) {
            Text(
                text = "Filter Events",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            FilterMenuItem(
                label = "CSUCI Events",
                isSelected = viewModel.filterCsuci,
                color = CoralRed,
                onClick = { viewModel.toggleFilterCsuci() }
            )
            FilterMenuItem(
                label = "Custom Events",
                isSelected = viewModel.filterCustom,
                color = CustomEventOrange,
                onClick = { viewModel.toggleFilterCustom() }
            )
            FilterMenuItem(
                label = "Pinned Events",
                isSelected = viewModel.filterPinned,
                color = PinnedEventPurple,
                onClick = { viewModel.toggleFilterPinned() }
            )
            FilterMenuItem(
                label = "Shared Events",
                isSelected = viewModel.filterShared,
                color = SharedEventBlue,
                onClick = { viewModel.toggleFilterShared() }
            )
        }
    }
}

@Composable
private fun FilterMenuItem(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) color else Color.Transparent)
                        .border(1.dp, color, RoundedCornerShape(4.dp))
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Dismiss",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CalendarModeTabs(
    selectedMode: CalendarMode,
    onModeSelected: (CalendarMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarMode.entries.forEach { mode ->
            ModeCard(
                mode = mode,
                isSelected = selectedMode == mode,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: CalendarMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = CoralRed
    val containerColor = if (isSelected) accentColor else DateCellWhite
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else accentColor
    val borderColor = if (isSelected) Color.Transparent else DateCellBorder

    Box(
        modifier = modifier
            .shadow(elevation = if (isSelected) 6.dp else 0.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.name,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalendarContent(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateEvents: List<CalendarEvent>,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit,
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null
) {
    when (mode) {
        CalendarMode.MONTH -> MonthView(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            dayEventInfoMap = dayEventInfoMap,
            selectedDateEvents = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers,
            highlightedEventId = highlightedEventId
        )

        CalendarMode.DAY -> DayView(
            selectedDate = selectedDate,
            events = selectedDateEvents,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers,
            highlightedEventId = highlightedEventId
        )

        CalendarMode.WEEK -> WeekView(
            selectedDate = selectedDate,
            dayEventInfoMap = dayEventInfoMap,
            eventsForSelectedDate = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers
        )
    }
}

@Composable
private fun DayView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit,

) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            HeaderWithArrows(
                title = selectedDate.format(formatter),
                onPrevious = onPreviousDay,
                onNext = onNextDay
            )
        }

        EventsSection(
            title = "Events",
            events = events,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers
        )
    }
}

@Composable
private fun WeekView(
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    eventsForSelectedDate: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit,
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit,

) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val weekDates = remember(selectedDate, firstDayOfWeek) {
        buildWeekDates(selectedDate, firstDayOfWeek)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            HeaderWithArrows(
                title = buildWeekTitle(weekDates.first()),
                onPrevious = onPreviousWeek,
                onNext = onNextWeek
            )

            Spacer(modifier = Modifier.height(12.dp))
            WeekdayHeader(firstDayOfWeek = firstDayOfWeek)
            Spacer(modifier = Modifier.height(10.dp))
            WeekRow(
                dates = weekDates,
                selectedDate = selectedDate,
                dayEventInfoMap = dayEventInfoMap,
                onDateSelected = onDateSelected
            )
        }

        EventsSection(
            title = "Selected day",
            events = eventsForSelectedDate,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers
        )
    }
}

@Composable
private fun MonthView(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    selectedDateEvents: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit,
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit
) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val monthCells = remember(visibleMonth, firstDayOfWeek) {
        buildMonthCells(visibleMonth, firstDayOfWeek)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard {
            HeaderWithArrows(
                title = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )

            Spacer(modifier = Modifier.height(12.dp))
            WeekdayHeader(firstDayOfWeek = firstDayOfWeek)
            Spacer(modifier = Modifier.height(10.dp))
            MonthGrid(
                monthCells = monthCells,
                selectedDate = selectedDate,
                dayEventInfoMap = dayEventInfoMap,
                onDateSelected = onDateSelected
            )
        }

        EventsSection(
            title = "Selected day",
            events = selectedDateEvents,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers
        )
    }
}

@Composable
private fun HeaderWithArrows(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavigationCircleButton(
            symbol = "‹",
            onClick = onPrevious
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        NavigationCircleButton(
            symbol = "›",
            onClick = onNext
        )
    }
}

@Composable
private fun NavigationCircleButton(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DateCellWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val orderedDays = remember(firstDayOfWeek) {
        buildOrderedDays(firstDayOfWeek)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        orderedDays.forEach { day ->
            WeekdayChip(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekdayChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DateCellWhite)
            .border(1.dp, DateCellBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeekRow(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dates.forEach { date ->
            DateCell(
                modifier = Modifier.weight(1f),
                date = date,
                isSelected = date == selectedDate,
                eventInfo = dayEventInfoMap[date] ?: DayEventInfo(),
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun MonthGrid(
    monthCells: List<LocalDate?>,
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        monthCells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { date ->
                    if (date == null) {
                        EmptyDateCell(modifier = Modifier.weight(1f))
                    } else {
                        DateCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            isSelected = date == selectedDate,
                            eventInfo = dayEventInfoMap[date] ?: DayEventInfo(),
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDateCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(68.dp)
            .padding(2.dp)
    )
}

@Composable
private fun DateCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    isSelected: Boolean,
    eventInfo: DayEventInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .padding(2.dp)
            .background(resolveDateCellBackgroundColor(isSelected), RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = resolveDateCellBorderColor(isSelected),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = resolveDateCellTextColor(isSelected),
                fontWeight = FontWeight.Bold
            )

            EventDots(
                info = eventInfo
            )
        }
    }
}

@Composable
private fun EventDots(
    info: DayEventInfo
) {
    // 2x2 Grid arrangement
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Top Left: CSUCI
            Dot(color = CoralRed, visible = info.hasCsuci)
            // Top Right: Pinned
            Dot(color = PinnedEventPurple, visible = info.hasPinned)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Bottom Left: Custom
            Dot(color = CustomEventOrange, visible = info.hasCustom)
            // Bottom Right: Shared
            Dot(color = SharedEventBlue, visible = info.hasShared)
        }
    }
}

@Composable
private fun Dot(color: Color, visible: Boolean) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(if (visible) color else Color.Transparent, CircleShape)
    )
}

@Composable
private fun EventsSection(
    title: String,
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit,
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(text = title)
        EventsList(
            events = events,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId,
            isEventNotificationEnabled = isEventNotificationEnabled,
            onToggleEventNotification = onToggleEventNotification,
            onToggleBookmark = onToggleBookmark,
            onRequestInvite = onRequestInvite,
            onShowMembers = onShowMembers
        )
    }
}

@Composable
private fun EventsList(
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null,

    // EVENT NOTIFICATION CHANGE:
    isEventNotificationEnabled: (CalendarEvent) -> Boolean,
    onToggleEventNotification: (CalendarEvent, Boolean) -> Unit,
    onToggleBookmark: (CalendarEvent) -> Unit,
    onRequestInvite: (CalendarEvent) -> Unit,
    onShowMembers: (CalendarEvent) -> Unit
) {
    if (events.isEmpty()) {
        EmptyEventsMessage()
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        events.forEach { event ->
            EventCard(
                event = event,
                onDelete = { onDeleteEvent(event) },
                onEdit = { onEditEvent(event) },
                onTogglePin = { onTogglePin(event) },
                isHighlighted = event.id == highlightedEventId,
                notificationsEnabled = isEventNotificationEnabled(event),
                onNotificationToggle = { enabled ->
                    onToggleEventNotification(event, enabled)
                },
                onToggleBookmark = { onToggleBookmark(event) },
                onInvite = { onRequestInvite(event) },
                onShowMembers = { onShowMembers(event) },
                isHighlighted = event.id == highlightedEventId
            )
        }
    }
}

@Composable
private fun EmptyEventsMessage() {
    SectionCard {
        Text(
            text = "No events for this date.",
            style = MaterialTheme.typography.bodyLarge,
            color = SoftText
        )
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    isHighlighted: Boolean = false,

    // EVENT NOTIFICATION CHANGE:
    notificationsEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onToggleBookmark: () -> Unit,
    onInvite: () -> Unit,
    onShowMembers: () -> Unit
) {
    val isCustom = event.calendarId == "custom"



    val currentUser = FirebaseAuth.getInstance().currentUser
    val isOwner = event.ownerId == currentUser?.uid

    val containerColor = when {
        !isCustom -> EventCardGrey
        isOwner && event.isShared -> SharedEventLightBlue
        isOwner -> CardOffWhite
        else -> SharedEventLightBlue
    }
    val borderColor = if (isHighlighted) CoralRed else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isHighlighted) 2.dp else 0.dp, borderColor, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCustom) 5.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCustom) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(24.dp).offset(y = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (event.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (event.isPinned) PinnedEventPurple else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = HtmlUtils.stripHtml(event.title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = HtmlUtils.stripHtml(event.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOwner && event.isShared) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = "Shared",
                                tint = SharedEventBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isCustom && (!isOwner || event.isShared)) {
                        Text(
                            text = if (isOwner) "Shared Event (You are Leader)" else "Shared Event",
                            style = MaterialTheme.typography.labelSmall,
                            color = SharedEventBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCustom) {
                        // Members Button
                        IconButton(
                            onClick = onShowMembers,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Outlined.Group, contentDescription = "Members", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                        }

                        if (isOwner) {
                            IconButton(
                                onClick = onInvite,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Blue.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = "Invite Friend",
                                        tint = Color.Blue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Gray.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CoralRed.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                                ) {
                                Icon(
                                    imageVector = if (isOwner) Icons.Default.Delete else Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = if (isOwner) "Delete" else "Leave",
                                    tint = CoralRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    EventBadge(
                        text = if (event.isPinned) "Pinned" else if (isCustom) (if (isOwner) (if (event.isShared) "Shared (Leader)" else "Custom") else "Shared") else "CSUCI",
                        color = if (event.isPinned) PinnedEventPurple else if (isCustom) (if (isOwner) (if (event.isShared) SharedEventBlue else CustomEventOrange) else SharedEventBlue) else CoralRed
                    )
                }
            }

            EventMetaLine(text = event.timeLabel())
            event.location?.let { EventMetaLine(text = "Location: ${HtmlUtils.stripHtml(it)}") }

            if (isCustom && !isOwner && event.isPinnedByLeader) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = PinnedEventPurple)
                    Spacer(Modifier.width(6.dp))
                    Text("The leader has this event pinned", style = MaterialTheme.typography.labelSmall, color = PinnedEventPurple)
                }
            }

            event.description?.let { EventDescription(text = HtmlUtils.stripHtml(it)) }

            // EVENT NOTIFICATION CHANGE:
            // Per-user event reminder toggle directly in the event card.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Push reminders",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationToggle
                )
            }
        }
    }
}

@Composable
fun EventMembersDialog(
    event: CalendarEvent,
    currentUserId: String,
    onDismiss: () -> Unit,
    onKick: (List<String>) -> Unit
) {
    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    val isOwner = event.ownerId == currentUserId

    val otherMembers = remember(members, event.ownerId) {
        members.filter { it.uid != event.ownerId }
    }

    DisposableEffect(event.id) {
        val registration = SocialRepository.listenToEventMembers(
            eventId = event.id,
            onMembersChanged = { newMembers ->
                members = newMembers
                isLoading = false
            },
            onError = { isLoading = false }
        )
        onDispose { registration.remove() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Event Members",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandRedDark
                        )
                        Text(
                            text = "${members.size + 1} members total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandRedDark, strokeWidth = 3.dp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Leader Section
                        item {
                            MemberGroupHeader(text = "Host")
                            PremiumMemberRow(
                                userId = event.ownerId ?: "",
                                isOwner = true,
                                currentUserId = currentUserId,
                                eventId = event.id
                            )
                        }

                        // Others Section
                        if (otherMembers.isNotEmpty() || !isOwner) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                MemberGroupHeader(text = "Participants")
                            }
                        }

                        if (otherMembers.isEmpty()) {
                            item {
                                EmptyMembersPlaceholder()
                            }
                        } else {
                            items(otherMembers, key = { it.uid }) { member ->
                                PremiumMemberRow(
                                    userId = member.uid,
                                    displayName = SocialRepository.displayNameOrEmail(member),
                                    photoUrl = member.photoUrl,
                                    isOwner = false,
                                    canKick = isOwner,
                                    isSelected = selectedUsers.contains(member.uid),
                                    onToggleSelect = {
                                        selectedUsers = if (selectedUsers.contains(member.uid)) {
                                            selectedUsers - member.uid
                                        } else {
                                            selectedUsers + member.uid
                                        }
                                    },
                                    currentUserId = currentUserId,
                                    eventId = event.id
                                )
                            }
                        }
                    }
                }

                if (isOwner && otherMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    val canKickSelected = selectedUsers.isNotEmpty()

                    Button(
                        onClick = { if (canKickSelected) onKick(selectedUsers.toList()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(if (canKickSelected) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canKickSelected) BrandRedDark else Color.Gray.copy(alpha = 0.2f),
                            contentColor = if (canKickSelected) Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canKickSelected
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canKickSelected) {
                                Icon(Icons.Default.PersonRemove, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Remove selected (${selectedUsers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            } else {
                                Text("Select members to remove", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberGroupHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyMembersPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Group,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No other members yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PremiumMemberRow(
    userId: String,
    displayName: String = "Loading...",
    photoUrl: String = "",
    isOwner: Boolean,
    canKick: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    currentUserId: String,
    eventId: String
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(userId) {
        if (displayName == "Loading..." && userId.isNotEmpty()) {
            SocialRepository.fetchUserProfile(userId, { profile = it }, {})
        }
    }

    val name = profile?.let { SocialRepository.displayNameOrEmail(it) } ?: displayName
    val photo = profile?.photoUrl ?: photoUrl
    val isSelectable = canKick && !isOwner && userId != currentUserId
    val isMe = userId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isSelectable) { onToggleSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandRedDark.copy(alpha = 0.05f) else Color.Transparent
        ),
        border = if (isSelected) BorderStroke(1.dp, BrandRedDark.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                UserAvatar(photoUrl = photo)
                if (isOwner) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(CustomEventOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "$name (You)" else name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isOwner || isMe) FontWeight.Bold else FontWeight.Medium,
                    color = if (isMe) BrandRedDark else Color.DarkGray
                )
                if (isOwner) {
                    Text("Event Host", style = MaterialTheme.typography.bodySmall, color = CustomEventOrange)
                } else if (isMe) {
                    Text("You", style = MaterialTheme.typography.bodySmall, color = BrandRedDark.copy(alpha = 0.7f))
                }
            }

            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandRedDark,
                        uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            } else if (isOwner) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Admin",
                    tint = CustomEventOrange.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime, List<UserProfile>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }

    var invitedFriends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showFriendPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { 
            Text(
                text = "New Shared Event",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CoralRed
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftText
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Invite Friends", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showFriendPicker = true }) {
                            Text("+ Add", color = SharedEventBlue)
                        }
                    }
                    if (invitedFriends.isEmpty()) {
                        Text("No one invited. You can do this later.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    } else {
                        Column(modifier = Modifier.heightIn(max = 120.dp)) {
                            invitedFriends.forEach { friend ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    UserAvatar(photoUrl = friend.photoUrl)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(SocialRepository.displayNameOrEmail(friend), style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { invitedFriends = invitedFriends.filter { it.uid != friend.uid } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Time Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = startTime, onTimeChange = { startTime = it }) }
                        Text("to", Modifier.padding(horizontal = 8.dp))
                        Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = endTime, onTimeChange = { endTime = it }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, location, startTime, endTime, invitedFriends) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Event", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showFriendPicker) {
        FriendPickerDialog(
            onDismiss = { showFriendPicker = false },
            onInvite = { friend ->
                if (!invitedFriends.any { it.uid == friend.uid }) {
                    invitedFriends = invitedFriends + friend
                }
                showFriendPicker = false
            }
        )
    }
}

@Composable
private fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var startTime by remember { mutableStateOf(event.start.toLocalTime()) }
    var endTime by remember { mutableStateOf(event.endExclusive.toLocalTime()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { 
            Text(
                text = "Edit Event",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CoralRed
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Date: ${event.start.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftText
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row {
                         Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = startTime, onTimeChange = { startTime = it }) }
                         Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = endTime, onTimeChange = { endTime = it }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, location, startTime, endTime) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun FriendPickerDialog(
    eventId: String? = null,
    onDismiss: () -> Unit,
    onInvite: (UserProfile) -> Unit
) {
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(eventId) {
        if (currentUser != null) {
            SocialRepository.fetchAcceptedFriends(
                currentUserId = currentUser.uid,
                onSuccess = { allFriends ->
                    if (eventId != null) {
                        SocialRepository.fetchInvitedUserIds(eventId,
                            onSuccess = { invitedIds ->
                                friends = allFriends.filter { friend -> !invitedIds.contains(friend.uid) }
                                isLoading = false
                            },
                            onError = {
                                friends = allFriends
                                isLoading = false
                            }
                        )
                    } else {
                        friends = allFriends
                        isLoading = false
                    }
                },
                onError = { isLoading = false }
            )
        }
    }

    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite a Friend", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search friends...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoralRed)
                    }
                } else if (friends.isEmpty()) {
                    Text(if (eventId != null) "All eligible friends are already invited." else "You don't have any friends to invite.",
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else if (filteredFriends.isEmpty()) {
                    Text("No friends match \"$searchQuery\"", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(filteredFriends, key = { it.uid }) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onInvite(friend) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = friend.photoUrl)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = SocialRepository.displayNameOrEmail(friend),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = friend.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun buildSelectedDateEvents(
    events: List<CalendarEvent>,
    selectedDate: LocalDate
): List<CalendarEvent> {
    return events.filter { it.occursOn(selectedDate) }.sortedWith(
        compareByDescending<CalendarEvent> { it.isPinned }
            .thenByDescending { it.calendarId == "custom" }
            .thenBy { it.start }
    )
}

private fun buildWeekTitle(weekStart: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return "Week of ${weekStart.format(formatter)}"
}

private fun buildWeekDates(
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<LocalDate> {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return (0..6).map { weekStart.plusDays(it.toLong()) }
}

private fun buildOrderedDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    return (0..6).map { firstDayOfWeek.plus(it.toLong()) }
}

@Composable
private fun resolveDateCellBackgroundColor(isSelected: Boolean): Color {
    return if (isSelected) CardOffWhite else Color.White
}

@Composable
private fun resolveDateCellBorderColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else Color.Transparent
}

@Composable
private fun resolveDateCellTextColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
}

private fun buildDayEventInfoMap(events: List<CalendarEvent>, currentUserId: String?): Map<LocalDate, DayEventInfo> {
    val infoMap = mutableMapOf<LocalDate, DayEventInfo>()

    for (event in events) {
        var day = event.start.toLocalDate()
        val lastDay = event.lastDateInclusive()
        val isCustom = event.calendarId == "custom"
        val isPinned = event.isPinned
        val isShared = isCustom && (event.ownerId != currentUserId || event.isShared)

        while (!day.isAfter(lastDay)) {
            val current = infoMap[day] ?: DayEventInfo()
            infoMap[day] = current.copy(
                hasCsuci = current.hasCsuci || (event.calendarId != "custom"),
                hasCustom = current.hasCustom || (isCustom && !isShared),
                hasPinned = current.hasPinned || isPinned,
                hasShared = current.hasShared || isShared
            )
            day = day.plusDays(1)
        }
    }

    return infoMap
}

private fun buildMonthCells(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek
): List<LocalDate?> {
    val firstOfMonth = month.atDay(1)
    val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val result = mutableListOf<LocalDate?>()

    repeat(offset) {
        result.add(null)
    }

    repeat(month.lengthOfMonth()) { index ->
        result.add(month.atDay(index + 1))
    }

    while (result.size % 7 != 0) {
        result.add(null)
    }

    return result
}

@Composable
private fun EventBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EventMetaLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = SoftText
    )
}

@Composable
private fun EventDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun InviteDetailDialog(
    invite: EventInvite,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = CoralRed)
                Spacer(Modifier.width(12.dp))
                Text(invite.eventTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("From: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(invite.fromDisplayName, style = MaterialTheme.typography.bodyMedium, color = SharedEventBlue)
                }

                val start = ZonedDateTime.parse(invite.eventStart)
                val end = ZonedDateTime.parse(invite.eventEnd)
                val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text(start.format(dayFormatter), style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("${start.format(timeFormatter)} - ${end.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall)
                }

                if (!invite.eventLocation.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text(invite.eventLocation, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (!invite.eventDescription.isNullOrBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(invite.eventDescription, style = MaterialTheme.typography.bodyMedium)
                }

                if (invite.isPinnedByLeader) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = PinnedEventPurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Outlined.PushPin, contentDescription = null, tint = PinnedEventPurple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("The leader has this event pinned", style = MaterialTheme.typography.labelSmall, color = PinnedEventPurple, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)) {
                Text("Accept", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}
