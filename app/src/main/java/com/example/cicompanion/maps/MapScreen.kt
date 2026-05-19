package com.example.cicompanion.maps

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.ui.theme.CoralRed
import com.example.cicompanion.ui.Routes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.annotation.SuppressLint
import kotlin.math.abs

val campusLocations = listOf(
    CampusLocation(id = "loc_bell_tower", name = "Bell Tower", position = LatLng(34.16138604361421, -119.0432651672823), description = "Center of Campus", type = LocationType.BUILDING, icon = Icons.Default.Business, color = Color(0xFFD32F2F)),
    CampusLocation(id = "loc_bell_tower_east", name = "Bell Tower East", position = LatLng(34.16134665298329, -119.04189180309578), description = "", type = LocationType.BUILDING, icon = Icons.Default.Business, color = Color(0xFFD32F2F), searchKeywords = listOf("BTE")),
    CampusLocation(id = "loc_bell_tower_west", name = "Bell Tower West", position = LatLng(34.16070116130278, -119.04439859472768), description = "", type = LocationType.BUILDING, icon = Icons.Default.Business, color = Color(0xFFD32F2F), searchKeywords = listOf("BTW")),
    CampusLocation(id = "loc_library", name = "John Spoor Broome Library", position = LatLng(34.16269668565898, -119.04094849136715), description = "Main Library", type = LocationType.BUILDING, icon = Icons.Default.MenuBook, color = Color(0xFF388E3C), searchKeywords = listOf("Broome Library", "Library", "JSB")),
    CampusLocation(id = "loc_union", name = "Student Union", position = LatLng(34.1610, -119.0436), description = "Dining and Lounge", type = LocationType.BUILDING, icon = Icons.Default.Groups, color = Color(0xFF388E3C), searchKeywords = listOf("SUB", "Union")),
    CampusLocation(id = "loc_marin", name = "Marin Hall", position = LatLng(34.164528096869034, -119.04507117740494), description = "Faculty Offices for Mathematics and Data Science", type = LocationType.BUILDING, icon = Icons.Default.Business, color = Color(0xFFD32F2F), searchKeywords = listOf("Marin")),
    CampusLocation(id = "loc_shasta", name = "Shasta Hall", position = LatLng(34.164576865185516, -119.04472618829523), description = "Faculty Offices for Computer Science and Engineering", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Shasta")),
    CampusLocation(id = "loc_gateway", name = "Gateway Hall", position = LatLng(34.16483652693463, -119.04547452459948), description = "Large building with classrooms and study rooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Gateway")),
    CampusLocation(id = "loc_napa", name = "Napa Hall", position = LatLng(34.16377605025435, -119.04540741204008), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Napa")),
    CampusLocation(id = "loc_solano", name = "Solano Hall", position = LatLng(34.16340620613301, -119.0450085042822), description = "Offices for faculty, Division of Technology and Innovation", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Solano")),
    CampusLocation(id = "loc_mvs", name = "Martin V. Smith Hall", position = LatLng(34.162909062826785, -119.04476226672068), description = "Houses Nursing Program", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("MVS", "Smith Hall")),
    CampusLocation(id = "loc_sierra", name = "Sierra Hall", position = LatLng(34.16229510038971, -119.04461124101422), description = "Multimode lecture halls, classrooms and faculty offices.", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Sierra")),
    CampusLocation(id = "loc_del_norte", name = "Del Norte Hall", position = LatLng(34.163180726190696, -119.04410235004629), description = "Lecture classrooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Del Norte")),
    CampusLocation(id = "loc_madera", name = "Madera Hall", position = LatLng(34.1629335125641, -119.04394147483745), description = "Lecture classrooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Madera")),
    CampusLocation(id = "loc_placer", name = "Placer Hall", position = LatLng(34.163344972360754, -119.04300312204607), description = "Offices of University Police and Parking Services", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Placer")),
    CampusLocation(id = "loc_rush", name = "Richard R. Rush Hall", position = LatLng(34.162673963555044, -119.04342008432133), description = "Houses the University President and administration.", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Rush", "Rush Hall")),
    CampusLocation(id = "loc_chaparral", name = "Chaparral Hall", position = LatLng(34.162096255189496, -119.04560917381353), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Chaparral")),
    CampusLocation(id = "loc_ironwood", name = "Ironwood Hall", position = LatLng(34.16245544107054, -119.04645265103974), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Ironwood")),
    CampusLocation(id = "loc_el_dorado", name = "El Dorado Hall", position = LatLng(34.16423447296913, -119.04710369220899), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("El Dorado")),
    CampusLocation(id = "loc_aliso", name = "Aliso Hall", position = LatLng(34.16133147263596, -119.04535150727516), description = "8 Science Labs and 16 Faculty Offices", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Aliso")),
    CampusLocation(id = "loc_yuba", name = "Yuba Hall", position = LatLng(34.16407767205871, -119.04109248173509), description = "Houses the Student Health Center", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Yuba")),
    CampusLocation(id = "loc_sage", name = "Sage Hall", position = LatLng(34.164167218645936, -119.04222881533492), description = "The Enrollment Center.", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFF388E3C), searchKeywords = listOf("Sage")),
    CampusLocation(id = "loc_malibu", name = "Malibu Hall", position = LatLng(34.16126160533967, -119.04086506383092), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFF388E3C), searchKeywords = listOf("Malibu")),
    CampusLocation(id = "loc_topanga", name = "Topanga Hall", position = LatLng(34.16019137315942, -119.04169333763335), description = "Art Program facilities and labs.", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Topanga")),
    CampusLocation(id = "loc_arroyo", name = "Arroyo Hall", position = LatLng(34.160354318454424, -119.04489629947327), description = "Wellness and Athletics Office. Recreation Center.", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFF388E3C), searchKeywords = listOf("Arroyo")),
    CampusLocation(id = "loc_trinity", name = "Trinity Hall", position = LatLng(34.15934671289535, -119.0423644726643), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Trinity")),
    CampusLocation(id = "loc_lindero", name = "Lindero Hall", position = LatLng(34.15956619235504, -119.04141202350661), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Lindero")),
    CampusLocation(id = "loc_ojai", name = "Ojai Hall", position = LatLng(34.16173923354911, -119.04257933412188), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = Color(0xFFD32F2F), searchKeywords = listOf("Ojai")),

    // Food category
    CampusLocation(id = "food_islands", name = "Islands Cafe", position = LatLng(34.16046259918211, -119.04156950739008), description = "Dining commons for students and employees.", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = Color(0xFFFF9800), searchKeywords = listOf("Islands")),
    CampusLocation(id = "food_coastal", name = "Coastal Cup", position = LatLng(34.16517607999232, -119.04492439787398), description = "A coffee shop inside Gateway Hall.", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = Color(0xFFFF9800), searchKeywords = listOf("Coastal")),
    CampusLocation(id = "food_mom_wong", name = "Mom Wong Kitchen", position = LatLng(34.162865389467136, -119.03934866185736), description = "Camarillo\u2019s Premier Chinese Restaurant", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = Color(0xFFFF9800), searchKeywords = listOf("Mom Wong")),
    CampusLocation(id = "food_tortillas", name = "Tortillas Grill", position = LatLng(34.16304512832548, -119.03936598760045), description = "Mexican Food", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = Color(0xFFFF9800), searchKeywords = listOf("Tortillas")),

    // Areas category
    CampusLocation(id = "area_north_quad", name = "North Quad", position = LatLng(34.163869256675795, -119.04439875392643), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = Color(0xFF9C27B0)),
    CampusLocation(id = "area_north_field", name = "North Field", position = LatLng(34.167561045970785, -119.04526582938406), description = "", type = LocationType.AREA, icon = Icons.Default.Park, color = Color(0xFF9C27B0)),
    CampusLocation(id = "area_central_mall", name = "Central Mall", position = LatLng(34.16182005886744, -119.04344776363348), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = Color(0xFF9C27B0)),
    CampusLocation(id = "area_potrero", name = "Potrero Fields", position = LatLng(34.159887809784415, -119.04743204832411), description = "", type = LocationType.AREA, icon = Icons.Default.Park, color = Color(0xFF9C27B0)),
    CampusLocation(id = "area_south_quad", name = "South Quad", position = LatLng(34.160229605621325, -119.04270063156984), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = Color(0xFF9C27B0)),

    // Housing category
    CampusLocation(id = "house_sc_v", name = "Santa Cruz Village", position = LatLng(34.15909088586997, -119.04255249590989), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000), searchKeywords = listOf("Santa Cruz")),
    CampusLocation(id = "house_sc_d", name = "Santa Cruz Village D", position = LatLng(34.16012344365498, -119.04398722341813), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_sc_e", name = "Santa Cruz Village E", position = LatLng(34.15991620770629, -119.04397330984331), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_sc_f", name = "Santa Cruz Village F", position = LatLng(34.1597406324177, -119.04394896107382), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_sc_g", name = "Santa Cruz Village G", position = LatLng(34.15951036919081, -119.04383417401763), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_sc_h", name = "Santa Cruz Village H", position = LatLng(34.1592628355215, -119.04385504439146), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_anacapa_a", name = "Anacapa Village A", position = LatLng(34.15935494115949, -119.04437332533838), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000), searchKeywords = listOf("Anacapa")),
    CampusLocation(id = "house_anacapa_b", name = "Anacapa Village B", position = LatLng(34.1597665369933, -119.04473507848517), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_anacapa_c", name = "Anacapa Village C", position = LatLng(34.15958520481026, -119.04532292734869), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),
    CampusLocation(id = "house_anacapa_com", name = "Anacapa Village Commons Building", position = LatLng(34.159208147754384, -119.04492291184982), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = Color(0xFF800000)),

    // Parking Lots
    CampusLocation(id = "park_a3", name = "Parking Lot A3", position = LatLng(34.166606172710715, -119.04703678095836), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A3")),
    CampusLocation(id = "park_a4", name = "Parking Lot A4", position = LatLng(34.164244290933254, -119.04646170905023), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A4")),
    CampusLocation(id = "park_a11", name = "Parking Lot A11", position = LatLng(34.164126287402695, -119.04786350249398), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A11")),
    CampusLocation(id = "park_a2", name = "Parking Lot A2", position = LatLng(34.164108680933254, -119.04164009131796), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A2")),
    CampusLocation(id = "park_a6", name = "Parking Lot A6", position = LatLng(34.16325952040607, -119.04202460036679), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A6")),
    CampusLocation(id = "park_a1", name = "Parking Lot A1", position = LatLng(34.163586436668446, -119.04267748158364), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A1")),
    CampusLocation(id = "park_a8", name = "Parking Lot A8", position = LatLng(34.16309446392495, -119.04030380989158), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A8")),
    CampusLocation(id = "park_ae", name = "Parking Lot A/E", position = LatLng(34.16186741815617, -119.04156570455609), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A/E")),
    CampusLocation(id = "park_a7", name = "Parking Lot A7", position = LatLng(34.160649977624885, -119.04118719083276), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A7")),
    CampusLocation(id = "park_a10", name = "Parking Lot A10", position = LatLng(34.15940842843663, -119.04062564402817), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A10")),
    CampusLocation(id = "park_sh1", name = "Parking Lot SH1", position = LatLng(34.15912561853075, -119.04537811915849), description = SH_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF00BCD4), searchKeywords = listOf("SH1")),
    CampusLocation(id = "park_r1", name = "Parking Lot R1", position = LatLng(34.1630169076814, -119.04316226033373), description = RESTRICTED_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF673AB7), searchKeywords = listOf("R1")),
    CampusLocation(id = "park_a5", name = "Parking Lot A5", position = LatLng(34.16050864644475, -119.04463491964691), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = Color(0xFF1976D2), searchKeywords = listOf("A5"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController, 
    calendarViewModel: CalendarViewModel,
    mapViewModel: MapViewModel = viewModel(),
    initialLat: Double? = null,
    initialLng: Double? = null,
    tempName: String? = null,
    tempDesc: String? = null,
    tempColor: Int? = null,
    tempEventId: String? = null,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showPinCreationDialog by remember { mutableStateOf(false) }
    var showEventPickerForSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    // search and filtering state
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<LocationType?>(null) }
    var showSearchResults by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else CSUCI_CENTER,
            17f
        )
    }

    // Handle shared temporary pin from chat
    LaunchedEffect(initialLat, initialLng, tempName) {
        if (initialLat != null && initialLng != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 18f),
                durationMs = 1000
            )
            
            if (tempName != null) {
                selectedLocation = CampusLocation(
                    id = "shared_temp",
                    name = tempName,
                    position = LatLng(initialLat, initialLng),
                    description = tempDesc ?: "Shared with you",
                    type = LocationType.CUSTOM,
                    icon = Icons.Default.PushPin,
                    color = if (tempColor != null) Color(tempColor) else CoralRed,
                    isCustom = true,
                    associatedEventId = tempEventId
                )
                showDetailsSheet = true
            }
        }
    }

    val combinedLocations = remember(mapViewModel.customPins) {
        campusLocations + mapViewModel.customPins.map { it.toCampusLocation() }
    }

    val filteredLocations = remember(searchQuery, filterType, combinedLocations, mapViewModel.isPinMode) {
        if (mapViewModel.isPinMode) emptyList()
        else {
            val trimmedQuery = searchQuery.trim()
            combinedLocations.filter {
                (trimmedQuery.isEmpty() || it.name.contains(trimmedQuery, ignoreCase = true)) &&
                        (filterType == null || it.type == filterType)
            }
        }
    }

    // Centering camera on the location (when selected)
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            if (location.id != "shared_temp") {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLng(location.position),
                    durationMs = 800
                )
            }
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
            if (initialLat == null || initialLng == null) {
                fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState, shouldAnimate = false) { userLocation = it }
            } else {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let { userLocation = LatLng(it.latitude, it.longitude) }
                } catch (_: Exception) {}
            }
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

    Box(modifier = Modifier.fillMaxSize()) {
        MapContent(
            cameraPositionState = cameraPositionState,
            hasLocationPermission = hasLocationPermission,
            userLocation = userLocation,
            selectedLocation = selectedLocation,
            onLocationClick = { location ->
                if (!mapViewModel.isPinMode) selectedLocation = location
            },
            onMapClick = { latLng ->
                if (mapViewModel.isPinMode) {
                    mapViewModel.setTempPin(latLng)
                } else {
                    selectedLocation = null
                    showSearchResults = false
                }
            },
            displayLocations = filteredLocations,
            events = calendarViewModel.events,
            isPinMode = mapViewModel.isPinMode,
            tempPinLocation = mapViewModel.tempPinLocation
        )

        if (!mapViewModel.isPinMode) {
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
                onFilterClick = { filterType = it },
                onMenuClick = onMenuClick,
                modifier = Modifier.statusBarsPadding()
            )
        }

        MapOverlays(
            hasLocationPermission = hasLocationPermission,
            isLoadingLocation = isLoadingLocation,
            isPinMode = mapViewModel.isPinMode,
            isEditing = mapViewModel.editingPinId != null,
            onLocationRequest = {
                scope.launch {
                    isLoadingLocation = true
                    fetchLocation(fusedLocationProviderClient = fusedLocationClient, cameraState = cameraPositionState, shouldAnimate = true) {
                        userLocation = it
                        isLoadingLocation = false
                    }
                }
            },
            onTogglePinMode = {
                if (mapViewModel.isPinMode) mapViewModel.exitPinMode()
                else mapViewModel.togglePinMode()
            },
            onConfirmPin = { showPinCreationDialog = true },
            onClearPin = { mapViewModel.clearTempPin() },
            tempPinSet = mapViewModel.tempPinLocation != null,
            isLoggedIn = mapViewModel.isLoggedIn
        )

        // Info card for selected location
        AnimatedVisibility(
            visible = selectedLocation != null && !mapViewModel.isPinMode,
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
                (it.calendarId == "custom" || it.isPinned) && isEventAtLocation(it, location)
            }
            
            val isAlreadySaved = remember(location, mapViewModel.customPins) {
                mapViewModel.customPins.any { 
                    abs(it.latitude - location.position.latitude) < 0.00001 && 
                    abs(it.longitude - location.position.longitude) < 0.00001 
                }
            }
            
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                dragHandle = null
            ) {
                LocationDetailsContent(
                    location = location,
                    events = locationEvents,
                    onGoToEvent = { event ->
                        calendarViewModel.onDateSelected(event.start.toLocalDate())
                        resetMapState()
                        navController.navigate(Routes.CALENDAR)
                    },
                    onDeletePin = {
                        showDeleteConfirmation = true
                    },
                    onTogglePin = {
                        mapViewModel.togglePinLocation(location)
                        selectedLocation = selectedLocation?.copy(isPinned = !location.isPinned)
                    },
                    onShareClick = {
                        showShareSheet = true
                    },
                    onAssociateEvent = {
                        showEventPickerForSelection = true
                    },
                    onEditPin = {
                        val pin = mapViewModel.customPins.find { it.id == location.id }
                        if (pin != null) {
                            showDetailsSheet = false
                            mapViewModel.startEditingLocation(pin.id)
                        }
                    },
                    onSaveSharedPin = {
                        if (isAlreadySaved) {
                            Toast.makeText(context, "This pin is already saved.", Toast.LENGTH_SHORT).show()
                            return@LocationDetailsContent
                        }
                        mapViewModel.saveSharedPin(location) {
                            selectedLocation = null
                            showDetailsSheet = false
                            Toast.makeText(context, "Pin saved to your map.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isAlreadySaved = isAlreadySaved
                )
            }
        }

        if (showShareSheet && selectedLocation != null) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                dragHandle = null
            ) {
                ShareLocationSheetContent(
                    location = selectedLocation!!,
                    onDismiss = { showShareSheet = false },
                    onSuccess = {
                        showShareSheet = false
                        Toast.makeText(context, "Location shared successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showDeleteConfirmation && selectedLocation != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Custom Pin", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove '${selectedLocation!!.name}' from your map? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            mapViewModel.deletePin(selectedLocation!!.id)
                            showDeleteConfirmation = false
                            showDetailsSheet = false
                            selectedLocation = null
                            Toast.makeText(context, "Pin removed.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        if (showPinCreationDialog) {
            PinCreationDialog(
                customEvents = calendarViewModel.events.filter { it.calendarId == "custom" },
                editingPin = mapViewModel.editingPin,
                onDismiss = {
                    showPinCreationDialog = false
                },
                onConfirm = { name, desc, color, eventId ->
                    mapViewModel.savePin(name, desc, color, eventId)
                    showPinCreationDialog = false
                }
            )
        }

        if (showEventPickerForSelection && selectedLocation != null) {
            EventSelectionDialog(
                customEvents = calendarViewModel.events.filter { it.calendarId == "custom" },
                currentEventId = selectedLocation?.associatedEventId,
                onDismiss = { showEventPickerForSelection = false },
                onConfirm = { eventId ->
                    mapViewModel.associateEvent(selectedLocation!!.id, eventId)
                    selectedLocation = selectedLocation?.copy(associatedEventId = eventId)
                    showEventPickerForSelection = false
                }
            )
        }
        
        mapViewModel.errorMessage?.let { error ->
            LaunchedEffect(error) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }
}

@SuppressLint("MissingPermission")
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
    } catch (_: Exception) {}
}
