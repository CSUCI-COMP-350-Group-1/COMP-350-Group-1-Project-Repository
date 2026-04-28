package com.example.cicompanion.maps

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.maps.model.LatLng

enum class LocationType { BUILDING, PARKING, FOOD, AREA, HOUSING, CUSTOM }

data class CampusLocation(
    val id: String,
    val name: String,
    val position: LatLng,
    val description: String,
    val type: LocationType,
    val icon: ImageVector,
    val color: Color,
    val isCustom: Boolean = false,
    val isFavorited: Boolean = false,
    val associatedEventId: String? = null
)
