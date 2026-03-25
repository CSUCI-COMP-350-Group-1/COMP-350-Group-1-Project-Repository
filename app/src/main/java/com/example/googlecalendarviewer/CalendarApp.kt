package com.example.googlecalendarviewer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.googlecalendarviewer.CalendarMode
import com.example.googlecalendarviewer.CalendarViewModel
import com.example.googlecalendarviewer.model.CalendarEvent
import com.example.googlecalendarviewer.model.EventDraft
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarApp(
    viewModel: CalendarViewModel,
    onConnectCalendar: () -> Unit
) {
    val selectedDateEvents = remember(viewModel.events, viewModel.selectedDate) {
        viewModel.events
            .filter { it.occursOn(viewModel.selectedDate) }
            .sortedBy { it.start }
    }

    val eventCountByDate = remember(viewModel.events) {
        buildEventCountMap(viewModel.events)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Google Calendar Viewer",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.accessToken == null) {
                Button(onClick = onConnectCalendar) {
                    Text("Connect Google Calendar")
                }
            } else {
                Row {
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Refresh")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.openCreateDialog() }) {
                        Text("New Event")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Calendar: ${viewModel.activeCalendarTitle}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading…")
            }

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = viewModel.mode.ordinal) {
                CalendarMode.entries.forEach { tab ->
                    Tab(
                        selected = viewModel.mode == tab,
                        onClick = { viewModel.updateMode(tab) },
                        text = { Text(tab.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (viewModel.mode) {
                CalendarMode.DAY -> DayView(
                    selectedDate = viewModel.selectedDate,
                    events = selectedDateEvents,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay
                )

                CalendarMode.WEEK -> WeekView(
                    selectedDate = viewModel.selectedDate,
                    eventCountByDate = eventCountByDate,
                    eventsForSelectedDate = selectedDateEvents,
                    onDateSelected = viewModel::onDateSelected,
                    onPreviousWeek = viewModel::previousWeek,
                    onNextWeek = viewModel::nextWeek
                )

                CalendarMode.MONTH -> MonthView(
                    visibleMonth = viewModel.visibleMonth,
                    selectedDate = viewModel.selectedDate,
                    eventCountByDate = eventCountByDate,
                    selectedDateEvents = selectedDateEvents,
                    onDateSelected = viewModel::onDateSelected,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth
                )
            }
        }
    }

    if (viewModel.showCreateDialog) {
        CreateEventDialog(
            initialDate = viewModel.selectedDate,
            onDismiss = { viewModel.closeCreateDialog() },
            onCreate = { draft -> viewModel.createEvent(draft) }
        )
    }
}

@Composable
private fun DayView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy") }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderWithArrows(
            title = selectedDate.format(formatter),
            onPrevious = onPreviousDay,
            onNext = onNextDay
        )

        Spacer(modifier = Modifier.height(12.dp))

        EventsList(
            modifier = Modifier.weight(1f),
            events = events
        )
    }
}

@Composable
private fun WeekView(
    selectedDate: LocalDate,
    eventCountByDate: Map<LocalDate, Int>,
    eventsForSelectedDate: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val dates = (0..6).map { weekStart.plusDays(it.toLong()) }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderWithArrows(
            title = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
            onPrevious = onPreviousWeek,
            onNext = onNextWeek
        )

        Spacer(modifier = Modifier.height(12.dp))
        WeekdayHeader(firstDayOfWeek = firstDayOfWeek)

        Row(modifier = Modifier.fillMaxWidth()) {
            dates.forEach { date ->
                DateCell(
                    modifier = Modifier.weight(1f),
                    date = date,
                    isSelected = date == selectedDate,
                    eventCount = eventCountByDate[date] ?: 0,
                    onClick = { onDateSelected(date) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selected day",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        EventsList(
            modifier = Modifier.weight(1f),
            events = eventsForSelectedDate
        )
    }
}

@Composable
private fun MonthView(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    eventCountByDate: Map<LocalDate, Int>,
    selectedDateEvents: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val monthCells = remember(visibleMonth, firstDayOfWeek) {
        buildMonthCells(visibleMonth, firstDayOfWeek)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderWithArrows(
            title = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )

        Spacer(modifier = Modifier.height(12.dp))
        WeekdayHeader(firstDayOfWeek = firstDayOfWeek)

        monthCells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .padding(2.dp)
                        )
                    } else {
                        DateCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            isSelected = date == selectedDate,
                            eventCount = eventCountByDate[date] ?: 0,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selected day",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        EventsList(
            modifier = Modifier.weight(1f),
            events = selectedDateEvents
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) {
            Text("<")
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        TextButton(onClick = onNext) {
            Text(">")
        }
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val orderedDays = (0..6).map { firstDayOfWeek.plus(it.toLong()) }

    Row(modifier = Modifier.fillMaxWidth()) {
        orderedDays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DateCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    isSelected: Boolean,
    eventCount: Int,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .padding(2.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge
            )

            if (eventCount > 0) {
                Text(
                    text = "$eventCount event${if (eventCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun EventsList(
    modifier: Modifier = Modifier,
    events: List<CalendarEvent>
) {
    if (events.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("No events.")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = events, key = { it.id }) { event ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.timeLabel())

                    event.location?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Location: $it")
                    }

                    event.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateEventDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onCreate: (EventDraft) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var startTimeText by rememberSaveable { mutableStateOf("09:00") }
    var endTimeText by rememberSaveable { mutableStateOf("10:00") }
    var isAllDay by rememberSaveable { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Event")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") }
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All day")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                if (!isAllDay) {
                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = { startTimeText = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = { endTimeText = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true
                    )
                }

                validationMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        if (title.isBlank()) {
                            validationMessage = "Title is required."
                            return@Button
                        }

                        val parsedDate = LocalDate.parse(dateText)
                        val parsedStart =
                            if (isAllDay) LocalTime.MIDNIGHT else LocalTime.parse(startTimeText)
                        val parsedEnd =
                            if (isAllDay) LocalTime.MIDNIGHT else LocalTime.parse(endTimeText)

                        if (!isAllDay && !parsedEnd.isAfter(parsedStart)) {
                            validationMessage = "End time must be after start time."
                            return@Button
                        }

                        onCreate(
                            EventDraft(
                                title = title.trim(),
                                description = description.trim(),
                                location = location.trim(),
                                date = parsedDate,
                                startTime = parsedStart,
                                endTime = parsedEnd,
                                isAllDay = isAllDay
                            )
                        )
                    } catch (_: Exception) {
                        validationMessage = "Please check the date and time format."
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun buildEventCountMap(events: List<CalendarEvent>): Map<LocalDate, Int> {
    val counts = mutableMapOf<LocalDate, Int>()

    for (event in events) {
        var day = event.start.toLocalDate()
        val lastDay = event.lastDateInclusive()

        while (!day.isAfter(lastDay)) {
            counts[day] = (counts[day] ?: 0) + 1
            day = day.plusDays(1)
        }
    }

    return counts
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