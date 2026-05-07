package com.example.cicompanion.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun CalendarScreen(
    navController: androidx.navigation.NavHostController,
    calendarViewModel: CalendarViewModel,
    initialTab: Int = 0
) {
    // CALENDAR SCHEDULE CHANGE:
    // The calendar screen now has two tabs:
    // 1) the existing calendar UI
    // 2) the new schedule UI
    // We keep the shared CalendarViewModel passed from MainActivity so the
    // schedule data and calendar data stay in sync across screens.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Calendar") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Schedule") }
            )
        }

        when (selectedTabIndex) {
            0 -> CalendarApp(viewModel = calendarViewModel)
            1 -> ScheduleTab(viewModel = calendarViewModel)
        }
    }
}