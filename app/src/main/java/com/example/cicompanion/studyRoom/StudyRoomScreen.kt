package com.example.cicompanion.studyRoom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.GrayIcon
import com.example.cicompanion.ui.theme.GreenAccent
import com.example.cicompanion.ui.theme.NavBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// *****IMPORTANT******
// *****This is for the study room*****
// Handle big picture view
// Shows all rooms and UI of page
// This is what the user will see
// Remembers what is going on
// Communicates with VIEWMODEL
@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBars in Material3
@Composable
fun RoomListScreen(viewModel: RoomViewModel, navController: NavHostController) {
    val weeklyData by viewModel.weeklyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedDate by remember { mutableStateOf("") }

    LaunchedEffect(weeklyData) {
        if (selectedDate.isEmpty() && weeklyData.isNotEmpty()) {
            selectedDate = weeklyData.keys.sorted().first()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground) // Changes top and bottom of background
                .padding(paddingValues)
                .padding(bottom = 16.dp)
                .background(AppBackground), //Changes background of study room
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize() //Don't add background here, does nothing
                    , contentAlignment =
                        androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val sortedDates = weeklyData.keys.sorted()

                // Legend and Tabs
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(16.dp) //background here changes legends box color
                    , horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(Color(0xFF2E7D32), "Available")
                    LegendItem(Color(0xFFD32F2F), "Booked")
                }

                if (sortedDates.isNotEmpty()) {
                    if (selectedDate.isBlank() || selectedDate !in sortedDates) {
                        selectedDate = sortedDates.first()
                    }

                    StudyRoomDaySelector(
                        selectedDate = selectedDate,
                        sortedDates = sortedDates,
                        onDateSelected = { selectedDate = it }
                    )
                }

                LazyColumn(modifier = Modifier
                    .padding(16.dp)) { //Don't add background here
                    items(weeklyData[selectedDate] ?: emptyList()) { room ->
                        RoomItem(room)
                    }
                }
            }
        }
    }
}


@Composable
private fun StudyRoomDaySelector(
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit
) {
    val selectedIndex = sortedDates.indexOf(selectedDate).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(AppBackground), // Changes day select box background
        //TODO Add round corners to this ^^^
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StudyRoomNavigationButton(
            symbol = "‹",
            enabled = selectedIndex > 0,
            onClick = { onDateSelected(sortedDates[selectedIndex - 1]) }
        )

        Text(
            text = formatStudyRoomSelectorDate(selectedDate),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        StudyRoomNavigationButton(
            symbol = "›",
            enabled = selectedIndex < sortedDates.lastIndex,
            onClick = { onDateSelected(sortedDates[selectedIndex + 1]) }
        )
    }
}

@Composable
private fun StudyRoomNavigationButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(NavBackground) // Changes color of day select arrow buttons
            .border(
                width = 1.dp,
                color = GrayIcon.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatStudyRoomSelectorDate(rawDate: String): String {
    return runCatching {
        LocalDate.parse(rawDate)
            .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }.getOrElse {
        rawDate
    }
}