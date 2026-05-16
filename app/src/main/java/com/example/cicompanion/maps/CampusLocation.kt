package com.example.cicompanion.maps

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cicompanion.calendar.model.CalendarEvent
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
    val isPinned: Boolean = false,
    val isFavorited: Boolean = false,
    val associatedEventId: String? = null,
    val searchKeywords: List<String> = emptyList()
)

fun isEventAtLocation(event: CalendarEvent, location: CampusLocation): Boolean {
    if (location.isCustom) {
        return event.id == location.associatedEventId
    }
    
    val eventLoc = event.location ?: return false
    
    // Check main name
    if (eventLoc.contains(location.name, ignoreCase = true)) return true
    
    // Check keywords
    return location.searchKeywords.any { keyword -> 
        eventLoc.contains(keyword, ignoreCase = true) 
    }
}
