package com.example.cicompanion.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cicompanion.appNavigation.FeatureCard
import com.example.cicompanion.appNavigation.allAvailableFeatures
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.maps.MapViewModel
import com.example.cicompanion.maps.CustomPin
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandRedDark
import com.google.firebase.auth.FirebaseAuth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    calendarViewModel: CalendarViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    // Observe auth state to ensure customization is loaded when user signs in/out
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    val allEvents = calendarViewModel.events
    val now = ZonedDateTime.now()

    val upcomingEvents = remember(allEvents) {
        allEvents
            .filter { it.endExclusive.isAfter(now) }
            .sortedBy { it.start }
            .take(5)
    }

    val bookmarkedEvents = remember(allEvents) {
        allEvents
            .filter { it.isBookmarked }
            .sortedBy { it.start }
    }
    
    val pinnedEvents = remember(allEvents) {
        allEvents.filter { it.isPinned }
    }

    val favoritePins = remember(mapViewModel.customPins) {
        mapViewModel.customPins.filter { it.isFavorited }
    }

    var showCustomizer by remember { mutableStateOf(false) }

    // Trigger load customization whenever the user ID changes (login, logout, or swap)
    LaunchedEffect(currentUser?.uid) {
        homeViewModel.loadCustomization()
        mapViewModel.loadCustomPins()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp)
    ) {
        item {
            CalendarWidget(
                upcomingEvents = upcomingEvents,
                bookmarkedEvents = bookmarkedEvents,
                modifier = Modifier.padding(horizontal = 16.dp),
                onInfoClick = { event ->
                    calendarViewModel.resetFilters()
                    calendarViewModel.onDateSelected(event.start.toLocalDate())
                    calendarViewModel.setHighlightedEvent(event.id)
                    navController.navigate(Routes.CALENDAR)
                }
            )
        }

        if (pinnedEvents.isNotEmpty() || favoritePins.isNotEmpty()) {
            item {
                Text(
                    text = "Pinned & Favorites",
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            // Pinned Events
            items(pinnedEvents) { event ->
                PinnedEventItem(
                    event = event,
                    onNavigateToEvent = {
                        calendarViewModel.resetFilters()
                        calendarViewModel.onDateSelected(event.start.toLocalDate())
                        calendarViewModel.setHighlightedEvent(event.id)
                        navController.navigate(Routes.CALENDAR)
                    }
                )
            }

            // Favorite Pins
            items(favoritePins) { pin ->
                FavoritePinItem(
                    pin = pin,
                    onNavigateToMap = {
                        navController.navigate(Routes.MAP)
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (currentUser != null) {
                    IconButton(onClick = { showCustomizer = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Customize",
                            tint = BrandRedDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (homeViewModel.isLoadingCustomization) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandRedDark)
                }
            }
        } else if (homeViewModel.displayedFeatures.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No Quick Access buttons added.", color = Color.Gray)
                }
            }
        } else {
            items(homeViewModel.displayedFeatures.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { feature ->
                        FeatureCard(
                            feature = feature,
                            onClick = { navController.navigate(feature.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCustomizer && currentUser != null) {
        QuickAccessCustomizerDialog(
            currentSelection = homeViewModel.displayedFeatures.map { it.route },
            onDismiss = { showCustomizer = false },
            onSave = { selectedRoutes ->
                homeViewModel.updateCustomization(selectedRoutes)
                showCustomizer = false
            }
        )
    }
}

@Composable
fun FavoritePinItem(pin: CustomPin, onNavigateToMap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFFD700), // Gold
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pin.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Custom Pin • ${pin.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandRedDark.copy(alpha = 0.1f))
                    .clickable(onClick = onNavigateToMap),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "View on Map",
                    tint = BrandRedDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuickAccessCustomizerDialog(
    currentSelection: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(currentSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Customize Quick Access", 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                allAvailableFeatures.forEach { feature ->
                    val isFeatureSelected = selected.contains(feature.route)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (isFeatureSelected) {
                                    selected.filter { it != feature.route }
                                } else {
                                    selected + feature.route
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isFeatureSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandRedDark,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isFeatureSelected) BrandRedDark else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isFeatureSelected) Color.Black else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(selected) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedDark),
                shape = RoundedCornerShape(50)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            @Suppress("DEPRECATION")
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = BrandRedDark)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PinnedEventItem(event: CalendarEvent, onNavigateToEvent: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = Color(0xFF9C27B0), // Pinned purple
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.start.format(DateTimeFormatter.ofPattern("MMM d"))} • ${event.timeLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandRedDark.copy(alpha = 0.1f))
                    .clickable(onClick = onNavigateToEvent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "View in Calendar",
                    tint = BrandRedDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
