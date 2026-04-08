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
import com.example.cicompanion.ui.theme.CICompanionTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.calendar.CalendarScreen
import com.example.cicompanion.home.HomeScreen
import com.example.cicompanion.maps.MapScreen
import com.example.cicompanion.social.ProfileScreen
import com.example.cicompanion.ui.NavBar
//Commented out for this sprint 2, needed for notifications in future
//import com.example.cicompanion.social.NotificationScreen
import com.example.cicompanion.social.UserSearchScreen
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import kotlinx.coroutines.launch
import com.example.cicompanion.studyRoom.RoomListScreen
import com.example.cicompanions.appNavigation.DrawerProfileContent
import com.example.cicompanions.appNavigation.TopBar
import com.example.cicompanions.appNavigation.screenTitleForRoute

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
    val navController = rememberNavController() // Create navigation controller
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreenTitle = screenTitleForRoute(currentRoute)


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, //Stop hamburger menu from opening when swiped
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
                    showBackButton = currentRoute == Routes.USER_SEARCH,
                    onHamburgerClick = {
                        scope.launch{drawerState.open()} },
                    onBackClick = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.PROFILE) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    //Commented out for this sprint 2, needed for notifications page in future
                    onNotificationClick = {
                        //navController.navigate("notifications")
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
                    //Commented out for this sprint 2, uncomment for future notification
                    /*composable(Routes.NOTIFICATIONS) {
                        NotificationScreen(navController)

                    }*/
                    composable(Routes.USER_SEARCH) {
                        UserSearchScreen(navController)
                    }
                }
            }
        }
    }
}

