package com.example.cicompanion.maps

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

val CSUCI_BOUNDS = LatLngBounds(
    LatLng(34.157, -119.050),
    LatLng(34.168, -119.035)
)

val CSUCI_CENTER = LatLng(34.16174111410685, -119.04342498111538)

const val SH_PARKING_DESC = "Student housing parking (only for SH permit holders)"
const val GENERAL_PARKING_DESC = "General parking (A, F, E or Visitor permit required)"
const val RESTRICTED_PARKING_DESC = "Restricted parking lot (only for R or RV permit holders)"

val MAP_STYLE_JSON = """
    [
      { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.business", "elementType": "labels", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.school", "elementType": "labels", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.park", "elementType": "labels", "stylers": [ { "visibility": "off" } ] }
    ]
""".trimIndent()
