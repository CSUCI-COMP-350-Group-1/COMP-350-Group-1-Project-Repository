package com.example.sprint1homeui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    val calendarViewModel: CalendarViewModel = viewModel()

    Scaffold(
    ) { paddingValues ->
        CalendarApp(
            viewModel = calendarViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}