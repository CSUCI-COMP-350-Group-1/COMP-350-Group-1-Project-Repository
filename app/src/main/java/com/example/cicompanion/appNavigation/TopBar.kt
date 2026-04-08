package com.example.cicompanions.appNavigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.ui.theme.BrandRedLighter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun screenTitleForRoute(route: String?): String {
    return when (route) {
        Routes.HOME -> "Home"
        Routes.SEARCH -> "Search"
        Routes.MAP -> "Map"
        Routes.CALENDAR -> "Calendar"
        Routes.STUDY_ROOM -> "Study Room"
        Routes.PROFILE -> "Profile"
        Routes.NOTIFICATIONS -> "Notifications"
        Routes.USER_SEARCH -> "User Search"
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
    onNotificationClick: () -> Unit) {
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
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(
                onClick = {
                    navController.navigate(Routes.PROFILE)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile"
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Profile", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}