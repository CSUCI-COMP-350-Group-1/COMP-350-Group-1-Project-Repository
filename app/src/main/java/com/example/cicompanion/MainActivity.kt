package com.example.cicompanion

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.appNavigation.DrawerProfileContent
import com.example.cicompanion.appNavigation.TopBar
import com.example.cicompanion.appNavigation.screenTitleForRoute
import com.example.cicompanion.calendar.CalendarScreen
import com.example.cicompanion.home.HomeScreen
import com.example.cicompanion.maps.MapScreen
import com.example.cicompanion.sidebar.SearchScreen
import com.example.cicompanion.social.FriendRequestsScreen
import com.example.cicompanion.social.ProfileScreen
import com.example.cicompanion.social.UserSearchScreen
import com.example.cicompanion.studyRoom.RoomListScreen
import com.example.cicompanion.ui.NavBar
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme
import kotlinx.coroutines.launch
import android.Manifest
import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : ComponentActivity() {
    // FOR PUSH NOTIFICATIONS runtime permission launcher for Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FriendRequestNotificationSender.syncCurrentUserFcmToken()
        }

        //PRINTS FCM TOKEN
        /*FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            android.util.Log.d("FCM_TOKEN", "Token: $token")
        }*/
        enableEdgeToEdge()
        setContent {
            CICompanionTheme {
                AppNavigation()
            }
        }
    }
    // PUSH NOTIFICATIONS helper for Android 13+ notification permission
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreenTitle = screenTitleForRoute(currentRoute)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerProfileContent(
                navController,
                drawerState,
                scope
            )
        }
    ) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                TopBar(
                    title = currentScreenTitle,
                    showBackButton = currentRoute == Routes.USER_SEARCH || currentRoute == Routes.FRIEND_REQUESTS,
                    onHamburgerClick = {
                        scope.launch { drawerState.open() }
                    },
                    onBackClick = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.PROFILE) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNotificationClick = {
                        // navController.navigate(Routes.NOTIFICATIONS)
                    },
                    navController = navController
                )
            },
            bottomBar = { NavBar(navController) }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(navController)
                    }
                    composable(Routes.MAP) {
                        MapScreen(navController)
                    }
                    composable(Routes.CALENDAR) {
                        CalendarScreen(navController = navController)
                    }
                    composable(Routes.STUDY_ROOM) {
                        RoomListScreen(viewModel = viewModel(), navController = navController)
                    }
                    composable(Routes.SEARCH) {
                        SearchScreen(navController)
                    }
                    composable(Routes.PROFILE) {
                        ProfileScreen(navController)
                    }
                    composable(Routes.USER_SEARCH) {
                        UserSearchScreen(navController)
                    }
                    composable(Routes.FRIEND_REQUESTS) {
                        FriendRequestsScreen(navController)
                    }
                }
            }
        }
    }
}
