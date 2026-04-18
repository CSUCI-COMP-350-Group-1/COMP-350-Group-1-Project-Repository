package com.example.cicompanion.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.BrandOrange
import com.example.cicompanion.ui.theme.BrandRedDark
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

// boundaries for CSUCI
private val CSUCI_BOUNDS = LatLngBounds(
    LatLng(34.157, -119.050),
    LatLng(34.168, -119.035)
)
private val CSUCI_CENTER = LatLng(34.16174111410685, -119.04342498111538)

private const val SH_PARKING_DESC = "Student housing parking (only for SH permit holders)"
private const val GENERAL_PARKING_DESC = "General parking (A, F, E or Visitor permit required)"
private const val RESTRICTED_PARKING_DESC = "Restricted parking lot (only for R or RV permit holders)"

private val MAP_STYLE_JSON = """
    [
      { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.business", "elementType": "labels", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.school", "elementType": "labels", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "poi.park", "elementType": "labels", "stylers": [ { "visibility": "off" } ] }
    ]
""".trimIndent()

enum class LocationType { BUILDING, PARKING, FOOD, AREA, HOUSING }

data class CampusLocation(
    val name: String,
    val position: LatLng,
    val description: String,
    val type: LocationType,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

val campusLocations = listOf(
    CampusLocation("Bell Tower", LatLng(34.16138604361421, -119.0432651672823), "Center of Campus", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("Bell Tower East", LatLng(34.16134665298329, -119.04189180309578), "", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("Bell Tower West", LatLng(34.16070116130278, -119.04439859472768), "", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
    CampusLocation("John Spoor Broome Library", LatLng(34.16269668565898, -119.04094849136715), "Main Library", LocationType.BUILDING, Icons.AutoMirrored.Filled.MenuBook, Color(0xFF388E3C)),
    CampusLocation("Student Union", LatLng(34.1610, -119.0436), "Dining and Lounge", LocationType.BUILDING, Icons.Default.Groups, Color(0xFF388E3C)),
    CampusLocation("Marin Hall", LatLng(34.164528096869034, -119.04507117740494), "Faculty Offices for Mathematics and Data Science", LocationType.BUILDING, Icons.Default.Business, Color(0xFFD32F2F)),
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
fun MapScreen(navController: NavHostController, calendarViewModel: CalendarViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    // search and filtering state
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<LocationType?>(null) }
    var showSearchResults by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CSUCI_CENTER, 17f)
    }

    val filteredLocations = remember(searchQuery, filterType) {
        val trimmedQuery = searchQuery.trim()
        campusLocations.filter {
            (trimmedQuery.isEmpty() || it.name.contains(trimmedQuery, ignoreCase = true)) &&
                    (filterType == null || it.type == filterType)
        }
    }

    // Centering camera on the location (when selected)
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(location.position),
                durationMs = 800
            )
        }
    }

    // permission launcher logic
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

    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState, shouldAnimate = false) { userLocation = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun resetMapState() {
        selectedLocation = null
        showDetailsSheet = false
        searchQuery = ""
        showSearchResults = false
        scope.launch {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(CSUCI_CENTER, 17f),
                durationMs = 500
            )
        }
    }

    Scaffold(
        topBar = {
            MapTopControls(
                searchQuery = searchQuery,
                onSearchChange = {
                    searchQuery = it
                    showSearchResults = it.isNotEmpty()
                },
                showSearchResults = showSearchResults,
                onClearSearch = {
                    searchQuery = ""
                    showSearchResults = false
                },
                filteredLocations = filteredLocations,
                onResultClick = { location ->
                    searchQuery = location.name
                    showSearchResults = false
                    selectedLocation = location
                },
                filterType = filterType,
                onFilterClick = { filterType = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            MapContent(
                cameraPositionState = cameraPositionState,
                hasLocationPermission = hasLocationPermission,
                userLocation = userLocation,
                selectedLocation = selectedLocation,
                onLocationClick = { location -> selectedLocation = location },
                onMapClick = {
                    selectedLocation = null
                    showSearchResults = false
                },
                displayLocations = filteredLocations,
                events = calendarViewModel.events
            )

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

            // Info card for selected location
            AnimatedVisibility(
                visible = selectedLocation != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedLocation?.let { location ->
                    LocationInfoCard(
                        location = location,
                        onClose = { selectedLocation = null },
                        onDetailsClick = { showDetailsSheet = true }
                    )
                }
            }

            if (showDetailsSheet && selectedLocation != null) {
                val location = selectedLocation!!
                val locationEvents = calendarViewModel.events.filter { 
                    (it.calendarId == "custom" || it.isPinned) &&
                    it.location?.contains(location.name, ignoreCase = true) == true 
                }
                
                ModalBottomSheet(
                    onDismissRequest = { showDetailsSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White
                ) {
                    LocationDetailsContent(
                        location = location,
                        events = locationEvents,
                        onGoToEvent = { event ->
                            calendarViewModel.onDateSelected(event.start.toLocalDate())
                            resetMapState()
                            navController.navigate(Routes.CALENDAR)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationDetailsContent(
    location: CampusLocation, 
    events: List<CalendarEvent>,
    onGoToEvent: (CalendarEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(location.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = location.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (location.description.isNotEmpty()) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = location.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (events.isNotEmpty()) {
            Text(
                text = "Events at this Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            events.forEach { event ->
                MapEventItem(
                    event = event,
                    onMoreClick = { onGoToEvent(event) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(
                text = "No upcoming custom or pinned events here.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MapEventItem(event: CalendarEvent, onMoreClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (event.isPinned) Color(0xFF9C27B0).copy(alpha = 0.1f) 
                        else BrandOrange.copy(alpha = 0.1f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.isPinned) Icons.Default.PushPin else Icons.Default.Event,
                    contentDescription = null,
                    tint = if (event.isPinned) Color(0xFF9C27B0) else BrandOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.timeLabel(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // calendar button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandRedDark.copy(alpha = 0.1f))
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Go to Calendar",
                    tint = BrandRedDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MapTopControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showSearchResults: Boolean,
    onClearSearch: () -> Unit,
    filteredLocations: List<CampusLocation>,
    onResultClick: (CampusLocation) -> Unit,
    filterType: LocationType?,
    onFilterClick: (LocationType?) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search campus...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedBorderColor = BrandRedDark,
                    focusedContainerColor = Color(0xFFF8F8F8),
                    unfocusedContainerColor = Color(0xFFF8F8F8)
                )
            )

            if (showSearchResults && filteredLocations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    LazyColumn {
                        items(filteredLocations) { location ->
                            ListItem(
                                headlineContent = { Text(location.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text(location.type.name, fontSize = 12.sp, color = Color.Gray) },
                                leadingContent = { Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(24.dp)) },
                                modifier = Modifier.clickable { onResultClick(location) }
                            )
                        }
                    }
                }
            }

            CategoryFilterRow(selectedType = filterType, onFilterClick = onFilterClick)
        }
    }
}

@Composable
fun CategoryFilterRow(selectedType: LocationType?, onFilterClick: (LocationType?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            CustomFilterChip(
                selected = selectedType == null,
                onClick = { onFilterClick(null) },
                label = "All",
                icon = null
            )
        }
        LocationType.entries.forEach { type ->
            item {
                val icon = when (type) {
                    LocationType.BUILDING -> Icons.Default.Business
                    LocationType.FOOD -> Icons.Default.Restaurant
                    LocationType.AREA -> Icons.Default.People
                    LocationType.HOUSING -> Icons.Default.Home
                    LocationType.PARKING -> Icons.Default.LocalParking
                }
                CustomFilterChip(
                    selected = selectedType == type,
                    onClick = { onFilterClick(type) },
                    label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = icon
                )
            }
        }
    }
}

@Composable
fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    Surface(
        onClick = onClick,
        color = if (selected) BrandRedDark else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) Color.White else Color.DarkGray
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Color.DarkGray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun MapContent(
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    userLocation: LatLng?,
    selectedLocation: CampusLocation?,
    onLocationClick: (CampusLocation) -> Unit,
    onMapClick: (LatLng) -> Unit,
    displayLocations: List<CampusLocation>,
    events: List<CalendarEvent>
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

        displayLocations.forEach { location ->
            val isSelected = selectedLocation?.name == location.name
            // Only include custom and pinned events on the map to reduce clutter
            val locationEvents = events.filter { 
                (it.calendarId == "custom" || it.isPinned) &&
                it.location?.contains(location.name, ignoreCase = true) == true 
            }
            val eventCount = locationEvents.size
            val hasPinnedEvent = locationEvents.any { it.isPinned }

            key(location.name, isSelected, eventCount, hasPinnedEvent) {
                MarkerComposable(
                    state = rememberMarkerState(position = location.position),
                    zIndex = if (isSelected) 100f else 1f,
                    anchor = Offset(0.5f, if (isSelected) 1.0f else 0.5f),
                    onClick = {
                        onLocationClick(location)
                        true 
                    }
                ) {
                    if (isSelected) {
                        SelectedPointerIcon(location, eventCount, hasPinnedEvent)
                    } else {
                        LandmarkIcon(icon = location.icon, color = location.color, eventCount = eventCount, hasPinnedEvent = hasPinnedEvent)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedPointerIcon(location: CampusLocation, eventCount: Int = 0, hasPinnedEvent: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f, 
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(75.dp) 
            .graphicsLayer(translationY = bounce)
    ) {
        // Pointer Pin color based on location color
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = location.color,
            modifier = Modifier.fillMaxSize()
        )
        // Icon on top in white circle with dynamic border if events exist
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.padding(top = 10.dp).size(30.dp),
            // Orange/Purple border depending on event type
            border = BorderStroke(2.5.dp, when {
                hasPinnedEvent -> Color(0xFF9C27B0) // Purple for pinned
                eventCount > 0 -> BrandOrange // Orange for regular events
                else -> location.color
            }),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = location.icon,
                    contentDescription = null,
                    tint = location.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        //  dot for events
        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun LandmarkIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, eventCount: Int = 0, hasPinnedEvent: Boolean = false) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color, CircleShape)
                // Border highlight for events
                .border(2.5.dp, when {
                    hasPinnedEvent -> Color(0xFF9C27B0) // Purple for pinned
                    eventCount > 0 -> BrandOrange // Orange for regular events
                    else -> Color.White
                }, CircleShape)
                .shadow(2.dp, CircleShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        //  dot for events
        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun LocationInfoCard(location: CampusLocation, onClose: () -> Unit, onDetailsClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.70f)
            .padding(bottom = 36.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(location.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (location.description.isNotEmpty()) {
                        Text(location.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Surface(
                    onClick = onDetailsClick,
                    color = AppBackground,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp).fillMaxWidth(),
                    border = BorderStroke(1.dp, BrandRedDark.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Details", color = BrandRedDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UserLocationMarker(position: LatLng) {
    MarkerComposable(state = rememberMarkerState(position = position)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PersonPinCircle, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(40.dp))
            Box(modifier = Modifier.size(12.dp).offset(y = (-2).dp).background(Color.White, CircleShape))
            Box(modifier = Modifier.size(8.dp).offset(y = (-2).dp).background(Color(0xFF1A73E8), CircleShape))
        }
    }
    Circle(center = position, radius = 25.0, fillColor = Color(0xFF1A73E8).copy(alpha = 0.15f), strokeColor = Color(0xFF1A73E8).copy(alpha = 0.4f), strokeWidth = 2f)
}

@Composable
fun MapOverlays(hasLocationPermission: Boolean, isLoadingLocation: Boolean, onLocationRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasLocationPermission) {
            FloatingActionButton(
                onClick = onLocationRequest,
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).padding(bottom = 90.dp),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = BrandRedDark,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(painter = painterResource(id = android.R.drawable.ic_menu_mylocation), contentDescription = "My Location", modifier = Modifier.size(24.dp))
            }
        }
        if (isLoadingLocation) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandRedDark, strokeWidth = 4.dp)
        if (!hasLocationPermission) PermissionCard(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun PermissionCard(modifier: Modifier) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Location permission is required to show your position on the map.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
    }
}

suspend fun fetchLocation(fusedLocationProviderClient: FusedLocationProviderClient, cameraState: CameraPositionState, shouldAnimate: Boolean = true, onResult: (LatLng) -> Unit) {
    try {
        var location = fusedLocationProviderClient.lastLocation.await()
        if (location == null || location.accuracy > 100) {
            location = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        }
        location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            onResult(latLng)
            if (shouldAnimate) cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
    } catch (e: Exception) {}
}
