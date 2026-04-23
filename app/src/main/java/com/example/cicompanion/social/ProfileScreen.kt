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
    val isOwnProfile = userId == null || userId == FirebaseAuth.getInstance().currentUser?.uid

    var displayName by remember { mutableStateOf("Loading...") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var friendCount by remember { mutableIntStateOf(0) }
    var requestStatus by remember { mutableStateOf<String?>(null) }
    var targetUserProfile by remember { mutableStateOf<UserProfile?>(null) }

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
        val targetUid = userId ?: currentUser?.uid
        if (targetUid != null) {
            SocialRepository.fetchUserProfile(
                userId = targetUid,
                onSuccess = { profile ->
                    targetUserProfile = profile
                    displayName = profile.displayName.ifBlank { profile.email }
                    email = profile.email
                    photoUrl = profile.photoUrl
                },
                onError = {
                    if (isOwnProfile) {
                        displayName = currentUser?.displayName ?: "Guest User"
                        email = currentUser?.email ?: ""
                        photoUrl = currentUser?.photoUrl?.toString()
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
            }
        } else {
            displayName = "Guest User"
            email = "Sign in to sync your data"
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
                friendCount = friendCount,
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
                if (isOwnProfile) {
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
                        }
                    )
                } else {
                    ViewOnlyProfileActions(
                        navController = navController,
                        targetUser = targetUserProfile,
                        requestStatus = requestStatus,
                        onStatusChange = { newStatus -> requestStatus = newStatus }
                    )
                }
            }
        }
    }
}

@Composable
fun ViewOnlyProfileActions(
    navController: NavHostController,
    targetUser: UserProfile?,
    requestStatus: String?,
    onStatusChange: (String?) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    SectionLabel("Actions")

    // Friend Request Logic
    if (targetUser != null && currentUser != null) {
        if (requestStatus == null) {
            Button(
                onClick = {
                    SocialRepository.sendFriendRequest(
                        currentUser = currentUser,
                        targetUser = targetUser,
                        onSuccess = { onStatusChange("pending") },
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
        } else {
            // Already friends or pending
            OutlinedButton(
                onClick = {
                    if (requestStatus == "accepted") {
                        SocialRepository.removeFriend(
                            currentUserId = currentUser.uid,
                            targetUserId = targetUser.uid,
                            onSuccess = { onStatusChange(null) },
                            onError = { /* Handle error */ }
                        )
                    } else if (requestStatus == "pending") {
                        // For now we use removeFriend to cancel a pending request too
                        SocialRepository.removeFriend(
                            currentUserId = currentUser.uid,
                            targetUserId = targetUser.uid,
                            onSuccess = { onStatusChange(null) },
                            onError = { /* Handle error */ }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (requestStatus == "accepted") Color.Gray else BrandRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (requestStatus == "accepted") Color.Gray else BrandRed)
            ) {
                val icon = if (requestStatus == "accepted") Icons.Default.Close else Icons.Default.Check
                val label = if (requestStatus == "accepted") "Remove Friend" else "Request Sent (Cancel)"
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = { /* TODO: Start conversation */ },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, BrandRed),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text("Send Message", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { navController.popBackStack() }) {
        Text("Go Back", color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldProfileScreen(navController: NavHostController) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var friendCount by remember { mutableIntStateOf(0) }
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

    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { signedInUser ->
            FirestoreManager.saveUserToFirestore(signedInUser) {
                FriendRequestNotificationSender.syncCurrentUserFcmToken()
            }
            SocialRepository.fetchFriendCount(
                currentUserId = signedInUser.uid,
                onSuccess = { count -> friendCount = count },
                onError = { friendCount = 0 }
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result)
    }

    Scaffold(
        containerColor = AppBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.USER_SEARCH) },
                shape = CircleShape,
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Search Users"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppBackground)
        ) {
            ProfileHeader(
                userDisplayName = currentUser?.displayName ?: "Signed out",
                userEmail = currentUser?.email ?: "user@example.com",
                photoUrl = currentUser?.photoUrl?.toString(),
                friendCount = friendCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .border(
                        width = 1.dp,
                        color = GrayIcon.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(NavBackground)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser == null) {
                    Button(onClick = {
                        val signInClient = FirebaseAuthManager.getGoogleSignInClient(context)
                        launcher.launch(signInClient.signInIntent)
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigate(Routes.FRIENDS_AND_REQUESTS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Friend Requests")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                FirebaseAuthManager.getGoogleSignInClient(context).signOut()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
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
    onSignOut: () -> Unit
) {
    val showEditAndSettings = false // Toggle for edit profile and settings buttons

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
//        Button(
//            onClick = onFindFriends,
//            modifier = Modifier.fillMaxWidth().height(56.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
//        ) {
//            Icon(Icons.Default.PersonAdd, contentDescription = null)
//            Spacer(Modifier.width(12.dp))
//            Text("Add Friends", fontSize = 16.sp, fontWeight = FontWeight.Bold)
//        }

        Spacer(modifier = Modifier.height(12.dp))

        // Updated "Friend Requests" button
        Button(
            onClick = onViewFriendRequests,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            // border = BorderStroke(1.5.dp, BrandRed),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Friends & Requests", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showEditAndSettings) {
            SectionLabel("Account Settings")

            OutlinedButton(
                onClick = { /* Mockup Edit */ },
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
    friendCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = userDisplayName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.Black
            )
            Text(
                text = userEmail,
                color = Color.DarkGray,
                fontSize = 14.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
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
