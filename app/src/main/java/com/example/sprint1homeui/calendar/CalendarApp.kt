package com.example.sprint1homeui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprint1homeui.calendar.model.CalendarEvent
import com.example.sprint1homeui.ui.theme.CoralRed
import com.example.sprint1homeui.ui.theme.HotPink
import com.example.sprint1homeui.ui.theme.SelectedDateFill
import com.example.sprint1homeui.ui.theme.SoftBorder
import com.example.sprint1homeui.ui.theme.SoftText
import com.example.sprint1homeui.ui.theme.StatOverlay
import com.example.sprint1homeui.ui.theme.SunsetOrange
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*

/** Shows the full calendar screen and routes state into smaller UI pieces. */
@Composable
fun CalendarApp(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDateEvents = remember(viewModel.events, viewModel.selectedDate) {
        buildSelectedDateEvents(viewModel.events, viewModel.selectedDate)
    }

    val eventCountByDate = remember(viewModel.events) {
        buildEventCountMap(viewModel.events)
    }
    
    PullToRefreshContainer(
        isRefreshing = viewModel.isLoading,
        onRefresh = viewModel::loadOnlineCalendar,
        modifier = modifier
    ) {
        CalendarScreenBody(
            mode = viewModel.mode,
            visibleMonth = viewModel.visibleMonth,
            selectedDate = viewModel.selectedDate,
            selectedDateEvents = selectedDateEvents,
            eventCountByDate = eventCountByDate,
            totalEventCount = viewModel.events.size,
            errorMessage = viewModel.errorMessage,
            onDismissError = viewModel::clearError,
            onModeSelected = viewModel::updateMode,
            onDateSelected = viewModel::onDateSelected,
            onPreviousDay = viewModel::previousDay,
            onNextDay = viewModel::nextDay,
            onPreviousWeek = viewModel::previousWeek,
            onNextWeek = viewModel::nextWeek,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth
        )
    }
}

/** Shows the scrollable page content that sits inside pull-to-refresh. */
@Composable
private fun CalendarScreenBody(
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateEvents: List<CalendarEvent>,
    eventCountByDate: Map<LocalDate, Int>,
    totalEventCount: Int,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onModeSelected: (CalendarMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    ScrollablePage(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        CalendarHeroHeader(
            totalEventCount = totalEventCount,
            selectedDate = selectedDate,
            mode = mode,
            activeDateCount = eventCountByDate.size
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
                SectionHeading(text = "Choose a view")
                Spacer(modifier = Modifier.height(12.dp))
                CalendarModeTabs(
                    selectedMode = mode,
                    onModeSelected = onModeSelected
                )
            }

            CalendarContent(
                mode = mode,
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                selectedDateEvents = selectedDateEvents,
                eventCountByDate = eventCountByDate,
                onDateSelected = onDateSelected,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }
    }
}

/** Shows the large top header that sets the new visual style for the screen. */
@Composable
private fun CalendarHeroHeader(
    totalEventCount: Int,
    selectedDate: LocalDate,
    mode: CalendarMode,
    activeDateCount: Int
) {
    val subtitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(CoralRed, HotPink)
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "CSUCI Calendar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = selectedDate.format(subtitleFormatter),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            StatsCard(
                totalEventCount = totalEventCount,
                activeDateCount = activeDateCount,
                mode = mode
            )
        }
    }
}

/** Shows the small summary card that sits inside the hero header. */
@Composable
private fun StatsCard(
    totalEventCount: Int,
    activeDateCount: Int,
    mode: CalendarMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StatOverlay)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeroStat(
            label = "Events",
            value = totalEventCount.toString(),
            modifier = Modifier.weight(1f)
        )

        HeroStat(
            label = "Active Days",
            value = activeDateCount.toString(),
            modifier = Modifier.weight(1f)
        )

        HeroStat(
            label = "View",
            value = mode.name,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Shows one stat item in the hero summary card. */
@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
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
    val containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else accentColor
    val borderColor = if (isSelected) Color.Transparent else accentColor.copy(alpha = 0.25f)

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
    mode: CalendarMode,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateEvents: List<CalendarEvent>,
    eventCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    when (mode) {
        CalendarMode.DAY -> DayView(
            selectedDate = selectedDate,
            events = selectedDateEvents,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay
        )

        CalendarMode.WEEK -> WeekView(
            selectedDate = selectedDate,
            eventCountByDate = eventCountByDate,
            eventsForSelectedDate = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )

        CalendarMode.MONTH -> MonthView(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            eventCountByDate = eventCountByDate,
            selectedDateEvents = selectedDateEvents,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

/** Shows the selected day and the events that fall on that day. */
@Composable
private fun DayView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
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
            events = events
        )
    }
}

/** Shows the current week grid and the events for the selected day. */
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
                eventCountByDate = eventCountByDate,
                onDateSelected = onDateSelected
            )
        }

        EventsSection(
            title = "Selected day",
            events = eventsForSelectedDate
        )
    }
}

/** Shows the current month grid and the events for the selected day. */
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
                eventCountByDate = eventCountByDate,
                onDateSelected = onDateSelected
            )
        }

        EventsSection(
            title = "Selected day",
            events = selectedDateEvents
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
    eventCountByDate: Map<LocalDate, Int>,
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
                eventCount = eventCountByDate[date] ?: 0,
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
    eventCountByDate: Map<LocalDate, Int>,
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
                            eventCount = eventCountByDate[date] ?: 0,
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
    eventCount: Int,
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

            EventCountLabel(
                eventCount = eventCount,
                isSelected = isSelected
            )
        }
    }
}

/** Shows the event count text only when a date has at least one event. */
@Composable
private fun EventCountLabel(
    eventCount: Int,
    isSelected: Boolean
) {
    if (eventCount > 0) {
        Text(
            text = if (eventCount == 1) "1\nevent" else "$eventCount\nevents",
            style = MaterialTheme.typography.labelSmall,
            color = resolveEventCountTextColor(isSelected),
            fontWeight = FontWeight.SemiBold,
            lineHeight = 12.sp,
            textAlign = TextAlign.Start
        )
    }
}

/** Shows a section title and the list of events below it. */
@Composable
private fun EventsSection(
    title: String,
    events: List<CalendarEvent>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(text = title)
        EventsList(events = events)
    }
}

/** Shows either an empty state or the full list of event cards. */
@Composable
private fun EventsList(events: List<CalendarEvent>) {
    if (events.isEmpty()) {
        EmptyEventsMessage()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        events.forEach { event ->
            EventCard(event = event)
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
private fun EventCard(event: CalendarEvent) {
    val displayTitle = formatEventTextForDisplay(event.title)
    val displayLocation = event.location?.let(::formatEventTextForDisplay)
    val displayDescription = event.description?.let(::formatEventTextForDisplay)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventCardHeader(title = displayTitle)
            EventMetaLine(text = event.timeLabel())
            displayLocation?.let { EventMetaLine(text = "Location: $it") }
            displayDescription?.let { EventDescription(text = it) }
        }
    }
}

/** Shows the top row of an event card. */
@Composable
private fun EventCardHeader(title: String) {
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

        EventBadge()
    }
}

/** Shows the small badge on the right side of an event card. */
@Composable
private fun EventBadge() {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Event",
            color = MaterialTheme.colorScheme.primary,
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
    return events.filter { it.occursOn(selectedDate) }.sortedBy { it.start }
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
        CalendarMode.WEEK -> HotPink
        CalendarMode.MONTH -> SunsetOrange
    }
}

/** Chooses the background color for a date cell. */
@Composable
private fun resolveDateCellBackgroundColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else MaterialTheme.colorScheme.surface
}

/** Chooses the border color for a date cell. */
@Composable
private fun resolveDateCellBorderColor(isSelected: Boolean): Color {
    return if (isSelected) CoralRed else SoftBorder
}

/** Chooses the main text color for a date cell. */
@Composable
private fun resolveDateCellTextColor(isSelected: Boolean): Color {
    return if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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

/** Counts how many events fall on each date in the loaded month. */
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
