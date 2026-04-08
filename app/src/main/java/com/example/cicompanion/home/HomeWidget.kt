package com.example.cicompanion.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.theme.BrandRedLight
import com.example.cicompanion.ui.theme.GrayIcon
import com.example.cicompanion.ui.theme.NavBackground
import java.time.format.DateTimeFormatter

@Composable
fun CalendarWidget(events: List<CalendarEvent>,
                   modifier: Modifier = Modifier) {
    if (events.isEmpty()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(
                    width = 1.dp,
                    color = GrayIcon.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            color = NavBackground,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayIcon
                )
            }
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { events.size })

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(
                width = 1.dp,
                color = GrayIcon.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        color = NavBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        //Inner widget
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 20.dp),
                userScrollEnabled = true,
                horizontalAlignment = Alignment.Start
            ) { page ->
                val event = events[page]
                EventWidgetCard(event)
            }

            //Three dot column
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                events.forEachIndexed { index, _ ->
                    // Dot turns red (or stays gray) based on current event page
                    val dotColor = if (pagerState.currentPage == index) BrandRedLight else GrayIcon
                    val dotBorder =
                        if (pagerState.currentPage == index) GrayIcon else Color.Transparent

                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .size(8.dp)
                            .background(dotColor, CircleShape)
                            .border(0.5.dp, dotBorder, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventWidgetCard(event: CalendarEvent) {

    val eventDateText = event.start.format(
        DateTimeFormatter.ofPattern("EEE, MMM d")
    )
    Column(modifier = Modifier
        .padding(10.dp)
        .fillMaxSize()
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = eventDateText,
            style = MaterialTheme.typography.bodySmall,
            color = GrayIcon,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = event.timeLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = GrayIcon,
            fontSize = 11.sp
        )
        if (!event.location.isNullOrBlank()) {
            Text(
                text = event.location,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    }
}



