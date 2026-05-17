package com.example.cicompanion.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.utils.HtmlUtils
import java.time.format.DateTimeFormatter

private val CustomEventOrange = Color(0xFFFF9800)
private val BrandRed = Color(0xFFEF3347)
private val BookmarkYellow = Color(0xFFFFC107)

@Composable
fun CalendarWidget(
    upcomingEvents: List<CalendarEvent>,
    bookmarkedEvents: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    onInfoClick: (CalendarEvent) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Upcoming, 1 for Bookmarked
    val currentEvents = if (selectedTab == 0) upcomingEvents else bookmarkedEvents

    Column(modifier = modifier.fillMaxWidth()) {
        // Mini Tabs
        /* COMMENTED OUT BOOKMARKING TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WidgetTab(
                text = "Upcoming",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            WidgetTab(
                text = "Bookmarked",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }
        */

        Text(
            text = "Upcoming Events",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (currentEvents.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No upcoming events" else "No bookmarked events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        val pagerState = rememberPagerState(pageCount = { currentEvents.size })

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp),
                    userScrollEnabled = true,
                    horizontalAlignment = Alignment.Start
                ) { page ->
                    val event = currentEvents[page]
                    EventWidgetCard(event, onInfoClick)
                }

                // Page Indicator
                if (currentEvents.size > 1) {
                    Column(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .wrapContentWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        repeat(currentEvents.size) { index ->
                            val dotColor = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .size(6.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventWidgetCard(event: CalendarEvent, onInfoClick: (CalendarEvent) -> Unit) {
    val isCustom = event.calendarId == "custom"
    val isBookmarked = event.isBookmarked
    val badgeColor = when {
        isBookmarked && event.calendarId != "custom" -> BookmarkYellow
        isCustom -> CustomEventOrange
        else -> MaterialTheme.colorScheme.primary
    }

    val eventDateText = event.start.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                /* COMMENTED OUT BOOKMARK ICON
                if (isBookmarked) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = BookmarkYellow,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                }
                */
                Text(
                    text = HtmlUtils.stripHtml(event.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Surface(
                color = badgeColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = when {
                        /* isBookmarked && event.calendarId != "custom" -> "BOOKMARKED" */
                        isCustom -> "Custom"
                        else -> "CSUCI"
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = eventDateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = event.timeLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (!event.location.isNullOrBlank()) {
            Text(
                text = HtmlUtils.stripHtml(event.location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // More Info Button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { onInfoClick(event) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text("More Info", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
