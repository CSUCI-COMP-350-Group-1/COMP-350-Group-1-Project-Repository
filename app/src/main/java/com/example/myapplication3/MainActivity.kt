package com.example.myapplication3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {
    private val viewModel: RoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Create a state to track which screen to show
                var currentScreen by remember { mutableStateOf("start") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen == "start") {
                        // Show landing page
                        StartScreen(onNavigateToTracker = { currentScreen = "tracker" })
                    } else {
                        // Show tracker page
                        RoomListScreen(
                            viewModel = viewModel,
                            onBackClicked = { currentScreen = "start" }
                        )
                    }
                }
            }
        }
    }
}



// Handle big picture view
// Shows all rooms and UI of page
// This is what the user will see
// Remembers what is going on
// Communicates with VIEWMODEL

@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBars in Material3
@Composable
fun RoomListScreen(viewModel: RoomViewModel, onBackClicked: () -> Unit) {
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
                title = { Text("CSUCI Weekly Availability", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF800000), // CSUCI Red??
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues -> // Prevents contents being hidden from top bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
