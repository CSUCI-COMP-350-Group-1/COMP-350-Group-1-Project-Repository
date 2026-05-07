package com.example.cicompanion.social

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.cicompanion.firebase.FirebaseAuthManager
import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme
import com.example.cicompanion.ui.theme.GrayIcon
import com.example.cicompanion.ui.theme.NavBackground
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

private val BrandRed = Color(0xFFEF3347)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, userId: String? = null) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    
    // Immediately clear target user data when sign-out is detected
    val isOwnProfile = remember(userId, currentUser?.uid) {
        userId == null || (currentUser != null && userId == currentUser?.uid)
    }

    var displayName by remember { mutableStateOf("Loading...") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var userNote by remember { mutableStateOf<UserNote?>(null) }
    var friendCount by remember { mutableIntStateOf(0) }
    var nickname by remember { mutableStateOf<String?>(null) }
    var requestStatus by remember { mutableStateOf<String?>(null) }
    var targetUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var mutualFriends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(userId, currentUser?.uid) {
        if (currentUser == null && userId != null) {
            displayName = "Guest User"
            email = "Sign in to sync your data"
            photoUrl = null
            bio = ""
            major = ""
            userNote = null
            friendCount = 0
            nickname = null
            requestStatus = null
            targetUserProfile = null
            mutualFriends = emptyList()
            return@LaunchedEffect
        }

        val targetUid = userId ?: currentUser?.uid
        if (targetUid != null) {
            SocialRepository.fetchUserProfile(
                userId = targetUid,
                onSuccess = { profile ->
                    targetUserProfile = profile
                    displayName = profile.displayName.ifBlank { profile.email }
                    email = profile.email
                    photoUrl = profile.photoUrl
                    bio = profile.bio
                    major = profile.major
                    userNote = if (profile.note?.isExpired() == false) profile.note else null
                },
                onError = {
                    if (isOwnProfile) {
                        displayName = currentUser?.displayName ?: "Guest User"
                        email = currentUser?.email ?: ""
                        photoUrl = currentUser?.photoUrl?.toString()
                        bio = ""
                        major = ""
                        userNote = null
                        mutualFriends = emptyList()
                    }
                }
            )

            SocialRepository.fetchFriendCount(
                currentUserId = targetUid,
                onSuccess = { count -> friendCount = count },
                onError = { friendCount = 0 }
            )

            if (!isOwnProfile && currentUser != null) {
                SocialRepository.fetchAllFriendRequestStatuses(
                    currentUserId = currentUser!!.uid,
                    onSuccess = { statuses ->
                        requestStatus = statuses[targetUid]
                    },
                    onError = { /* Handle error */ }
                )

                SocialRepository.fetchMutualFriends(
                    currentUserId = currentUser!!.uid,
                    targetUserId = targetUid,
                    onSuccess = { mutualFriends = it },
                    onError = { mutualFriends = emptyList() }
                )
            } else {
                mutualFriends = emptyList()

                SocialRepository.fetchNicknames(
                    currentUserId = currentUser!!.uid,
                    onSuccess = { map ->
                        nickname = map[targetUid]
                    },
                    onError = { /* Ignore */ }
                )
            }
        } else {
            displayName = "Guest User"
            email = "Sign in to sync your data"
            photoUrl = null
            bio = ""
            major = ""
            userNote = null
            friendCount = 0
            nickname = null
            requestStatus = null
            targetUserProfile = null
            mutualFriends = emptyList()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result)
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            ProfileHeader(
                userDisplayName = displayName,
                userEmail = email,
                photoUrl = photoUrl,
                userMajor = major,
                friendCount = friendCount,
                userNote = userNote,
                isOwnProfile = isOwnProfile,
                isSignedIn = currentUser != null,
                onAddStatusClick = { showStatusDialog = true },
                showFriendCount = currentUser != null,
                nickname = nickname,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NavBackground.copy(alpha = 0.5f))
                    .padding(20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Mutual Friends Section
                if (!isOwnProfile && mutualFriends.isNotEmpty()) {
                    MutualFriendsSection(mutualFriends = mutualFriends)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bio displayed between user info and actions
                if (bio.isNotBlank()) {
                    Text(
                        text = bio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = Color.DarkGray,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                if (isOwnProfile || currentUser == null) {
                    ProfileActionArea(
                        currentUser = currentUser,
                        onSignIn = {
                            val signInClient = FirebaseAuthManager.getGoogleSignInClient(context)
                            launcher.launch(signInClient.signInIntent)
                        },
                        onFindFriends = {
                            navController.navigate(Routes.USER_SEARCH)
                        },
                        onViewFriendRequests = {
                            navController.navigate(Routes.FRIENDS_AND_REQUESTS)
                        },
                        onSignOut = {
                            FirebaseAuth.getInstance().signOut()
                            FirebaseAuthManager.getGoogleSignInClient(context).signOut()
                        },
                        navController = navController
                    )
                } else {
                    if (requestStatus == "accepted") {
                        FriendshipIndicator("You are Friends", Color(0xFF4CAF50), Icons.Default.CheckCircle)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (requestStatus == "pending_received") {
                        FriendshipIndicator("${targetUserProfile?.displayName ?: "This user"} sent you a friend request!", BrandRed, Icons.Default.PersonAdd)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ViewOnlyProfileActions(
                        navController = navController,
                        targetUser = targetUserProfile,
                        requestStatus = requestStatus,
                        onStatusChange = { newStatus -> requestStatus = newStatus },
                        nickname = nickname,
                        onNicknameClick = { showNicknameDialog = true }
                    )
                }
            }
        }
    }

    if (showNicknameDialog && !isOwnProfile && currentUser != null && userId != null) {
        NicknameDialog(
            initialNickname = nickname,
            onDismiss = { showNicknameDialog = false },
            onConfirm = { newNickname ->
                SocialRepository.setNickname(
                    currentUserId = currentUser!!.uid,
                    friendUid = userId,
                    nickname = newNickname,
                    onSuccess = {
                        nickname = newNickname.ifBlank { null }
                        showNicknameDialog = false
                    },
                    onError = { showNicknameDialog = false }
                )
            }
        )
    }

    if (showStatusDialog && currentUser != null) {
        StatusDialog(
            currentNote = userNote,
            onDismiss = { showStatusDialog = false },
            onConfirm = { content, durationMs ->
                val newNote = UserNote(
                    content = content,
                    expiresAt = System.currentTimeMillis() + durationMs
                )
                SocialRepository.updateUserNote(
                    userId = currentUser!!.uid,
                    note = newNote,
                    onSuccess = {
                        userNote = newNote
                        showStatusDialog = false
                    },
                    onError = { /* Handle error */ }
                )
            },
            onClear = {
                SocialRepository.updateUserNote(
                    userId = currentUser!!.uid,
                    note = null,
                    onSuccess = {
                        userNote = null
                        showStatusDialog = false
                    },
                    onError = { /* Handle error */ }
                )
            }
        )
    }

}

@Composable
fun NicknameDialog(
    initialNickname: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialNickname ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNickname == null) "Set Nickname" else "Change Nickname") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
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
fun MutualFriendsSection(mutualFriends: List<UserProfile>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Overlapping Avatars
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(((mutualFriends.take(3).size - 1) * 16 + 32).dp)
        ) {
            mutualFriends.take(3).forEachIndexed { index, friend ->
                Surface(
                    modifier = Modifier
                        .offset(x = (index * 16).dp)
                        .size(32.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White),
                    color = Color.LightGray
                ) {
                    if (friend.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = friend.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text description
        val friendNames = mutualFriends.take(2).map { it.displayName.ifBlank { "A friend" } }
        val remainingCount = mutualFriends.size - friendNames.size

        val text = when {
            mutualFriends.size == 1 -> "Mutual friend with ${friendNames[0]}"
            mutualFriends.size == 2 -> "Mutual friends with ${friendNames[0]} and ${friendNames[1]}"
            else -> "Mutual friends with ${friendNames[0]}, ${friendNames[1]} and $remainingCount others"
        }

        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun FriendshipIndicator(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ViewOnlyProfileActions(
    navController: NavHostController,
    targetUser: UserProfile?,
    requestStatus: String?,
    onStatusChange: (String?) -> Unit,
    nickname: String? = null,
    onNicknameClick: () -> Unit = {}
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    SectionLabel("Actions")

    if (targetUser != null && currentUser != null) {
        when (requestStatus) {
            null -> {
                Button(
                    onClick = {
                        SocialRepository.sendFriendRequest(
                            currentUser = currentUser,
                            targetUser = targetUser,
                            onSuccess = { onStatusChange("pending_sent") },
                            onError = { /* Handle error */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Send Friend Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            "pending_received" -> {
                Button(
                    onClick = {
                        val requestId = SocialRepository.createFriendRequestId(targetUser.uid, currentUser.uid)
                        SocialRepository.acceptFriendRequestById(
                            requestId = requestId,
                            onSuccess = { onStatusChange("accepted") },
                            onError = { /* Handle error */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Accept Friend Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        val requestId = SocialRepository.createFriendRequestId(targetUser.uid, currentUser.uid)
                        // Using a dummy request object since we just need the ID to decline/delete
                        SocialRepository.declineFriendRequest(
                            request = FriendRequest(id = requestId),
                            onSuccess = { onStatusChange(null) },
                            onError = { /* Handle error */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Decline Request", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            "pending_sent" -> {
                OutlinedButton(
                    onClick = {
                        SocialRepository.removeFriend(
                            currentUserId = currentUser.uid,
                            targetUserId = targetUser.uid,
                            onSuccess = { onStatusChange(null) },
                            onError = { /* Handle error */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BrandRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Request Sent (Cancel)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            "accepted" -> {
                OutlinedButton(
                    onClick = onNicknameClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (nickname == null) "Set Nickname" else "Change Nickname", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showRemoveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Remove Friend", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val conversationId = MessagingRepository.createConversationId(currentUser.uid, targetUser.uid)
                        navController.navigate(Routes.messageThread(conversationId, targetUser.uid))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BrandRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Send Message", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showRemoveDialog && targetUser != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove ${SocialRepository.displayNameOrEmail(targetUser)}?") },
            text = { Text("Do you really want to remove this person from your friends list?") },
            confirmButton = {
                Button(
                    onClick = {
                        SocialRepository.removeFriend(
                            currentUserId = currentUser.uid,
                            targetUserId = targetUser.uid,
                            onSuccess = {
                                showRemoveDialog = false
                                onStatusChange(null)
                            },
                            onError = { showRemoveDialog = false }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { navController.popBackStack() }) {
        Text("Go Back", color = Color.Gray)
    }
}

private fun handleGoogleSignInResult(result: ActivityResult) {
    if (result.resultCode != Activity.RESULT_OK) return
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
        val account = task.getResult(ApiException::class.java)
        account?.idToken?.let { idToken ->
            FirebaseAuthManager.firebaseAuthWithGoogle(idToken)
        }
    } catch (_: ApiException) {}
}

@Composable
private fun ColumnScope.ProfileActionArea(
    currentUser: FirebaseUser?,
    onSignIn: () -> Unit,
    onFindFriends: () -> Unit,
    onViewFriendRequests: () -> Unit,
    onSignOut: () -> Unit,
    navController: NavHostController
) {
    val showSettings = false // Toggle for edit profile and settings buttons

    if (currentUser == null) {
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        // --- START OF SOCIAL SECTION ---
        Spacer(modifier = Modifier.height(12.dp))

        // Updated "Friend Requests" button
        Button(
            onClick = onViewFriendRequests,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Friends & Requests", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("Account Settings")

        OutlinedButton(
            onClick = { navController.navigate(Routes.EDIT_PROFILE) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("Edit Profile", fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }

            Spacer(modifier = Modifier.height(8.dp))

        if (showSettings) {

            OutlinedButton(
                onClick = { /* Mockup Settings */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Settings", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        // --- END OF SOCIAL SECTION ---

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out at the bottom
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = Color.Gray,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ProfileHeader(
    userDisplayName: String,
    userEmail: String,
    photoUrl: String?,
    userMajor: String,
    friendCount: Int,
    userNote: UserNote? = null,
    isOwnProfile: Boolean = false,
    isSignedIn: Boolean = false,
    onAddStatusClick: () -> Unit = {},
    showFriendCount: Boolean = true,
    modifier: Modifier = Modifier,
    nickname: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopStart) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DefaultAvatar()
                    }
                }

                UserNoteBubble(
                    note = userNote,
                    modifier = Modifier.offset(x = (-12).dp, y = (-16).dp)
                )
            }

            if (isOwnProfile && isSignedIn) {
                TextButton(
                    onClick = onAddStatusClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = if (userNote != null && !userNote.isExpired()) "Edit Status" else "Add Status",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = nickname ?: userDisplayName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.Black
            )
            if (nickname != null) {
                Text(
                    text = "($userDisplayName)",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Text(
                text = userEmail,
                color = Color.DarkGray,
                fontSize = 14.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFriendCount) {
                    Surface(
                        color = BrandRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val friendText = if (friendCount == 1) "1 Friend" else "$friendCount Friends"
                        Text(
                            text = friendText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandRed
                        )
                    }
                }

                if (userMajor.isNotBlank()) {
                    if (showFriendCount) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Surface(
                        color = BrandRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = userMajor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultAvatar() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = Color.LightGray
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CICompanionTheme {
        val navController = rememberNavController()
        ProfileScreen(navController = navController)
    }
}
