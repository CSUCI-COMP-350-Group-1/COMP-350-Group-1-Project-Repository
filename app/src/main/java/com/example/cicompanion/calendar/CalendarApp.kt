package com.example.cicompanion.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
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
private val SoftBorder = Color(0xFFE7C2B8)
private val SelectedDateFill = Color(0xFFFFE2E7)
private val CardOffWhite = Color(0xFFF6E6D8)
private val DateCellWhite = Color(0xFFF7F4F8)
private val DateCellBorder = Color(0xFFE2BFB7)
private val CustomEventOrange = Color(0xFFFF9800)

data class DayEventInfo(val hasCsuci: Boolean = false, val hasCustom: Boolean = false)

/** Shows the full calendar screen and routes state into smaller UI pieces. */
@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    val selectedDateEvents = remember(viewModel.events, viewModel.selectedDate) {
        buildSelectedDateEvents(viewModel.events, viewModel.selectedDate)
    }

    val dayEventInfoMap = remember(viewModel.events) {
        buildDayEventInfoMap(viewModel.events)
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
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
    ) { padding ->
        PullToRefreshContainer(
            isRefreshing = viewModel.isLoading,
            onRefresh = viewModel::loadOnlineCalendar,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CalendarScreenBody(
                viewModel = viewModel,
                mode = viewModel.mode,
                filter = viewModel.filter,
                visibleMonth = viewModel.visibleMonth,
                selectedDate = viewModel.selectedDate,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                errorMessage = viewModel.errorMessage,
                onDismissError = viewModel::clearError,
                onModeSelected = viewModel::updateMode,
                onFilterSelected = viewModel::updateFilter,
                onDateSelected = viewModel::onDateSelected,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek = viewModel::nextWeek,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onRequestDelete = { eventToDelete = it }
            )
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            selectedDate = viewModel.selectedDate,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, description, location, startTime, endTime ->
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

    // Deletion Confirmation Dialog
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
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    var startHour by remember { mutableStateOf("9") }
    var startMin by remember { mutableStateOf("00") }
    var startIsAm by remember { mutableStateOf(true) }
    
    var endHour by remember { mutableStateOf("10") }
    var endMin by remember { mutableStateOf("00") }
    var endIsAm by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Event for ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true
                )
                
                Text("Start Time", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startHour, onValueChange = { startHour = it }, label = { Text("HH") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = startMin, onValueChange = { startMin = it }, label = { Text("MM") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    AmPmToggle(isAm = startIsAm, onToggle = { startIsAm = it })
                }
                
                Text("End Time", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = endHour, onValueChange = { endHour = it }, label = { Text("HH") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = endMin, onValueChange = { endMin = it }, label = { Text("MM") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    AmPmToggle(isAm = endIsAm, onToggle = { endIsAm = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sHour = (startHour.toIntOrNull() ?: 9).let { 
                    if (!startIsAm && it < 12) it + 12 else if (startIsAm && it == 12) 0 else it 
                }
                val eHour = (endHour.toIntOrNull() ?: 10).let { 
                    if (!endIsAm && it < 12) it + 12 else if (endIsAm && it == 12) 0 else it 
                }
                
                val startTime = LocalTime.of(sHour % 24, startMin.toIntOrNull() ?: 0)
                val endTime = LocalTime.of(eHour % 24, endMin.toIntOrNull() ?: 0)
                onConfirm(title, description, location, startTime, endTime)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AmPmToggle(isAm: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DateCellWhite)
            .border(1.dp, DateCellBorder, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .clickable { onToggle(true) }
                .background(if (isAm) CoralRed else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AM",
                color = if (isAm) Color.White else SoftText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .clickable { onToggle(false) }
                .background(if (!isAm) CoralRed else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PM",
                color = if (!isAm) Color.White else SoftText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Shows the scrollable page content that sits inside pull-to-refresh. */
@Composable
private fun CalendarScreenBody(
    viewModel: CalendarViewModel,
    mode: CalendarMode,
    filter: EventFilter,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateEvents: List<CalendarEvent>,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onModeSelected: (CalendarMode) -> Unit,
    onFilterSelected: (EventFilter) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRequestDelete: (CalendarEvent) -> Unit
) {
    ScrollablePage(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        CalendarHeroHeader(
            selectedDate = selectedDate,
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
            errorMessage?.let { message ->
                ErrorMessage(
                    message = message,
                    onDismiss = onDismissError
                )
            }

            SectionCard {
                SectionHeading(text = "View")
                Spacer(modifier = Modifier.height(12.dp))
                CalendarModeTabs(
                    selectedMode = mode,
                    onModeSelected = onModeSelected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeading(text = "Filter")
                Spacer(modifier = Modifier.height(12.dp))
                CalendarFilterTabs(
                    selectedFilter = filter,
                    onFilterSelected = onFilterSelected
                )
            }

            CalendarContent(
                viewModel = viewModel,
                mode = mode,
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                selectedDateEvents = selectedDateEvents,
                dayEventInfoMap = dayEventInfoMap,
                onDateSelected = onDateSelected,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDeleteEvent = onRequestDelete
            )
        }
    }
}

@Composable
private fun CalendarFilterTabs(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EventFilter.entries.forEach { filter ->
            FilterCard(
                filter = filter,
                isSelected = selectedFilter == filter,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterCard(
    filter: EventFilter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) CoralRed else DateCellWhite
    val textColor = if (isSelected) Color.White else CoralRed
    val borderColor = if (isSelected) Color.Transparent else DateCellBorder

    Box(
        modifier = modifier
            .shadow(elevation = if (isSelected) 6.dp else 0.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = filter.name,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/** Shows the large top header that sets the new visual style for the screen. */
@Composable
private fun CalendarHeroHeader(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(CoralRed, HotPink)
                )
            )
            .padding(20.dp)
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
    }
}

/** Shows the current error and a button that clears it. */
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

/** Shows the Day, Week, and Month tabs as rounded cards. */
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

/** Shows one clickable card used to change the current calendar mode. */
@Composable
private fun ModeCard(
    mode: CalendarMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = modeAccentColor(mode)
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

/** Chooses which calendar layout to show for the current mode. */
@Composable
private fun CalendarContent(
    viewModel: CalendarViewModel,
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
    onDeleteEvent: (CalendarEvent) -> Unit
) {
    when (mode) {
        CalendarMode.DAY -> DayView(
            selectedDate = selectedDate,
            events = selectedDateEvents,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onDeleteEvent = onDeleteEvent
        )

        CalendarMode.WEEK -> WeekView(
            selectedDate = selectedDate,
            dayEventInfoMap = dayEventInfoMap,
            eventsForSelectedDate = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onDeleteEvent = onDeleteEvent
        )

        CalendarMode.MONTH -> MonthView(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            dayEventInfoMap = dayEventInfoMap,
            selectedDateEvents = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDeleteEvent = onDeleteEvent
        )
    }
}

/** Shows the selected day and the events that fall on that day. */
@Composable
private fun DayView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
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
            onDeleteEvent = onDeleteEvent
        )
    }
}

/** Shows the current week grid and the events for the selected day. */
@Composable
private fun WeekView(
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    eventsForSelectedDate: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
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
            onDeleteEvent = onDeleteEvent
        )
    }
}

/** Shows the current month grid and the events for the selected day. */
@Composable
private fun MonthView(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    dayEventInfoMap: Map<LocalDate, DayEventInfo>,
    selectedDateEvents: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
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
            onDeleteEvent = onDeleteEvent
        )
    }
}

/** Wraps content in the rounded white card style used throughout the screen. */
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

/** Shows a section title in the calendar body. */
@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

/** Shows a title with left and right navigation buttons. */
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

/** Shows one circular previous or next button. */
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

/** Shows the weekday names in the current locale order. */
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

/** Shows one small label for a weekday in the calendar grid header. */
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

/** Shows one row of seven dates for the week view. */
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

/** Shows all rows needed for the month grid. */
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

/** Shows one empty placeholder cell in the month grid. */
@Composable
private fun EmptyDateCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(68.dp)
            .padding(2.dp)
    )
}

/** Shows one clickable date cell with a day number and event count. */
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
                info = eventInfo,
                isSelected = isSelected
            )
        }
    }
}

/** Shows colorful dots for events instead of text. */
@Composable
private fun EventDots(
    info: DayEventInfo,
    isSelected: Boolean
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
        if (info.hasCustom) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(CustomEventOrange, CircleShape)
            )
        }
    }
}

/** Shows a section title and the list of events below it. */
@Composable
private fun EventsSection(
    title: String,
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(text = title)
        EventsList(events = events, onDeleteEvent = onDeleteEvent)
    }
}

/** Shows either an empty state or the full list of event cards. */
@Composable
private fun EventsList(events: List<CalendarEvent>, onDeleteEvent: (CalendarEvent) -> Unit) {
    if (events.isEmpty()) {
        EmptyEventsMessage()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        events.forEach { event ->
            EventCard(event = event, onDelete = { onDeleteEvent(event) })
        }
    }
}

/** Shows the message used when no events are available for the selected day. */
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

/** Shows one event card. */
@Composable
private fun EventCard(event: CalendarEvent, onDelete: () -> Unit) {
    val displayTitle = formatEventTextForDisplay(event.title)
    val displayLocation = event.location?.let(::formatEventTextForDisplay)
    val displayDescription = event.description?.let(::formatEventTextForDisplay)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventCardHeader(title = displayTitle, isCustom = event.calendarId == "custom", onDelete = onDelete)
            EventMetaLine(text = event.timeLabel())
            displayLocation?.let { EventMetaLine(text = "Location: $it") }
            displayDescription?.let { EventDescription(text = it) }
        }
    }
}

/** Shows the top row of an event card. */
@Composable
private fun EventCardHeader(title: String, isCustom: Boolean, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCustom) {
                Surface(
                    onClick = onDelete,
                    color = Color.White,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CoralRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete Event",
                            color = CoralRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            EventBadge(
                text = if (isCustom) "Custom" else "CSUCI",
                color = if (isCustom) CustomEventOrange else CoralRed
            )
        }
    }
}

/** Shows the small badge on the right side of an event card. */
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Shows one short line of metadata inside an event card. */
@Composable
private fun EventMetaLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = SoftText
    )
}

/** Shows the longer description text inside an event card. */
@Composable
private fun EventDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Returns only the events that overlap the selected date. */
private fun buildSelectedDateEvents(
    events: List<CalendarEvent>,
    selectedDate: LocalDate
): List<CalendarEvent> {
    return events.filter { it.occursOn(selectedDate) }.sortedWith(
        compareByDescending<CalendarEvent> { it.calendarId == "custom" }
            .thenBy { it.start }
    )
}

/** Builds the title used in week view. */
private fun buildWeekTitle(weekStart: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return "Week of ${weekStart.format(formatter)}"
}

/** Builds the seven dates that belong to the selected week. */
private fun buildWeekDates(
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<LocalDate> {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return (0..6).map { weekStart.plusDays(it.toLong()) }
}

/** Builds the ordered list of weekday names used in calendar headers. */
private fun buildOrderedDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    return (0..6).map { firstDayOfWeek.plus(it.toLong()) }
}

/** Chooses the accent color for each calendar mode card. */
private fun modeAccentColor(mode: CalendarMode): Color {
    return when (mode) {
        CalendarMode.DAY -> CoralRed
        CalendarMode.WEEK -> CoralRed
        CalendarMode.MONTH -> CoralRed
    }
}

/** Chooses the background color for a date cell. */
@Composable
private fun resolveDateCellBackgroundColor(isSelected: Boolean): Color {
    return if (isSelected) CardOffWhite else Color.White
}

/** Chooses the border color for a date cell. */
@Composable
private fun resolveDateCellBorderColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else SoftBorder
}

/** Chooses the main text color for a date cell. */
@Composable
private fun resolveDateCellTextColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
}

/** Chooses the badge fill color used by the event count label. */
@Composable
private fun resolveEventCountBadgeColor(isSelected: Boolean): Color {
    return if (isSelected) Color.White.copy(alpha = 0.22f) else SelectedDateFill
}

/** Chooses the badge text color used by the event count label. */
@Composable
private fun resolveEventCountTextColor(isSelected: Boolean): Color {
    return if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
}

/** Builds the day event info map. */
private fun buildDayEventInfoMap(events: List<CalendarEvent>): Map<LocalDate, DayEventInfo> {
    val infoMap = mutableMapOf<LocalDate, DayEventInfo>()

    for (event in events) {
        var day = event.start.toLocalDate()
        val lastDay = event.lastDateInclusive()
        val isCustom = event.calendarId == "custom"

        while (!day.isAfter(lastDay)) {
            val current = infoMap[day] ?: DayEventInfo()
            infoMap[day] = if (isCustom) {
                current.copy(hasCustom = true)
            } else {
                current.copy(hasCsuci = true)
            }
            day = day.plusDays(1)
        }
    }

    return infoMap
}

/** Builds the month grid with leading and trailing empty cells. */
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