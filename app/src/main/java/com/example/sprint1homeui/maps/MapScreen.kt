package com.example.sprint1homeui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// sets the boundaries, preventing user from scrolling away from CSUCI
private val CSUCI_BOUNDS = LatLngBounds(
    LatLng(34.155, -119.055), // Southwest corner
    LatLng(34.170, -119.030)  // Northeast corner
)
private val CSUCI_CENTER = LatLng(34.16222772570257, -119.0435017637274)

// Parking lot descriptions, called here since it's not ethical to copy-paste them for each
private const val SH_PARKING_DESC = "Student housing parking (only for SH permit holders)"
private const val GENERAL_PARKING_DESC = "General parking (A, F, E or Visitor permit required)"
private const val RESTRICTED_PARKING_DESC = "Restricted parking lot (only for R or RV permit holders)"

// Google maps customizations, to get rid of preexisting names and locations on maps
private val MAP_STYLE_JSON = """
    [
      {
        "featureType": "poi",
        "stylers": [
          { "visibility": "off" }
        ]
      },
      {
        "featureType": "poi.business",
        "elementType": "labels",
        "stylers": [
          { "visibility": "off" }
        ]
      },
      {
        "featureType": "poi.school",
        "elementType": "labels",
        "stylers": [
          { "visibility": "off" }
        ]
      },
      {
        "featureType": "poi.park",
        "elementType": "labels",
        "stylers": [
          { "visibility": "off" }
        ]
      }
    ]
""".trimIndent()

// Location categories for filtering, called in actual location itself
enum class LocationType { BUILDING, PARKING }

// we have a few data classes for each campus location, customizable
data class CampusLocation(
    val name: String,
    val position: LatLng,
    val description: String,
    val type: LocationType,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// List of all campus locations including buildings, food and parking lots
val campusLocations = listOf(
    // Buildings
    CampusLocation("Bell Tower", LatLng(34.1614, -119.0428), "Center of Campus", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("John Spoor Broome Library", LatLng(34.1624, -119.0434), "Main Library", LocationType.BUILDING, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF1976D2)),
    CampusLocation("Student Union", LatLng(34.1610, -119.0436), "Dining and Lounge", LocationType.BUILDING, Icons.Default.Groups, Color(0xFF388E3C)),
    CampusLocation("Marin Hall", LatLng(34.164528096869034, -119.04507117740494), "Faculty Offices for Mathematics and Data Science", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Shasta Hall", LatLng(34.164576865185516, -119.04472618829523), "Faculty Offices for Computer Science and Engineering", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Gateway Hall", LatLng(34.16483652693463, -119.04547452459948), "Large building with classrooms and study rooms", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Napa Hall", LatLng(34.16377605025435, -119.04540741204008), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Solano Hall", LatLng(34.16340620613301, -119.0450085042822), "Offices for faculty, Division of Technology and Innovation", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Martin V. Smith Hall", LatLng(34.162909062826785, -119.04476226672068), "Houses Nursing Program", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Sierra Hall", LatLng(34.16229510038971, -119.04461124101422), "Multimode lecture halls, classrooms and faculty offices.", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Del Norte Hall", LatLng(34.163180726190696, -119.04410235004629), "Lecture classrooms", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Madera Hall", LatLng(34.1629335125641, -119.04394147483745), "Lecture classrooms", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Placer Hall", LatLng(34.163344972360754, -119.04300312204607), "Offices of University Police and Parking Services", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Richard R. Rush Hall", LatLng(34.162673963555044, -119.04342008432133), "Houses the University President and other administrative functions for the campus.", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Chaparral Hall", LatLng(34.162096255189496, -119.04560917381353), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Ironwood Hall", LatLng(34.16245544107054, -119.04645265103974), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("El Dorado Hall", LatLng(34.16423447296913, -119.04710369220899), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Aliso Hall", LatLng(34.16133147263596, -119.04535150727516), "8 Science Labs and 16 Faculty Offices", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Yuba Hall", LatLng(34.16407767205871, -119.04109248173509), "Houses the Student Health Center", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Sage Hall", LatLng(34.164167218645936, -119.04222881533492), "The Enrollment Center.", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Malibu Hall", LatLng(34.16126160533967, -119.04086506383092), "", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Topanga Hall", LatLng(34.16019137315942, -119.04169333763335), "Serves the Art Program, labs and other art facilities.", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Arroyo Hall", LatLng(34.160354318454424, -119.04489629947327), "Wellness and Athletics Office. Recreation Center.", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Trinity Hall", LatLng(34.15934671289535, -119.0423644726643), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Lindero Hall", LatLng(34.15956619235504, -119.04141202350661), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Ojai Hall", LatLng(34.16173923354911, -119.04257933412188), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    
    // Parking Lots, in blue
    CampusLocation("Parking Lot A3", LatLng(34.166606172710715, -119.04703678095836), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A4", LatLng(34.164244290933254, -119.04646170905023), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A11", LatLng(34.164126287402695, -119.04786350249398), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A2", LatLng(34.16410868093253, -119.04164009131796), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A6", LatLng(34.16325952040607, -119.04202460036679), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A1", LatLng(34.163586436668446, -119.04267748158364), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A8", LatLng(34.16309446392495, -119.04030380989158), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A/E", LatLng(34.16186741815617, -119.04156570455609), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A7", LatLng(34.160649977624885, -119.04118719083276), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot A10", LatLng(34.15940842843663, -119.04062564402817), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2)),
    CampusLocation("Parking Lot SH1", LatLng(34.15912561853075, -119.04537811915849), SH_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF00BCD4)),
    CampusLocation("Parking Lot R1", LatLng(34.1630169076814, -119.04316226033373), RESTRICTED_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF673AB7)),
    CampusLocation("Parking Lot A5", LatLng(34.16050864644475, -119.04463491964691), GENERAL_PARKING_DESC, LocationType.PARKING, Icons.Default.LocalParking, Color(0xFF1976D2))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// map screen function
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedDestination by remember { mutableStateOf<LatLng?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Search and Filtering State
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<LocationType?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CSUCI_CENTER, 17f)
    }

    // Filter locations based on search query and selected filter type
    val filteredLocations = remember(searchQuery, filterType) {
        val trimmedQuery = searchQuery.trim()
        campusLocations.filter { 
            (trimmedQuery.isEmpty() || it.name.contains(trimmedQuery, ignoreCase = true)) && 
            (filterType == null || it.type == filterType)
        }
    }

    // Permission launcher logic
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            scope.launch { 
                fetchLocation(fusedLocationClient, cameraPositionState) { userLocation = it } 
            }
        }
    }

    // Check for permissions on start
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            fetchLocation(fusedLocationClient, cameraPositionState) { userLocation = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column {
                    TopAppBar(
                        title = { Text("CSUCI Campus Map", fontSize = 20.sp) }
                    )
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search campus...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filterType == null,
                                onClick = { filterType = null },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == LocationType.BUILDING,
                                onClick = { filterType = LocationType.BUILDING },
                                label = { Text("Buildings") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == LocationType.PARKING,
                                onClick = { filterType = LocationType.PARKING },
                                label = { Text("Parking Only") },
                                leadingIcon = { Icon(Icons.Default.LocalParking, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Refactored Map Content
            MapContent(
                cameraPositionState = cameraPositionState,
                hasLocationPermission = hasLocationPermission,
                userLocation = userLocation,
                selectedDestination = selectedDestination,
                onLocationClick = { selectedDestination = it },
                onMapClick = { selectedDestination = null },
                displayLocations = filteredLocations
            )

            // Refactored UI Overlays
            MapOverlays(
                hasLocationPermission = hasLocationPermission,
                isLoadingLocation = isLoadingLocation,
                onLocationRequest = {
                    scope.launch {
                        isLoadingLocation = true
                        fetchLocation(fusedLocationClient, cameraPositionState) { 
                            userLocation = it 
                            isLoadingLocation = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
// content of the map
fun MapContent(
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    userLocation: LatLng?,
    selectedDestination: LatLng?,
    onLocationClick: (LatLng) -> Unit,
    onMapClick: (LatLng) -> Unit,
    displayLocations: List<CampusLocation>
) {
    val uiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = false, // Custom back to location button used
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            latLngBoundsForCameraTarget = CSUCI_BOUNDS, // Strict restriction to campus
            mapStyleOptions = MapStyleOptions(MAP_STYLE_JSON) // Hides preset POIs and labels
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapClick = { 
            onMapClick(it)
        }
    ) {
        // User Location Marker with custom icon
        if (userLocation != null) {
            UserLocationMarker(userLocation)
        }
        
        // Render filtered markers (landmarks and parking lots)
        displayLocations.forEach { location ->
            key(location.name) {
                MarkerComposable(
                    state = rememberMarkerState(key = location.name, position = location.position),
                    title = location.name,
                    snippet = location.description,
                    onClick = {
                        onLocationClick(it.position)
                        false // return false to show info window
                    }
                ) {
                    LandmarkIcon(icon = location.icon, contentDescription = location.name, color = location.color)
                }
            }
        }

        // Draw walking path, a straight line
        if (userLocation != null && selectedDestination != null) {
            Polyline(
                points = listOf(userLocation, selectedDestination),
                color = Color(0xFF1A73E8), // a blue line
                width = 8f,
                geodesic = true
            )
        }
    }
}

@Composable
fun LandmarkIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    contentDescription: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(27.dp) // size of the icon
            .background(color, CircleShape) // we set this individually
            .padding(4.dp), // padding of icons
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White, // default, icon is set
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun UserLocationMarker(position: LatLng) {
    MarkerComposable(
        state = rememberMarkerState(position = position),
        title = "You are here!" // current location
    ) {
        Icon(
            imageVector = Icons.Default.PersonPinCircle,
            contentDescription = "User Location", // current location
            tint = Color.Gray,
            modifier = Modifier.size(36.dp)
        )
    }
    
    // Accuracy circle
    Circle(
        center = position,
        radius = 20.0, // size of circle
        fillColor = Color.Gray.copy(alpha = 0.2f),
        strokeColor = Color.Gray.copy(alpha = 0.5f),
        strokeWidth = 2f
    )
}

@Composable
// overlays, handles icons
fun MapOverlays(
    hasLocationPermission: Boolean,
    isLoadingLocation: Boolean,
    onLocationRequest: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // My Location Button
            if (hasLocationPermission) {
                MyLocationFab(onClick = onLocationRequest)
            }
        }

        if (isLoadingLocation) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (!hasLocationPermission) {
            PermissionCard(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
// the custom location button
fun MyLocationFab(onClick: () -> Unit) { 
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.systemBarsPadding()
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
            contentDescription = "My Location"
        )
    }
}

@Composable
// function for the location services permission
fun PermissionCard(modifier: Modifier) { 
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        @Suppress("DEPRECATION")
        Text(
            text = "Location permission is required to show your position on the map.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

// fetch user location logic
suspend fun fetchLocation(
    client: FusedLocationProviderClient,
    cameraState: CameraPositionState,
    onResult: (LatLng) -> Unit
) {
    try {
        var location = client.lastLocation.await()
        if (location == null || location.accuracy > 100) {
            location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            onResult(latLng)
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
    } catch (e: SecurityException) {
        // Handle no permissions
    } catch (e: Exception) {
        // Handle errors fetching location
    }
}

// reset camera to CSUCI center
suspend fun resetCameraToCampus(cameraState: CameraPositionState) {
    cameraState.animate(CameraUpdateFactory.newLatLngZoom(CSUCI_CENTER, 17f))
}

// move to a certain landmark
suspend fun moveToLandmark(cameraState: CameraPositionState, landmark: LatLng) {
    cameraState.animate(CameraUpdateFactory.newLatLngZoom(landmark, 18f))
}

/**
 * Quick jump to Bell Tower
 */
suspend fun jumpToBellTower(cameraState: CameraPositionState) = moveToLandmark(cameraState, campusLocations[0].position)

/**
 * Quick jump to Broome Library
 */
suspend fun jumpToLibrary(cameraState: CameraPositionState) = moveToLandmark(cameraState, campusLocations[1].position)
