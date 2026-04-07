package com.example.cicompanion.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sprint1homeui.ui.theme.DarkBackground
import com.example.sprint1homeui.ui.theme.AppWhite
import com.example.sprint1homeui.ui.theme.BrandRedLight
import com.example.sprint1homeui.ui.theme.GrayIcon
import com.example.sprint1homeui.ui.theme.NavBackground

//For easy mapping in other files
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val MAP = "map"
    const val CALENDAR = "calendar"
    const val STUDY_ROOM = "studyRoom"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavBarItem("Home", "home", Icons.Filled.Home),
        NavBarItem("Search", "search", Icons.Filled.Search),
        NavBarItem("Map", "map", Icons.Filled.LocationOn)
    )
    val navBarBackground = NavBackground
    val borderColor = Color.Gray.copy(alpha = 0.3f)

    NavigationBar(
        containerColor = navBarBackground,
        modifier = Modifier
            .height(72.dp)
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
                label = null,
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
