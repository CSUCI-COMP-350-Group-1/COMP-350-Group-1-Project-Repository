package com.example.cicompanion.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.MessagingRepository
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserProfile
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandOrange
import com.example.cicompanion.ui.theme.CoralRed
import com.google.firebase.auth.FirebaseAuth

private val FaintGray = Color(0xFFF5F5F5)

@Composable
fun LocationDetailsContent(
    location: CampusLocation, 
    events: List<CalendarEvent>,
    onGoToEvent: (CalendarEvent) -> Unit,
    onDeletePin: () -> Unit,
    onTogglePin: () -> Unit,
    onShareClick: () -> Unit,
    onAssociateEvent: () -> Unit,
    onEditPin: () -> Unit,
    onSaveSharedPin: () -> Unit = {},
    isAlreadySaved: Boolean = false
) {
    val isTemp = location.id == "shared_temp"
    val contentPadding = PaddingValues(horizontal = 20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // Top Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
        )

        // Header Section
        Row(
            modifier = Modifier.padding(contentPadding).padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = location.color.copy(alpha = 0.1f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = if (isTemp) "Shared Location" else location.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            if (!isTemp && location.isCustom) {
                FilledTonalIconButton(
                    onClick = onEditPin,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Pin", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row
        if (isTemp || location.isCustom) {
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTemp) {
                    if (!isAlreadySaved) {
                        Button(
                            onClick = onSaveSharedPin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save to My Map", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Already Saved to Map", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (location.isCustom) {
                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeletePin,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (location.isCustom && !isTemp) {
            Surface(
                onClick = onAssociateEvent,
                modifier = Modifier.padding(contentPadding).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CoralRed.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = CoralRed)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (location.associatedEventId != null) "Change Linked Event" else "Link an Event",
                        color = CoralRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CoralRed)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Description Section
        if (location.description.isNotEmpty()) {
            Column(modifier = Modifier.padding(contentPadding)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Events Section
        val visibleEvents = if (isTemp && location.associatedEventId != null) {
            events.filter { it.calendarId == "custom" && it.ownerId != FirebaseAuth.getInstance().currentUser?.uid }
        } else {
            events
        }

        Column(modifier = Modifier.padding(contentPadding)) {
            Text(
                text = if (location.isCustom && location.associatedEventId != null) "Linked Event" else "Upcoming Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (visibleEvents.isNotEmpty()) {
                visibleEvents.forEach { event ->
                    MapEventItem(
                        event = event,
                        onMoreClick = { onGoToEvent(event) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FaintGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No events scheduled for this location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShareLocationSheetContent(
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
    val contentPadding = PaddingValues(horizontal = 20.dp)

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 650.dp)
            .padding(bottom = 32.dp)
    ) {
        // Top Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
        )

        Row(
            modifier = Modifier.padding(contentPadding).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Share Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "Send \"${location.name}\" to friends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search friends...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.padding(contentPadding).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoralRed,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Friends List
        Box(modifier = Modifier.weight(1f).padding(contentPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CoralRed)
            } else if (friends.isEmpty()) {
                Text("No friends found to share with.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredFriends) { friend ->
                        val isSelected = selectedFriends.contains(friend.uid)
                        Surface(
                            onClick = {
                                selectedFriends = if (isSelected) selectedFriends - friend.uid else selectedFriends + friend.uid
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CoralRed.copy(alpha = 0.05f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, CoralRed.copy(alpha = 0.2f)) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) CoralRed else Color.LightGray.copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (friend.displayName.takeIf { it.isNotBlank() } ?: friend.email).take(1).uppercase(),
                                            color = if (isSelected) Color.White else Color.DarkGray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.displayName.takeIf { it.isNotBlank() } ?: friend.email,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) CoralRed else Color.Black
                                    )
                                    if (friend.displayName.isNotBlank()) {
                                        Text(friend.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedFriends = if (it) selectedFriends + friend.uid else selectedFriends - friend.uid
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CoralRed)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Send Button
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
                            messageText = "Check out this location: ${location.name}",
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
                                if (sentCount == selectedFriendProfiles.size) onSuccess()
                            },
                            onError = {
                                sentCount++
                                if (sentCount == selectedFriendProfiles.size) onSuccess()
                            }
                        )
                    }
                }
            },
            enabled = selectedFriends.isNotEmpty() && !isSending,
            modifier = Modifier.padding(contentPadding).fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (selectedFriends.isEmpty()) "Select Friends" else "Send to ${selectedFriends.size} Friend${if (selectedFriends.size > 1) "s" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MapEventItem(event: CalendarEvent, onMoreClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onMoreClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (event.isPinned) Color(0xFF9C27B0).copy(alpha = 0.1f) else BrandOrange.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.isPinned) Icons.Default.PushPin else Icons.Default.Event,
                    contentDescription = null,
                    tint = if (event.isPinned) Color(0xFF9C27B0) else BrandOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.timeLabel(),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LocationInfoCard(location: CampusLocation, onClose: () -> Unit, onDetailsClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(bottom = 48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = location.color.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (location.isCustom) Icons.Default.PushPin else location.icon,
                            contentDescription = null, 
                            tint = location.color, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = location.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = location.type.name.lowercase().replaceFirstChar { it.uppercase() }, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onDetailsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text("View Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}
