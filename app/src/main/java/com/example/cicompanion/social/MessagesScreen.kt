package com.example.cicompanion.social

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                                nickname = nicknames[friend.uid],
                                onClick = {
                                    navController.navigate(
                                        Routes.messageThread(
                                            MessagingRepository.createConversationId(
                                                currentUser?.uid ?: "",
                                                friend.uid
                                            ),
                                            friend.uid
                                        )
                                    )
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
                        /*val friendUserId = MessagingRepository.findOtherParticipantId(
                            conversation = conversation,
                            currentUserId = currentUser?.uid ?: ""
                        ).orEmpty()*/

                        val friendUserId = MessagingRepository.findOtherParticipantId(conversation, currentUser.uid).orEmpty()
                        val friend = friendsById[friendUserId]

                        ConversationCard(
                            friendName = nicknames[friendUserId] ?: friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Friend",
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
    navController: NavHostController, // warning about unused param is wrong
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
    var isFriend by remember { mutableStateOf(true) }

    var userPins by remember { mutableStateOf<List<CustomPin>>(emptyList()) }
    var userEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var showPinPicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(currentUser?.uid, friendUserId) {
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
            nickname = null
            messages = emptyList()
            messageText = ""
            errorMessage = null
            isSending = false
            conversationExists = false
            return@LaunchedEffect
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

    // For a brand-new chat, the conversation doc may not exist yet.
    LaunchedEffect(conversationId) {
        MessagingRepository.checkConversationExists(
            conversationId = conversationId,
            onResult = { exists ->
                conversationExists = exists


                // If the check succeeds, clear any stale permission error.
                errorMessage = null
            },
            onError = { errorMessage = it }
        )
    }


    // Do not start listening to /messages until the parent conversation exists.
    DisposableEffect(conversationId, conversationExists) {
        if (!conversationExists) {
            onDispose { }
        } else {
            val registration = MessagingRepository.listenToMessages(
                conversationId = conversationId,
                onUpdate = {
                    messages = it

                    // If messages are loading successfully, clear stale errors.
                    errorMessage = null
                },
                onError = { errorMessage = it }
            )

            onDispose {
                registration.remove()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (currentUser == null) {
        SignedOutMessagingMessage()
        return
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

                                    // The first sent message creates the conversation doc,
                                    // so start listening immediately after send succeeds.
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
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!nickname.isNullOrBlank() && currentFriend != null) {
                Text(
                    text = "(${SocialRepository.displayNameOrEmail(currentFriend)})",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (currentFriend != null && currentFriend.email.isNotBlank()) {
                Text(
                    text = currentFriend.email,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }


            // Only show errors for existing conversations.
            // A brand-new conversation should not show a scary red permission message.
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
                            text = message.text,
                            sentAt = message.sentAt,
                            isMine = message.senderId == currentUser.uid
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
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = nickname ?: SocialRepository.displayNameOrEmail(user),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(friendName, fontWeight = FontWeight.Bold)
            if (friendEmail.isNotBlank()) {
                Text(
                    text = friendEmail,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = preview,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageBubble(
    text: String,
    sentAt: Long,
    isMine: Boolean
) {
    val timeString = remember(sentAt) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(sentAt))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) Color(0xFFEF3347) else Color.White
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isMine) Color.White else Color.Black
            )
        }
        Text(
            text = timeString,
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Mail, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.Gray)
        }
    }
}

@Composable
private fun SignedOutMessagingMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Text("Please sign in to view your messages.")
    }
}
