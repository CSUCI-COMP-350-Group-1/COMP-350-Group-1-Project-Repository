package com.example.cicompanion.maps

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
    LatLng(34.157, -119.050), // Southwest corner
    LatLng(34.168, -119.035)  // Northeast corner
)
// Middle of CSUCI
private val CSUCI_CENTER = LatLng(34.16174111410685, -119.04342498111538)

// Parking lot descriptions
private const val SH_PARKING_DESC = "Student housing parking (only for SH permit holders)"
private const val GENERAL_PARKING_DESC = "General parking (A, F, E or Visitor permit required)"
private const val RESTRICTED_PARKING_DESC = "Restricted parking lot (only for R or RV permit holders)"

// Google maps customizations to hide preset locations and labels
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

// Location categories for filtering
enum class LocationType { BUILDING, PARKING, FOOD, AREA, HOUSING }

// Data class for each campus location
data class CampusLocation(
    val name: String,
    val position: LatLng,
    val description: String,
    val type: LocationType,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// Master list of all campus locations
val campusLocations = listOf(
    // Buildings
    CampusLocation("Bell Tower", LatLng(34.16138604361421, -119.0432651672823), "Center of Campus", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("Bell Tower East", LatLng(34.16134665298329, -119.04189180309578), "", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("Bell Tower West", LatLng(34.16070116130278, -119.04439859472768), "", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("John Spoor Broome Library", LatLng(34.16269668565898, -119.04094849136715), "Main Library", LocationType.BUILDING, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF388E3C)),
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
    CampusLocation("Richard R. Rush Hall", LatLng(34.162673963555044, -119.04342008432133), "Houses the University President and administration.", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Chaparral Hall", LatLng(34.162096255189496, -119.04560917381353), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Ironwood Hall", LatLng(34.16245544107054, -119.04645265103974), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("El Dorado Hall", LatLng(34.16423447296913, -119.04710369220899), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Aliso Hall", LatLng(34.16133147263596, -119.04535150727516), "8 Science Labs and 16 Faculty Offices", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Yuba Hall", LatLng(34.16407767205871, -119.04109248173509), "Houses the Student Health Center", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Sage Hall", LatLng(34.164167218645936, -119.04222881533492), "The Enrollment Center.", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Malibu Hall", LatLng(34.16126160533967, -119.04086506383092), "", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Topanga Hall", LatLng(34.16019137315942, -119.04169333763335), "Art Program facilities and labs.", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Arroyo Hall", LatLng(34.160354318454424, -119.04489629947327), "Wellness and Athletics Office. Recreation Center.", LocationType.BUILDING, Icons.Default.School, Color(0xFF388E3C)),
    CampusLocation("Trinity Hall", LatLng(34.15934671289535, -119.0423644726643), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Lindero Hall", LatLng(34.15956619235504, -119.04141202350661), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),
    CampusLocation("Ojai Hall", LatLng(34.16173923354911, -119.04257933412188), "", LocationType.BUILDING, Icons.Default.School, Color(0xFFD32F2F)),

    // Food category
    CampusLocation("Islands Cafe", LatLng(34.16046259918211, -119.04156950739008), "Dining commons for students and employees.", LocationType.FOOD, Icons.Default.Restaurant, Color(0xFFFF9800)),
    CampusLocation("Coastal Cup", LatLng(34.16517607999232, -119.04492439787398), "A coffee shop inside Gateway Hall.", LocationType.FOOD, Icons.Default.Restaurant, Color(0xFFFF9800)),
    CampusLocation("Mom Wong Kitchen", LatLng(34.162865389467136, -119.03934866185736), "Camarillo’s Premier Chinese Restaurant", LocationType.FOOD, Icons.Default.Restaurant, Color(0xFFFF9800)),
    CampusLocation("Tortillas Grill", LatLng(34.16304512832548, -119.03936598760045), "Mexican Food", LocationType.FOOD, Icons.Default.Restaurant, Color(0xFFFF9800)),

    // Areas category
    CampusLocation("North Quad", LatLng(34.163869256675795, -119.04439875392643), "", LocationType.AREA, Icons.Default.People, Color(0xFF9C27B0)),
    CampusLocation("North Field", LatLng(34.167561045970785, -119.04526582938406), "", LocationType.AREA, Icons.Default.Park, Color(0xFF9C27B0)),
    CampusLocation("Central Mall", LatLng(34.16182005886744, -119.04344776363348), "", LocationType.AREA, Icons.Default.People, Color(0xFF9C27B0)),
    CampusLocation("Potrero Fields", LatLng(34.159887809784415, -119.04743204832411), "", LocationType.AREA, Icons.Default.Park, Color(0xFF9C27B0)),
    CampusLocation("South Quad", LatLng(34.160229605621325, -119.04270063156984), "", LocationType.AREA, Icons.Default.People, Color(0xFF9C27B0)),

    // Housing category
    CampusLocation("Santa Cruz Village", LatLng(34.15909088586997, -119.04255249590989), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Santa Cruz Village D", LatLng(34.16012344365498, -119.04398722341813), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Santa Cruz Village E", LatLng(34.15991620770629, -119.04397330984331), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Santa Cruz Village F", LatLng(34.1597406324177, -119.04394896107382), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Santa Cruz Village G", LatLng(34.15951036919081, -119.04383417401763), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Santa Cruz Village H", LatLng(34.1592628355215, -119.04385504439146), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Anacapa Village A", LatLng(34.15935494115949, -119.04437332533838), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Anacapa Village B", LatLng(34.1597665369933, -119.04473507848517), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Anacapa Village C", LatLng(34.15958520481026, -119.04532292734869), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),
    CampusLocation("Anacapa Village Commons Building", LatLng(34.159208147754384, -119.04492291184982), "", LocationType.HOUSING, Icons.Default.Home, Color(0xFF800000)),

    // Parking Lots
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
        // Default camera position set to CSUCI center (default location)
        position = CameraPosition.fromLatLngZoom(CSUCI_CENTER, 17f)
    }

    // Filter locations based on search and selected filter type
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
                fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState) { userLocation = it }
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
            // Get location for the marker, but don't force a camera move on start
            fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState, shouldAnimate = false) { userLocation = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column {
                    /*
                    TopAppBar(
                        title = { Text("CSUCI Campus Map", fontSize = 20.sp) }
                    )
                     */
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
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
                    // Chips on the filter
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
                                selected = filterType == LocationType.FOOD,
                                onClick = { filterType = LocationType.FOOD },
                                label = { Text("Food") },
                                leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == LocationType.AREA,
                                onClick = { filterType = LocationType.AREA },
                                label = { Text("Areas") },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == LocationType.HOUSING,
                                onClick = { filterType = LocationType.HOUSING },
                                label = { Text("Housing") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterType == LocationType.PARKING,
                                onClick = { filterType = LocationType.PARKING },
                                label = { Text("Parking") },
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
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            // content of the map
            MapContent(
                cameraPositionState = cameraPositionState,
                hasLocationPermission = hasLocationPermission,
                userLocation = userLocation,
                selectedDestination = selectedDestination,
                onLocationClick = { selectedDestination = it },
                onMapClick = { selectedDestination = null },
                displayLocations = filteredLocations
            )

            // Overlays (location button, indicators)
            MapOverlays(
                hasLocationPermission = hasLocationPermission,
                isLoadingLocation = isLoadingLocation,
                onLocationRequest = {
                    scope.launch {
                        isLoadingLocation = true
                        fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState, shouldAnimate = true) {
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
            myLocationButtonEnabled = false,
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = false, // Disable default blue dot to use our custom red one
            latLngBoundsForCameraTarget = CSUCI_BOUNDS,
            minZoomPreference = 15f, // Limits how much the user can zoom out
            mapStyleOptions = MapStyleOptions(MAP_STYLE_JSON) // Hides preset locations
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
        // User Location Marker with solid red dot inside grey pin
        if (userLocation != null) {
            UserLocationMarker(userLocation)
        }

        // Show filtered markers
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

        // Draw walking path
        if (userLocation != null && selectedDestination != null) {
            Polyline(
                points = listOf(userLocation, selectedDestination),
                color = Color(0xFF1A73E8),
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
            .size(27.dp)
            .background(color, CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun UserLocationMarker(position: LatLng) {
    MarkerComposable(
        state = rememberMarkerState(position = position),
        title = "You are here!"
    ) {
        Box(contentAlignment = Alignment.Center) {
            // pointer
            Icon(
                imageVector = Icons.Default.PersonPinCircle,
                contentDescription = "User Location",
                tint = Color.Gray,
                modifier = Modifier.size(36.dp)
            )
            // Actual solid circle
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(y = (-2).dp)
                    .background(Color(0xFFE11D48), CircleShape)
            )
        }
    }

    // Accuracy circle
    Circle(
        center = position,
        radius = 20.0,
        fillColor = Color(0xFF1A73E8).copy(alpha = 0.2f),
        strokeColor = Color(0xFF1A73E8).copy(alpha = 0.5f),
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
fun MyLocationFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
            contentDescription = "My Location"
        )
    }
}

@Composable
fun PermissionCard(modifier: Modifier) {
    @Suppress("DEPRECATION")
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

// fetch user location logic
suspend fun fetchLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    cameraState: CameraPositionState,
    shouldAnimate: Boolean = true,
    onResult: (LatLng) -> Unit
) {
    try {
        var location = fusedLocationProviderClient.lastLocation.await()
        if (location == null || location.accuracy > 100) {
            location = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            onResult(latLng)
            // Only move the camera if requested
            if (shouldAnimate) {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            }
        }
    } catch (e: SecurityException) {
        // Handle no permissions
    } catch (e: Exception) {
        // Handle errors fetching location
    }
}