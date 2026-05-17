package com.example.cicompanion.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cicompanion.calendar.CalendarViewModel
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
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

private val DARK_MAP_STYLE_JSON = """
[
  { "elementType": "geometry", "stylers": [ { "color": "#000000" } ] },
  { "elementType": "labels.text.fill", "stylers": [ { "color": "#746855" } ] },
  { "elementType": "labels.text.stroke", "stylers": [ { "color": "#242f3e" } ] },
  { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
  { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
  { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#38414e" } ] },
  { "featureType": "road", "elementType": "geometry.stroke", "stylers": [ { "color": "#212a37" } ] },
  { "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#9ca5b3" } ] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [ { "color": "#746855" } ] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [ { "color": "#1f2835" } ] },
  { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [ { "color": "#f3d19c" } ] },
  { "featureType": "transit", "elementType": "geometry", "stylers": [ { "color": "#2f3948" } ] },
  { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
  { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#17263c" } ] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [ { "color": "#515c6d" } ] },
  { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [ { "color": "#17263c" } ] }
]
""".trimIndent()

val campusLocations = listOf(
    CampusLocation(id = "loc_bell_tower", name = "Bell Tower", position = LatLng(34.16138604361421, -119.0432651672823), description = "Center of Campus", type = LocationType.BUILDING, icon = Icons.Default.Business, color = MapBuildingRed),
    CampusLocation(id = "loc_bell_tower_east", name = "Bell Tower East", position = LatLng(34.16134665298329, -119.04189180309578), description = "", type = LocationType.BUILDING, icon = Icons.Default.Business, color = MapBuildingRed, searchKeywords = listOf("BTE")),
    CampusLocation(id = "loc_bell_tower_west", name = "Bell Tower West", position = LatLng(34.16070116130278, -119.04439859472768), description = "", type = LocationType.BUILDING, icon = Icons.Default.Business, color = MapBuildingRed, searchKeywords = listOf("BTW")),
    CampusLocation(id = "loc_library", name = "John Spoor Broome Library", position = LatLng(34.16269668565898, -119.04094849136715), description = "Main Library", type = LocationType.BUILDING, icon = Icons.AutoMirrored.Filled.MenuBook, color = MapLibraryGreen, searchKeywords = listOf("Broome Library", "Library", "JSB")),
    CampusLocation(id = "loc_union", name = "Student Union", position = LatLng(34.1610, -119.0436), description = "Dining and Lounge", type = LocationType.BUILDING, icon = Icons.Default.Groups, color = MapLibraryGreen, searchKeywords = listOf("SUB", "Union")),
    CampusLocation(id = "loc_marin", name = "Marin Hall", position = LatLng(34.164528096869034, -119.04507117740494), description = "Faculty Offices for Mathematics and Data Science", type = LocationType.BUILDING, icon = Icons.Default.Business, color = MapBuildingRed, searchKeywords = listOf("Marin")),
    CampusLocation(id = "loc_shasta", name = "Shasta Hall", position = LatLng(34.164576865185516, -119.04472618829523), description = "Faculty Offices for Computer Science and Engineering", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Shasta")),
    CampusLocation(id = "loc_gateway", name = "Gateway Hall", position = LatLng(34.16483652693463, -119.04547452459948), description = "Large building with classrooms and study rooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Gateway")),
    CampusLocation(id = "loc_napa", name = "Napa Hall", position = LatLng(34.16377605025435, -119.04540741204008), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Napa")),
    CampusLocation(id = "loc_solano", name = "Solano Hall", position = LatLng(34.16340620613301, -119.0450085042822), description = "Offices for faculty, Division of Technology and Innovation", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Solano")),
    CampusLocation(id = "loc_mvs", name = "Martin V. Smith Hall", position = LatLng(34.162909062826785, -119.04476226672068), description = "Houses Nursing Program", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("MVS", "Smith Hall")),
    CampusLocation(id = "loc_sierra", name = "Sierra Hall", position = LatLng(34.16229510038971, -119.04461124101422), description = "Multimode lecture halls, classrooms and faculty offices.", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Sierra")),
    CampusLocation(id = "loc_del_norte", name = "Del Norte Hall", position = LatLng(34.163180726190696, -119.04410235004629), description = "Lecture classrooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Del Norte")),
    CampusLocation(id = "loc_madera", name = "Madera Hall", position = LatLng(34.1629335125641, -119.04394147483745), description = "Lecture classrooms", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Madera")),
    CampusLocation(id = "loc_placer", name = "Placer Hall", position = LatLng(34.163344972360754, -119.04300312204607), description = "Offices of University Police and Parking Services", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Placer")),
    CampusLocation(id = "loc_rush", name = "Richard R. Rush Hall", position = LatLng(34.162673963555044, -119.04342008432133), description = "Houses the University President and administration.", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Rush", "Rush Hall")),
    CampusLocation(id = "loc_chaparral", name = "Chaparral Hall", position = LatLng(34.162096255189496, -119.04560917381353), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Chaparral")),
    CampusLocation(id = "loc_ironwood", name = "Ironwood Hall", position = LatLng(34.16245544107054, -119.04645265103974), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Ironwood")),
    CampusLocation(id = "loc_el_dorado", name = "El Dorado Hall", position = LatLng(34.16423447296913, -119.04710369220899), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("El Dorado")),
    CampusLocation(id = "loc_aliso", name = "Aliso Hall", position = LatLng(34.16133147263596, -119.04535150727516), description = "8 Science Labs and 16 Faculty Offices", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Aliso")),
    CampusLocation(id = "loc_yuba", name = "Yuba Hall", position = LatLng(34.16407767205871, -119.04109248173509), description = "Houses the Student Health Center", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Yuba")),
    CampusLocation(id = "loc_sage", name = "Sage Hall", position = LatLng(34.164167218645936, -119.04222881533492), description = "The Enrollment Center.", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapLibraryGreen, searchKeywords = listOf("Sage")),
    CampusLocation(id = "loc_malibu", name = "Malibu Hall", position = LatLng(34.16126160533967, -119.04086506383092), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapLibraryGreen, searchKeywords = listOf("Malibu")),
    CampusLocation(id = "loc_topanga", name = "Topanga Hall", position = LatLng(34.16019137315942, -119.04169333763335), description = "Art Program facilities and labs.", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Topanga")),
    CampusLocation(id = "loc_arroyo", name = "Arroyo Hall", position = LatLng(34.160354318454424, -119.04489629947327), description = "Wellness and Athletics Office. Recreation Center.", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapLibraryGreen, searchKeywords = listOf("Arroyo")),
    CampusLocation(id = "loc_trinity", name = "Trinity Hall", position = LatLng(34.15934671289535, -119.0423644726643), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Trinity")),
    CampusLocation(id = "loc_lindero", name = "Lindero Hall", position = LatLng(34.15956619235504, -119.04141202350661), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Lindero")),
    CampusLocation(id = "loc_ojai", name = "Ojai Hall", position = LatLng(34.16173923354911, -119.04257933412188), description = "", type = LocationType.BUILDING, icon = Icons.Default.School, color = MapBuildingRed, searchKeywords = listOf("Ojai")),

    // Food category
    CampusLocation(id = "food_islands", name = "Islands Cafe", position = LatLng(34.16046259918211, -119.04156950739008), description = "Dining commons for students and employees.", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = MapFoodOrange, searchKeywords = listOf("Islands")),
    CampusLocation(id = "food_coastal", name = "Coastal Cup", position = LatLng(34.16517607999232, -119.04492439787398), description = "A coffee shop inside Gateway Hall.", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = MapFoodOrange, searchKeywords = listOf("Coastal")),
    CampusLocation(id = "food_mom_wong", name = "Mom Wong Kitchen", position = LatLng(34.162865389467136, -119.03934866185736), description = "Camarillo\u2019s Premier Chinese Restaurant", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = MapFoodOrange, searchKeywords = listOf("Mom Wong")),
    CampusLocation(id = "food_tortillas", name = "Tortillas Grill", position = LatLng(34.16304512832548, -119.03936598760045), description = "Mexican Food", type = LocationType.FOOD, icon = Icons.Default.Restaurant, color = MapFoodOrange, searchKeywords = listOf("Tortillas")),

    // Areas category
    CampusLocation(id = "area_north_quad", name = "North Quad", position = LatLng(34.163869256675795, -119.04439875392643), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = MapAreaPurple),
    CampusLocation(id = "area_north_field", name = "North Field", position = LatLng(34.167561045970785, -119.04526582938406), description = "", type = LocationType.AREA, icon = Icons.Default.Park, color = MapAreaPurple),
    CampusLocation(id = "area_central_mall", name = "Central Mall", position = LatLng(34.16182005886744, -119.04344776363348), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = MapAreaPurple),
    CampusLocation(id = "area_potrero", name = "Potrero Fields", position = LatLng(34.159887809784415, -119.04743204832411), description = "", type = LocationType.AREA, icon = Icons.Default.Park, color = MapAreaPurple),
    CampusLocation(id = "area_south_quad", name = "South Quad", position = LatLng(34.160229605621325, -119.04270063156984), description = "", type = LocationType.AREA, icon = Icons.Default.People, color = MapAreaPurple),

    // Housing category
    CampusLocation(id = "house_sc_v", name = "Santa Cruz Village", position = LatLng(34.15909088586997, -119.04255249590989), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon, searchKeywords = listOf("Santa Cruz")),
    CampusLocation(id = "house_sc_d", name = "Santa Cruz Village D", position = LatLng(34.16012344365498, -119.04398722341813), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_sc_e", name = "Santa Cruz Village E", position = LatLng(34.15991620770629, -119.04397330984331), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_sc_f", name = "Santa Cruz Village F", position = LatLng(34.1597406324177, -119.04394896107382), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_sc_g", name = "Santa Cruz Village G", position = LatLng(34.15951036919081, -119.04383417401763), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_sc_h", name = "Santa Cruz Village H", position = LatLng(34.1592628355215, -119.04385504439146), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_anacapa_a", name = "Anacapa Village A", position = LatLng(34.15935494115949, -119.04437332533838), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon, searchKeywords = listOf("Anacapa")),
    CampusLocation(id = "house_anacapa_b", name = "Anacapa Village B", position = LatLng(34.1597665369933, -119.04473507848517), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_anacapa_c", name = "Anacapa Village C", position = LatLng(34.15958520481026, -119.04532292734869), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),
    CampusLocation(id = "house_anacapa_com", name = "Anacapa Village Commons Building", position = LatLng(34.159208147754384, -119.04492291184982), description = "", type = LocationType.HOUSING, icon = Icons.Default.Home, color = MapHousingMaroon),

    // Parking Lots
    CampusLocation(id = "park_a3", name = "Parking Lot A3", position = LatLng(34.166606172710715, -119.04703678095836), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A3")),
    CampusLocation(id = "park_a4", name = "Parking Lot A4", position = LatLng(34.164244290933254, -119.04646170905023), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A4")),
    CampusLocation(id = "park_a11", name = "Parking Lot A11", position = LatLng(34.164126287402695, -119.04786350249398), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A11")),
    CampusLocation(id = "park_a2", name = "Parking Lot A2", position = LatLng(34.164108680933254, -119.04164009131796), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A2")),
    CampusLocation(id = "park_a6", name = "Parking Lot A6", position = LatLng(34.16325952040607, -119.04202460036679), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A6")),
    CampusLocation(id = "park_a1", name = "Parking Lot A1", position = LatLng(34.163586436668446, -119.04267748158364), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A1")),
    CampusLocation(id = "park_a8", name = "Parking Lot A8", position = LatLng(34.16309446392495, -119.04030380989158), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A8")),
    CampusLocation(id = "park_ae", name = "Parking Lot A/E", position = LatLng(34.16186741815617, -119.04156570455609), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A/E")),
    CampusLocation(id = "park_a7", name = "Parking Lot A7", position = LatLng(34.160649977624885, -119.04118719083276), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A7")),
    CampusLocation(id = "park_a10", name = "Parking Lot A10", position = LatLng(34.15940842843663, -119.04062564402817), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A10")),
    CampusLocation(id = "park_sh1", name = "Parking Lot SH1", position = LatLng(34.15912561853075, -119.04537811915849), description = SH_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingCyan, searchKeywords = listOf("SH1")),
    CampusLocation(id = "park_r1", name = "Parking Lot R1", position = LatLng(34.1630169076814, -119.04316226033373), description = RESTRICTED_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingDeepPurple, searchKeywords = listOf("R1")),
    CampusLocation(id = "park_a5", name = "Parking Lot A5", position = LatLng(34.16050864644475, -119.04463491964691), description = GENERAL_PARKING_DESC, type = LocationType.PARKING, icon = Icons.Default.LocalParking, color = MapParkingBlue, searchKeywords = listOf("A5"))
)

/**
 * Helper function to check if a location matches an event's location string.
 */
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
    tempEventId: String? = null
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
                // Just fetch but don't move camera if we have an initial position
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
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
                    onFilterClick = { filterType = it }
                )
            }
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
                tempPinSet = mapViewModel.tempPinLocation != null
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
                
                ModalBottomSheet(
                    onDismissRequest = { showDetailsSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface
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
                        onSendMessage = {
                            navController.navigate("${Routes.SOCIAL}?shareLocation=${location.name}")
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
                            scope.launch {
                                val newPin = CustomPin(
                                    id = java.util.UUID.randomUUID().toString(),
                                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                    name = location.name,
                                    latitude = location.position.latitude,
                                    longitude = location.position.longitude,
                                    description = location.description,
                                    colorArgb = location.color.toArgb(),
                                    associatedEventId = location.associatedEventId
                                )
                                FirestoreManager.saveCustomPin(newPin)
                                selectedLocation = null
                                showDetailsSheet = false
                            }
                        }
                    )
                }
            }

            if (showDeleteConfirmation && selectedLocation != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Delete Custom Pin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    text = { Text("Are you sure you want to remove '${selectedLocation!!.name}' from your map? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                            Text("Delete", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
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
        }
    }
}

@Composable
fun EventSelectionDialog(
    customEvents: List<CalendarEvent>,
    currentEventId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredEvents = remember(searchQuery, customEvents) {
        customEvents.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            @Suppress("DEPRECATION")
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                @Suppress("DEPRECATION")
                Text(
                    "Link an Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your events...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    item {
                        Surface(
                            onClick = { onConfirm(null) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (currentEventId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Text("None (Unlink Event)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    if (filteredEvents.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No events found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filteredEvents) { event ->
                            val isSelected = event.id == currentEventId
                            Surface(
                                onClick = { onConfirm(event.id) },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(event.title, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        Text(event.timeLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    @Suppress("DEPRECATION")
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PinCreationDialog(
    customEvents: List<CalendarEvent>,
    editingPin: CustomPin? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Color, String?) -> Unit
) {
    var name by remember { mutableStateOf(editingPin?.name ?: "") }
    var description by remember { mutableStateOf(editingPin?.description ?: "") }
    var selectedColor by remember { mutableStateOf(editingPin?.color ?: CoralRed) }
    var selectedEventId by remember { mutableStateOf<String?>(editingPin?.associatedEventId) }
    var showEventPicker by remember { mutableStateOf(false) }

    val colors = listOf(CoralRed, MapAreaPurple, MapParkingBlue, MapLibraryGreen, MapFoodOrange)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingPin != null) "Edit Custom Pin" else "Create Custom Pin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(16.dp))

                Text("Pick a Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(if (selectedColor == color) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Linked Event", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))

                val selectedEvent = customEvents.find { it.id == selectedEventId }
                Surface(
                    onClick = { showEventPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedEvent != null) Icons.Default.Event else Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = if (selectedEvent != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedEvent?.title ?: "No event linked (Tap to select)",
                            color = if (selectedEvent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedColor, selectedEventId) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editingPin != null) "Save Changes" else "Confirm", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )

    if (showEventPicker) {
        EventSelectionDialog(
            customEvents = customEvents,
            currentEventId = selectedEventId,
            onDismiss = { showEventPicker = false },
            onConfirm = {
                selectedEventId = it
                showEventPicker = false
            }
        )
    }
}

@Composable
fun LocationDetailsContent(
    location: CampusLocation, 
    events: List<CalendarEvent>,
    onGoToEvent: (CalendarEvent) -> Unit,
    onDeletePin: () -> Unit,
    onTogglePin: () -> Unit,
    onSendMessage: () -> Unit,
    onAssociateEvent: () -> Unit,
    onEditPin: () -> Unit,
    onSaveSharedPin: () -> Unit = {}
) {
    val isTemp = location.id == "shared_temp"

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
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                @Suppress("DEPRECATION")
                Text(
                    text = if (isTemp) "Shared Temporary Pin" else location.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))
                if (isTemp) {
                    Text(
                        text = "Clicking empty space on the map or another point will remove this pin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (!isTemp && location.isCustom) {
                IconButton(onClick = onEditPin) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Pin", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (location.description.isNotEmpty()) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            @Suppress("DEPRECATION")
            Text(
                text = location.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isTemp) {
                Button(
                    onClick = onSaveSharedPin,
                    colors = ButtonDefaults.buttonColors(containerColor = MapSuccessGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    @Suppress("DEPRECATION")
                    Text("Save to My Map", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                if (location.isCustom) {
                    Button(
                        onClick = onDeletePin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), 
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                    Button(
                        onClick = onSendMessage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        @Suppress("DEPRECATION")
                        Text("Share")
                    }
                }
            }
        }

        if (location.isCustom && !isTemp) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAssociateEvent,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                @Suppress("DEPRECATION")
                Text(if (location.associatedEventId != null) "Change Linked Event" else "Link an Event", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (events.isNotEmpty()) {
            val visibleEvents = if (isTemp && location.associatedEventId != null) {
                // If it's a shared pin with an associated event, only show if user is already a member
                events.filter { it.calendarId == "custom" && it.ownerId != FirebaseAuth.getInstance().currentUser?.uid }
            } else {
                events
            }

            if (visibleEvents.isNotEmpty()) {
                @Suppress("DEPRECATION")
                Text(
                    text = if (location.isCustom && location.associatedEventId != null) "Linked Event" else "Events at this Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                visibleEvents.forEach { event ->
                    MapEventItem(
                        event = event,
                        onMoreClick = { onGoToEvent(event) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            Text(
                text = "No upcoming events here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            event.isPinned -> MapAreaPurple.copy(alpha = 0.1f)
                            else -> MapFoodOrange.copy(alpha = 0.1f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                @Suppress("DEPRECATION")
                Icon(
                    imageVector = if (event.isPinned) Icons.Default.PushPin else Icons.Default.Event,
                    contentDescription = null,
                    tint = if (event.isPinned) MapAreaPurple else MapFoodOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            @Suppress("DEPRECATION")
            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                @Suppress("DEPRECATION")
                Text(
                    text = event.timeLabel(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // calendar button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Go to Calendar",
                    tint = MaterialTheme.colorScheme.primary,
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
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search campus...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn {
                        items(filteredLocations) { location ->
                            ListItem(
                                headlineContent = { Text(location.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                supportingContent = { Text(location.type.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingContent = { Icon(location.icon, contentDescription = null, tint = location.color, modifier = Modifier.size(24.dp)) },
                                modifier = Modifier.clickable { onResultClick(location) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                    LocationType.CUSTOM -> Icons.Default.PushPin
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
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    events: List<CalendarEvent>,
    isPinMode: Boolean = false,
    tempPinLocation: LatLng? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    val mapStyle = remember(isDarkTheme) {
        MapStyleOptions(if (isDarkTheme) DARK_MAP_STYLE_JSON else MAP_STYLE_JSON)
    }

    val uiSettings = remember(isPinMode) {
        MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = !isPinMode,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(hasLocationPermission, isPinMode, mapStyle) {
        MapProperties(
            isMyLocationEnabled = false,
            latLngBoundsForCameraTarget = CSUCI_BOUNDS,
            minZoomPreference = 15f,
            mapStyleOptions = mapStyle
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
                        color = CoralRed,
                        isPinned = false,
                        eventCount = 0,
                        isEditing = true
                    )
                }
            }
        } else {
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
                            SelectedPointerIcon(location, eventCount, hasPinnedEvent, false)
                        } else {
                            if (location.isCustom) {
                                CustomPinMarkerIcon(location.color, location.isPinned, eventCount)
                            } else {
                                LandmarkIcon(
                                    icon = location.icon,
                                    color = location.color,
                                    eventCount = eventCount,
                                    hasPinnedEvent = hasPinnedEvent,
                                    hasBookmarkedEvent = false,
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

@Composable
fun CustomPinMarkerIcon(color: Color, isPinned: Boolean, eventCount: Int, isEditing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEditing) 1.6f else 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        // Pulsing background ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                .background(color.copy(alpha = pulseAlpha), CircleShape)
        )

        // Pin body (Teardrop shape)
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer(rotationZ = 45f)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp))
                .background(color, RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp))
                .border(
                    width = 2.dp,
                    color = if (isPinned) MapAreaPurple else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp)
                )
        )

        // Inner white circle to make it pop
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                .shadow(2.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SelectedPointerIcon(
    location: CampusLocation, 
    eventCount: Int = 0, 
    hasPinnedEvent: Boolean = false,
    @Suppress("UNUSED_PARAMETER") hasBookmarkedEvent: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f, 
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(70.dp) 
            .graphicsLayer(translationY = bounce)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = location.color,
            modifier = Modifier.fillMaxSize().shadow(4.dp, CircleShape)
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(top = 8.dp).size(32.dp),
            border = BorderStroke(2.5.dp, when {
                location.isPinned -> MapAreaPurple
                hasPinnedEvent -> MapAreaPurple
                eventCount > 0 -> MapFoodOrange
                else -> location.color
            }),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (location.isCustom) Icons.Default.PushPin else location.icon,
                    contentDescription = null,
                    tint = location.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LandmarkIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    eventCount: Int = 0, 
    hasPinnedEvent: Boolean = false,
    @Suppress("UNUSED_PARAMETER") hasBookmarkedEvent: Boolean = false,
    isCustom: Boolean = false,
    isPinned: Boolean = false
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color, CircleShape)
                .border(2.5.dp, when {
                    isPinned -> MapAreaPurple
                    hasPinnedEvent -> MapAreaPurple
                    eventCount > 0 -> MapFoodOrange
                    else -> MaterialTheme.colorScheme.surface
                }, CircleShape)
                .shadow(4.dp, CircleShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCustom) Icons.Default.PushPin else icon,
                contentDescription = null, 
                tint = Color.White, // THIS HARDCODING IS OKAY!
                modifier = Modifier.size(18.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(location.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                       imageVector = if (location.isCustom) Icons.Default.PushPin else location.icon,
                        contentDescription = null, 
                        tint = location.color, 
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                @Suppress("DEPRECATION")
                Column(Modifier.weight(1f)) {
                    @Suppress("DEPRECATION")
                    Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (location.description.isNotEmpty()) {
                        @Suppress("DEPRECATION")
                        Text(location.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Surface(
                    onClick = onDetailsClick,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp).fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Details", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            Icon(Icons.Default.PersonPinCircle, contentDescription = null, tint = MapUserBlue, modifier = Modifier.size(40.dp))
            Box(modifier = Modifier.size(12.dp).offset(y = (-2).dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
            Box(modifier = Modifier.size(8.dp).offset(y = (-2).dp).background(MapUserBlue, CircleShape))
        }
    }
    Circle(center = position, radius = 25.0, fillColor = MapUserBlue.copy(alpha = 0.15f), strokeColor = MapUserBlue.copy(alpha = 0.4f), strokeWidth = 2f)
}

@Composable
fun MapOverlays(
    hasLocationPermission: Boolean, 
    @Suppress("UNUSED_PARAMETER") isLoadingLocation: Boolean,
    isPinMode: Boolean,
    isEditing: Boolean = false,
    onLocationRequest: () -> Unit,
    onTogglePinMode: () -> Unit,
    onConfirmPin: () -> Unit,
    onClearPin: () -> Unit,
    tempPinSet: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPinMode) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).padding(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = onLocationRequest,
                        modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 1f)), CircleShape),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_menu_mylocation), contentDescription = "My Location", modifier = Modifier.size(24.dp))
                    }
                }

                FloatingActionButton(
                    onClick = onTogglePinMode,
                    modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 1f)), CircleShape),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = "Custom Pin",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (isPinMode) {
            // Header for pin mode
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = if (isEditing) "Move the pin or confirm location."
                           else if (tempPinSet) "Pin placed! Confirm to continue."
                           else "Tap on map to place your pin",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom controls for pin mode
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (tempPinSet) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onClearPin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface, 
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text("Change Location")
                        }
                        Button(
                            onClick = onConfirmPin,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text("Confirm Location", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                // Exit pin mode button
                Surface(
                    onClick = onTogglePinMode,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (!hasLocationPermission && !isPinMode) PermissionCard(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun PermissionCard(modifier: Modifier) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        @Suppress("DEPRECATION")
        Text("Location permission is required to show your position on the map.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
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
