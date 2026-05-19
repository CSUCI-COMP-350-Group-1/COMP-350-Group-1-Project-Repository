package com.example.cicompanion.calendar

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.data.repository.CourseCatalogRepository
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.social.EventInvite
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserProfile
import com.example.cicompanion.social.UserAvatar
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
import com.example.cicompanion.firebase.EventInviteNotificationSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToInvite by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventMembersToShow by remember { mutableStateOf<CalendarEvent?>(null) }
    var inviteToShowDetails by remember { mutableStateOf<EventInvite?>(null) }
    var eventToShowDetails by remember { mutableStateOf<CalendarEvent?>(null) }
    
    var classToEdit by remember { mutableStateOf<SelectedClass?>(null) }
    var classToDelete by remember { mutableStateOf<SelectedClass?>(null) }
    var classToShowDetails by remember { mutableStateOf<SelectedClass?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val repository = remember(context) { CourseCatalogRepository(context.applicationContext) }
    var majors by remember { mutableStateOf<List<CourseCatalogMajor>>(emptyList()) }

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    LaunchedEffect(Unit) {
        viewModel.loadEventNotificationPreferences()
        runCatching { repository.loadMajors() }.onSuccess { majors = it.sortedBy { m -> m.code } }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.loadSelectedClasses()
        }
    }

    // Optimization: Read from ViewModel's derived states to reduce recompositions in the main composable
    val selectedDateEvents by viewModel.selectedDateEventsByPriority
    val dayEventInfoMap by viewModel.dayEventInfoMap

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            if (currentUser != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        if (viewModel.mode == CalendarMode.SCHEDULE) showAddClassDialog = true
                        else showAddEventDialog = true
                    },
                    containerColor = CoralRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                    icon = {
                        val baseIcon = if (viewModel.mode == CalendarMode.SCHEDULE) Icons.Default.School else Icons.Default.CalendarMonth
                        Box(contentAlignment = Alignment.Center) {
                            Icon(baseIcon, contentDescription = null)
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
                    text = { 
                        Text(
                            text = if (viewModel.mode == CalendarMode.SCHEDULE) "Add Class" else "Add Event",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            CalendarScreenBody(
                viewModel = viewModel,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                onDismissError = viewModel::clearError,
                onDateSelected = {
                    viewModel.onDateSelected(it)
                    viewModel.setHighlightedEvent(null)
                },
                onEventClick = { eventToShowDetails = it },
                onInviteClick = { inviteToShowDetails = it },
                onClassClick = { classToShowDetails = it }
            )
        }
    }

    if (eventToShowDetails != null) {
        ModalBottomSheet(onDismissRequest = { eventToShowDetails = null }, sheetState = sheetState, containerColor = Color.White) {
            EventDetailsContent(
                event = eventToShowDetails!!,
                onDismiss = { eventToShowDetails = null },
                onDelete = { 
                    eventToDelete = eventToShowDetails
                    eventToShowDetails = null 
                },
                onEdit = { 
                    eventToEdit = eventToShowDetails
                    eventToShowDetails = null 
                },
                onTogglePin = { viewModel.togglePinEvent(eventToShowDetails!!) },
                onInvite = { 
                    eventToInvite = eventToShowDetails
                    // Reverting logic: open dialog instead of nested sheet to ensure it works
                },
                onShowMembers = { 
                    eventMembersToShow = eventToShowDetails
                },
                notificationsEnabled = viewModel.isEventNotificationEnabled(eventToShowDetails!!),
                onNotificationToggle = { enabled -> viewModel.setEventNotificationEnabled(eventToShowDetails!!, enabled) }
            )
        }
    }

    if (classToShowDetails != null) {
        ModalBottomSheet(onDismissRequest = { classToShowDetails = null }, sheetState = sheetState, containerColor = Color.White) {
            ClassDetailsContent(
                selectedClass = classToShowDetails!!,
                onEdit = { classToEdit = classToShowDetails; classToShowDetails = null },
                onDelete = { classToDelete = classToShowDetails; classToShowDetails = null }
            )
        }
    }

    if (showAddEventDialog) {
        ModalBottomSheet(onDismissRequest = { showAddEventDialog = false }, sheetState = sheetState, containerColor = Color.White) {
            AddEventContent(
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
                        isShared = invitedFriends.isNotEmpty()
                    )
                    viewModel.addCustomEvent(newEvent)
                    currentUser?.let { user ->
                        invitedFriends.forEach { friend ->
                            SocialRepository.sendEventInvite(user, friend.uid, newEvent, onSuccess = {
                                EventInviteNotificationSender.sendEventInviteNotification(friend.uid, user.displayName ?: user.email ?: "Someone", newEvent.title, newEvent.id)
                            }, onError = { })
                        }
                    }
                    showAddEventDialog = false
                }
            )
        }
    }

    if (showAddClassDialog) {
        ModalBottomSheet(onDismissRequest = { showAddClassDialog = false }, sheetState = sheetState, containerColor = Color.White) {
            AddClassContent(
                majors = majors,
                onDismiss = { showAddClassDialog = false },
                onConfirm = { selectedClass ->
                    viewModel.saveSelectedClass(selectedClass) { success ->
                        if (success) {
                            showAddClassDialog = false
                            Toast.makeText(context, "Class added!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    eventToEdit?.let { event ->
        ModalBottomSheet(onDismissRequest = { eventToEdit = null }, sheetState = sheetState, containerColor = Color.White) {
            EditEventContent(
                event = event,
                onDismiss = { eventToEdit = null },
                onConfirm = { title, description, location, startTime, endTime ->
                    val startZdt = ZonedDateTime.of(event.start.toLocalDate(), startTime, event.start.zone)
                    val endZdt = ZonedDateTime.of(event.start.toLocalDate(), endTime, event.endExclusive.zone)
                    viewModel.updateCustomEvent(event, event.copy(title = title, description = description, location = location, start = startZdt, endExclusive = endZdt))
                    eventToEdit = null
                }
            )
        }
    }

    classToEdit?.let { item ->
        ModalBottomSheet(onDismissRequest = { classToEdit = null }, sheetState = sheetState, containerColor = Color.White) {
            AddClassContent(
                majors = majors, 
                editingClass = item, 
                onDismiss = { classToEdit = null }, 
                onConfirm = { updatedClass -> 
                    viewModel.saveSelectedClass(updatedClass) { success -> if (success) classToEdit = null } 
                }
            )
        }
    }

    eventToDelete?.let { event ->
        val isOwner = event.ownerId == currentUser?.uid
        AlertDialog(onDismissRequest = { eventToDelete = null }, title = { Text(if (isOwner) "Delete Event" else "Leave Event") }, text = { Text(if (isOwner) "Delete this event for everyone?" else "Remove this event from your calendar?") }, confirmButton = { Button(onClick = { viewModel.deleteEvent(event); eventToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = CoralRed)) { Text(if (isOwner) "Delete" else "Leave") } }, dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Cancel", color = Color.Gray) } })
    }

    classToDelete?.let { item ->
        AlertDialog(onDismissRequest = { classToDelete = null }, title = { Text("Remove Class") }, text = { Text("Are you sure you want to remove ${item.courseCode}?") }, confirmButton = { Button(onClick = { viewModel.deleteSelectedClass(item.id) { success -> if (success) classToDelete = null } }, colors = ButtonDefaults.buttonColors(containerColor = CoralRed)) { Text("Remove") } }, dismissButton = { TextButton(onClick = { classToDelete = null }) { Text("Cancel", color = Color.Gray) } })
    }
    
    // REVERT: Use Dialogs for Invite and Members to ensure they work over sheets
    eventToInvite?.let { event ->
        FriendPickerDialog(eventId = event.id, onDismiss = { eventToInvite = null }, onInvite = { friend ->
            currentUser?.let { user ->
                SocialRepository.sendEventInvite(user, friend.uid, event, onSuccess = {
                    EventInviteNotificationSender.sendEventInviteNotification(friend.uid, user.displayName ?: user.email ?: "Someone", event.title, event.id)
                    Toast.makeText(context, "Invite sent!", Toast.LENGTH_SHORT).show()
                    eventToInvite = null
                }, onError = { })
            }
        })
    }

    eventMembersToShow?.let { event ->
        EventMembersDialog(event = event, currentUserId = currentUser?.uid ?: "", onDismiss = { eventMembersToShow = null }, onKick = { targetUids ->
            viewModel.kickUsers(event.id, targetUids)
            eventMembersToShow = null
        })
    }

    inviteToShowDetails?.let { invite ->
        InviteDetailDialog(invite = invite, onDismiss = { inviteToShowDetails = null }, onAccept = { viewModel.acceptInvite(invite); inviteToShowDetails = null }, onDecline = { viewModel.declineInvite(invite); inviteToShowDetails = null })
    }
}

@Composable
private fun CalendarScreenBody(
    viewModel: CalendarViewModel,
    selectedDateEvents: List<CalendarEvent>,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    onDismissError: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onInviteClick: (EventInvite) -> Unit,
    onClassClick: (SelectedClass) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CalendarHeroHeader(
            title = if (viewModel.mode == CalendarMode.SCHEDULE) "Class Schedule" else "CI Companion Calendar",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            trailingContent = { if (viewModel.mode != CalendarMode.SCHEDULE) FilterDropdown(viewModel = viewModel) }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (viewModel.mode != CalendarMode.SCHEDULE && viewModel.incomingInvites.isNotEmpty()) {
                IncomingInvitesSection(invites = viewModel.incomingInvites, onAccept = viewModel::acceptInvite, onDecline = viewModel::declineInvite, onInviteClick = onInviteClick)
            }

            viewModel.errorMessage?.let { ErrorMessage(message = it, onDismiss = onDismissError) }

            CalendarModeTabs(selectedMode = viewModel.mode, onModeSelected = viewModel::updateMode, modifier = Modifier.fillMaxWidth())

            CalendarContent(
                mode = viewModel.mode,
                visibleMonth = viewModel.visibleMonth,
                selectedDate = viewModel.selectedDate,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                selectedClasses = viewModel.selectedClasses,
                onDateSelected = onDateSelected,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek = viewModel::nextWeek,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onEventClick = onEventClick,
                onClassClick = onClassClick,
                highlightedEventId = viewModel.highlightedEventId
            )
        }
    }
}

@Composable
private fun CalendarContent(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateEvents: List<CalendarEvent>,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    selectedClasses: List<SelectedClass>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onClassClick: (SelectedClass) -> Unit,
    highlightedEventId: String? = null
) {
    when (mode) {
        CalendarMode.MONTH -> MonthView(visibleMonth, selectedDate, dayEventInfoMap, selectedDateEvents, onDateSelected, onPreviousMonth, onNextMonth, onEventClick, highlightedEventId)
        CalendarMode.DAY -> DayView(selectedDate, selectedDateEvents, onPreviousDay, onNextDay, onEventClick, highlightedEventId)
        CalendarMode.WEEK -> WeekView(selectedDate, dayEventInfoMap, selectedDateEvents, onDateSelected, onPreviousWeek, onNextWeek, onEventClick, highlightedEventId)
        CalendarMode.SCHEDULE -> ScheduleListView(selectedClasses, onClassClick)
    }
}

@Composable
private fun ScheduleListView(classes: List<SelectedClass>, onClassClick: (SelectedClass) -> Unit) {
    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No classes added yet. Tap '+' to start.", color = Color.Gray)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            classes.forEach { savedClass -> SavedClassCard(selectedClass = savedClass, onClick = { onClassClick(savedClass) }) }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CalendarModeTabs(selectedMode: CalendarMode, onModeSelected: (CalendarMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarMode.entries.forEach { mode ->
            val isSelected = selectedMode == mode
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) CoralRed else Color.White).border(1.dp, if (isSelected) Color.Transparent else DateCellBorder, RoundedCornerShape(12.dp)).clickable { onModeSelected(mode) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(text = when(mode) { CalendarMode.MONTH -> "Month"; CalendarMode.WEEK -> "Week"; CalendarMode.DAY -> "Day"; CalendarMode.SCHEDULE -> "Class" }, color = if (isSelected) Color.White else CoralRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MonthView(visibleMonth: YearMonth, selectedDate: LocalDate, dayEventInfoMap: Map<LocalDate, DayEventInfo>, selectedDateEvents: List<CalendarEvent>, onDateSelected: (LocalDate) -> Unit, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onEventClick: (CalendarEvent) -> Unit, highlightedEventId: String? = null) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val monthCells = remember(visibleMonth, firstDayOfWeek) { buildMonthCells(visibleMonth, firstDayOfWeek) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard { HeaderWithArrows(title = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), onPrevious = onPreviousMonth, onNext = onNextMonth); Spacer(modifier = Modifier.height(8.dp)); WeekdayHeader(firstDayOfWeek); Spacer(modifier = Modifier.height(8.dp)); MonthGrid(monthCells, selectedDate, dayEventInfoMap, onDateSelected) }
        EventsSection("Selected day", selectedDateEvents, onEventClick, highlightedEventId)
    }
}

@Composable
private fun DayView(selectedDate: LocalDate, events: List<CalendarEvent>, onPreviousDay: () -> Unit, onNextDay: () -> Unit, onEventClick: (CalendarEvent) -> Unit, highlightedEventId: String? = null) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard { HeaderWithArrows(selectedDate.format(formatter), onPreviousDay, onNextDay) }
        EventsSection("Events", events, onEventClick, highlightedEventId)
    }
}

@Composable
private fun WeekView(selectedDate: LocalDate, dayEventInfoMap: Map<LocalDate, DayEventInfo>, eventsForSelectedDate: List<CalendarEvent>, onDateSelected: (LocalDate) -> Unit, onPreviousWeek: () -> Unit, onNextWeek: () -> Unit, onEventClick: (CalendarEvent) -> Unit, highlightedEventId: String? = null) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val weekDates = remember(selectedDate, firstDayOfWeek) { buildWeekDates(selectedDate, firstDayOfWeek) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard { HeaderWithArrows(buildWeekTitle(weekDates.first()), onPreviousWeek, onNextWeek); Spacer(modifier = Modifier.height(8.dp)); WeekdayHeader(firstDayOfWeek); Spacer(modifier = Modifier.height(8.dp)); WeekRow(weekDates, selectedDate, dayEventInfoMap, onDateSelected) }
        EventsSection("Selected day", eventsForSelectedDate, onEventClick, highlightedEventId)
    }
}

@Composable private fun HeaderWithArrows(title: String, onPrevious: () -> Unit, onNext: () -> Unit) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { NavigationCircleButton("‹", onPrevious); Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp); NavigationCircleButton("›", onNext) } }
@Composable private fun NavigationCircleButton(symbol: String, onClick: () -> Unit) { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(DateCellWhite).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(text = symbol, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) { val orderedDays = (0..6).map { firstDayOfWeek.plus(it.toLong()) }; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { orderedDays.forEach { day -> Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(DateCellWhite).border(1.dp, DateCellBorder, RoundedCornerShape(8.dp)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) } } } }
@Composable private fun WeekRow(dates: List<LocalDate>, selectedDate: LocalDate, dayEventInfoMap: Map<LocalDate, DayEventInfo>, onDateSelected: (LocalDate) -> Unit) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { dates.forEach { date -> DateCell(Modifier.weight(1f), date, date == selectedDate, dayEventInfoMap[date] ?: DayEventInfo(), { onDateSelected(date) }) } } }
@Composable private fun MonthGrid(cells: List<LocalDate?>, selectedDate: LocalDate, infoMap: Map<LocalDate, DayEventInfo>, onDateSelected: (LocalDate) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { cells.chunked(7).forEach { week -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { week.forEach { date -> if (date == null) Box(Modifier.weight(1f).height(48.dp)) else DateCell(Modifier.weight(1f), date, date == selectedDate, infoMap[date] ?: DayEventInfo(), { onDateSelected(date) }) } } } } }
@Composable private fun DateCell(modifier: Modifier, date: LocalDate, isSelected: Boolean, info: DayEventInfo, onClick: () -> Unit) { Box(modifier.height(52.dp).background(if (isSelected) Color(0xFFF6E6D8) else Color.White, RoundedCornerShape(6.dp)).border(1.dp, if (isSelected) CoralRed else Color.Transparent, RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(2.dp)) { Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) { Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp); Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(1.dp)) { Dot(CoralRed, info.hasCsuci); Dot(PinnedEventPurple, info.hasPinned); Dot(CustomEventOrange, info.hasCustom); Dot(SharedEventBlue, info.hasShared) } } } }
@Composable private fun Dot(color: Color, visible: Boolean) { if (visible) Box(Modifier.size(4.dp).background(color, CircleShape)) }
@Composable private fun EventsSection(title: String, events: List<CalendarEvent>, onEventClick: (CalendarEvent) -> Unit, highlightedEventId: String? = null) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SectionHeading(title); if (events.isEmpty()) SectionCard { Text("No events for this date.", color = SoftText) } else events.forEach { event -> EventCard(event, { onEventClick(event) }, event.id == highlightedEventId) } } }
@Composable private fun EventCard(event: CalendarEvent, onClick: () -> Unit, isHighlighted: Boolean) { val isCustom = event.calendarId == "custom"; val isOwner = event.ownerId == FirebaseAuth.getInstance().currentUser?.uid; Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(if (isHighlighted) 1.5.dp else 0.dp, if (isHighlighted) CoralRed else Color.Transparent, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (!isCustom) EventCardGrey else if (isOwner && event.isShared) Color(0xFFE3F2FD) else if (isOwner) Color(0xFFF6E6D8) else Color(0xFFE3F2FD))) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Column(modifier = Modifier.weight(1f)) { Text(HtmlUtils.stripHtml(event.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1); Text(event.timeLabel(), style = MaterialTheme.typography.bodySmall, color = SoftText) }; EventBadge(if (event.isPinned) "Pinned" else if (isCustom) (if (isOwner) (if (event.isShared) "Shared" else "Custom") else "Shared") else "CSUCI", if (event.isPinned) PinnedEventPurple else if (isCustom) (if (isOwner) (if (event.isShared) SharedEventBlue else CustomEventOrange) else SharedEventBlue) else CoralRed); Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) } } }
@Composable private fun EventBadge(text: String, color: Color) { Box(Modifier.padding(start = 4.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) } }
@Composable private fun FilterDropdown(viewModel: CalendarViewModel) { var exp by remember { mutableStateOf(false) }; Box { IconButton(onClick = { exp = true }, modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) { Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(18.dp)) }; DropdownMenu(expanded = exp, onDismissRequest = { exp = false }, modifier = Modifier.background(Color.White).width(200.dp)) { Text("Filter Events", Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); FilterMenuItem("CSUCI Events", viewModel.filterCsuci, CoralRed) { viewModel.toggleFilterCsuci() }; FilterMenuItem("Custom Events", viewModel.filterCustom, CustomEventOrange) { viewModel.toggleFilterCustom() }; FilterMenuItem("Pinned Events", viewModel.filterPinned, PinnedEventPurple) { viewModel.toggleFilterPinned() }; FilterMenuItem("Shared Events", viewModel.filterShared, SharedEventBlue) { viewModel.toggleFilterShared() } } } }
@Composable private fun FilterMenuItem(label: String, isSel: Boolean, color: Color, onClick: () -> Unit) { DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(if (isSel) color else Color.Transparent).border(1.dp, color, RoundedCornerShape(4.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { if (isSel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }; Text(label, style = MaterialTheme.typography.bodyMedium) } }, onClick = onClick) }
@Composable private fun ErrorMessage(message: String, onDismiss: () -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall); Text("Dismiss", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp)) } } }

@Composable
private fun EventDetailsContent(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onInvite: () -> Unit,
    onShowMembers: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit
) {
    val isCustom = event.calendarId == "custom"
    val isOwner = event.ownerId == FirebaseAuth.getInstance().currentUser?.uid
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HtmlUtils.stripHtml(event.title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isCustom && (!isOwner || event.isShared)) {
                    Text(
                        text = if (isOwner) "Shared Event (Leader)" else "Shared Event",
                        color = SharedEventBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (isCustom) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (event.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (event.isPinned) PinnedEventPurple else Color.Gray
                    )
                }
            }
        }
        DetailRow(Icons.Outlined.Schedule, event.timeLabel())
        event.location?.let { DetailRow(Icons.Outlined.LocationOn, HtmlUtils.stripHtml(it)) }
        event.description?.let {
            HorizontalDivider()
            Text(HtmlUtils.stripHtml(it))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray.copy(alpha = 0.05f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(20.dp), CoralRed)
                Text("Reminders")
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = CoralRed)
            )
        }
        if (isCustom) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onShowMembers, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Members")
                }
                if (isOwner) {
                    Button(
                        onClick = onInvite,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SharedEventBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Invite")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwner) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Edit")
                    }
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isOwner) "Delete" else "Leave")
                }
            }
        }
    }
}

private fun buildMonthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> { val firstOfMonth = month.atDay(1); val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7; val res = mutableListOf<LocalDate?>(); repeat(offset) { res.add(null) }; repeat(month.lengthOfMonth()) { res.add(month.atDay(it + 1)) }; while (res.size % 7 != 0) res.add(null); return res }
private fun buildWeekTitle(weekStart: LocalDate): String = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
private fun buildWeekDates(selectedDate: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> { val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)); return (0..6).map { weekStart.plusDays(it.toLong()) } }
