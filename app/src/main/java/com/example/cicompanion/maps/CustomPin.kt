package com.example.cicompanion.maps

import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin

data class CustomPin(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val colorArgb: Int = Color(0xFFE91E63).toArgb(), // Default pinkish
    val isPinned: Boolean = false,
    val isFavorited: Boolean = false,
    val associatedEventId: String? = null
) {
    val position: LatLng get() = LatLng(latitude, longitude)
    val color: Color get() = Color(colorArgb)
    
    fun toCampusLocation(): CampusLocation {
        return CampusLocation(
            id = id,
            name = name,
            position = position,
            description = description,
            type = LocationType.CUSTOM,
            icon = Icons.Default.PushPin,
            color = color,
            isCustom = true,
            isPinned = isPinned,
            isFavorited = isFavorited,
            associatedEventId = associatedEventId
        )
    }
}
