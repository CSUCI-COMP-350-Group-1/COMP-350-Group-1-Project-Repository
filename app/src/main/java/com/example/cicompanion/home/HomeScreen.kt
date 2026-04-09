package com.example.cicompanion.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cicompanion.appNavigation.FeatureCard
import com.example.cicompanion.appNavigation.featureItems
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.home.CalendarWidget
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val calendarViewModel: CalendarViewModel = viewModel()

    val widgetEvents = remember(calendarViewModel.events) {
        upcomingHomeWidgetEvents(calendarViewModel.events)
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp)
    ) {
        //dummy events for widget
        CalendarWidget(
            events = widgetEvents,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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

private fun upcomingHomeWidgetEvents(events: List<CalendarEvent>): List<CalendarEvent> {
    val now = ZonedDateTime.now()

    return events
        .filter { event -> event.endExclusive.isAfter(now) }
        .sortedBy { event -> event.start }
        .take(3)
}
