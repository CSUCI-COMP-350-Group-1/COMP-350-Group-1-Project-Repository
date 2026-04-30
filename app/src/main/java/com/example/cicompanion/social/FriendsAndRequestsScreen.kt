package com.example.cicompanion.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.delay

@Composable
fun FriendsAndRequestsScreen(
    navController: NavHostController,
    initialTab: Int = 1
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    // PUSH NOTIFICATIONS CHANGE:
    // Allow callers to choose which tab opens first.
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please sign in to view friends and requests.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFFEF3347),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFEF3347)
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Friends", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Add Friends", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Requests", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        when (selectedTab) {
            0 -> FriendsTab(currentUser, navController)
            1 -> AddFriendsTab(currentUser, navController)
            2 -> RequestsTab(currentUser, navController)
        }
    }
}

@Composable
fun FriendsTab(currentUser: FirebaseUser, navController: NavHostController) {
    var searchQuery by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var friendToRemove by remember { mutableStateOf<UserProfile?>(null) }
    var friendToNickname by remember { mutableStateOf<UserProfile?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val refreshFriends = {
        isLoading = true
        // PERMISSION_DENIED fix: use fetchAcceptedFriends which avoids listing the /users collection
        SocialRepository.fetchAcceptedFriends(
            currentUserId = currentUser.uid,
            onSuccess = { loadedFriends ->
                friends = loadedFriends
                SocialRepository.fetchNicknames(
                    currentUserId = currentUser.uid,
                    onSuccess = { map ->
                        nicknames = map
                        isLoading = false
                        errorMessage = null // Clear any stale error on success
                    },
                    onError = {
                        nicknames = emptyMap()
                        isLoading = false 
                    }
                )
            },
            onError = { err -> 
                errorMessage = err
                isLoading = false 
            }
        )
    }

    LaunchedEffect(Unit) {
        refreshFriends()
    }

    val displayFriends = remember(friends, searchQuery, nicknames) {
        if (searchQuery.isBlank()) {
            friends
        } else {
            friends.filter { friend ->
                val nickname = nicknames[friend.uid] ?: ""
                friend.displayName.contains(searchQuery, ignoreCase = true) ||
                        friend.email.contains(searchQuery, ignoreCase = true) ||
                        nickname.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search friends by name or email") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFEF3347),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEF3347))
            }
        } else if (friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You don't have any friends yet.", textAlign = TextAlign.Center, color = Color.Gray)
            }
        } else if (displayFriends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends found matching \"$searchQuery\"", textAlign = TextAlign.Center, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(displayFriends, key = { it.uid }) { friend ->
                    FriendCard(
                        user = friend,
                        nickname = nicknames[friend.uid],
                        onCardClick = { navController.navigate("${Routes.PROFILE}/${friend.uid}") },
                        onNickname = { friendToNickname = friend },
                        onRemove = { friendToRemove = friend }
                    )
                }
            }
        }
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("Unfriend ${SocialRepository.displayNameOrEmail(friend)}?") },
            text = { Text("Do you really want to remove this person from your friends list?") },
            confirmButton = {
                Button(
                    onClick = {
                        SocialRepository.removeFriend(
                            currentUserId = currentUser.uid,
                            targetUserId = friend.uid,
                            onSuccess = {
                                friendToRemove = null
                                refreshFriends()
                            },
                            onError = { errorMessage = it; friendToRemove = null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3347))
                ) {
                    Text("Unfriend")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { friendToRemove = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    friendToNickname?.let { friend ->
        var currentNickname by remember { mutableStateOf(nicknames[friend.uid] ?: "") }
        
        AlertDialog(
            onDismissRequest = { friendToNickname = null },
            title = { Text("Set Nickname") },
            text = {
                Column {
                    Text("Nickname for ${SocialRepository.displayNameOrEmail(friend)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentNickname,
                        onValueChange = { currentNickname = it },
                        placeholder = { Text("Enter nickname") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SocialRepository.setNickname(
                            currentUserId = currentUser.uid,
                            friendUid = friend.uid,
                            nickname = currentNickname,
                            onSuccess = {
                                friendToNickname = null
                                refreshFriends()
                            },
                            onError = { errorMessage = it; friendToNickname = null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3347))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { friendToNickname = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddFriendsTab(currentUser: FirebaseUser, navController: NavHostController) {
    var searchQuery by remember { mutableStateOf("") }
    val users = remember { mutableStateListOf<UserProfile>() }
    var lastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var canLoadMore by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val requestStatuses = remember { mutableStateMapOf<String, String>() }
    val listState = rememberLazyListState()

    val fetchUsers = { isNextPage: Boolean ->
        if (!isLoading && (isNextPage && canLoadMore || !isNextPage)) {
            isLoading = true
            if (!isNextPage) {
                users.clear()
                lastDocument = null
            }
            SocialRepository.fetchSearchableUsers(
                currentUserId = currentUser.uid,
                searchQuery = searchQuery,
                lastDocument = lastDocument,
                onSuccess = { newUsers, nextDoc ->
                    users.addAll(newUsers)
                    lastDocument = nextDoc
                    canLoadMore = newUsers.size >= 20
                    isLoading = false
                    errorMessage = null
                },
                onError = {
                    errorMessage = it
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) delay(500)
        fetchUsers(false)
    }

    LaunchedEffect(Unit) {
        SocialRepository.fetchAllFriendRequestStatuses(
            currentUserId = currentUser.uid,
            onSuccess = { statuses ->
                requestStatuses.putAll(statuses)
            },
            onError = { errorMessage = it }
        )
    }

    // Filter out users who are already friends to avoid the empty spaces
    val displayUsers = users.filter { requestStatuses[it.uid] != "accepted" }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or email") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFEF3347),
                unfocusedBorderColor = Color.LightGray
            )
        )

        if (statusMessage != null) {
            Text(text = statusMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
        }
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        if (isLoading && users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEF3347))
            }
        } else if (!isLoading && displayUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "No users left to add." else "No users found matching \"$searchQuery\"",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayUsers, key = { it.uid }) { user ->
                    UserSearchCard(
                        user = user,
                        requestStatus = requestStatuses[user.uid],
                        onCardClick = { navController.navigate("${Routes.PROFILE}/${user.uid}") },
                        onSendRequest = {
                            SocialRepository.sendFriendRequest(
                                currentUser = currentUser,
                                targetUser = user,
                                onSuccess = {
                                    requestStatuses[user.uid] = "pending"
                                    statusMessage = "Friend request sent to ${SocialRepository.displayNameOrEmail(user)}."
                                },
                                onError = { errorMessage = it }
                            )
                        }
                    )
                }

                if (canLoadMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFFEF3347), modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { fetchUsers(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
                                ) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsTab(currentUser: FirebaseUser, navController: NavHostController) {
    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var outgoingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val refreshRequests = {
        isLoading = true
        SocialRepository.fetchIncomingFriendRequests(
            currentUserId = currentUser.uid,
            onSuccess = { incoming ->
                incomingRequests = incoming
                SocialRepository.fetchOutgoingFriendRequests(
                    currentUserId = currentUser.uid,
                    onSuccess = { outgoing ->
                        outgoingRequests = outgoing
                        isLoading = false
                    },
                    onError = { errorMessage = it; isLoading = false }
                )
            },
            onError = { errorMessage = it; isLoading = false }
        )
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (statusMessage != null) {
            Text(text = statusMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(bottom = 8.dp))
        }
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEF3347))
            }
        } else {
            if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pending requests.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (incomingRequests.isNotEmpty()) {
                        item {
                            Text("Incoming Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(incomingRequests, key = { it.id }) { request ->
                            IncomingRequestCard(
                                request = request,
                                onCardClick = { navController.navigate("${Routes.PROFILE}/${request.fromUserId}") },
                                onAccept = {
                                    SocialRepository.acceptFriendRequest(
                                        request = request,
                                        onSuccess = { statusMessage = "Accepted request."; refreshRequests() },
                                        onError = { errorMessage = it }
                                    )
                                },
                                onDecline = {
                                    SocialRepository.declineFriendRequest(
                                        request = request,
                                        onSuccess = { statusMessage = "Declined request."; refreshRequests() },
                                        onError = { errorMessage = it }
                                    )
                                }
                            )
                        }
                    }

                    if (outgoingRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Outgoing Requests", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(outgoingRequests, key = { it.id }) { request ->
                            OutgoingRequestCard(
                                request = request,
                                onCardClick = { navController.navigate("${Routes.PROFILE}/${request.toUserId}") },
                                onCancel = {
                                    SocialRepository.declineFriendRequest( // Reuse decline logic to delete outgoing too
                                        request = request,
                                        onSuccess = { statusMessage = "Cancelled request."; refreshRequests() },
                                        onError = { errorMessage = it }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    user: UserProfile, 
    nickname: String?,
    onCardClick: () -> Unit, 
    onNickname: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUrl = user.photoUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!nickname.isNullOrBlank()) {
                    Text(text = nickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "(${SocialRepository.displayNameOrEmail(user)})", fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(text = SocialRepository.displayNameOrEmail(user), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = user.email, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row {
                IconButton(onClick = onNickname) {
                    Icon(Icons.Default.Edit, contentDescription = "Set Nickname", tint = Color.Gray)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Unfriend", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun UserSearchCard(user: UserProfile, requestStatus: String?, onCardClick: () -> Unit, onSendRequest: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUrl = user.photoUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = SocialRepository.displayNameOrEmail(user), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = user.email, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = onSendRequest,
                enabled = requestStatus == null
            ) {
                val icon = if (requestStatus == "pending") Icons.Default.HourglassEmpty else Icons.Default.PersonAdd
                Icon(icon, contentDescription = null, tint = if (requestStatus == null) Color(0xFFEF3347) else Color.Gray)
            }
        }
    }
}

@Composable
fun IncomingRequestCard(request: FriendRequest, onCardClick: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUrl = request.fromPhotoUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.fromDisplayName.ifBlank { request.fromEmail }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = request.fromEmail, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row {
                IconButton(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onDecline) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun OutgoingRequestCard(request: FriendRequest, onCardClick: () -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(photoUrl = "") // recipient photoUrl removed from schema
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.toDisplayName.ifBlank { request.toEmail }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = request.toEmail, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Request", tint = Color.Red)
            }
        }
    }
}

@Composable
fun UserAvatar(photoUrl: String) {
    if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).background(Color.LightGray, CircleShape)
        )
    } else {
        Box(modifier = Modifier.size(48.dp).background(Color(0xFFEF3347).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFEF3347))
        }
    }
}
