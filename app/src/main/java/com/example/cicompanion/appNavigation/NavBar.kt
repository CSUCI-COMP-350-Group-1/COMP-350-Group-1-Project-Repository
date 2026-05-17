package com.example.cicompanion.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavBarItem("Home", Routes.HOME, Icons.Filled.Home),
        NavBarItem("Social", Routes.SOCIAL, Icons.Filled.People),
        NavBarItem("Planning", Routes.CALENDAR, Icons.AutoMirrored.Filled.EventNote),
        NavBarItem("Map", Routes.MAP, Icons.Filled.LocationOn)
    )

    // Capture theme colors in a composable context
    val backgroundColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBar(
        containerColor = backgroundColor,
        windowInsets = NavigationBarDefaults.windowInsets,
        modifier = Modifier
            .heightIn(min = 80.dp)
            .drawWithContent {
                drawContent()
                // Top border for the Nav Bar
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 3.dp.toPx()
                )
            }
    ){
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute?.substringBefore('?') == item.route

            NavigationBarItem(
                icon = { Icon(
                    imageVector = item.icon,
                    contentDescription = item.title
                ) },
                label = { 
                    Text(
                        text = item.title,
                        maxLines = 1
                    )
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    selectedTextColor = selectedColor,
                    unselectedTextColor = unselectedColor
                ),

                onClick = {
                    val currentDestination = navController.currentBackStackEntry?.destination?.route

                    if (currentDestination != item.route) {
                        navController.navigate(item.route)
                    } else {
                        // User is already on this screen, we could scroll to top or do nothing
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
