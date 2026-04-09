package com.example.cicompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.cicompanion.social.ProfileScreen
import com.example.cicompanion.social.UserSearchScreen
import com.example.cicompanion.studyRoom.RoomListScreen
import com.example.cicompanion.ui.NavBar
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CICompanionTheme {
                AppNavigation()
            }
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
                    showBackButton = currentRoute == Routes.USER_SEARCH,
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
                    }
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
                }
            }
        }
    }
}
