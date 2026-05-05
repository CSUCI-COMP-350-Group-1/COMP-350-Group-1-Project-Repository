package com.example.cicompanion.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
private fun rememberAuthUser(): FirebaseUser? {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // React to sign-in/sign-out immediately while already on the messaging UI.
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
fun MessagesScreen(navController: NavHostController) {
    // This now updates immediately when the user signs out.
    val currentUser = rememberAuthUser()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var rawConversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingFriends by remember { mutableStateOf(currentUser != null) }

    // Clear stale messaging UI state when the user signs out.
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
    
    // Only show conversations if the other participant is still a friend
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
            // keep friend-management reachable
            Button(
                onClick = { navController.navigate(Routes.FRIENDS_AND_REQUESTS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF3347), // same red
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                                    navController.navigate(
                                        Routes.messageThread(
                                            MessagingRepository.createConversationId(
                                                currentUser.uid,
                                                friend.uid
                                            ),
                                            friend.uid
                                        )
                                    )
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
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
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
                        val friendUserId = MessagingRepository.findOtherParticipantId(
                            conversation = conversation,
                            currentUserId = currentUser.uid
                        ).orEmpty()

                        val friend = friendsById[friendUserId]

                        ConversationCard(
                            friendName = friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Friend",
                            friendEmail = friend?.email ?: "",
                            preview = conversation.lastMessageText,
                            onClick = {
                                navController.navigate(
                                    Routes.messageThread(conversation.id, friendUserId)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageThreadScreen(
    navController: NavHostController,
    conversationId: String,
    friendUserId: String
) {
    // MESSAGING CHANGE:
    // React immediately to sign-out while already inside a thread.
    val currentUser = rememberAuthUser()

    var friend by remember { mutableStateOf<UserProfile?>(null) }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var conversationExists by remember { mutableStateOf(false) }
    var isFriend by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Clear stale thread data when the user signs out.
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
        // Check if they are still friends
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
                // Clear any old/stale error once profile loading succeeds.
                errorMessage = null
            },
            onError = { errorMessage = it }
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
                        Icon(
                            Icons.Default.Send,
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
            Text(
                text = friend?.let { SocialRepository.displayNameOrEmail(it) } ?: "Chat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!friend?.email.isNullOrBlank()) {
                Text(
                    text = friend!!.email,
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
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = SocialRepository.displayNameOrEmail(user),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ConversationCard(
    friendName: String,
    friendEmail: String,
    preview: String,
    onClick: () -> Unit
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
