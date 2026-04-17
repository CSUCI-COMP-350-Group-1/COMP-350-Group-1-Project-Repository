package com.example.cicompanion.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cicompanion.ui.theme.DarkBackground
import com.example.cicompanion.ui.theme.AppWhite
import com.example.cicompanion.ui.theme.BrandRedLight
import com.example.cicompanion.ui.theme.GrayIcon
import com.example.cicompanion.ui.theme.NavBackground
import kotlin.math.min
import androidx.navigation.NavGraph.Companion.findStartDestination

//For easy mapping in other files
object Routes {
    const val HOME = "home"
    const val SOCIAL = "profile" // go to profile page ATM for social
    // def change that to another new page when you make it
    const val MAP = "map"
    const val CALENDAR = "calendar"
    const val STUDY_ROOM = "studyRoom"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val USER_SEARCH = "user_search"
    const val FRIEND_REQUESTS = "friendRequests"
    const val SEARCH = "search" // Keeping just in case it breaks anything
}

@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavBarItem("Home", "home", Icons.Filled.Home),
        NavBarItem("Social", "profile", Icons.Filled.People),
        NavBarItem("Calendar", "calendar", Icons.Filled.CalendarMonth),
        NavBarItem("Map", "map", Icons.Filled.LocationOn)
    )
    val navBarBackground = NavBackground
    val borderColor = Color.Gray.copy(alpha = 0.3f)

    NavigationBar(
        containerColor = navBarBackground,
        windowInsets = NavigationBarDefaults.windowInsets,
        modifier = Modifier
            .heightIn(min = 80.dp)
            .drawWithContent {
                drawContent()
                drawLine(color =
                    borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ){
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = { Icon(
                    imageVector =
                        item.icon,
                    contentDescription = item.title,
                    tint = if(isSelected) BrandRedLight else GrayIcon
                ) },
                label = { 
                    Text(
                        text = item.title,
                        color = if(isSelected) BrandRedLight else GrayIcon
                    )
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),

                onClick = {
                    val currentDestination = navController.currentBackStackEntry?.destination?.route

                    if (currentDestination != item.route) {
                        navController.navigate(item.route)
                    } else {
                        navController.navigate(item.route) {

                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }

                            launchSingleTop = true

                            //restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class NavBarItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
