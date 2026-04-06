package com.example.sprint1homeui.studyRoom

//Imports for the VIEW

import android.R.attr.textColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.sprint1homeui.ui.theme.GrayIcon
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// *******IMPORTANT NOTE********
// This is one of the VIEW layers for application
// This VIEW handles displaying room name, time slot row, formatting time, and colors
// Shows single room only

@Composable
fun RoomItem(room: StudyRoom) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Display room name
            Text(text = room.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            // Display row of time slots
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(room.slots) { slot ->
                    // Format the time from 24h to 12h time
                    val displayTime = try {
                        LocalTime.parse(slot.startTime)
                            .format(DateTimeFormatter.ofPattern("h:mm a"))
                    } catch (e: Exception) {
                        slot.startTime
                    }

                    // Change color of time slot based on availability
                    val color = if (slot.isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, GrayIcon.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = displayTime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = color,
                                fontWeight = if (slot.isAvailable) FontWeight.Bold else FontWeight.Normal,
                                // Slash through unavailable time slots
                                textDecoration = if (slot.isAvailable) null else TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }
        }
    }
}