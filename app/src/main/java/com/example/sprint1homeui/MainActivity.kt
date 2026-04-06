package com.example.sprint1homeui

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
import com.example.sprint1homeui.ui.theme.Sprint1HomeUITheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sprint1homeui.calendar.CalendarScreen
import com.example.sprint1homeui.home.HomeScreen
import com.example.sprint1homeui.maps.MapScreen
import com.example.sprint1homeui.social.ProfileScreen
import com.example.sprint1homeui.ui.NavBar
import com.example.sprint1homeui.appNavigation.DrawerProfileContent
import com.example.sprint1homeui.appNavigation.TopBar
import com.example.sprint1homeui.appNavigation.screenTitleForRoute
import com.example.sprint1homeui.social.NotificationScreen
import com.example.sprint1homeui.ui.Routes
import com.example.sprint1homeui.ui.theme.AppBackground
import kotlinx.coroutines.launch
import com.example.sprint1homeui.studyRoom.RoomListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Sprint1HomeUITheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController() // Create navigation controller
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreenTitle = screenTitleForRoute(navBackStackEntry?.destination?.route)


    ModalNavigationDrawer(
        drawerState =
            drawerState,
        drawerContent = {
            DrawerProfileContent(
                navController,
                drawerState,
                scope)
        }
    ){
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                TopBar(
                    title = currentScreenTitle,
                    onHamburgerClick = {
                        scope.launch{drawerState.open()}},
                    onNotificationClick = {
                        navController.navigate("notifications")
                    }
                )},
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
                        CalendarScreen(navController)
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
                    composable(Routes.NOTIFICATIONS) {
                        NotificationScreen(navController)

                    }
                }
            }
        }
    }
}

