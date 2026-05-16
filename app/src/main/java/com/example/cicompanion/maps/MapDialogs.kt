package com.example.cicompanion.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.theme.CoralRed

@Composable
fun EventSelectionDialog(
    customEvents: List<CalendarEvent>,
    currentEventId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredEvents = remember(searchQuery, customEvents) {
        customEvents.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Link an Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CoralRed
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your events...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralRed,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    item {
                        Surface(
                            onClick = { onConfirm(null) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (currentEventId == null) CoralRed.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, tint = Color.Gray)
                                Spacer(Modifier.width(12.dp))
                                Text("None (Unlink Event)", fontWeight = FontWeight.Medium)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    }

                    if (filteredEvents.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No events found.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(filteredEvents) { event ->
                            val isSelected = event.id == currentEventId
                            Surface(
                                onClick = { onConfirm(event.id) },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isSelected) CoralRed.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = if (isSelected) CoralRed else Color.Gray)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(event.title, fontWeight = FontWeight.Bold, color = if (isSelected) CoralRed else Color.Black)
                                        Text(event.timeLabel(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun PinCreationDialog(
    customEvents: List<CalendarEvent>,
    editingPin: CustomPin? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Color, String?) -> Unit
) {
    var name by remember { mutableStateOf(editingPin?.name ?: "") }
    var description by remember { mutableStateOf(editingPin?.description ?: "") }
    var selectedColor by remember { mutableStateOf(editingPin?.color ?: CoralRed) }
    var selectedEventId by remember { mutableStateOf<String?>(editingPin?.associatedEventId) }
    var showEventPicker by remember { mutableStateOf(false) }

    val colors = listOf(CoralRed, Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingPin != null) "Edit Custom Pin" else "Create Custom Pin", fontWeight = FontWeight.Bold, color = CoralRed) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoralRed, focusedLabelColor = CoralRed)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoralRed, focusedLabelColor = CoralRed)
                )
                Spacer(Modifier.height(16.dp))

                Text("Pick a Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(if (selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape)
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Linked Event", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                val selectedEvent = customEvents.find { it.id == selectedEventId }
                Surface(
                    onClick = { showEventPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    color = Color(0xFFF8F8F8)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedEvent != null) Icons.Default.Event else Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = if (selectedEvent != null) CoralRed else Color.Gray
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedEvent?.title ?: "No event linked (Tap to select)",
                            color = if (selectedEvent != null) Color.Black else Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedColor, selectedEventId) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editingPin != null) "Save Changes" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )

    if (showEventPicker) {
        EventSelectionDialog(
            customEvents = customEvents,
            currentEventId = selectedEventId,
            onDismiss = { showEventPicker = false },
            onConfirm = {
                selectedEventId = it
                showEventPicker = false
            }
        )
    }
}
