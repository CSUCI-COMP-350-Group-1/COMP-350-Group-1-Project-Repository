package com.example.cicompanion.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.ui.theme.CoralRed

@Composable
fun MapTopControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showSearchResults: Boolean,
    onClearSearch: () -> Unit,
    filteredLocations: List<CampusLocation>,
    onResultClick: (CampusLocation) -> Unit,
    filterType: LocationType?,
    onFilterClick: (LocationType?) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Floating search bar with menu button
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search campus...") },
                    leadingIcon = { 
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.DarkGray)
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.padding(end = 8.dp))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                if (showSearchResults && filteredLocations.isNotEmpty()) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(filteredLocations) { location ->
                            ListItem(
                                headlineContent = { Text(location.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                supportingContent = { Text(location.type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = Color.Gray) },
                                leadingContent = { 
                                    Surface(
                                        shape = CircleShape,
                                        color = location.color.copy(alpha = 0.1f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                location.icon, 
                                                contentDescription = null, 
                                                tint = location.color, 
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { onResultClick(location) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CategoryFilterRow(selectedType = filterType, onFilterClick = onFilterClick)
    }
}

@Composable
fun CategoryFilterRow(selectedType: LocationType?, onFilterClick: (LocationType?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            CustomFilterChip(
                selected = selectedType == null,
                onClick = { onFilterClick(null) },
                label = "All",
                icon = null
            )
        }
        LocationType.entries.forEach { type ->
            item {
                val icon = when (type) {
                    LocationType.BUILDING -> Icons.Default.Business
                    LocationType.FOOD -> Icons.Default.Restaurant
                    LocationType.AREA -> Icons.Default.People
                    LocationType.HOUSING -> Icons.Default.Home
                    LocationType.PARKING -> Icons.Default.LocalParking
                    LocationType.CUSTOM -> Icons.Default.PushPin
                }
                CustomFilterChip(
                    selected = selectedType == type,
                    onClick = { onFilterClick(type) },
                    label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = icon
                )
            }
        }
    }
}

@Composable
fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    Surface(
        onClick = onClick,
        color = if (selected) CoralRed else Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = if (selected) null else BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) Color.White else Color.DarkGray
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MapOverlays(
    hasLocationPermission: Boolean, 
    isLoadingLocation: Boolean,
    isPinMode: Boolean,
    isEditing: Boolean = false,
    onLocationRequest: () -> Unit,
    onTogglePinMode: () -> Unit,
    onConfirmPin: () -> Unit,
    onClearPin: () -> Unit,
    tempPinSet: Boolean,
    isLoggedIn: Boolean = true
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPinMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = onLocationRequest,
                        shape = CircleShape,
                        containerColor = Color.White,
                        contentColor = CoralRed,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .size(64.dp)
                            .border(3.dp, CoralRed, CircleShape)
                    ) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = CoralRed)
                        } else {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation), 
                                contentDescription = "My Location", 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                if (isLoggedIn) {
                    FloatingActionButton(
                        onClick = onTogglePinMode,
                        shape = CircleShape,
                        containerColor = CoralRed,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .size(64.dp)
                            .border(3.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = "Custom Pin",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        if (isPinMode) {
            // Header for pin mode
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = CoralRed,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = if (isEditing) "Move the pin or confirm location."
                           else if (tempPinSet) "Pin placed! Confirm to continue."
                           else "Tap on map to place your pin",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Bottom controls for pin mode
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (tempPinSet) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onClearPin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CoralRed),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CoralRed),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                        Button(
                            onClick = onConfirmPin,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirm")
                        }
                    }
                }

                // Exit pin mode button
                Surface(
                    onClick = { onTogglePinMode() },
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (!hasLocationPermission && !isPinMode) PermissionCard(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun PermissionCard(modifier: Modifier) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Location permission is required to show your position on the map.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
    }
}
