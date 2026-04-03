package com.example.sprint1homeui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.sprint1homeui.appNavigation.FeatureCard
import com.example.sprint1homeui.appNavigation.featureItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // This forces exactly 2 items per row
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), // Space around the whole grid
        verticalArrangement = Arrangement.spacedBy(16.dp), // Space between rows
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between columns
    ) {
        items(featureItems) { feature ->
            FeatureCard(
                feature = feature,
                onClick = { navController.navigate(feature.route) }
            )
        }
    }
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

@Composable
fun ButtonStudyRoom(navController: NavHostController) {
    Button(onClick =  { navController.navigate("studyRoom") }) {
        Text("Study Room")
    }
}