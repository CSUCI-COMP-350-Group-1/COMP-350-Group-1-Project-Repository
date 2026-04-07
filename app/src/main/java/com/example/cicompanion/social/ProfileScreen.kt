package com.example.cicompanion.social

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.ui.theme.CICompanionTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.cicompanion.firebase.FirebaseAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {

    val context = LocalContext.current
    
    // State to hold the current Firebase user
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // Listen for authentication state changes (sign-in/sign-out)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    FirebaseAuthManager.firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                // Handle error
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontSize = 20.sp) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Pass the current user's name and email to the header
            ProfileHeader(
                userDisplayName = currentUser?.displayName ?: "Signed out",
                userEmail = currentUser?.email ?: "user@example.com",
                photoUrl = currentUser?.photoUrl?.toString(),
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth()
            )

            // Gray content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE0E0E0)), // Light gray background
                contentAlignment = Alignment.Center
            ) {
                if (currentUser == null) {
                    Button(
                        onClick = {
                            val signInClient = FirebaseAuthManager.getGoogleSignInClient(context)
                            launcher.launch(signInClient.signInIntent)
                        }
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            FirebaseAuthManager.getGoogleSignInClient(context).signOut()
                        }
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userDisplayName: String,
    userEmail: String,
    photoUrl: String?,
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
                .background(Color(0xFFE6E0F8)), // Light purple background
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
                text = "0 Friends",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}

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
                .background(Color(0xFF6750A4)) // Darker purple
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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CICompanionTheme() {
        val navController = rememberNavController()
        ProfileScreen(navController = navController)
    }
}
