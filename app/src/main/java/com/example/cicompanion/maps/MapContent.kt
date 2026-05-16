package com.example.cicompanion.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.cicompanion.calendar.model.CalendarEvent
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
fun MapContent(
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    userLocation: LatLng?,
    selectedLocation: CampusLocation?,
    onLocationClick: (CampusLocation) -> Unit,
    onMapClick: (LatLng) -> Unit,
    displayLocations: List<CampusLocation>,
    events: List<CalendarEvent>,
    isPinMode: Boolean = false,
    tempPinLocation: LatLng? = null
) {
    val uiSettings = remember(isPinMode) {
        MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = !isPinMode,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(hasLocationPermission, isPinMode) {
        MapProperties(
            isMyLocationEnabled = false,
            latLngBoundsForCameraTarget = CSUCI_BOUNDS,
            minZoomPreference = 15f,
            mapStyleOptions = MapStyleOptions(MAP_STYLE_JSON)
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapClick = onMapClick
    ) {
        if (userLocation != null) UserLocationMarker(userLocation)

        if (isPinMode) {
            tempPinLocation?.let {
                MarkerComposable(
                    state = rememberMarkerState(position = it),
                    anchor = Offset(0.5f, 1.0f)
                ) {
                    CustomPinMarkerIcon(
                        color = com.example.cicompanion.ui.theme.CoralRed,
                        isPinned = false,
                        eventCount = 0,
                        isEditing = true
                    )
                }
            }
        } else {
            // Draw shared temporary pin if exists
            selectedLocation?.let { location ->
                if (location.id == "shared_temp") {
                    MarkerComposable(
                        state = rememberMarkerState(position = location.position),
                        zIndex = 100f,
                        anchor = Offset(0.5f, 1.0f),
                        onClick = {
                            onLocationClick(location)
                            true
                        }
                    ) {
                        SelectedPointerIcon(location, 0, false, false)
                    }
                }
            }

            displayLocations.forEach { location ->
                val isSelected = selectedLocation?.id == location.id
                val locationEvents = events.filter {
                    (it.calendarId == "custom" || it.isPinned) && isEventAtLocation(it, location)
                }
                val eventCount = locationEvents.size
                val hasPinnedEvent = locationEvents.any { it.isPinned }

                key(location.id, location.name, isSelected, eventCount, hasPinnedEvent, location.isCustom, location.isPinned) {
                    MarkerComposable(
                        state = rememberMarkerState(position = location.position),
                        zIndex = if (isSelected) 100f else 1f,
                        anchor = if (isSelected || location.isCustom) Offset(0.5f, 1.0f) else Offset(0.5f, 0.5f),
                        onClick = {
                            onLocationClick(location)
                            true
                        }
                    ) {
                        if (isSelected) {
                            SelectedPointerIcon(location, eventCount, hasPinnedEvent)
                        } else {
                            if (location.isCustom) {
                                CustomPinMarkerIcon(location.color, location.isPinned, eventCount)
                            } else {
                                LandmarkIcon(
                                    icon = location.icon,
                                    color = location.color,
                                    eventCount = eventCount,
                                    hasPinnedEvent = hasPinnedEvent,
                                    isCustom = location.isCustom,
                                    isPinned = location.isPinned
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
