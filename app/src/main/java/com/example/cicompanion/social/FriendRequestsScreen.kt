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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
 * Displays the full friend requests screen for the signed-in user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(_navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(currentUser != null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LoadFriendRequestsEffect(
        currentUserId = currentUser?.uid,
        onLoadingStarted = { isLoading = true },
        onLoadingFinished = { isLoading = false },
        onRequestsLoaded = { requests = it },
        onLoadError = { errorMessage = it }
    )

    Scaffold(
        topBar = {
            FriendRequestsTopBar()
        }
    ) { innerPadding ->
        FriendRequestsScaffoldContent(
            currentUserId = currentUser?.uid,
            requests = requests,
            isLoading = isLoading,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            onAcceptRequest = { request ->
                acceptSelectedFriendRequest(
                    request = request,
                    currentRequests = requests,
                    onRequestsChanged = { requests = it },
                    onStatusMessageChanged = { statusMessage = it },
                    onErrorMessageChanged = { errorMessage = it }
                )
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Loads pending incoming friend requests when the signed-in user changes.
 */
@Composable
private fun LoadFriendRequestsEffect(
    currentUserId: String?,
    onLoadingStarted: () -> Unit,
    onLoadingFinished: () -> Unit,
    onRequestsLoaded: (List<FriendRequest>) -> Unit,
    onLoadError: (String) -> Unit
) {
    LaunchedEffect(currentUserId) {
        if (currentUserId == null) {
            onLoadingFinished()
            return@LaunchedEffect
        }

        onLoadingStarted()

        SocialRepository.fetchIncomingFriendRequests(
            currentUserId = currentUserId,
            onSuccess = { loadedRequests ->
                onRequestsLoaded(loadedRequests)
                onLoadingFinished()
            },
            onError = { message ->
                onLoadError(message)
                onLoadingFinished()
            }
        )
    }
}

/**
 * Displays the top app bar for the friend requests screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendRequestsTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Friend Requests",
                fontSize = 20.sp
            )
        }
    )
}

/**
 * Chooses whether to show the signed-out message or the signed-in content.
 */
@Composable
private fun FriendRequestsScaffoldContent(
    currentUserId: String?,
    requests: List<FriendRequest>,
    isLoading: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onAcceptRequest: (FriendRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentUserId == null) {
        SignedOutFriendRequestsMessage(modifier = modifier)
        return
    }

    FriendRequestsContent(
        requests = requests,
        isLoading = isLoading,
        statusMessage = statusMessage,
        errorMessage = errorMessage,
        onAcceptRequest = onAcceptRequest,
        modifier = modifier
    )
}

/**
 * Accepts one friend request and updates the local UI state afterward.
 */
private fun acceptSelectedFriendRequest(
    request: FriendRequest,
    currentRequests: List<FriendRequest>,
    onRequestsChanged: (List<FriendRequest>) -> Unit,
    onStatusMessageChanged: (String?) -> Unit,
    onErrorMessageChanged: (String?) -> Unit
) {
    SocialRepository.acceptFriendRequest(
        request = request,
        onSuccess = {
            onRequestsChanged(removeAcceptedRequest(currentRequests, request.id))
            onStatusMessageChanged(buildAcceptedRequestMessage(request))
            onErrorMessageChanged(null)
        },
        onError = { message ->
            onErrorMessageChanged(message)
            onStatusMessageChanged(null)
        }
    )
}

/**
 * Removes the accepted request from the currently displayed list.
 */
private fun removeAcceptedRequest(
    currentRequests: List<FriendRequest>,
    acceptedRequestId: String
): List<FriendRequest> {
    return currentRequests.filterNot { it.id == acceptedRequestId }
}

/**
 * Builds the success message shown after a request is accepted.
 */
private fun buildAcceptedRequestMessage(request: FriendRequest): String {
    return "Accepted ${incomingRequestDisplayName(request)}."
}

/**
 * Returns the best display name for the sender of a friend request.
 */
private fun incomingRequestDisplayName(request: FriendRequest): String {
    return request.fromDisplayName.ifBlank { request.fromEmail }
}

/**
 * Displays the message shown when the user is signed out.
 */
@Composable
private fun SignedOutFriendRequestsMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please sign in on the profile page before viewing friend requests.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Displays the signed-in friend requests content area.
 */
@Composable
private fun FriendRequestsContent(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onAcceptRequest: (FriendRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FriendRequestsMessages(
            statusMessage = statusMessage,
            errorMessage = errorMessage
        )

        FriendRequestsBody(
            requests = requests,
            isLoading = isLoading,
            onAcceptRequest = onAcceptRequest
        )
    }
}

/**
 * Displays the success and error messages for the screen.
 */
@Composable
private fun FriendRequestsMessages(
    statusMessage: String?,
    errorMessage: String?
) {
    RequestMessageText(
        message = statusMessage,
        color = Color(0xFF2E7D32)
    )

    RequestMessageText(
        message = errorMessage,
        color = Color(0xFFC62828)
    )
}

/**
 * Displays the correct body state for loading, empty, or populated data.
 */
@Composable
private fun FriendRequestsBody(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAcceptRequest: (FriendRequest) -> Unit
) {
    when {
        isLoading -> FriendRequestsLoadingState()
        requests.isEmpty() -> EmptyFriendRequestsMessage()
        else -> IncomingRequestsList(
            requests = requests,
            onAcceptRequest = onAcceptRequest
        )
    }
}

/**
 * Displays one status or error message when a message exists.
 */
@Composable
private fun RequestMessageText(
    message: String?,
    color: Color
) {
    if (message == null) {
        return
    }

    Text(
        text = message,
        color = color,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/**
 * Displays the loading state while friend requests are being fetched.
 */
@Composable
private fun FriendRequestsLoadingState() {
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
 * Displays the message shown when there are no pending requests.
 */
@Composable
private fun EmptyFriendRequestsMessage() {
    Text(
        text = "You have no pending friend requests.",
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * Displays the list of incoming friend requests.
 */
@Composable
private fun IncomingRequestsList(
    requests: List<FriendRequest>,
    onAcceptRequest: (FriendRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            FriendRequestCard(
                request = request,
                onAcceptRequest = { onAcceptRequest(request) }
            )
        }
    }
}

/**
 * Displays one incoming friend request row.
 */
@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAcceptRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IncomingRequestSummary(request = request)
            IncomingRequestAcceptButton(onClick = onAcceptRequest)
        }
    }
}

/**
 * Displays the accept button for one incoming request.
 */
@Composable
private fun IncomingRequestAcceptButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Accept")
    }
}

/**
 * Displays the sender information for one incoming request.
 */
@Composable
private fun IncomingRequestSummary(request: FriendRequest) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IncomingRequestAvatar(photoUrl = request.fromPhotoUrl)
        IncomingRequestText(request = request)
    }
}

/**
 * Displays the sender name and email text for one incoming request.
 */
@Composable
private fun IncomingRequestText(request: FriendRequest) {
    Column {
        IncomingRequestNameText(request = request)
        IncomingRequestEmailText(request = request)
    }
}

/**
 * Displays the sender display name for one incoming request.
 */
@Composable
private fun IncomingRequestNameText(request: FriendRequest) {
    Text(
        text = incomingRequestDisplayName(request),
        style = MaterialTheme.typography.titleMedium
    )
}

/**
 * Displays the sender email for one incoming request.
 */
@Composable
private fun IncomingRequestEmailText(request: FriendRequest) {
    Text(
        text = request.fromEmail,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
}

/**
 * Displays the sender avatar image when one exists.
 */
@Composable
private fun IncomingRequestAvatar(photoUrl: String) {
    if (photoUrl.isBlank()) {
        EmptyIncomingRequestAvatar()
        return
    }

    AsyncImage(
        model = photoUrl,
        contentDescription = "Sender profile image",
        modifier = Modifier.size(48.dp)
    )
}

/**
 * Displays the placeholder avatar when the sender has no photo.
 */
@Composable
private fun EmptyIncomingRequestAvatar() {
    Card(modifier = Modifier.size(48.dp)) {}
}

