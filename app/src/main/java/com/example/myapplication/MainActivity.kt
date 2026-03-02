package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showTraffic by remember { mutableStateOf(false) }

    // Fallback location (Moorpark, CA)
    val moorparkLocation = LatLng(34.285, -118.882)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(moorparkLocation, 14f)
    }

    // Fetch real user location logic
    val fetchUserLocation: suspend () -> Unit = {
        isLoadingLocation = true
        try {
            var location = fusedLocationClient.lastLocation.await()
            if (location == null || location.accuracy > 100) {
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            }
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(userLocation!!, 16f)
                )
            }
        } catch (e: SecurityException) {
            // Permission lost
        } catch (e: Exception) {
            // Error fetching location
        } finally {
            isLoadingLocation = false
        }
    }

    // launch permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            scope.launch { fetchUserLocation() }
        }
    }

    // check permissions
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            fetchUserLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val uiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = true
        )
    }

    val mapProperties = remember(showTraffic, hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            isTrafficEnabled = showTraffic,
            mapType = MapType.NORMAL
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            // Moorpark Marker
            Marker(
                //moorpark marker
                state = rememberMarkerState(position = moorparkLocation),
                title = "Moorpark",
                snippet = "Ventura County, California"
            )

            userLocation?.let { pos ->
                Marker(
                    // for location services
                    state = rememberMarkerState(position = pos),
                    title = "You are here",
                    snippet = "Your approximate location"
                )

                Circle(
                    // color of the circle
                    center = pos,
                    radius = 200.0,
                    fillColor = Color(0, 150, 255, 80),
                    strokeColor = Color.Blue,
                    strokeWidth = 2f
                )
            }

            Polyline(
                points = listOf(
                    // list of locations for my sake
                    LatLng(34.285, -118.882),
                    LatLng(34.290, -118.870),
                    LatLng(34.275, -118.865),
                    LatLng(34.285, -118.882)
                ),
                color = Color.Red,
                width = 8f,
                clickable = true
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (userLocation != null) {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(userLocation!!, 17f)
                                )
                            } else {
                                fetchUserLocation()
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = "My Location"
                    )
                }
            }

            FloatingActionButton(
                onClick = { showTraffic = !showTraffic }
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_mapmode),
                    contentDescription = if (showTraffic) "Hide Traffic" else "Show Traffic"
                )
            }
        }

        if (isLoadingLocation) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!hasLocationPermission) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Location permission is required to show your position and nearby features.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
