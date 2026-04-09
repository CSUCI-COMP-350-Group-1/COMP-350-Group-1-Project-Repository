package com.example.cicompanion.social

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.cicompanion.ui.theme.CICompanionTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Displays the profile screen and connects the UI to the current Firebase auth state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var friendCount by remember { mutableIntStateOf(0) }

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
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontSize = 20.sp) }
            )
        }
    ) { innerPadding ->
        ProfileScreenContent(
            currentUser = currentUser,
            friendCount = friendCount,
            navController = navController,
            launcher = launcher,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Handles the Google sign-in result and passes the ID token to Firebase auth.
 */
private fun handleGoogleSignInResult(result: ActivityResult) {
    if (result.resultCode != Activity.RESULT_OK) {
        return
    }

    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

    try {
        val account = task.getResult(ApiException::class.java)
        account?.idToken?.let { idToken ->
            FirebaseAuthManager.firebaseAuthWithGoogle(idToken)
        }
    } catch (_: ApiException) {
    }
}

/**
 * Displays the profile header and the sign-in or signed-in actions.
 */
@Composable
private fun ProfileScreenContent(
    currentUser: FirebaseUser?,
    friendCount: Int,
    navController: NavHostController,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ProfileHeader(
            userDisplayName = currentUser?.displayName ?: "Signed out",
            userEmail = currentUser?.email ?: "user@example.com",
            photoUrl = currentUser?.photoUrl?.toString(),
            friendCount = friendCount,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
        )

        ProfileActionArea(
            currentUser = currentUser,
            onSignIn = {
                val signInClient = FirebaseAuthManager.getGoogleSignInClient(context)
                launcher.launch(signInClient.signInIntent)
            },
            onFindFriends = {
                navController.navigate("userSearch")
            },
            onViewFriendRequests = {
                navController.navigate("friendRequests")
            },
            onSignOut = {
                FirebaseAuth.getInstance().signOut()
                FirebaseAuthManager.getGoogleSignInClient(context).signOut()
            }
        )
    }
}

/**
 * Displays the profile action area shown below the header.
 */
@Composable
private fun ColumnScope.ProfileActionArea(
    currentUser: FirebaseUser?,
    onSignIn: () -> Unit,
    onFindFriends: () -> Unit,
    onViewFriendRequests: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        if (currentUser == null) {
            SignInActionButton(onSignIn = onSignIn)
        } else {
            SignedInActionButtons(
                onFindFriends = onFindFriends,
                onViewFriendRequests = onViewFriendRequests,
                onSignOut = onSignOut
            )
        }
    }
}

/**
 * Displays the Google sign-in button.
 */
@Composable
private fun SignInActionButton(onSignIn: () -> Unit) {
    Button(onClick = onSignIn) {
        Text("Sign in with Google")
    }
}

/**
 * Displays the actions available after the user signs in.
 */
@Composable
private fun SignedInActionButtons(
    onFindFriends: () -> Unit,
    onViewFriendRequests: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onFindFriends) {
            Text("Find Friends")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onViewFriendRequests) {
            Text("Friend Requests")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

/**
 * Displays the user's basic profile information at the top of the screen.
 */
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
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFE6E0F8)),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DisplayDefaultAvatar()
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column {
            Text(
                text = userDisplayName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.Black
            )
            Text(
                text = userEmail,
                color = Color.DarkGray,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "$friendCount Friends",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}

/**
 * Displays the placeholder avatar used when a user photo is unavailable.
 */
@Composable
fun DisplayDefaultAvatar() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF6750A4))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(80.dp, 40.dp)
                .clip(CircleShape)
                .background(Color(0xFF6750A4))
        )
    }
}

/**
 * Shows a preview of the profile screen.
 */
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CICompanionTheme {
        val navController = rememberNavController()
        ProfileScreen(navController = navController)
    }
}