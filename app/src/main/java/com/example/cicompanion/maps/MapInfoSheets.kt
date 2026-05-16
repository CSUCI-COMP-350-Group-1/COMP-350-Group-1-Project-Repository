package com.example.cicompanion.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandOrange
import com.example.cicompanion.ui.theme.CoralRed
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LocationDetailsContent(
    location: CampusLocation, 
    events: List<CalendarEvent>,
    onGoToEvent: (CalendarEvent) -> Unit,
    onDeletePin: () -> Unit,
    onTogglePin: () -> Unit,
    onSendMessage: () -> Unit,
    onAssociateEvent: () -> Unit,
    onEditPin: () -> Unit,
    onSaveSharedPin: () -> Unit = {}
) {
    val isTemp = location.id == "shared_temp"
    val detailsContentModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 32.dp)
    ) {
        Row(
            modifier = detailsContentModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(location.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (isTemp) "Shared Temporary Pin" else location.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isTemp) "Clicking empty space on the map or another point will remove this pin." else location.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (!isTemp && location.isCustom) {
                IconButton(onClick = onEditPin) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Pin", tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (location.description.isNotEmpty()) {
            Column(modifier = detailsContentModifier) {
                @Suppress("DEPRECATION")
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    lineHeight = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = detailsContentModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isTemp) {
                Button(
                    onClick = onSaveSharedPin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save to My Map")
                }
            } else if (location.isCustom) {
                Button(
                    onClick = onDeletePin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
                Button(
                    onClick = onSendMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed.copy(alpha = 0.1f), contentColor = CoralRed),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }

        if (location.isCustom && !isTemp) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAssociateEvent,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                modifier = detailsContentModifier,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (location.associatedEventId != null) "Change Linked Event" else "Link an Event")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (events.isNotEmpty()) {
            val visibleEvents = if (isTemp && location.associatedEventId != null) {
                events.filter { it.calendarId == "custom" && it.ownerId != FirebaseAuth.getInstance().currentUser?.uid }
            } else {
                events
            }

            if (visibleEvents.isNotEmpty()) {
                Column(modifier = detailsContentModifier) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = if (location.isCustom && location.associatedEventId != null) "Linked Event" else "Events at this Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                visibleEvents.forEach { event ->
                    Box(modifier = detailsContentModifier) {
                        MapEventItem(
                            event = event,
                            onMoreClick = { onGoToEvent(event) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            Text(
                text = "No upcoming events here.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = detailsContentModifier.padding(vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MapEventItem(event: CalendarEvent, onMoreClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (event.isPinned) Color(0xFF9C27B0).copy(alpha = 0.1f) else BrandOrange.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.isPinned) Icons.Default.PushPin else Icons.Default.Event,
                    contentDescription = null,
                    tint = if (event.isPinned) Color(0xFF9C27B0) else BrandOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                @Suppress("DEPRECATION")
                Text(
                    text = event.timeLabel(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoralRed.copy(alpha = 0.1f))
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Go to Calendar",
                    tint = CoralRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LocationInfoCard(location: CampusLocation, onClose: () -> Unit, onDetailsClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.70f)
            .padding(bottom = 36.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        @Suppress("DEPRECATION")
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(location.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (location.isCustom) Icons.Default.PushPin else location.icon,
                        contentDescription = null, 
                        tint = location.color, 
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (location.description.isNotEmpty()) {
                        Text(location.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Surface(
                    onClick = onDetailsClick,
                    color = AppBackground,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp).fillMaxWidth(),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Details", color = CoralRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
