package com.example.cicompanion.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

/**
 * Displays pending incoming friend requests for the signed-in user.
 */
@Composable
fun FriendRequestsScreen(_navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(currentUser != null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        SocialRepository.fetchIncomingFriendRequests(
            currentUserId = currentUser.uid,
            onSuccess = { loadedRequests ->
                requests = loadedRequests
                isLoading = false
            },
            onError = { message ->
                errorMessage = message
                isLoading = false
            }
        )
    }

    if (currentUser == null) {
        SignedOutFriendRequestsMessage()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RequestMessageText(
            message = statusMessage,
            color = Color(0xFF2E7D32)
        )

        RequestMessageText(
            message = errorMessage,
            color = Color(0xFFC62828)
        )

        when {
            isLoading -> FriendRequestsLoadingState()
            requests.isEmpty() -> EmptyFriendRequestsMessage()
            else -> IncomingRequestsList(
                requests = requests,
                onAcceptRequest = { request ->
                    SocialRepository.acceptFriendRequest(
                        request = request,
                        onSuccess = {
                            requests = requests.filterNot { it.id == request.id }
                            statusMessage = "Accepted ${request.fromDisplayName.ifBlank { request.fromEmail }}."
                            errorMessage = null
                        },
                        onError = { message ->
                            errorMessage = message
                            statusMessage = null
                        }
                    )
                },
                onDeclineRequest = { request ->
                    SocialRepository.declineFriendRequest(
                        request = request,
                        onSuccess = {
                            requests = requests.filterNot { it.id == request.id }
                            statusMessage = "Declined ${request.fromDisplayName.ifBlank { request.fromEmail }}."
                            errorMessage = null
                        },
                        onError = { message ->
                            errorMessage = message
                            statusMessage = null
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun SignedOutFriendRequestsMessage() {
    Column(
        modifier = Modifier
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

@Composable
private fun FriendRequestsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyFriendRequestsMessage() {
    Text(
        text = "You have no pending friend requests.",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun RequestMessageText(
    message: String?,
    color: Color
) {
    if (message == null) return

    Text(
        text = message,
        color = color,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun IncomingRequestsList(
    requests: List<FriendRequest>,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            FriendRequestCard(
                request = request,
                onAcceptRequest = { onAcceptRequest(request) },
                onDeclineRequest = { onDeclineRequest(request) }
            )
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAcceptRequest: () -> Unit,
    onDeclineRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IncomingRequestSummary(request = request)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDeclineRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB0B0B0),
                        contentColor = Color.White
                    )
                ) {
                    Text("Decline")
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))

                Button(onClick = onAcceptRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xff49c46a),
                        contentColor = Color.White
                    )
                    ) {
                    Text("Accept")
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestSummary(request: FriendRequest) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IncomingRequestAvatar(photoUrl = request.fromPhotoUrl)
        Column {
            Text(
                text = request.fromDisplayName.ifBlank { request.fromEmail },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = request.fromEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun IncomingRequestAvatar(photoUrl: String) {
    if (photoUrl.isBlank()) {
        Card(modifier = Modifier.size(48.dp)) {}
        return
    }

    AsyncImage(
        model = photoUrl,
        contentDescription = "Sender profile image",
        modifier = Modifier.size(48.dp)
    )
}
