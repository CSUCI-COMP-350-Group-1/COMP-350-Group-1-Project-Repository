package com.example.sprint1homeui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home", fontSize = 20.sp) }
            )
        },
        content = { paddingValues ->
            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // apply scaffold padding
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ButtonMap(navController)
                    ButtonCalendar(navController)
                    // ADDED: Study Room button to page
                    ButtonStudyRoom(navController)

                }
            }
        }
    )
}

@Composable
fun ButtonMap(navController: NavHostController) {
    Button(onClick = { navController.navigate("map") }) {
        Text("Map")
    }
}

@Composable
fun ButtonCalendar(navController: NavHostController) {
    Button(onClick = { navController.navigate("calendar") }) {
        Text("Calendar")
    }
}

fun ButtonStudyRoom(navController: NavHostController) {
    Button(onClick =  { navController.navigate("studyRoom") }) {
        Text("Study Room")
    }
}