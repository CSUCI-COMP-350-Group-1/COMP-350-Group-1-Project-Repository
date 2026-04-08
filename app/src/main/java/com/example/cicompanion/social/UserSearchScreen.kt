package com.example.cicompanion.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

/**
 * Displays a searchable list of users and lets the current user send friend requests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(_navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(currentUser != null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sentRequestUserIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoading = false
            return@LaunchedEffect
        }

        loadSearchableUsers(
            currentUserId = currentUser.uid,
            onLoadingChanged = { isLoading = it },
            onUsersLoaded = { users = it },
            onError = { errorMessage = it }
        )
    }

    val filteredUsers = filterUsers(users, searchQuery)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Friends", fontSize = 20.sp) }
            )
        }
    ) { innerPadding ->
        if (currentUser == null) {
            SignedOutSearchMessage(modifier = Modifier.padding(innerPadding))
        } else {
            UserSearchContent(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                users = filteredUsers,
                isLoading = isLoading,
                statusMessage = statusMessage,
                errorMessage = errorMessage,
                sentRequestUserIds = sentRequestUserIds,
                onSendRequest = { targetUser ->
                    sendFriendRequest(
                        targetUser = targetUser,
                        sentRequestUserIds = sentRequestUserIds,
                        onStatusChanged = {
                            statusMessage = it
                            errorMessage = null
                        },
                        onError = {
                            errorMessage = it
                            statusMessage = null
                        }
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Loads the list of users that can appear in the search results.
 */
private fun loadSearchableUsers(
    currentUserId: String,
    onLoadingChanged: (Boolean) -> Unit,
    onUsersLoaded: (List<UserProfile>) -> Unit,
    onError: (String) -> Unit
) {
    onLoadingChanged(true)

    SocialRepository.fetchSearchableUsers(
        currentUserId = currentUserId,
        onSuccess = { loadedUsers ->
            onUsersLoaded(loadedUsers)
            onLoadingChanged(false)
        },
        onError = { message ->
            onError(message)
            onLoadingChanged(false)
        }
    )
}

/**
 * Sends a friend request to the selected user and tracks the request locally.
 */
private fun sendFriendRequest(
    targetUser: UserProfile,
    sentRequestUserIds: MutableList<String>,
    onStatusChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser == null) {
        onError("Please sign in before sending a friend request.")
        return
    }

    SocialRepository.sendFriendRequest(
        currentUser = currentUser,
        targetUser = targetUser,
        onSuccess = {
            if (!sentRequestUserIds.contains(targetUser.uid)) {
                sentRequestUserIds.add(targetUser.uid)
            }
            onStatusChanged("Friend request sent to ${SocialRepository.displayNameOrEmail(targetUser)}.")
        },
        onError = onError
    )
}

/**
 * Filters the user list by display name or email.
 */
private fun filterUsers(users: List<UserProfile>, searchQuery: String): List<UserProfile> {
    if (searchQuery.isBlank()) {
        return users
    }

    return users.filter { user ->
        matchesSearchQuery(user, searchQuery)
    }
}

/**
 * Returns whether the search query matches a user's display name or email.
 */
private fun matchesSearchQuery(user: UserProfile, searchQuery: String): Boolean {
    val query = searchQuery.trim()
    return user.displayName.contains(query, ignoreCase = true) ||
        user.email.contains(query, ignoreCase = true)
}

/**
 * Displays the signed-out message for the friend-search screen.
 */
@Composable
private fun SignedOutSearchMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please sign in on the profile page before searching for friends.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Displays the complete friend-search screen content for signed-in users.
 */
@Composable
private fun UserSearchContent(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    users: List<UserProfile>,
    isLoading: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    sentRequestUserIds: List<String>,
    onSendRequest: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SearchUsersField(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged
        )

        SearchMessageText(message = statusMessage, color = Color(0xFF2E7D32))
        SearchMessageText(message = errorMessage, color = Color(0xFFC62828))

        when {
            isLoading -> SearchLoadingState()
            users.isEmpty() -> EmptyUsersMessage()
            else -> UserResultsList(
                users = users,
                sentRequestUserIds = sentRequestUserIds,
                onSendRequest = onSendRequest
            )
        }
    }
}

/**
 * Displays the text field used to search for users.
 */
@Composable
private fun SearchUsersField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or email") },
        singleLine = true
    )
}

/**
 * Displays a status or error message when one is available.
 */
@Composable
private fun SearchMessageText(message: String?, color: Color) {
    if (message == null) {
        return
    }

    Text(
        text = message,
        color = color,
        modifier = Modifier.padding(top = 12.dp)
    )
}

/**
 * Displays the loading state while users are being fetched from Firestore.
 */
@Composable
private fun SearchLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Displays the message shown when there are no matching users.
 */
@Composable
private fun EmptyUsersMessage() {
    Text(
        text = "No users matched your search.",
        modifier = Modifier.padding(top = 24.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * Displays the list of matching user search results.
 */
@Composable
private fun UserResultsList(
    users: List<UserProfile>,
    sentRequestUserIds: List<String>,
    onSendRequest: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.uid }) { user ->
            UserSearchResultCard(
                user = user,
                requestAlreadySent = sentRequestUserIds.contains(user.uid),
                onSendRequest = { onSendRequest(user) }
            )
        }
    }
}

/**
 * Displays one user result with a friend-request button.
 */
@Composable
private fun UserSearchResultCard(
    user: UserProfile,
    requestAlreadySent: Boolean,
    onSendRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserSummary(user = user)

            Button(
                onClick = onSendRequest,
                enabled = !requestAlreadySent
            ) {
                Text(if (requestAlreadySent) "Sent" else "Add Friend")
            }
        }
    }
}

/**
 * Displays the basic information for one user in the result list.
 */
@Composable
private fun UserSummary(user: UserProfile) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(photoUrl = user.photoUrl)

        Column {
            Text(
                text = SocialRepository.displayNameOrEmail(user),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Displays the user photo when available.
 */
@Composable
private fun UserAvatar(photoUrl: String) {
    if (photoUrl.isBlank()) {
        EmptyUserAvatar()
        return
    }

    AsyncImage(
        model = photoUrl,
        contentDescription = "User profile image",
        modifier = Modifier.size(48.dp)
    )
}

/**
 * Displays a small placeholder avatar for users without a photo.
 */
@Composable
private fun EmptyUserAvatar() {
    Card(modifier = Modifier.size(48.dp)) {}
}
