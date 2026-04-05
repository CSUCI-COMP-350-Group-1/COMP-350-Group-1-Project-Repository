package com.example.sprint1homeui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint1homeui.ui.theme.Sprint1HomeUITheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sprint1homeui.calendar.CalendarScreen
import com.example.sprint1homeui.home.HomeScreen
import com.example.sprint1homeui.maps.MapScreen
import com.example.sprint1homeui.social.ProfileScreen
import com.example.sprint1homeui.studyRoom.LegendItem
import com.example.sprint1homeui.studyRoom.RoomItem
import com.example.sprint1homeui.studyRoom.RoomViewModel
import com.example.sprint1homeui.ui.NavBar

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

    Scaffold(
        bottomBar = { NavBar(navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(navController)
                }
                composable("map") {
                    MapScreen(navController)
                }
                composable("calendar") {
                    CalendarScreen(navController)
                }
                // ADDED: for study room
                composable("studyRoom") {
                    RoomListScreen(viewModel = viewModel(), navController = navController)
                }
                composable("search") {
                    SearchScreen(navController)
                }
                composable("profile") {
                    ProfileScreen(navController)
                }
            }
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { NavBar(navController) } // persistent bottom bar
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("search") { SearchScreen(navController) }
            composable("profile") { ProfileScreen(navController) }

            // ADDED: Study Room Tracker route
            composable("studyRoom") {
                RoomListScreen(viewModel = viewModel(), navController = navController)
            }
        }
    }
}


// *****IMPORTANT******
// *****This is for the study room*****
// Handle big picture view
// Shows all rooms and UI of page
// This is what the user will see
// Remembers what is going on
// Communicates with VIEWMODEL
@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBars in Material3
@Composable
// Updated to match Lorenzo's navigation
fun RoomListScreen(viewModel: RoomViewModel, navController: NavHostController) {
    val weeklyData by viewModel.weeklyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedDate by remember { mutableStateOf("") }

    LaunchedEffect(weeklyData) {
        if (selectedDate.isEmpty() && weeklyData.isNotEmpty()) {
            selectedDate = weeklyData.keys.sorted().first()
        }
    }

    // SCAFFOLD is the standard "frame" for an Android Screen
    // Replace column here with scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                // Display text at top of page
                title = { Text("Study Room Weekly Availability") },
            )
        }
    ) { paddingValues -> // Prevents contents being hidden from top bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 16.dp), // For back bottom button
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val sortedDates = weeklyData.keys.sorted()

                // Legend and Tabs
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(Color(0xFF2E7D32), "Available")
                    LegendItem(Color(0xFFD32F2F), "Booked")
                }

                ScrollableTabRow(selectedTabIndex = sortedDates.indexOf(selectedDate).coerceAtLeast(0)) {
                    sortedDates.forEach { date ->
                        Tab(
                            selected = selectedDate == date,
                            onClick = { selectedDate = date },
                            text = { Text(date.substring(5).replace("-", "/")) }
                        )
                    }
                }

                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(weeklyData[selectedDate] ?: emptyList()) { room ->
                        RoomItem(room)
                    }
                }
            }
        }
    }
}