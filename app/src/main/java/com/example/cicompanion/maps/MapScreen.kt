package com.example.sprint1homeui.maps

import android.Manifest
import android.R
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    // The map's logic
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // this is how we determine hard coded locations, to be used in a future user story
    // my example was just moorpark.
    //val hardcodedlocation = LatLng(34.285, -118.882)

    val cameraPositionState = rememberCameraPositionState { // animation for the camera, this variable is NEEDED.
        // will be used for future user stories
        // position = CameraPosition.fromLatLngZoom(hardcodedlocation, 14f)
    }

    // getting the user location
    val fetchUserLocation: suspend () -> Unit = {
        isLoadingLocation = true
        try {
            var location = fusedLocationClient.lastLocation.await()
            if (location == null || location.accuracy > 100) { // accuracy settings i got from a tutorial
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY, // prio high accuracy for location
                    null
                ).await()
            }
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                cameraPositionState.animate( // this is for the button/when the user moves!
                    CameraUpdateFactory.newLatLngZoom(userLocation!!, 16f)
                )
            }
        } catch (e: SecurityException) {
            // when there are no permissions
        } catch (e: Exception) {
            // an error occured!
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
            scope.launch { fetchUserLocation() } // when location is accepted
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
            myLocationButtonEnabled = false, // We have our own location button
            zoomControlsEnabled = true, // very useful
            compassEnabled = true, // useful
            mapToolbarEnabled = false //  gets rid of that annoying button at the bottom of the maps
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            isTrafficEnabled = false, // was for testing, not needed
            mapType = MapType.NORMAL
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map", fontSize = 20.sp) }
            )
        },
        content = { paddingValues ->
            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // apply scaffold padding
                contentAlignment = Alignment.Center
            ) {
                // the map view
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings
                ) {
                    // this was the marker for 'moorpark', but it is not needed
                    // It WILL be needed for key locations on campus map.

                    userLocation?.let { pos ->
                        Marker(
                            state = rememberMarkerState(position = pos), // the markerstate needs to be there for the state to work.
                            title = "You are here!",
                            snippet = "Your approximate location"
                        )

                        Circle(
                            center = pos, // radius around the location, can be edited
                            radius = 25.0,
                            fillColor = Color(0, 150, 255, 80),
                            strokeColor = Color.Blue,
                            strokeWidth = 2f
                        )
                    }
                }

                // overlaying my location button
                // location button using a floating action button
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (userLocation != null) {
                                    cameraPositionState.animate( // when clicked, animate to the user location
                                        CameraUpdateFactory.newLatLngZoom(userLocation!!, 17f)
                                    )
                                } else {
                                    fetchUserLocation()
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart) //bottom left start
                            .padding(16.dp)
                            .systemBarsPadding()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_mylocation), // icon for location, embedded from google maps themselves
                            contentDescription = "My Location" // you cant see it, but we have to call it this
                        )
                    }
                }

                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (!hasLocationPermission) { // location permissions
                    Card(
                        modifier = Modifier // prompts the location permission to show
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
    )
}
