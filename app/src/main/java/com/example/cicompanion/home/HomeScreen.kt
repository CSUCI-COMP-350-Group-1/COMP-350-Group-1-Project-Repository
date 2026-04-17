package com.example.cicompanion.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cicompanion.appNavigation.FeatureCard
import com.example.cicompanion.appNavigation.featureItems
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandRedDark
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val widgetEvents = remember(calendarViewModel.events) {
        upcomingHomeWidgetEvents(calendarViewModel.events)
    }
    
    val pinnedEvents = remember(calendarViewModel.events) {
        calendarViewModel.events.filter { it.isPinned }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp)
    ) {
        item {
            // Upcoming events widget at the top
            CalendarWidget(
                events = widgetEvents,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (pinnedEvents.isNotEmpty()) {
            item {
                Text(
                    text = "Pinned Reminders",
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            items(pinnedEvents) { event ->
                PinnedEventItem(
                    event = event,
                    onNavigateToEvent = {
                        calendarViewModel.resetFilters()
                        calendarViewModel.onDateSelected(event.start.toLocalDate())
                        calendarViewModel.setHighlightedEvent(event.id)
                        navController.navigate(Routes.CALENDAR)
                    }
                )
            }
        }

        item {
            Text(
                text = "Quick Access",
                modifier = Modifier.padding(start = 16.dp, top = 24.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Feature grid items
            featureItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { feature ->
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureCard(
                                feature = feature,
                                onClick = { navController.navigate(feature.route) }
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PinnedEventItem(event: CalendarEvent, onNavigateToEvent: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = Color(0xFF9C27B0), // Pinned purple
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.start.format(DateTimeFormatter.ofPattern("MMM d"))} • ${event.timeLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // Replaced '...' button with circular calendar button to match MapScreen
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandRedDark.copy(alpha = 0.1f))
                    .clickable(onClick = onNavigateToEvent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "View in Calendar",
                    tint = BrandRedDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun upcomingHomeWidgetEvents(events: List<CalendarEvent>): List<CalendarEvent> {
    val now = ZonedDateTime.now()

    return events
        .filter { event -> event.endExclusive.isAfter(now) }
        .sortedBy { event -> event.start }
        .take(3)
}
