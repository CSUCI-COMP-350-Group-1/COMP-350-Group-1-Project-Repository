package com.example.cicompanion.social

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.maps.CustomPin
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandOrange
import com.example.cicompanion.ui.theme.CoralRed
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

private val FaintGray = Color(0xFFF5F5F5)

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

private fun displayUserName(
    userId: String,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    if (userId == currentUser?.uid) {
        return "You"
    }

    return nicknames[userId]
        ?.takeIf { it.isNotBlank() }
        ?: profilesById[userId]?.let { SocialRepository.displayNameOrEmail(it) }
        ?: "Member"
}

private fun conversationTitle(
    conversation: ConversationSummary,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    if (conversation.groupName.isNotBlank()) return conversation.groupName
    
    val otherIds = conversation.participantIds.filter { it != currentUser?.uid }
    if (otherIds.isEmpty()) return "Solo Chat"
    
    val names = otherIds.map { id -> displayUserName(id, currentUser, profilesById, nicknames) }
    return when {
        names.size <= 2 -> names.joinToString(" & ")
        else -> "${names.take(2).joinToString(", ")} & ${names.size - 2} more"
    }
}

private fun conversationSubtitle(
    conversation: ConversationSummary,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    val otherIds = conversation.participantIds.filter { it != currentUser?.uid }
    return otherIds.joinToString(", ") { id -> 
        displayUserName(id, currentUser, profilesById, nicknames)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavHostController, sharedLocation: String? = null) {
    val currentUser = rememberAuthUser()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rawConversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingFriends by remember { mutableStateOf(currentUser != null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var isCreatingGroup by remember { mutableStateOf(false) }

    if (showCreateGroupDialog && currentUser != null) {
        val signedInUser = currentUser
        CreateGroupChatDialog(
            friends = friends,
            nicknames = nicknames,
            isCreating = isCreatingGroup,
            onDismiss = {
                if (!isCreatingGroup) showCreateGroupDialog = false
            },
            onCreate = { selectedFriendIds, groupName ->
                isCreatingGroup = true
                MessagingRepository.createGroupConversation(
                    currentUser = signedInUser,
                    selectedFriendIds = selectedFriendIds,
                    groupName = groupName,
                    onSuccess = { newConversationId ->
                        isCreatingGroup = false
                        showCreateGroupDialog = false
                        navController.navigate(Routes.groupMessageThread(newConversationId))
                    },
                    onError = { error ->
                        isCreatingGroup = false
                        errorMessage = error
                    }
                )
            }
        )
    }

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
    val currentUserId = currentUser?.uid.orEmpty()

    val activeConversations = remember(rawConversations, friendsById, currentUserId) {
        if (currentUserId.isBlank()) {
            emptyList()
        } else {
            rawConversations.filter { conversation ->
                val otherParticipantIds = MessagingRepository.findOtherParticipantIds(
                    conversation = conversation,
                    currentUserId = currentUserId
                )

                currentUserId in conversation.participantIds &&
                        if (conversation.isGroup) {
                            true
                        } else {
                            otherParticipantIds.size == 1 &&
                                    friendsById.containsKey(otherParticipantIds.first())
                        }
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Social", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.FRIENDS_AND_REQUESTS) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Friends", tint = Color.Black)
                    }
                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group", tint = Color.Black)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (sharedLocation != null) {
                Surface(
                    color = CoralRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = CoralRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sharing location: \"$sharedLocation\". Pick a chat to send to.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                }
            }

            Text(
                text = "Friends",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when {
                isLoadingFriends -> {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoralRed, modifier = Modifier.size(24.dp))
                    }
                }
                friends.isEmpty() -> {
                    Surface(
                        onClick = { navController.navigate(Routes.FRIENDS_AND_REQUESTS) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text("Find friends to start chatting", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                else -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Messages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (activeConversations.isEmpty() && !isLoadingFriends) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No conversations yet", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = activeConversations,
                        key = { conversation: ConversationSummary -> conversation.id }
                    ) { conversation: ConversationSummary ->

                        val friendUserId = MessagingRepository
                            .findOtherParticipantId(conversation, currentUser?.uid ?: "")
                            .orEmpty()

                        val friend = friendsById[friendUserId]

                        val title = conversationTitle(
                            conversation = conversation,
                            currentUser = currentUser,
                            profilesById = friendsById,
                            nicknames = nicknames
                        )

                        val subtitle = if (conversation.isGroup) {
                            conversationSubtitle(
                                conversation = conversation,
                                currentUser = currentUser,
                                profilesById = friendsById,
                                nicknames = nicknames
                            )
                        } else {
                            friend?.email.orEmpty()
                        }

                        val previewText = when {
                            conversation.lastMessageText.contains("[location]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You sent a location" else "Sent a location"
                            }
                            conversation.lastMessageText.contains("[pin]", ignoreCase = true) ||
                                    conversation.lastMessageText.contains("custom pin:", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You shared a pin" else "Shared a pin"
                            }
                            conversation.lastMessageText.contains("[event_invite]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) "You sent an event invite" else "Sent an event invite"
                            }
                            else -> conversation.lastMessageText
                        }

                        ConversationCard(
                            title = title,
                            subtitle = subtitle,
                            preview = previewText,
                            photoUrl = if (conversation.isGroup) "" else friend?.photoUrl,
                            isGroup = conversation.isGroup,
                            timestamp = conversation.lastMessageAt,
                            onClick = {
                                val initialMsg = if (sharedLocation != null) "Check out this custom pin: $sharedLocation" else null
                                if (conversation.isGroup) {
                                    navController.navigate(Routes.groupMessageThread(conversation.id, initialMsg))
                                } else {
                                    navController.navigate(Routes.messageThread(conversation.id, friendUserId, initialMsg))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
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

    val isGroupThread = friendUserId == Routes.GROUP_THREAD_USER_ID

    var conversation by remember { mutableStateOf<ConversationSummary?>(null) }
    var memberProfilesById by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var friendStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showEditGroupNameDialog by remember { mutableStateOf(false) }

    var friend by remember { mutableStateOf<UserProfile?>(null) }
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
                    if (location != null && currentUser != null) {
                        val threadMessage = "My current location"
                        val meta = mapOf("lat" to location.latitude.toString(), "lng" to location.longitude.toString())
                        
                        if (isGroupThread && conversation != null) {
                            MessagingRepository.sendMessageToConversation(currentUser, conversation!!, threadMessage, "location", meta, {}, { _ -> })
                        } else if (friend != null) {
                            MessagingRepository.sendMessage(currentUser, friend!!, threadMessage, "location", meta, {}, { _ -> })
                        }
                    }
                }
        }
    }

    if (currentUser == null) {
        SignedOutMessagingMessage()
        return
    }

    val currentConversation = conversation
    val groupHasAtLeastOneFriend = currentConversation
        ?.participantIds
        ?.filter { it != currentUser.uid }
        ?.any { friendStatuses[it] == "accepted" }
        ?: false

    val canSendMessages = if (isGroupThread) {
        currentConversation?.participantIds?.contains(currentUser.uid) == true && groupHasAtLeastOneFriend
    } else {
        isFriend
    }

    fun sendThreadMessage(
        text: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit = {}
    ) {
        if (isGroupThread) {
            val targetConversation = conversation ?: return
            MessagingRepository.sendMessageToConversation(currentUser, targetConversation, text, type, metadata, {
                conversationExists = true; errorMessage = null; isSending = false; onSuccess()
            }, { errorMessage = it; isSending = false })
        } else {
            val targetFriend = friend ?: return
            MessagingRepository.sendMessage(currentUser, targetFriend, text, type, metadata, {
                conversationExists = true; errorMessage = null; isSending = false; onSuccess()
            }, { errorMessage = it; isSending = false })
        }
    }

    LaunchedEffect(currentUser.uid, currentConversation?.participantIds, isGroupThread) {
        if (!isGroupThread) { memberProfilesById = emptyMap(); return@LaunchedEffect }
        val participantIds = currentConversation?.participantIds.orEmpty()
        if (participantIds.isEmpty()) return@LaunchedEffect
        MessagingRepository.fetchUserProfiles(participantIds, { memberProfilesById = it; errorMessage = null }, { errorMessage = it })
    }

    LaunchedEffect(currentUser.uid, friendUserId, isGroupThread) {
        SocialRepository.fetchAllFriendRequestStatuses(currentUser.uid, { statuses ->
            friendStatuses = statuses
            if (!isGroupThread) isFriend = statuses[friendUserId] == "accepted"
        }, { friendStatuses = emptyMap(); if (!isGroupThread) isFriend = false })

        SocialRepository.fetchNicknames(currentUser.uid, { map ->
            nicknames = map
            if (!isGroupThread) friend = friend // trigger re-eval
        }, { })

        if (!isGroupThread) {
            MessagingRepository.fetchUserProfile(friendUserId, { friend = it; errorMessage = null }, { errorMessage = it })
        }
    }

    DisposableEffect(conversationId, isGroupThread) {
        if (!isGroupThread) onDispose { } else {
            val reg = MessagingRepository.listenToConversation(conversationId, { conversation = it; conversationExists = it != null; errorMessage = null }, { errorMessage = it })
            onDispose { reg.remove() }
        }
    }

    LaunchedEffect(conversationId, isGroupThread) {
        if (isGroupThread) return@LaunchedEffect
        MessagingRepository.checkConversationExists(conversationId, { conversationExists = it; errorMessage = null }, { errorMessage = it })
    }

    DisposableEffect(conversationId, conversationExists) {
        if (!conversationExists) onDispose { } else {
            val reg = MessagingRepository.listenToMessages(conversationId, { messages = it; errorMessage = null }, { errorMessage = it })
            onDispose { reg.remove() }
        }
    }

    DisposableEffect(currentUser.uid) {
        val pinsReg = SocialRepository.listenToCustomPins(currentUser.uid, { userPins = it }, {})
        val eventsReg = SocialRepository.listenToCustomEvents(currentUser.uid, { userEvents = it }, {})
        onDispose { pinsReg.remove(); eventsReg.remove() }
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    fun sendEventInviteToThread(event: CalendarEvent, onSuccess: () -> Unit = {}) {
        if (isGroupThread) {
            val targetConversation = conversation ?: return
            val recipientIds = targetConversation.participantIds.filter { it != currentUser.uid }
            if (recipientIds.isEmpty()) return
            var completedCount = 0
            var failed = false
            recipientIds.forEach { targetUserId ->
                SocialRepository.sendEventInvite(currentUser, targetUserId, event, {
                    completedCount++
                    if (!failed && completedCount == recipientIds.size) {
                        sendThreadMessage("Sent an invite for: ${event.title}", "event_invite", mapOf("eventId" to event.id, "eventTitle" to event.title), onSuccess)
                    }
                }, { if (!failed) { failed = true; errorMessage = it } })
            }
        } else {
            SocialRepository.sendEventInvite(currentUser, friendUserId, event, {
                sendThreadMessage("Sent you an invite for: ${event.title}", "event_invite", mapOf("eventId" to event.id, "eventTitle" to event.title), onSuccess)
            }, { errorMessage = it })
        }
    }

    if (showPinPicker) {
        ItemPickerBottomSheet(
            title = "Share a Pin",
            items = userPins,
            itemName = { it.name },
            itemIcon = Icons.Default.PushPin,
            onItemSelected = { pin ->
                sendThreadMessage("Pin: ${pin.name}", "pin", mapOf("pinId" to pin.id, "pinName" to pin.name, "lat" to pin.latitude.toString(), "lng" to pin.longitude.toString(), "description" to pin.description, "colorArgb" to pin.colorArgb.toString(), "associatedEventId" to (pin.associatedEventId ?: "")))
                showPinPicker = false
            },
            onDismiss = { showPinPicker = false }
        )
    }

    if (showEventPicker) {
        ItemPickerBottomSheet(
            title = "Share an Event",
            items = userEvents.filter { it.ownerId == currentUser.uid },
            itemName = { it.title },
            itemIcon = Icons.Default.Event,
            onItemSelected = { event ->
                sendEventInviteToThread(event) { conversationExists = true; errorMessage = null }
                showEventPicker = false
            },
            onDismiss = { showEventPicker = false }
        )
    }

    if (showEditGroupNameDialog && isGroupThread && currentConversation != null) {
        EditGroupNameDialog(
            initialName = currentConversation.groupName,
            onDismiss = { showEditGroupNameDialog = false },
            onSave = { newName ->
                MessagingRepository.updateGroupName(conversationId, currentUser.uid, newName, { showEditGroupNameDialog = false; errorMessage = null }, { errorMessage = it })
            }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (canSendMessages) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = CoralRed)
                        }
                        DropdownMenu(expanded = showAttachmentMenu, onDismissRequest = { showAttachmentMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("My Location") },
                                onClick = {
                                    showAttachmentMenu = false
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                                            if (location != null) sendThreadMessage("My current location", "location", mapOf("lat" to location.latitude.toString(), "lng" to location.longitude.toString()))
                                        }
                                    } else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Pin") },
                                onClick = { showAttachmentMenu = false; showPinPicker = true },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Event") },
                                onClick = { showAttachmentMenu = false; showEventPicker = true },
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
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { isSending = true; sendThreadMessage(messageText, onSuccess = { messageText = ""; isSending = false }) },
                        enabled = messageText.isNotBlank() && !isSending && canSendMessages
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = CoralRed)
                    }
                }
            } else {
                Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isGroupThread) "You must be friends with a member to chat." else "You must be friends to chat.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(AppBackground).padding(16.dp)) {
            val currentFriend = friend
            val displayName = if (isGroupThread && currentConversation != null) {
                conversationTitle(currentConversation, currentUser, memberProfilesById, nicknames)
            } else {
                nicknames[friendUserId] ?: currentFriend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Chat"
            }

            val subtitleText = if (isGroupThread && currentConversation != null) {
                conversationSubtitle(currentConversation, currentUser, memberProfilesById, nicknames)
            } else {
                currentFriend?.email.orEmpty()
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (isGroupThread) {
                    Surface(shape = CircleShape, color = BrandOrange.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = BrandOrange) }
                    }
                } else {
                    UserAvatar(photoUrl = currentFriend?.photoUrl ?: "")
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitleText.isNotBlank()) Text(subtitleText, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isGroupThread) {
                    TextButton(onClick = { showEditGroupNameDialog = true }) { Text("Rename", color = CoralRed, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = { navController.navigate("${Routes.PROFILE}/$friendUserId") }) { Text("Profile", color = CoralRed, fontWeight = FontWeight.Bold) }
                }
            }
            if (errorMessage != null && conversationExists) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No messages yet.", color = Color.Gray) }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isMine = message.senderId == currentUser.uid,
                            navController = navController,
                            ownedPins = userPins,
                            senderName = if (isGroupThread) displayUserName(message.senderId, currentUser, memberProfilesById, nicknames) else null,
                            currentUserId = currentUser.uid
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendPickerCard(user: UserProfile, nickname: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(64.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(photoUrl = user.photoUrl, size = 56.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = nickname ?: SocialRepository.displayNameOrEmail(user).split(" ").firstOrNull() ?: "Friend",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ItemPickerBottomSheet(
    title: String,
    items: List<T>,
    itemName: (T) -> String,
    itemIcon: ImageVector,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items else items.filter { itemName(it).contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoralRed),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No items found.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filteredItems) { item ->
                        Surface(
                            onClick = { onItemSelected(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = FaintGray
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(itemIcon, null, tint = CoralRed, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(itemName(item), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupChatDialog(
    friends: List<UserProfile>,
    nicknames: Map<String, String>,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (selectedFriendIds: List<String>, groupName: String) -> Unit
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    val selectedFriendIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group Chat", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(80) },
                    label = { Text("Group name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Select Members", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends, key = { it.uid }) { friend ->
                        val checked = friend.uid in selectedFriendIds
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (checked) selectedFriendIds.remove(friend.uid) else selectedFriendIds.add(friend.uid)
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { 
                                if (it) selectedFriendIds.add(friend.uid) else selectedFriendIds.remove(friend.uid)
                            }, colors = CheckboxDefaults.colors(checkedColor = CoralRed))
                            UserAvatar(photoUrl = friend.photoUrl, size = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(nicknames[friend.uid] ?: SocialRepository.displayNameOrEmail(friend), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(selectedFriendIds.toList(), groupName) }, enabled = selectedFriendIds.size >= 2 && !isCreating, colors = ButtonDefaults.buttonColors(containerColor = CoralRed)) {
                Text(if (isCreating) "Creating..." else "Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}

@Composable
private fun EditGroupNameDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var groupName by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Group") },
        text = {
            OutlinedTextField(value = groupName, onValueChange = { groupName = it.take(80) }, label = { Text("Group name") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { onSave(groupName) }, colors = ButtonDefaults.buttonColors(containerColor = CoralRed)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConversationCard(
    title: String,
    subtitle: String,
    photoUrl: String?,
    preview: String,
    timestamp: Long,
    isGroup: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isGroup) {
                Surface(shape = CircleShape, color = BrandOrange.copy(alpha = 0.1f), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = BrandOrange, modifier = Modifier.size(28.dp)) }
                }
            } else {
                UserAvatar(photoUrl = photoUrl ?: "", size = 52.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (timestamp > 0L) {
                        Text(" • ${formatShortTimeAgo(timestamp)}", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(preview, color = Color.DarkGray, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun UserAvatar(photoUrl: String?, size: androidx.compose.ui.unit.Dp = 40.dp) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile Photo",
            modifier = Modifier.size(size).clip(CircleShape).background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = Color.LightGray.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(size * 0.6f))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: DirectMessage,
    isMine: Boolean,
    navController: NavHostController,
    ownedPins: List<CustomPin> = emptyList(),
    senderName: String? = null,
    currentUserId: String = ""
) {
    val context = LocalContext.current
    val timeString = remember(message.sentAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.sentAt))
    }
    
    var invite by remember { mutableStateOf<EventInvite?>(null) }
    var addressText by remember { mutableStateOf<String?>(null) }
    
    val isAlreadySaved = remember(message.id, ownedPins) {
        val lat = message.metadata["lat"]?.toDoubleOrNull()
        val lng = message.metadata["lng"]?.toDoubleOrNull()
        (message.type == "pin" || message.type == "location") && lat != null && lng != null &&
        ownedPins.any { abs(it.latitude - lat) < 0.00001 && abs(it.longitude - lng) < 0.00001 }
    }

    var isPinDeletedBySender by remember { mutableStateOf(false) }

    if (message.type == "event_invite" && !isMine) {
        LaunchedEffect(message.id, currentUserId) {
            val eventId = message.metadata["eventId"].orEmpty()
            val inviteRecipientId = message.receiverId.ifBlank { currentUserId }
            if (eventId.isNotBlank() && inviteRecipientId.isNotBlank()) {
                val inviteId = "${message.senderId}_${inviteRecipientId}_${eventId}"
                SocialRepository.fetchEventInvite(inviteId, { invite = it }, { })
            }
        }
    }

    if (message.type == "pin") {
        LaunchedEffect(message.metadata["pinId"]) {
            val pinId = message.metadata["pinId"]
            if (pinId != null) {
                SocialRepository.checkPinExists(message.senderId, pinId) { exists -> isPinDeletedBySender = !exists }
            }
        }
    }

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
                        addressText = when {
                            !addr.featureName.isNullOrEmpty() && addr.featureName != addr.thoroughfare -> addr.featureName
                            !addr.thoroughfare.isNullOrEmpty() -> addr.thoroughfare
                            else -> addr.locality
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        if (!isMine && !senderName.isNullOrBlank()) {
            Text(text = senderName, color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isMine) CoralRed else Color.White),
            border = if (isMine) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message.type) {
                    "location", "pin" -> {
                        val isLocation = message.type == "location"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isLocation) Icons.Default.LocationOn else Icons.Default.PushPin, null, tint = if (isMine) Color.White else CoralRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (message.type == "pin" && isPinDeletedBySender) "Pin no longer exists" else message.text, color = if (isMine) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (!addressText.isNullOrEmpty() && isLocation) {
                            Text("Near $addressText", color = if (isMine) Color.White.copy(alpha = 0.8f) else Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 26.dp, top = 2.dp))
                        }
                        if (!isPinDeletedBySender && isAlreadySaved) {
                            Text(if (isMine) "Already on map" else "Saved to Map", color = if (isMine) Color.White.copy(alpha = 0.9f) else Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(start = 26.dp, top = 2.dp))
                        }
                        if (!(message.type == "pin" && isPinDeletedBySender)) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                onClick = {
                                    if (lat != null && lng != null) {
                                        val route = if (message.type == "pin") Routes.mapWithLocation(lat, lng, message.metadata["pinName"] ?: "Shared Pin", message.metadata["description"], message.metadata["colorArgb"]?.toIntOrNull(), message.metadata["associatedEventId"])
                                        else Routes.mapWithLocation(lat, lng, if (isMine) "My Shared Location" else "Shared Location", if (isMine) "A location you shared" else "A location shared with you")
                                        navController.navigate(route)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f)
                            ) {
                                Text(if (isAlreadySaved || (message.type == "pin" && isMine)) "View on Map" else "View/Add Pin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isMine) Color.White else Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                    "event_invite" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = if (isMine) Color.White else CoralRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Event Invite", color = if (isMine) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(message.metadata["eventTitle"] ?: "Event", color = if (isMine) Color.White else Color.Black, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp, modifier = Modifier.padding(start = 28.dp))
                        if (!isMine && invite?.status == "pending") {
                            Row(Modifier.padding(start = 28.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { invite?.let { SocialRepository.acceptEventInvite(it, { invite = it.copy(status = "accepted") }, {}) } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Accept", fontSize = 12.sp) }
                                Button(onClick = { invite?.let { SocialRepository.declineEventInvite(it, { invite = null }, {}) } }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Decline", fontSize = 12.sp, color = Color.Black) }
                            }
                        } else if (!isMine && invite?.status == "accepted") {
                            Text("Accepted", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 28.dp, top = 4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(onClick = { navController.navigate(Routes.CALENDAR) }, shape = RoundedCornerShape(8.dp), color = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 28.dp)) { Text("Go to Calendar", fontSize = 12.sp, color = if (isMine) Color.White else Color.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                    else -> {
                        Text(text = message.text, color = if (isMine) Color.White else Color.Black, fontSize = 15.sp)
                    }
                }
            }
        }
        Text(text = timeString, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
    }
}

private fun formatShortTimeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""
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
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun SignedOutMessagingMessage() {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Text("Please sign in to view your messages.")
    }
}
