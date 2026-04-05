package com.example.sprint1homeui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sprint1homeui.appNavigation.FeatureCard
import com.example.sprint1homeui.appNavigation.featureItems
import com.example.sprint1homeui.ui.theme.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp)
    ) {
        //dummy events for widget
        CalendarWidget(events = getDummyCalendarEvents())

        Text(
            text = "Quick Access",
            modifier = Modifier
                .padding(start = 16.dp, top = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(featureItems) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { navController.navigate(feature.route) }
                )
            }
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