package com.example.myapplication3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    // Get VIEWMODEL
    private val viewModel: RoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Launch Study Room availability screen
                    RoomListScreen(viewModel)
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
@Composable
fun RoomListScreen(viewModel: RoomViewModel) {
    val weeklyData by viewModel.weeklyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedDate by remember { mutableStateOf("") }

    // Initialize selected date once data loads
    LaunchedEffect(weeklyData) {
        if (selectedDate.isEmpty() && weeklyData.isNotEmpty()) {
            selectedDate = weeklyData.keys.sorted().first()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Display text at top of page
        Text("CSUCI Weekly Availability", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val sortedDates = weeklyData.keys.sorted()

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(Color(0xFF2E7D32), "Available")
                LegendItem(Color(0xFFD32F2F), "Unavailable")
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