package com.example.sprint1homeui.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState



@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavBarItem("Home", "home"),
        NavBarItem("Search", "search"),
        NavBarItem("Profile", "profile")
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { /* add later */ },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route

                    val homeRoutes = listOf("home", "map", "calendar")

                    val isOnTab = when (item.route) {
                        "home" -> currentRoute in homeRoutes
                        else -> currentRoute == item.route
                    }

                    if (isOnTab) {
                        navController.popBackStack(item.route, inclusive = false)
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                }
            )
        }
    }
}

data class NavBarItem(val title: String, val route: String)
