package com.example.cicompanion.sidebar

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.appNavigation.screenTitleForRoute
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme
import androidx.core.net.toUri

/**
 * Represents a searchable item in the app.
 * Can be a navigation route or an external action.
 */
data class AppFeature(
    val name: String,
    val path: String,
    val action: FeatureAction
)

sealed class FeatureAction {
    data class Navigate(val route: String) : FeatureAction()
    data class OpenUrl(val url: String) : FeatureAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    val context = LocalContext.current

    val appFeaturesList = remember {
        val features = mutableListOf<AppFeature>()

        val navRoutes = listOf(
            Routes.HOME,
            Routes.MAP,
            Routes.CALENDAR,
            Routes.STUDY_ROOM,
            Routes.PROFILE,
            Routes.USER_SEARCH,
            Routes.FRIEND_REQUESTS
        )

        navRoutes.forEach { route ->
            val name = screenTitleForRoute(route)
            features.add(
                AppFeature(
                    name = name,
                    path = if (route == Routes.HOME) "Home" else "Home > $name",
                    action = FeatureAction.Navigate(route)
                )
            )
        }

        features.sortBy { it.name }
        features
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredResults = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            appFeaturesList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a feature...") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredResults) { feature ->
                    SearchResultItem(feature) {
                        when (val action = feature.action) {
                            is FeatureAction.Navigate -> {
                                navController.navigate(action.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            is FeatureAction.OpenUrl -> {
                                val intent = Intent(Intent.ACTION_VIEW, action.url.toUri())
                                context.startActivity(intent)
                            }
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(feature: AppFeature, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = feature.name, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(
                text = feature.path,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    CICompanionTheme {
        val navController = rememberNavController()
        SearchScreen(navController = navController)
    }
}