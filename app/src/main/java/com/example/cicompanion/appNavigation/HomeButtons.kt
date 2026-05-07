package com.example.cicompanion.appNavigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.*

data class FeatureItem(val route: String, val icon: ImageVector, val label: String)

// Premade Feature Definitions
val CalendarFeature = FeatureItem(Routes.CALENDAR, Icons.Default.CalendarMonth, "Calendar")
val ScheduleFeature = FeatureItem(Routes.SCHEDULE, Icons.Default.Schedule, "Schedule")
val StudyRoomFeature = FeatureItem(Routes.STUDY_ROOM, Icons.AutoMirrored.Filled.MenuBook, "Study Room")
val MapFeature = FeatureItem(Routes.MAP, Icons.Default.LocationOn, "Map")
val ProfileFeature = FeatureItem(Routes.PROFILE, Icons.Default.Person, "Profile")
val FriendsAndRequestsFeature = FeatureItem(Routes.FRIENDS_AND_REQUESTS, Icons.Default.People, "Friends & Requests")

val allAvailableFeatures = listOf(
    CalendarFeature,
    ScheduleFeature,
    StudyRoomFeature,
    MapFeature,
    ProfileFeature,
    FriendsAndRequestsFeature
)

val defaultFeatureItems = listOf(
    StudyRoomFeature,
    ScheduleFeature
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(feature: FeatureItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val buttonGradient = Brush.linearGradient(
        colors = listOf(BrandOrange, BrandCrimson, BrandPink)
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(buttonGradient),
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DEPRECATION")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feature.label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
