package com.example.cicompanion.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    onFilterClick: (LocationType?) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search campus...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedBorderColor = CoralRed,
                    focusedContainerColor = Color(0xFFF8F8F8),
                    unfocusedContainerColor = Color(0xFFF8F8F8)
                )
            )

            if (showSearchResults && filteredLocations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    LazyColumn {
                        items(filteredLocations) { location ->
                            ListItem(
                                headlineContent = { Text(location.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text(location.type.name, fontSize = 12.sp, color = Color.Gray) },
                                leadingContent = { Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(24.dp)) },
                                modifier = Modifier.clickable { onResultClick(location) }
                            )
                        }
                    }
                }
            }

            CategoryFilterRow(selectedType = filterType, onFilterClick = onFilterClick)
        }
    }
}

@Composable
fun CategoryFilterRow(selectedType: LocationType?, onFilterClick: (LocationType?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
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
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) Color.White else Color.DarkGray
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Color.DarkGray,
                fontSize = 13.sp
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
    tempPinSet: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPinMode) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).padding(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = onLocationRequest,
                        shape = CircleShape,
                        containerColor = Color.White,
                        contentColor = CoralRed,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_menu_mylocation), contentDescription = "My Location", modifier = Modifier.size(24.dp))
                    }
                }

                FloatingActionButton(
                    onClick = onTogglePinMode,
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = CoralRed,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = "Custom Pin",
                        modifier = Modifier.size(24.dp)
                    )
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
                    fontWeight = FontWeight.Bold
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
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, CoralRed)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Change Location")
                        }
                        Button(
                            onClick = onConfirmPin,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirm Location")
                        }
                    }
                }

                // Exit pin mode button
                Surface(
                    onClick = onTogglePinMode,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
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
