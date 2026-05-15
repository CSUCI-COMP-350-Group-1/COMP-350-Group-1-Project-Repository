package com.example.cicompanion.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController

@Composable
fun CalendarScreen(
    navController: NavHostController,
    calendarViewModel: CalendarViewModel,
    initialTab: Int = 0
) {
    // If navigating from Routes.SCHEDULE, set mode to SCHEDULE
    LaunchedEffect(initialTab) {
        if (initialTab == 1) {
            calendarViewModel.updateMode(CalendarMode.SCHEDULE)
        }
    }

    CalendarApp(viewModel = calendarViewModel)
}
