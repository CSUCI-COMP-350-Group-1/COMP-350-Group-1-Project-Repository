package com.example.cicompanion.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme

data class AppFeature(
    val name: String,
    val route: String,
    val path: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    val appFeaturesList = remember {
        // hardcoded list of features.
        // in the future, the search feature can be expanded into a dynamic equivalent
        // then, adding to the potential search results will be less tedious,
        // especially if a future feature is within a subscreen, e.g.
        // "Home > Study Room > Reserve Room"
        listOf(
            // format: AppFeature("<name>", "<route>", "<path string>"),
            AppFeature("Map", "map", "Home > Map"),
            AppFeature("Calendar", "calendar", "Home > Calendar"),
            AppFeature("Study Room Weekly Availability", "studyRoom", "Home > Study Room"),
            AppFeature("Profile", "profile", "Home > Profile")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredResults = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        appFeaturesList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = AppBackground,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for a feature...") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    items(filteredResults) { result ->
                        SearchResultItem(result) {
                            navController.navigate(result.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // re-selecting the same item
                                launchSingleTop = true
                                // Restore state when re-selecting a previously selected item
                                restoreState = true
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    )
}

@Composable
fun SearchResultItem(feature: AppFeature, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = feature.name, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = feature.path,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier.clickable { onClick() }
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
