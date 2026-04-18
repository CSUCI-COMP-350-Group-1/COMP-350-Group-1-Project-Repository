
package com.example.cicompanion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.ui.Routes
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
        listOf(
            AppFeature("Map", Routes.MAP, "Home > Map"),
            AppFeature("Calendar", Routes.CALENDAR, "Home > Calendar"),
            AppFeature("Study Room Weekly Availability", Routes.STUDY_ROOM, "Home > Study Room"),
            AppFeature("Profile", Routes.PROFILE, "Home > Profile")
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search for a feature...") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    DropdownMenu( // changed search to be a dropdown menu for a smoother experience somewhat
                        expanded = filteredResults.isNotEmpty(),
                        onDismissRequest = { searchQuery = "" },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White),
                        properties = PopupProperties(focusable = false)
                    ) {
                        filteredResults.forEach { result ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = result.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = result.path,
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    navController.navigate(result.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    searchQuery = ""
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
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