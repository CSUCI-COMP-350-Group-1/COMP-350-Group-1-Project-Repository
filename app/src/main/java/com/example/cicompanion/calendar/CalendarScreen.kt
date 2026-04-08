package com.example.cicompanion.calendar

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun CalendarScreen(navController: NavHostController) {
    val calendarViewModel: CalendarViewModel = viewModel()
    CalendarApp(viewModel = calendarViewModel)
}
