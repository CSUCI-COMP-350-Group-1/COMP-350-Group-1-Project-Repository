package com.example.cicompanion.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.ui.theme.BrandRedLighter
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
private val CustomEventOrange = Color(0xFFFF9800)
private val PinnedEventPurple = Color(0xFF9C27B0)

data class DayEventInfo(
    val hasCsuci: Boolean = false, 
    val hasCustom: Boolean = false,
    val hasPinned: Boolean = false
)

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }
    val scrollState = rememberScrollState()

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

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
        buildDayEventInfoMap(viewModel.events)
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
                onTogglePin = viewModel::togglePinEvent
            )
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            selectedDate = viewModel.selectedDate,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title: String, description: String, location: String, startTime: LocalTime, endTime: LocalTime ->
                val startZdt = ZonedDateTime.of(viewModel.selectedDate, startTime, ZonedDateTime.now().zone)
                val endZdt = ZonedDateTime.of(viewModel.selectedDate, endTime, ZonedDateTime.now().zone)
                val newEvent = CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    calendarId = "custom",
                    title = title,
                    description = description,
                    location = location,
                    htmlLink = null,
                    start = startZdt,
                    endExclusive = endZdt,
                    isAllDay = false
                )
                viewModel.addCustomEvent(newEvent)
                showAddEventDialog = false
            }
        )
    }

    eventToEdit?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, description, location, startTime, endTime ->
                val startZdt = ZonedDateTime.of(event.start.toLocalDate(), startTime, event.start.zone)
                val endZdt = ZonedDateTime.of(event.start.toLocalDate(), endTime, event.endExclusive?.zone ?: event.start.zone)
                val updatedEvent = event.copy(
                    title = title,
                    description = description,
                    location = location,
                    start = startZdt,
                    endExclusive = endZdt
                )
                viewModel.addCustomEvent(updatedEvent) // saveCustomEvent works for updates too
                eventToEdit = null
            }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Event") },
            text = { Text("Do you really want to delete this event?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomEvent(event.id)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
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
    onTogglePin: (CalendarEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(0.dp)
    ) {
        CalendarHeroHeader(
            selectedDate = viewModel.selectedDate,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                highlightedEventId = viewModel.highlightedEventId
            )
        }
    }
}

@Composable
private fun CalendarHeroHeader(
    selectedDate: LocalDate,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandRedLighter, BrandRedDark)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CI Companion Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            FilterDropdown(viewModel = viewModel)
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
    val accentColor = modeAccentColor()
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
            highlightedEventId = highlightedEventId
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
    highlightedEventId: String? = null
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
            highlightedEventId = highlightedEventId
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
    highlightedEventId: String? = null
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
            highlightedEventId = highlightedEventId
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
    highlightedEventId: String? = null
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
            highlightedEventId = highlightedEventId
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (info.hasCsuci) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(8.dp)
                    .background(CoralRed, CircleShape)
            )
        }
        if (info.hasPinned) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(8.dp)
                    .background(PinnedEventPurple, CircleShape)
            )
        }
        if (info.hasCustom) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(CustomEventOrange, CircleShape)
            )
        }
    }
}

@Composable
private fun EventsSection(
    title: String,
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(text = title)
        EventsList(
            events = events, 
            onDeleteEvent = onDeleteEvent, 
            onEditEvent = onEditEvent,
            onTogglePin = onTogglePin,
            highlightedEventId = highlightedEventId
        )
    }
}

@Composable
private fun EventsList(
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onTogglePin: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null
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
    isHighlighted: Boolean = false
) {
    val isCustom = event.calendarId == "custom"

    // Removed the purple highlight when navigating from pinned events.
    // Using normal CardOffWhite and no border.
    val containerColor = CardOffWhite
    val borderColor = Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.dp, borderColor, RoundedCornerShape(22.dp)),
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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isCustom) {
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
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CoralRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    EventBadge(
                        text = if (event.isPinned) "Pinned" else if (isCustom) "Custom" else "CSUCI",
                        color = if (event.isPinned) PinnedEventPurple else if (isCustom) CustomEventOrange else CoralRed
                    )
                }
            }

            EventMetaLine(text = event.timeLabel())
            event.location?.let { EventMetaLine(text = "Location: ${HtmlUtils.stripHtml(it)}") }
            event.description?.let { EventDescription(text = HtmlUtils.stripHtml(it)) }
        }
    }
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

private fun modeAccentColor(): Color = CoralRed

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

private fun buildDayEventInfoMap(events: List<CalendarEvent>): Map<LocalDate, DayEventInfo> {
    val infoMap = mutableMapOf<LocalDate, DayEventInfo>()

    for (event in events) {
        var day = event.start.toLocalDate()
        val lastDay = event.lastDateInclusive()
        val isCustom = event.calendarId == "custom"
        val isPinned = event.isPinned

        while (!day.isAfter(lastDay)) {
            val current = infoMap[day] ?: DayEventInfo()
            infoMap[day] = current.copy(
                hasCsuci = current.hasCsuci || (event.calendarId != "custom"),
                hasCustom = current.hasCustom || isCustom,
                hasPinned = current.hasPinned || isPinned
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
private fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { 
            Text(
                text = "Add Event",
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

                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Start Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    WheelTimePicker(initialTime = startTime, onTimeChange = { startTime = it })
                    
                    Text("End Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    WheelTimePicker(initialTime = endTime, onTimeChange = { endTime = it })
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
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var startTime by remember { mutableStateOf(event.start.toLocalTime()) }
    var endTime by remember { mutableStateOf(event.endExclusive?.toLocalTime() ?: event.start.toLocalTime().plusHours(1)) }

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
                    Text("Start Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    WheelTimePicker(initialTime = startTime, onTimeChange = { startTime = it })
                    
                    Text("End Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    WheelTimePicker(initialTime = endTime, onTimeChange = { endTime = it })
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
fun WheelTimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    var hour by remember { mutableStateOf(if (initialTime.hour == 0) 12 else if (initialTime.hour > 12) initialTime.hour - 12 else initialTime.hour) }
    var minute by remember { mutableStateOf(initialTime.minute) }
    var amPm by remember { mutableStateOf(if (initialTime.hour < 12) "AM" else "PM") }

    LaunchedEffect(hour, minute, amPm) {
        val h = when {
            amPm == "AM" && hour == 12 -> 0
            amPm == "AM" -> hour
            amPm == "PM" && hour == 12 -> 12
            else -> hour + 12
        }
        onTimeChange(LocalTime.of(h, minute))
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = (1..12).toList(),
            initialIndex = hour - 1,
            onItemSelected = { hour = it },
            modifier = Modifier.weight(1f)
        )
        Text(":", style = MaterialTheme.typography.headlineMedium)
        WheelPicker(
            items = (0..59).toList(),
            initialIndex = minute,
            onItemSelected = { minute = it },
            format = { String.format(Locale.US, "%02d", it) },
            modifier = Modifier.weight(1f)
        )
        WheelPicker(
            items = listOf("AM", "PM"),
            initialIndex = if (amPm == "AM") 0 else if (amPm == "PM") 1 else 0,
            onItemSelected = { amPm = it },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    format: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in items.indices) {
                onItemSelected(items[centerIndex])
            }
        }
    }

    Box(modifier = modifier.height(itemHeight * 3), contentAlignment = Alignment.Center) {
        // Overlay for selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(CoralRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(items[index]),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (listState.firstVisibleItemIndex == index) CoralRed else Color.Gray
                    )
                }
            }
        }
    }
}
