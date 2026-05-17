package com.example.cicompanion.social

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

// GROUP MESSAGING CHANGE
// one display name helper used by list, header, and message bubbles
private fun displayUserName(
    userId: String,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    if (userId == currentUser?.uid) {
        return currentUser.displayName
            ?.takeIf { it.isNotBlank() }
            ?: currentUser.email
            ?: "You"
    }

    return nicknames[userId]
        ?.takeIf { it.isNotBlank() }
        ?: profilesById[userId]?.let { SocialRepository.displayNameOrEmail(it) }
        ?: "Member"
}

// GROUP MESSAGING
// if no custom group name exists, show all participant names/nicknames.
private fun conversationTitle(
    conversation: ConversationSummary,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    return conversation.groupName
        .takeIf { it.isNotBlank() }
        ?: conversation.participantIds.joinToString(", ") { userId ->
            displayUserName(userId, currentUser, profilesById, nicknames)
        }
}

private fun conversationSubtitle(
    conversation: ConversationSummary,
    currentUser: FirebaseUser?,
    profilesById: Map<String, UserProfile>,
    nicknames: Map<String, String>
): String {
    return conversation.participantIds.joinToString(", ") { userId ->
        displayUserName(userId, currentUser, profilesById, nicknames)
    }
}

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
                // GROUP MESSAGING
                // create the group conversation before navigating
                // so shared group name can sync immediately
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

    // GROUP MESSAGING
    // Direct chats require the other user to be your friend.
    // Group chats are shown if you are in the group and at least one other group member is your friend.
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
                            otherParticipantIds.any { friendId -> friendsById.containsKey(friendId) }
                        } else {
                            otherParticipantIds.size == 1 &&
                                    friendsById.containsKey(otherParticipantIds.first())
                        }
            }
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


            Button(
                onClick = { showCreateGroupDialog = true },
                enabled = friends.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF3347),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFEF3347).copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Group Chat")
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
                    items(
                        items = activeConversations,
                        key = { conversation: ConversationSummary -> conversation.id }
                    ) { conversation: ConversationSummary ->

                        val friendUserId = MessagingRepository
                            .findOtherParticipantId(conversation, currentUser?.uid ?: "")
                            .orEmpty()

                        val friend = friendsById[friendUserId]

                        // GROUP MESSAGING
                        // group conversations display their shared name,
                        // or back to all participant names/nicknames.
                        val title = if (conversation.isGroup) {
                            conversationTitle(
                                conversation = conversation,
                                currentUser = currentUser,
                                profilesById = friendsById,
                                nicknames = nicknames
                            )
                        } else {
                            nicknames[friendUserId]
                                ?: friend?.let { SocialRepository.displayNameOrEmail(it) }
                                ?: "Friend"
                        }

                        // GROUP MESSAGING
                        // group subtitle shows members direct subtitle shows email
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

                        val messageDisplayPreview = when {
                            conversation.lastMessageText.contains("[location]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) {
                                    "You sent a location"
                                } else {
                                    "Sent a location"
                                }
                            }

                            conversation.lastMessageText.contains("[pin]", ignoreCase = true) ||
                                    conversation.lastMessageText.contains("custom pin:", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) {
                                    "You shared a pin"
                                } else {
                                    "Shared a pin"
                                }
                            }

                            conversation.lastMessageText.contains("[event_invite]", ignoreCase = true) -> {
                                if (conversation.lastMessageSenderId == currentUser?.uid) {
                                    "You sent an event invitation"
                                } else {
                                    "Sent an event invitation"
                                }
                            }

                            else -> conversation.lastMessageText
                        }

                        ConversationCard(
                            friendName = title,
                            friendEmail = subtitle,
                            preview = messageDisplayPreview,
                            photoUrl = if (conversation.isGroup) "" else friend?.photoUrl,
                            timestamp = conversation.lastMessageAt,
                            onClick = {
                                val initialMsg = if (sharedLocation != null) {
                                    "Check out this custom pin: $sharedLocation"
                                } else {
                                    null
                                }

                                if (conversation.isGroup) {
                                    navController.navigate(
                                        Routes.groupMessageThread(conversation.id, initialMsg)
                                    )
                                } else {
                                    navController.navigate(
                                        Routes.messageThread(conversation.id, friendUserId, initialMsg)
                                    )
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

    // GROUP MESSAGING
    // true when this screen was opened for a group chat
    val isGroupThread = friendUserId == Routes.GROUP_THREAD_USER_ID

    // GROUP MESSAGING
    // live conversation document, used for group name, members, and sending.
    var conversation by remember { mutableStateOf<ConversationSummary?>(null) }

    // GROUP MESSAGING
    // profiles/nicknames for group member display.
    var memberProfilesById by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // GROUP MESSAGING:
    // used to allow group sending if current user is friends with at least one group member.
    var friendStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // GROUP MESSAGING C
    // rename dialog for group chats.
    var showEditGroupNameDialog by remember { mutableStateOf(false) }

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

    val currentConversation = conversation

// GROUP MESSAGING
// A group member can send if:
// 1. they are actually in the group, and
// 2. they are friends with at least one other member of the group.
    val groupHasAtLeastOneFriend = currentConversation
        ?.participantIds
        ?.filter { userId -> userId != currentUser.uid }
        ?.any { userId -> friendStatuses[userId] == "accepted" }
        ?: false

    val canSendMessages = if (isGroupThread) {
        currentConversation?.participantIds?.contains(currentUser.uid) == true &&
                groupHasAtLeastOneFriend
    } else {
        isFriend
    }

    // GROUP MESSAGING CHANG:
    fun sendThreadMessage(
        text: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit = {}
    ) {
        if (isGroupThread) {
            val targetConversation = conversation
            if (targetConversation == null) {
                errorMessage = "Group chat is still loading."
                isSending = false
                return
            }

            MessagingRepository.sendMessageToConversation(
                currentUser = currentUser,
                conversation = targetConversation,
                messageText = text,
                type = type,
                metadata = metadata,
                onSuccess = {
                    conversationExists = true
                    errorMessage = null
                    isSending = false
                    onSuccess()
                },
                onError = {
                    errorMessage = it
                    isSending = false
                }
            )
        } else {
            val targetFriend = friend
            if (targetFriend == null) {
                errorMessage = "Chat is still loading."
                isSending = false
                return
            }

            MessagingRepository.sendMessage(
                currentUser = currentUser,
                friend = targetFriend,
                messageText = text,
                type = type,
                metadata = metadata,
                onSuccess = {
                    conversationExists = true
                    errorMessage = null
                    isSending = false
                    onSuccess()
                },
                onError = {
                    errorMessage = it
                    isSending = false
                }
            )
        }
    }

    // GROUP MESSAGING CHANGE:
    // Load group member profiles so group title can fall back to names/nicknames.
    LaunchedEffect(currentUser.uid, currentConversation?.participantIds, isGroupThread) {
        if (!isGroupThread) {
            memberProfilesById = emptyMap()
            return@LaunchedEffect
        }

        val participantIds = currentConversation?.participantIds.orEmpty()
        if (participantIds.isEmpty()) {
            memberProfilesById = emptyMap()
            return@LaunchedEffect
        }

        MessagingRepository.fetchUserProfiles(
            userIds = participantIds,
            onSuccess = { profiles ->
                memberProfilesById = profiles
                errorMessage = null
            },
            onError = { errorMessage = it }
        )
    }

    LaunchedEffect(currentUser.uid, friendUserId, isGroupThread) {
        SocialRepository.fetchAllFriendRequestStatuses(
            currentUserId = currentUser.uid,
            onSuccess = { statuses ->
                friendStatuses = statuses

                // GROUP MESSAGING CHANGE:
                // Direct chats still require friendship with the other user.
                // Group chats are handled by canSendMessages
                if (!isGroupThread) {
                    isFriend = statuses[friendUserId] == "accepted"
                }
            },
            onError = {
                friendStatuses = emptyMap()
                if (!isGroupThread) {
                    isFriend = false
                }
            }
        )

        SocialRepository.fetchNicknames(
            currentUserId = currentUser.uid,
            onSuccess = { map ->
                nicknames = map
                nickname = if (isGroupThread) null else map[friendUserId]
            },
            onError = {
                // Ignore nickname load error.
            }
        )

        if (isGroupThread) {
            // GROUP MESSAGING CHANGE:
            // Do not try to fetch user profile for the fake route id "group".
            friend = null
            nickname = null
            isFriend = true
        } else {
            MessagingRepository.fetchUserProfile(
                userId = friendUserId,
                onSuccess = {
                    friend = it
                    errorMessage = null
                },
                onError = { errorMessage = it }
            )
        }
    }

    // GROUP MESSAGING CHANGE:
    // Group chats need a live conversation listener so group-name changes sync
    // Direct chats keep the old existence-check flow so new 1-on-1 chats do not crash or act like group chats.
    DisposableEffect(conversationId, isGroupThread) {
        if (!isGroupThread) {
            onDispose { }
        } else {
            val registration = MessagingRepository.listenToConversation(
                conversationId = conversationId,
                onUpdate = { updatedConversation ->
                    conversation = updatedConversation
                    conversationExists = updatedConversation != null
                    errorMessage = null
                },
                onError = { errorMessage = it }
            )

            onDispose {
                registration.remove()
            }
        }
    }

    LaunchedEffect(conversationId, isGroupThread) {
        if (isGroupThread) {
            return@LaunchedEffect
        }

        // Direct messages use the original direct-chat behavior.
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

    // GROUP MESSAGING CHANGE:
    // EventInvite documents are still one recipient perdocument.
    // For group chats, create one invite per group member, then send one group chat message.
    fun sendEventInviteToThread(
        event: CalendarEvent,
        onSuccess: () -> Unit = {}
    ) {
        if (isGroupThread) {
            val targetConversation = conversation
            if (targetConversation == null) {
                errorMessage = "Group chat is still loading."
                return
            }

            val recipientIds = targetConversation.participantIds
                .filter { userId -> userId != currentUser.uid }

            if (recipientIds.isEmpty()) {
                errorMessage = "No group members found."
                return
            }

            var completedCount = 0
            var failed = false

            recipientIds.forEach { targetUserId ->
                SocialRepository.sendEventInvite(
                    currentUser = currentUser,
                    targetUserId = targetUserId,
                    event = event,
                    onSuccess = {
                        completedCount++

                        if (!failed && completedCount == recipientIds.size) {
                            sendThreadMessage(
                                text = "Sent an invite for: ${event.title}",
                                type = "event_invite",
                                metadata = mapOf(
                                    "eventId" to event.id,
                                    "eventTitle" to event.title
                                ),
                                onSuccess = onSuccess
                            )
                        }
                    },
                    onError = { error ->
                        if (!failed) {
                            failed = true
                            errorMessage = error
                        }
                    }
                )
            }
        } else {
            // Direct chat behavior stays the same: one invite to the one friend.
            SocialRepository.sendEventInvite(
                currentUser = currentUser,
                targetUserId = friendUserId,
                event = event,
                onSuccess = {
                    sendThreadMessage(
                        text = "Sent you an invite for: ${event.title}",
                        type = "event_invite",
                        metadata = mapOf(
                            "eventId" to event.id,
                            "eventTitle" to event.title
                        ),
                        onSuccess = onSuccess
                    )
                },
                onError = { errorMessage = it }
            )
        }
    }

    if (showPinPicker) {
        ItemPickerDialog(
            title = "Select a Pin",
            items = userPins,
            itemName = { it.name },
            onItemSelected = { pin ->
                // GROUP MESSAGING CHANGE:
                // custom pins now work in direct and group chats.
                sendThreadMessage(
                    text = "Pin: ${pin.name}",
                    type = "pin",
                    metadata = mapOf(
                        "pinId" to pin.id,
                        "pinName" to pin.name,
                        "lat" to pin.latitude.toString(),
                        "lng" to pin.longitude.toString(),
                        "description" to pin.description,
                        "colorArgb" to pin.colorArgb.toString(),
                        "associatedEventId" to (pin.associatedEventId ?: "")
                    )
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
                // GROUP MESSAGING CHANG
                // Works for both direct and group chats
                // Direct chat sends one invite
                // Group chat sends one invite per group member, then one group message
                sendEventInviteToThread(
                    event = event,
                    onSuccess = {
                        conversationExists = true
                        errorMessage = null
                    }
                )

                showEventPicker = false
            },
            onDismiss = { showEventPicker = false }
        )
    }

    // GROUP MESSAGING CHANGE:
    // Any member can rename the group. The name is stored on the conversation document,
    // so every group member sees the new name live
    if (showEditGroupNameDialog && isGroupThread && currentConversation != null) {
        EditGroupNameDialog(
            initialName = currentConversation.groupName,
            onDismiss = { showEditGroupNameDialog = false },
            onSave = { newName ->
                MessagingRepository.updateGroupName(
                    conversationId = conversationId,
                    currentUserId = currentUser.uid,
                    newGroupName = newName,
                    onSuccess = {
                        showEditGroupNameDialog = false
                        errorMessage = null
                    },
                    onError = { errorMessage = it }
                )
            }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (canSendMessages) {
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
                                                if (location != null) {
                                                    // GROUP MESSAGING CHANGE:
                                                    // location sharing now works in direct and group chats
                                                    sendThreadMessage(
                                                        text = "My current location",
                                                        type = "location",
                                                        metadata = mapOf(
                                                            "lat" to location.latitude.toString(),
                                                            "lng" to location.longitude.toString()
                                                        )
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
                            isSending = true

                            // GROUP MESSAGING CHANGE:
                            // Use the shared direct/group send helper.
                            // Group chats do not have a single friend object.
                            sendThreadMessage(
                                text = messageText,
                                onSuccess = {
                                    messageText = ""
                                    isSending = false
                                }
                            )
                        },
                        enabled = messageText.isNotBlank() && !isSending && canSendMessages
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFFEF3347)
                        )
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
                        text = if (isGroupThread) {
                            "You must be friends with at least one group member to send messages."
                        } else {
                            "You must be friends to send messages."
                        },
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

// GROUP MESSAGING CHANGE:
// Direct chat title uses friend/nickname.
// Group chat title uses groupName, or falls back to participant names/nicknames.
            val displayName = if (isGroupThread && currentConversation != null) {
                conversationTitle(
                    conversation = currentConversation,
                    currentUser = currentUser,
                    profilesById = memberProfilesById,
                    nicknames = nicknames
                )
            } else {
                nickname ?: currentFriend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Chat"
            }

            val subtitle = if (isGroupThread && currentConversation != null) {
                conversationSubtitle(
                    conversation = currentConversation,
                    currentUser = currentUser,
                    profilesById = memberProfilesById,
                    nicknames = nicknames
                )
            } else {
                when {
                    !nickname.isNullOrBlank() && currentFriend != null ->
                        "(${SocialRepository.displayNameOrEmail(currentFriend)})"

                    currentFriend != null && currentFriend.email.isNotBlank() ->
                        currentFriend.email

                    else -> ""
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(photoUrl = if (isGroupThread) "" else currentFriend?.photoUrl ?: "")

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isGroupThread) {
                    // GROUP MESSAGING group members can rename the shared group chat
                    Button(
                        onClick = { showEditGroupNameDialog = true },
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
                            text = "Rename",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
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
                            },
                            // GROUP MESSAGING CHANGE: only show sender names in group chats.
                            senderName = if (isGroupThread) {
                                displayUserName(
                                    userId = message.senderId,
                                    currentUser = currentUser,
                                    profilesById = memberProfilesById,
                                    nicknames = nicknames
                                )
                            } else {
                                null
                            },
                            currentUserId = currentUser.uid
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
        title = { Text("New Group Chat") },
        text = {
            Column {
                // GROUP MESSAGING optional name, blank falls back to participant names.
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(80) },
                    label = { Text("Group name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select at least 2 friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(friends, key = { it.uid }) { friend ->
                        val checked = friend.uid in selectedFriendIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedFriendIds.remove(friend.uid)
                                    else selectedFriendIds.add(friend.uid)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedFriendIds.add(friend.uid)
                                    else selectedFriendIds.remove(friend.uid)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            UserAvatar(photoUrl = friend.photoUrl)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nicknames[friend.uid]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: SocialRepository.displayNameOrEmail(friend),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (friend.email.isNotBlank()) {
                                    Text(
                                        text = friend.email,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedFriendIds.toList(), groupName) },
                enabled = selectedFriendIds.size >= 2 && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF3347),
                    contentColor = Color.White
                )
            ) {
                Text(if (isCreating) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditGroupNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var groupName by rememberSaveable(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Chat Name") },
        text = {
            Column {
                // GROUP MESSAGING
                // saving blank clears the custom name and restores
                // the automatic participant names/nicknames fallback.
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(80) },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(groupName) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF3347),
                    contentColor = Color.White
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    onPinSaved: () -> Unit = {},
    senderName: String? = null,

    // GROUP MESSAGING CHANGE:
    // Needed so group event invites can find the invite document for this user.
    currentUserId: String = ""
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
        LaunchedEffect(message.id, currentUserId) {
            val eventId = message.metadata["eventId"].orEmpty()

            // GROUP MESSAGING CHANGE:
            // Direct messages use receiverId.
            // Group messages have receiverId blank, so use the signed-in user's uid.
            val inviteRecipientId = message.receiverId.ifBlank { currentUserId }

            if (eventId.isNotBlank() && inviteRecipientId.isNotBlank()) {
                val inviteId = "${message.senderId}_${inviteRecipientId}_${eventId}"
                SocialRepository.fetchEventInvite(
                    inviteId,
                    { invite = it },
                    { inviteError = it }
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        // GROUP MESSAGING:
        // show sender name above incoming group messages.
        if (!isMine && !senderName.isNullOrBlank()) {
            Text(
                text = senderName,
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
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

                        if (message.type == "pin" && !isMine && isPinAlreadySaved) {
                            Text("Saved to Map", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val lat = message.metadata["lat"]?.toDoubleOrNull()
                                val lng = message.metadata["lng"]?.toDoubleOrNull()
                                if (lat != null && lng != null) {
                                    val route = if (message.type == "pin") {
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
                                            tempName = "Shared Location",
                                            tempDesc = "A location shared with you"
                                        )
                                    }
                                    navController.navigate(route)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMine) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (isMine) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (message.type == "pin") "View/Add Pin on Map" else "View on Map",
                                fontSize = 12.sp
                            )
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
