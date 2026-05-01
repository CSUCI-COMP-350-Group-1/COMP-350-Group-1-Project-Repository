package com.example.cicompanion.social

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.maps.CustomPin
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
private fun rememberAuthUser(): FirebaseUser? {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    return currentUser
}

@Composable
fun MessagesScreen(navController: NavHostController, sharedLocation: String? = null) {
    val currentUser = rememberAuthUser()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var rawConversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingFriends by remember { mutableStateOf(currentUser != null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            friends = emptyList()
            rawConversations = emptyList()
            errorMessage = null
            isLoadingFriends = false
        } else {
            isLoadingFriends = true
        }
    }

    if (currentUser == null) {
        SignedOutMessagingMessage()
        return
    }

    LaunchedEffect(currentUser.uid) {
        SocialRepository.fetchAcceptedFriends(
            currentUserId = currentUser.uid,
            onSuccess = {
                friends = it
                isLoadingFriends = false
            },
            onError = {
                errorMessage = it
                isLoadingFriends = false
            }
        )
    }

    DisposableEffect(currentUser.uid) {
        val registration = MessagingRepository.listenToConversations(
            currentUserId = currentUser.uid,
            onUpdate = { rawConversations = it },
            onError = { errorMessage = it }
        )
        onDispose {
            registration.remove()
        }
    }

    val friendsById = remember(friends) { friends.associateBy { it.uid } }
    
    val activeConversations = remember(rawConversations, friendsById) {
        rawConversations.filter { conversation ->
            val otherId = MessagingRepository.findOtherParticipantId(conversation, currentUser.uid)
            otherId != null && friendsById.containsKey(otherId)
        }
    }

    Scaffold(
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppBackground)
                .padding(16.dp)
        ) {
            if (sharedLocation != null) {
                Surface(
                    color = Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Sharing location: $sharedLocation. Pick a friend to send to.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { navController.navigate(Routes.FRIENDS_AND_REQUESTS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF3347),
                    contentColor = Color.White
                )
            ) {
                Text("Manage Friends")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Start a Chat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoadingFriends -> {
                    Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFEF3347))
                    }
                }
                friends.isEmpty() -> {
                    EmptyCard("Add a friend first to start messaging.")
                }
                else -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(friends, key = { it.uid }) { friend ->
                            FriendPickerCard(
                                user = friend,
                                onClick = {
                                    val conversationId = MessagingRepository.createConversationId(currentUser.uid, friend.uid)
                                    val initialMsg = if (sharedLocation != null) "Check out this custom pin: $sharedLocation" else null
                                    navController.navigate(Routes.messageThread(conversationId, friend.uid, initialMsg))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeConversations.isEmpty()) {
                EmptyCard("No conversations yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeConversations, key = { it.id }) { conversation ->
                        val friendUserId = MessagingRepository.findOtherParticipantId(conversation, currentUser.uid).orEmpty()
                        val friend = friendsById[friendUserId]

                        ConversationCard(
                            friendName = friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Friend",
                            friendEmail = friend?.email ?: "",
                            preview = conversation.lastMessageText,
                            onClick = {
                                val initialMsg = if (sharedLocation != null) "Check out this custom pin: $sharedLocation" else null
                                navController.navigate(Routes.messageThread(conversation.id, friendUserId, initialMsg))
                            }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MessageThreadScreen(
    navController: NavHostController,
    conversationId: String,
    friendUserId: String,
    initialMessage: String? = null
) {
    val currentUser = rememberAuthUser()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var friend by remember { mutableStateOf<UserProfile?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by rememberSaveable { mutableStateOf(initialMessage ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var conversationExists by remember { mutableStateOf(false) }
    var isFriend by remember { mutableStateOf(true) }

    var userPins by remember { mutableStateOf<List<CustomPin>>(emptyList()) }
    var userEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var showPinPicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null && friend != null && currentUser != null) {
                        MessagingRepository.sendMessage(
                            currentUser = currentUser,
                            friend = friend!!,
                            messageText = "My current location",
                            type = "location",
                            metadata = mapOf(
                                "lat" to location.latitude.toString(),
                                "lng" to location.longitude.toString()
                            ),
                            onSuccess = { conversationExists = true },
                            onError = { errorMessage = it }
                        )
                    }
                }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            friend = null
            messages = emptyList()
            messageText = ""
            errorMessage = null
            isSending = false
            conversationExists = false
        }
    }

    if (currentUser == null) {
        SignedOutMessagingMessage()
        return
    }

    LaunchedEffect(currentUser.uid, friendUserId) {
        SocialRepository.fetchAllFriendRequestStatuses(
            currentUserId = currentUser.uid,
            onSuccess = { statuses ->
                isFriend = statuses[friendUserId] == "accepted"
            },
            onError = { isFriend = false }
        )

        MessagingRepository.fetchUserProfile(
            userId = friendUserId,
            onSuccess = {
                friend = it
                errorMessage = null
            },
            onError = { errorMessage = it }
        )
    }

    LaunchedEffect(conversationId) {
        MessagingRepository.checkConversationExists(
            conversationId = conversationId,
            onResult = { exists ->
                conversationExists = exists
                errorMessage = null
            },
            onError = { errorMessage = it }
        )
    }

    DisposableEffect(conversationId, conversationExists) {
        if (!conversationExists) {
            onDispose { }
        } else {
            val registration = MessagingRepository.listenToMessages(
                conversationId = conversationId,
                onUpdate = {
                    messages = it
                    errorMessage = null
                },
                onError = { errorMessage = it }
            )
            onDispose {
                registration.remove()
            }
        }
    }

    DisposableEffect(currentUser.uid) {
        val pinsReg = SocialRepository.listenToCustomPins(currentUser.uid, { userPins = it }, {})
        val eventsReg = SocialRepository.listenToCustomEvents(currentUser.uid, { userEvents = it }, {})
        onDispose {
            pinsReg.remove()
            eventsReg.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showPinPicker) {
        ItemPickerDialog(
            title = "Select a Pin",
            items = userPins,
            itemName = { it.name },
            onItemSelected = { pin ->
                val targetFriend = friend ?: return@ItemPickerDialog
                MessagingRepository.sendMessage(
                    currentUser = currentUser,
                    friend = targetFriend,
                    messageText = "Pin: ${pin.name}",
                    type = "pin",
                    metadata = mapOf(
                        "pinId" to pin.id,
                        "pinName" to pin.name,
                        "lat" to pin.latitude.toString(),
                        "lng" to pin.longitude.toString(),
                        "description" to pin.description,
                        "colorArgb" to pin.colorArgb.toString(),
                        "associatedEventId" to (pin.associatedEventId ?: "")
                    ),
                    onSuccess = { conversationExists = true },
                    onError = { errorMessage = it }
                )
                showPinPicker = false
            },
            onDismiss = { showPinPicker = false }
        )
    }

    if (showEventPicker) {
        ItemPickerDialog(
            title = "Select an Event",
            items = userEvents.filter { it.ownerId == currentUser.uid },
            itemName = { it.title },
            onItemSelected = { event ->
                val targetFriend = friend ?: return@ItemPickerDialog
                SocialRepository.sendEventInvite(
                    currentUser = currentUser,
                    targetUserId = friendUserId,
                    event = event,
                    onSuccess = {
                        MessagingRepository.sendMessage(
                            currentUser = currentUser,
                            friend = targetFriend,
                            messageText = "Sent you an invite for: ${event.title}",
                            type = "event_invite",
                            metadata = mapOf(
                                "eventId" to event.id,
                                "eventTitle" to event.title
                            ),
                            onSuccess = { conversationExists = true },
                            onError = { errorMessage = it }
                        )
                    },
                    onError = { errorMessage = it }
                )
                showEventPicker = false
            },
            onDismiss = { showEventPicker = false }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (isFriend) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                            Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = Color(0xFFEF3347))
                        }
                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ping Current Location") },
                                onClick = {
                                    showAttachmentMenu = false
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                            .addOnSuccessListener { location ->
                                                if (location != null && friend != null) {
                                                    MessagingRepository.sendMessage(
                                                        currentUser = currentUser,
                                                        friend = friend!!,
                                                        messageText = "My current location",
                                                        type = "location",
                                                        metadata = mapOf(
                                                            "lat" to location.latitude.toString(),
                                                            "lng" to location.longitude.toString()
                                                        ),
                                                        onSuccess = { conversationExists = true },
                                                        onError = { errorMessage = it }
                                                    )
                                                }
                                            }
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Send Custom Pin") },
                                onClick = {
                                    showAttachmentMenu = false
                                    showPinPicker = true
                                },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Event") },
                                onClick = {
                                    showAttachmentMenu = false
                                    showEventPicker = true
                                },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val targetFriend = friend ?: return@IconButton
                            isSending = true
                            MessagingRepository.sendMessage(
                                currentUser = currentUser,
                                friend = targetFriend,
                                messageText = messageText,
                                onSuccess = {
                                    messageText = ""
                                    isSending = false
                                    conversationExists = true
                                    errorMessage = null
                                },
                                onError = {
                                    errorMessage = it
                                    isSending = false
                                }
                            )
                        },
                        enabled = messageText.isNotBlank() && !isSending && friend != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFFEF3347))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("You must be friends to send messages.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(AppBackground).padding(16.dp)) {
            Text(friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!friend?.email.isNullOrBlank()) {
                Text(text = friend!!.email, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            if (errorMessage != null && conversationExists) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Say hello!", color = Color.Gray)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isMine = message.senderId == currentUser.uid,
                            navController = navController,
                            ownedPins = userPins,
                            onPinSaved = { 
                                // already handled by listener
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ItemPickerDialog(
    title: String,
    items: List<T>,
    itemName: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items
        else items.filter { itemName(it).contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFEF3347))
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEF3347),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No items found.", textAlign = TextAlign.Center, color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filteredItems) { item ->
                            Text(
                                text = itemName(item),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemSelected(item) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun FriendPickerCard(user: UserProfile, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Text(text = SocialRepository.displayNameOrEmail(user), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ConversationCard(friendName: String, friendEmail: String, preview: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(friendName, fontWeight = FontWeight.Bold)
            if (friendEmail.isNotBlank()) {
                Text(text = friendEmail, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = preview, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MessageBubble(
    message: DirectMessage,
    isMine: Boolean,
    navController: NavHostController,
    ownedPins: List<CustomPin> = emptyList(),
    onPinSaved: () -> Unit = {}
) {
    val timeString = remember(message.sentAt) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.sentAt))
    }
    val scope = rememberCoroutineScope()
    
    var invite by remember { mutableStateOf<EventInvite?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    var isPinAlreadySaved by remember(message.id, ownedPins) { 
        mutableStateOf(message.type == "pin" && ownedPins.any { it.latitude == message.metadata["lat"]?.toDoubleOrNull() && it.longitude == message.metadata["lng"]?.toDoubleOrNull() })
    }

    if (message.type == "event_invite" && !isMine) {
        LaunchedEffect(message.id) {
            val inviteId = "${message.senderId}_${message.receiverId}_${message.metadata["eventId"]}"
            SocialRepository.fetchEventInvite(inviteId, { invite = it }, { inviteError = it })
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isMine) Color(0xFFEF3347) else Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message.type) {
                    "location", "pin" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (message.type == "pin") Icons.Default.PushPin else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isMine) Color.White else Color(0xFFEF3347)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.text,
                                color = if (isMine) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (message.type == "pin" && !isMine && !isPinAlreadySaved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val lat = message.metadata["lat"]?.toDoubleOrNull() ?: return@Button
                                    val lng = message.metadata["lng"]?.toDoubleOrNull() ?: return@Button
                                    val name = message.metadata["pinName"] ?: "Shared Pin"
                                    val desc = message.metadata["description"] ?: ""
                                    val color = message.metadata["colorArgb"]?.toIntOrNull() ?: Color(0xFFEF3347).toArgb()
                                    val eventId = message.metadata["associatedEventId"]
                                    
                                    val newPin = CustomPin(
                                        id = java.util.UUID.randomUUID().toString(),
                                        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                        name = name,
                                        latitude = lat,
                                        longitude = lng,
                                        description = desc,
                                        colorArgb = color,
                                        associatedEventId = if (eventId.isNullOrBlank()) null else eventId
                                    )
                                    
                                    scope.launch {
                                        FirestoreManager.saveCustomPin(newPin)
                                        isPinAlreadySaved = true
                                        onPinSaved()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to My Map", fontSize = 12.sp)
                            }
                        } else if (message.type == "pin" && !isMine && isPinAlreadySaved) {
                            Text("Saved to Map", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val lat = message.metadata["lat"]
                                val lng = message.metadata["lng"]
                                if (lat != null && lng != null) {
                                    navController.navigate(Routes.mapWithLocation(lat.toDouble(), lng.toDouble()))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (isMine) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("View on Map", fontSize = 12.sp)
                        }
                    }
                    "event_invite" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = if (isMine) Color.White else Color(0xFFEF3347)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Event Invitation",
                                color = if (isMine) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = message.metadata["eventTitle"] ?: "Unknown Event",
                            color = if (isMine) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (!isMine && invite?.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        invite?.let {
                                            SocialRepository.acceptEventInvite(it, { invite = it.copy(status = "accepted") }, {})
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Accept", fontSize = 12.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        invite?.let {
                                            SocialRepository.declineEventInvite(it, { invite = null }, {})
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Decline", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        } else if (!isMine && invite?.status == "accepted") {
                            Text("Accepted", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { navController.navigate(Routes.CALENDAR) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (isMine) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Go to Calendar", fontSize = 12.sp)
                        }
                    }
                    else -> {
                        Text(text = message.text, color = if (isMine) Color.White else Color.Black)
                    }
                }
            }
        }
        Text(text = timeString, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mail, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.Gray)
        }
    }
}

@Composable
private fun SignedOutMessagingMessage() {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Text("Please sign in to view your messages.")
    }
}

fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
}
