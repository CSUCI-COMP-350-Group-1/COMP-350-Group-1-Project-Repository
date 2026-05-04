package com.example.cicompanion

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cicompanion.appNavigation.DrawerProfileContent
import com.example.cicompanion.appNavigation.TopBar
import com.example.cicompanion.appNavigation.screenTitleForRoute
import com.example.cicompanion.calendar.CalendarScreen
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.example.cicompanion.home.HomeScreen
import com.example.cicompanion.home.HomeViewModel
import com.example.cicompanion.maps.MapScreen
import com.example.cicompanion.maps.MapViewModel
import com.example.cicompanion.notifications.PushNotificationService
import com.example.cicompanion.sidebar.SearchScreen
import com.example.cicompanion.social.*
import com.example.cicompanion.studyRoom.RoomListScreen
import com.example.cicompanion.ui.NavBar
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    // FOR PUSH NOTIFICATIONS runtime permission launcher for Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private var pendingNotificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        pendingNotificationRoute = intent.getStringExtra(
            PushNotificationService.EXTRA_DESTINATION_ROUTE
        )

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FriendRequestNotificationSender.syncCurrentUserFcmToken()
        }

        enableEdgeToEdge()
        setContent {
            CICompanionTheme {
                AppNavigation(
                    notificationRoute = pendingNotificationRoute,
                    onNotificationRouteConsumed = {
                        pendingNotificationRoute = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        pendingNotificationRoute = intent.getStringExtra(
            PushNotificationService.EXTRA_DESTINATION_ROUTE
        )
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
fun AppNavigation(notificationRoute: String? = null,
                  onNotificationRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Create shared ViewModels here to sync across screens and persist during navigation
    val calendarViewModel: CalendarViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val mapViewModel: MapViewModel = viewModel()

    LaunchedEffect(notificationRoute) {
        if (!notificationRoute.isNullOrBlank()) {
            navController.navigate(notificationRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onNotificationRouteConsumed()
        }
    }

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
                    showBackButton =
                        currentRoute == Routes.FRIENDS_AND_REQUESTS ||
                                currentRoute?.startsWith(Routes.MESSAGE_THREAD_BASE) == true, // MESSAGING
                    onHamburgerClick = {
                        scope.launch { drawerState.open() }
                    },
                    onBackClick = {
                        // MESSAGING  prefer normal back-stack behavior for thread screens
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                                launchSingleTop = true
                            }
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
                        HomeScreen(navController, calendarViewModel, homeViewModel)
                    }
                    composable(
                        route = "${Routes.MAP}?lat={lat}&lng={lng}&tempName={tempName}&tempDesc={tempDesc}&tempColor={tempColor}&tempEventId={tempEventId}",
                        arguments = listOf(
                            navArgument("lat") { type = NavType.StringType; nullable = true },
                            navArgument("lng") { type = NavType.StringType; nullable = true },
                            navArgument("tempName") { type = NavType.StringType; nullable = true },
                            navArgument("tempDesc") { type = NavType.StringType; nullable = true },
                            navArgument("tempColor") { type = NavType.StringType; nullable = true },
                            navArgument("tempEventId") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                        val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
                        val tempName = backStackEntry.arguments?.getString("tempName")
                        val tempDesc = backStackEntry.arguments?.getString("tempDesc")
                        val tempColor = backStackEntry.arguments?.getString("tempColor")?.toIntOrNull()
                        val tempEventId = backStackEntry.arguments?.getString("tempEventId")

                        MapScreen(
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            mapViewModel = mapViewModel,
                            initialLat = lat,
                            initialLng = lng,
                            tempName = tempName,
                            tempDesc = tempDesc,
                            tempColor = tempColor,
                            tempEventId = tempEventId
                        )
                    }
                    composable(Routes.CALENDAR) {
                        CalendarScreen(
                            navController = navController,
                            calendarViewModel = calendarViewModel
                        )
                    }
                    composable(Routes.SCHEDULE) {
                        CalendarScreen(
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            initialTab = 1
                        )
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

                    composable(
                        route = "${Routes.PROFILE}/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")
                        ProfileScreen(navController, userId)
                    }

                    composable(Routes.SOCIAL) { backStackEntry ->
                        val sharedLocation = backStackEntry.arguments?.getString("shareLocation")
                        MessagesScreen(navController, sharedLocation)
                    }

                    composable(
                        route = Routes.MESSAGE_THREAD,
                        arguments = listOf(
                            navArgument("conversationId") { type = NavType.StringType },
                            navArgument("friendUserId") { type = NavType.StringType },
                            navArgument("initialMessage") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
                        val friendUserId = backStackEntry.arguments?.getString("friendUserId").orEmpty()
                        val initialMessage = backStackEntry.arguments?.getString("initialMessage")

                        MessageThreadScreen(
                            navController = navController,
                            conversationId = conversationId,
                            friendUserId = friendUserId,
                            initialMessage = initialMessage
                        )
                    }

                    composable(Routes.FRIENDS_AND_REQUESTS) {
                        FriendsAndRequestsScreen(navController, initialTab = 1)
                    }

                    composable(Routes.FRIEND_REQUESTS) {
                        FriendsAndRequestsScreen(navController, initialTab = 2)
                    }
                }
            }
        }
    }
}
