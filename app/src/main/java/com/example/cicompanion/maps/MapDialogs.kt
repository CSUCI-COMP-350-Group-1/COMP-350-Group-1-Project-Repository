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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.MessagingRepository
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserProfile
import com.example.cicompanion.ui.theme.CoralRed
import com.google.firebase.auth.FirebaseAuth

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

@Composable
fun ShareLocationDialog(
    location: CampusLocation,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedFriends by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            SocialRepository.fetchAcceptedFriends(
                currentUserId = user.uid,
                onSuccess = {
                    friends = it
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        }
    }

    val filteredFriends = remember(searchQuery, friends) {
        friends.filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }
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
                    "Share Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CoralRed
                )
                Text(
                    "Share \"${location.name}\" with friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends...") },
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

                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CoralRed)
                    } else if (friends.isEmpty()) {
                        Text("No friends found.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn {
                            items(filteredFriends) { friend ->
                                val isSelected = selectedFriends.contains(friend.uid)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFriends = if (isSelected) {
                                                selectedFriends - friend.uid
                                            } else {
                                                selectedFriends + friend.uid
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) CoralRed else Color.LightGray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (friend.displayName.takeIf { it.isNotBlank() } ?: friend.email).take(1).uppercase(),
                                                color = if (isSelected) Color.White else Color.DarkGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friend.displayName.takeIf { it.isNotBlank() } ?: friend.email,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (friend.displayName.isNotBlank()) {
                                            Text(friend.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedFriends = if (it) {
                                                selectedFriends + friend.uid
                                            } else {
                                                selectedFriends - friend.uid
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = CoralRed)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !isSending) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (currentUser != null && selectedFriends.isNotEmpty()) {
                                isSending = true
                                val selectedFriendProfiles = friends.filter { selectedFriends.contains(it.uid) }
                                var sentCount = 0
                                selectedFriendProfiles.forEach { friend ->
                                    MessagingRepository.sendMessage(
                                        currentUser = currentUser,
                                        friend = friend,
                                        messageText = "I shared a location: ${location.name}",
                                        type = "location",
                                        metadata = mapOf(
                                            "lat" to location.position.latitude.toString(),
                                            "lng" to location.position.longitude.toString(),
                                            "name" to location.name,
                                            "desc" to location.description,
                                            "color" to location.color.toArgb().toString()
                                        ),
                                        onSuccess = {
                                            sentCount++
                                            if (sentCount == selectedFriendProfiles.size) {
                                                onSuccess()
                                            }
                                        },
                                        onError = {
                                            // Handle error if needed
                                            sentCount++
                                            if (sentCount == selectedFriendProfiles.size) {
                                                onSuccess()
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        enabled = selectedFriends.isNotEmpty() && !isSending,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Send (${selectedFriends.size})")
                        }
                    }
                }
            }
        }
    }
}
