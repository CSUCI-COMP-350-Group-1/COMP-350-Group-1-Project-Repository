package com.example.cicompanion.social

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rawConversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingFriends by remember { mutableStateOf(currentUser != null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            friends = emptyList()
            nicknames = emptyMap()
            rawConversations = emptyList()
            errorMessage = null
            isLoadingFriends = false
        } else {
            isLoadingFriends = true
            SocialRepository.fetchAcceptedFriends(
                currentUserId = currentUser.uid,
                onSuccess = {
                    friends = it
                    SocialRepository.fetchNicknames(
                        currentUserId = currentUser.uid,
                        onSuccess = { map ->
                            nicknames = map
                            isLoadingFriends = false
                        },
                        onError = { err -> errorMessage = err; isLoadingFriends = false }
                    )
                },
                onError = {
                    errorMessage = it
                    isLoadingFriends = false
                }
            )
        }
    }

    DisposableEffect(currentUser?.uid) {
        if (currentUser == null) return@DisposableEffect onDispose {}

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
            val otherId = MessagingRepository.findOtherParticipantId(conversation, currentUser!!.uid)
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
                                nickname = nicknames[friend.uid],
                                onClick = {
                                    val conversationId = MessagingRepository.createConversationId(currentUser?.uid ?: "", friend.uid)
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
                        val friendUserId = MessagingRepository.findOtherParticipantId(conversation, currentUser?.uid ?: "").orEmpty()
                        val friend = friendsById[friendUserId]

                        val messageDisplayPreview = when {
                            conversation.lastMessageText.contains("[location]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You sent a location" else "Sent you a location"
                            }
                            conversation.lastMessageText.contains("[pin]", ignoreCase = true) || conversation.lastMessageText.contains("custom pin:", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You shared a pin" else "Sent you a pin"
                            }
                            conversation.lastMessageText.contains("[event_invite]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You sent an event invitation" else "Sent you an event invitation"
                            }
                            else -> conversation.lastMessageText
                        }

                        ConversationCard(
                            friendName = nicknames[friendUserId] ?: friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Friend",
                            friendEmail = friend?.email ?: "",
                            preview = messageDisplayPreview,
                            photoUrl = friend?.photoUrl,
                            timestamp = conversation.lastMessageAt,
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
    var nickname by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by rememberSaveable { mutableStateOf(initialMessage ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var conversationExists by remember { mutableStateOf(false) }
    var isFriend by remember { mutableStateOf<Boolean>(true) }

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

        SocialRepository.fetchNicknames(
            currentUserId = currentUser.uid,
            onSuccess = { map ->
                nickname = map[friendUserId]
            },
            onError = { /* ignore nickname load error */ }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "You must be friends to send messages.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppBackground)
                .padding(16.dp)
        ) {
            val currentFriend = friend
            val displayName = nickname ?: currentFriend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Chat"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(photoUrl = currentFriend?.photoUrl ?: "")

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!nickname.isNullOrBlank() && currentFriend != null) {
                        Text(
                            text = "(${SocialRepository.displayNameOrEmail(currentFriend)})",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (currentFriend != null && currentFriend.email.isNotBlank()) {
                        Text(
                            text = currentFriend.email,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        navController.navigate("${Routes.PROFILE}/$friendUserId")
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .padding(start = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF3347),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "View Profile",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (errorMessage != null && conversationExists) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet. Say hello!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
private fun FriendPickerCard(
    user: UserProfile,
    nickname: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(photoUrl = user.photoUrl)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nickname ?: SocialRepository.displayNameOrEmail(user),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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
private fun ConversationCard(
    friendName: String,
    friendEmail: String,
    photoUrl: String?,
    preview: String,
    timestamp: Long,
    onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            UserAvatar(
                photoUrl = photoUrl ?: "",
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friendName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (friendEmail.isNotBlank()) {
                    Text(
                        text = friendEmail,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preview,
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (timestamp > 0L) {
                        Text(
                            text = " • ${formatShortTimeAgo(timestamp)}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
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
private fun MessageBubble(
    message: DirectMessage,
    isMine: Boolean,
    navController: NavHostController,
    ownedPins: List<CustomPin> = emptyList(),
    onPinSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val timeString = remember(message.sentAt) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.sentAt))
    }
    
    var invite by remember { mutableStateOf<EventInvite?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    var addressText by remember { mutableStateOf<String?>(null) }
    
    // Check if the coordinate in the metadata is already saved as a pin by the current user
    val isAlreadySaved = remember(message.id, ownedPins) {
        val lat = message.metadata["lat"]?.toDoubleOrNull()
        val lng = message.metadata["lng"]?.toDoubleOrNull()
        (message.type == "pin" || message.type == "location") && lat != null && lng != null &&
        ownedPins.any { 
            abs(it.latitude - lat) < 0.00001 && 
            abs(it.longitude - lng) < 0.00001 
        }
    }

    var isPinDeletedBySender by remember { mutableStateOf(false) }

    if (message.type == "event_invite" && !isMine) {
        LaunchedEffect(message.id) {
            val inviteId = "${message.senderId}_${message.receiverId}_${message.metadata["eventId"]}"
            SocialRepository.fetchEventInvite(inviteId, { invite = it }, { inviteError = it })
        }
    }

    if (message.type == "pin") {
        LaunchedEffect(message.metadata["pinId"]) {
            val pinId = message.metadata["pinId"]
            if (pinId != null) {
                SocialRepository.checkPinExists(message.senderId, pinId) { exists ->
                    isPinDeletedBySender = !exists
                }
            }
        }
    }

    // Geocoding for rough area implementation
    val lat = message.metadata["lat"]?.toDoubleOrNull()
    val lng = message.metadata["lng"]?.toDoubleOrNull()
    if ((message.type == "location" || message.type == "pin") && lat != null && lng != null) {
        LaunchedEffect(lat, lng) {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val area = when {
                            !addr.featureName.isNullOrEmpty() && addr.featureName != addr.thoroughfare -> addr.featureName
                            !addr.thoroughfare.isNullOrEmpty() -> addr.thoroughfare
                            else -> addr.locality
                        }
                        addressText = area
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isMine) Color(0xFFEF3347) else Color.White),
            border = if (isMine) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message.type) {
                    "location", "pin" -> {
                        val isLocation = message.type == "location"
                        val isPin = message.type == "pin"
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isLocation) Icons.Default.LocationOn else Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isMine) Color.White else Color(0xFFEF3347),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPin && isPinDeletedBySender) "Pin no longer exists" else message.text,
                                color = if (isMine) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Remove 'near' for custom pin, only show for location shares
                        if (!addressText.isNullOrEmpty() && isLocation) {
                            Text(
                                text = "Near $addressText",
                                color = if (isMine) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                            )
                        }

                        if (!isPinDeletedBySender && isAlreadySaved) {
                            Text(
                                text = if (isMine) "Already on your map" else "Saved to Map", 
                                color = if (isMine) Color.White.copy(alpha = 0.9f) else Color(0xFF4CAF50), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp, 
                                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                            )
                        }

                        val shouldHideButton = (isPin && isPinDeletedBySender)

                        if (!shouldHideButton) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (lat != null && lng != null) {
                                        val route = if (isPin) {
                                            Routes.mapWithLocation(
                                                lat = lat,
                                                lng = lng,
                                                tempName = message.metadata["pinName"] ?: "Shared Pin",
                                                tempDesc = message.metadata["description"],
                                                tempColor = message.metadata["colorArgb"]?.toIntOrNull(),
                                                tempEventId = message.metadata["associatedEventId"]
                                            )
                                        } else {
                                            Routes.mapWithLocation(
                                                lat = lat,
                                                lng = lng,
                                                tempName = if (isMine) "My Shared Location" else "Shared Location",
                                                tempDesc = if (isMine) "A location you shared" else "A location shared with you"
                                            )
                                        }
                                        navController.navigate(route)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f),
                                    contentColor = if (isMine) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(34.dp).wrapContentWidth()
                            ) {
                                Text(
                                    text = if (isAlreadySaved || (isPin && isMine)) "View on Map" else "View/Add Pin on Map",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    "event_invite" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = if (isMine) Color.White else Color(0xFFEF3347),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Event Invitation",
                                color = if (isMine) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = message.metadata["eventTitle"] ?: "Unknown Event",
                            color = if (isMine) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isMine && invite?.status == "pending") {
                            Row(modifier = Modifier.padding(start = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        invite?.let {
                                            SocialRepository.acceptEventInvite(it, { invite = it.copy(status = "accepted") }, {})
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Accept", fontSize = 13.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        invite?.let {
                                            SocialRepository.declineEventInvite(it, { invite = null }, {})
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Decline", fontSize = 13.sp, color = Color.Black)
                                }
                            }
                        } else if (!isMine && invite?.status == "accepted") {
                            Text("Accepted", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 30.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { navController.navigate(Routes.CALENDAR) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (isMine) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(34.dp).wrapContentWidth().padding(start = 30.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Go to Calendar", fontSize = 14.sp)
                        }
                    }
                    else -> {
                        Text(text = message.text, color = if (isMine) Color.White else Color.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        Text(text = timeString, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp))
    }
}

private fun formatShortTimeAgo(timestamp: Long): String {if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
        }
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
