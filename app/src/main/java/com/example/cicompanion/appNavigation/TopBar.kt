package com.example.cicompanion.appNavigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cicompanion.firebase.FirebaseAuthManager
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.ui.theme.BrandRedLighter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun screenTitleForRoute(route: String?): String {
    return when {
        route == Routes.HOME -> "Home"
        route == Routes.SOCIAL -> "Messages" //Changed to messages from Social
        route == Routes.MAP -> "Map"
        route == Routes.CALENDAR -> "Calendar"
        route == Routes.STUDY_ROOM -> "Study Room"
        route == Routes.PROFILE || route?.startsWith("${Routes.PROFILE}/") == true -> "Profile"
        route == Routes.NOTIFICATIONS -> "Notifications"
        route == Routes.USER_SEARCH -> "User Search"
        route == Routes.FRIEND_REQUESTS -> "Friend Requests"
        route == Routes.FRIENDS_AND_REQUESTS -> "Friends & Requests"
        route == Routes.SEARCH -> "Search"
        route?.startsWith(Routes.MESSAGE_THREAD_BASE) == true -> "Chat" // MESSAGING
        else -> "CI Companion"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    showBackButton: Boolean,
    onHamburgerClick: () -> Unit,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    navController: NavController
) {
    val topBarGradient = Brush.linearGradient(
        colors = listOf(BrandRedLighter, BrandRedDark)
    )

    Box(modifier = Modifier.background(topBarGradient)) {
        CenterAlignedTopAppBar(
            title = { Text(title, color = Color.White) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } else {
                    IconButton(onClick = onHamburgerClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            },
            actions = {
                //Commented out for the sprint 2, uncomment for notification page in future
                /*IconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                }*/
            }
        )
    }
}

@Composable
fun DrawerProfileContent(navController: NavController, drawerState: DrawerState, scope: CoroutineScope) {
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val context = LocalContext.current

    // Observe auth state changes to update the UI immediately on sign in/out
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Profile Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6E0F8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.photoUrl != null) {
                        AsyncImage(
                            model = currentUser!!.photoUrl.toString(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF6750A4)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = currentUser?.displayName ?: "Signed Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.email ?: "Log in to sync data",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // View Profile Button
            NavigationDrawerItem(
                label = { Text("View Profile") },
                selected = false,
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = {
                    navController.navigate(Routes.PROFILE)
                    scope.launch { drawerState.close() }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Search Button (which was moved from the bottom to here)
            NavigationDrawerItem(
                label = { Text("Search for Feature") },
                selected = false,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = {
                    navController.navigate(Routes.SEARCH)
                    scope.launch { drawerState.close() }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Sign out button on the bottom
            if (currentUser != null) {
                NavigationDrawerItem(
                    label = { Text("Sign Out") },
                    selected = false,
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        FirebaseAuthManager.getGoogleSignInClient(context).signOut()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = Color(0xFFEF3347),
                        unselectedIconColor = Color(0xFFEF3347)
                    )
                )
            }
        }
    }
}
