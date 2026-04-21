package com.example.cicompanion.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(currentUser != null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val requestStatuses = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoading = false
            return@LaunchedEffect
        }

        SocialRepository.fetchSearchableUsers(
            currentUserId = currentUser.uid,
            onSuccess = { loadedUsers ->
                users = loadedUsers
                isLoading = false
            },
            onError = {
                errorMessage = it
                isLoading = false
            }
        )

        SocialRepository.fetchAllFriendRequestStatuses(
            currentUserId = currentUser.uid,
            onSuccess = { statuses ->
                requestStatuses.clear()
                requestStatuses.putAll(statuses)
            },
            onError = { errorMessage = it }
        )
    }

    val filteredUsers = if (searchQuery.isBlank()) {
        users
    } else {
        users.filter { user ->
            user.displayName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = AppBackground
    ) { innerPadding ->
        if (currentUser == null) {
            SignedOutSearchMessage(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name or email") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                if (statusMessage != null) {
                    Text(text = statusMessage!!, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                }
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            UserSearchResultCard(
                                user = user,
                                requestStatus = requestStatuses[user.uid],
                                onCardClick = {
                                    navController.navigate("${Routes.PROFILE}/${user.uid}")
                                },
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
                                },
                                onRemoveFriend = {
                                    SocialRepository.removeFriend(
                                        currentUserId = currentUser.uid,
                                        targetUserId = user.uid,
                                        onSuccess = {
                                            requestStatuses.remove(user.uid)
                                            statusMessage = "Friendship removed for ${SocialRepository.displayNameOrEmail(user)}."
                                        },
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
private fun UserSearchResultCard(
    user: UserProfile,
    requestStatus: String?,
    onCardClick: () -> Unit,
    onSendRequest: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(photoUrl = user.photoUrl)
                Column {
                    Text(
                        text = SocialRepository.displayNameOrEmail(user),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Remove Friend button (red X)
                // Visible ONLY when they are already friends (accepted)
                if (requestStatus == "accepted") {
                    IconButton(onClick = onRemoveFriend) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Friend",
                            tint = Color.Red
                        )
                    }
                } else {
                    // Add/Status button
                    IconButton(
                        onClick = onSendRequest,
                        enabled = requestStatus == null
                    ) {
                        val icon = if (requestStatus == "pending") Icons.Default.Check else Icons.Default.Add
                        val tint = if (requestStatus == "pending") Color.Gray else LocalContentColor.current

                        Icon(
                            imageVector = icon,
                            contentDescription = if (requestStatus == null) "Add Friend" else "Status",
                            tint = tint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(photoUrl: String) {
    if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "User photo",
            modifier = Modifier.size(48.dp).background(Color.LightGray, CircleShape)
        )
    } else {
        Box(modifier = Modifier.size(48.dp).background(Color.LightGray, CircleShape))
    }
}

@Composable
private fun SignedOutSearchMessage(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Please sign in to search for friends.")
    }
}