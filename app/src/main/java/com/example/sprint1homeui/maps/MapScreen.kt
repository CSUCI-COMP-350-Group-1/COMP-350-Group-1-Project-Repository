package com.example.sprint1homeui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// CSUCI Boundaries - prevents scrolling far from campus area in Camarillo
private val CSUCI_BOUNDS = LatLngBounds(
    LatLng(34.155, -119.055), // Southwest corner
    LatLng(34.170, -119.030)  // Northeast corner
)
private val CSUCI_CENTER = LatLng(34.16222772570257, -119.0435017637274)

// Updated Campus Landmark Coordinates near the new center
private val BELL_TOWER = LatLng(34.1614, -119.0428)
private val BROOME_LIBRARY = LatLng(34.1624, -119.0434)
private val STUDENT_UNION = LatLng(34.1610, -119.0436)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedDestination by remember { mutableStateOf<LatLng?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CSUCI_CENTER, 17f)
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
            TopAppBar(
                title = { Text("CSUCI Campus Map", fontSize = 20.sp) }
            )
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
                onLandmarkClick = { selectedDestination = it },
                onMapClick = { selectedDestination = null }
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
fun MapContent(
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    userLocation: LatLng?,
    selectedDestination: LatLng?,
    onLandmarkClick: (LatLng) -> Unit,
    onMapClick: (LatLng) -> Unit
) {
    val uiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = false, // Custom FAB used instead
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            latLngBoundsForCameraTarget = CSUCI_BOUNDS // Strict restriction to campus
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapClick = onMapClick
    ) {
        // User Location Marker
        if (userLocation != null) {
            UserLocationMarker(userLocation)
        }
        
        // Landmark Markers
        CampusLandmarks(onLandmarkClick = onLandmarkClick)

        // Draw walking path (straight line for now as a mock)
        if (userLocation != null && selectedDestination != null) {
            Polyline(
                points = listOf(userLocation, selectedDestination),
                color = Color(0xFF1A73E8), // Google Maps Blue
                width = 8f,
                geodesic = true
            )
        }
    }
}

@Composable
fun CampusLandmarks(onLandmarkClick: (LatLng) -> Unit) {
    Marker(
        state = rememberMarkerState(position = BELL_TOWER),
        title = "Bell Tower",
        snippet = "Center of Campus",
        onClick = {
            onLandmarkClick(it.position)
            false // return false to show info window
        }
    )
    Marker(
        state = rememberMarkerState(position = BROOME_LIBRARY),
        title = "John Spoor Broome Library",
        snippet = "Main Library",
        onClick = {
            onLandmarkClick(it.position)
            false
        }
    )
    Marker(
        state = rememberMarkerState(position = STUDENT_UNION),
        title = "Student Union",
        snippet = "Dining and Lounge",
        onClick = {
            onLandmarkClick(it.position)
            false
        }
    )
}

@Composable
fun UserLocationMarker(position: LatLng) {
    Marker(
        state = rememberMarkerState(position = position),
        title = "You are here!",
        snippet = "Your approximate location"
    )
    Circle(
        center = position,
        radius = 25.0,
        fillColor = Color(0, 150, 255, 80),
        strokeColor = Color.Blue,
        strokeWidth = 2f
    )
}

@Composable
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
            // The 'my location' button
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
fun MyLocationFab(onClick: () -> Unit) { // ui that handles the location tab
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
fun PermissionCard(modifier: Modifier) { // permission check for location services
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = "Location permission is required to show your position on the map.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

// the logic to fetch user location, camera animations
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

// move to a certain landmark when searched or clicked
suspend fun moveToLandmark(cameraState: CameraPositionState, landmark: LatLng) {
    cameraState.animate(CameraUpdateFactory.newLatLngZoom(landmark, 18f))
}

/**
 * Quick jump to Bell Tower
 */
suspend fun jumpToBellTower(cameraState: CameraPositionState) = moveToLandmark(cameraState, BELL_TOWER)

/**
 * Quick jump to Broome Library
 */
suspend fun jumpToLibrary(cameraState: CameraPositionState) = moveToLandmark(cameraState, BROOME_LIBRARY)
