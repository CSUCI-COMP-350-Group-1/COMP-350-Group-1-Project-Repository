package com.example.cicompanion.social

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
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
fun ProfileScreen(navController: NavHostController) {
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
            FirestoreManager.saveUserToFirestore(signedInUser)
            SocialRepository.fetchFriendCount(
                currentUserId = signedInUser.uid,
                onSuccess = { count -> friendCount = count },
                onError = { friendCount = 0 }
            )
        } ?: run {
            friendCount = 0
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result)
    }

    Scaffold(
        containerColor = Color.White // Cleaner white background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            ProfileHeader(
                userDisplayName = currentUser?.displayName ?: "Guest User",
                userEmail = currentUser?.email ?: "Sign in to sync your data",
                photoUrl = currentUser?.photoUrl?.toString(),
                friendCount = friendCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NavBackground.copy(alpha = 0.5f))
                    .padding(20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable area for actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
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
                        navController.navigate(Routes.FRIEND_REQUESTS)
                    },
                    onSignOut = {
                        FirebaseAuth.getInstance().signOut()
                        FirebaseAuthManager.getGoogleSignInClient(context).signOut()
                    }
                )
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
    if (currentUser == null) {
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        // --- START OF MOCKUP SOCIAL SECTION ---
        // New "Add Friends" primary button
        Button(
            onClick = onFindFriends,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Add Friends", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Updated "Friend Requests" button
        OutlinedButton(
            onClick = onViewFriendRequests,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, BrandRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Friend Requests", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Additional Mockup Buttons
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
        // --- END OF MOCKUP SOCIAL SECTION ---

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out at the bottom
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
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
                Text(
                    text = "$friendCount Friends",
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
