package com.example.myapplication3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.Connection
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

//Data Model For Study Room Availability For 1 Week
data class TimeSlot(val startTime: String, val isAvailable: Boolean)
data class StudyRoom(val name: String, val slots: List<TimeSlot>)
typealias WeeklyAvailability = Map<String, List<StudyRoom>>

//Main
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RoomListScreen()
                }
            }
        }
    }
}

//Format Data
//Sets time color
// - red w/ slash = unavailable (taken/checked out)
// - green = available

//Flow Row = format/layout control
@OptIn(ExperimentalLayoutApi::class) // Needed for FlowRow
@Composable
fun RoomItem(room: StudyRoom) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = room.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(room.slots) { slot ->
                    // Convert 24hour time to 12 hour time
                    val displayTime = try {
                        java.time.LocalTime.parse(slot.startTime)
                            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                    } catch (e: Exception) {
                        slot.startTime
                    }

                    // Set color of time square to green or red
                    val color = if (slot.isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = displayTime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = color,
                                fontWeight = if (slot.isAvailable) androidx.compose.ui.text.font.FontWeight.Bold
                                else androidx.compose.ui.text.font.FontWeight.Normal,
                                textDecoration = if (slot.isAvailable) null
                                else androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomListScreen() {
    var weeklyData by remember { mutableStateOf<WeeklyAvailability>(emptyMap()) }
    var selectedDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = fetchWeeklyAvailability()
        if (result.isNotEmpty()) {
            weeklyData = result
            selectedDate = result.keys.sorted().first()
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "CSUCI Library Study Room Availability",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (weeklyData.isEmpty()) {
            // SAFE STATE: If the library is closed/error, show a message instead of crashing
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No data available. Check back tomorrow!", color = Color.Gray)
            }
        } else {
            val sortedDates = weeklyData.keys.sorted()
            val selectedIndex = sortedDates.indexOf(selectedDate).coerceAtLeast(0)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(Color(0xFF2E7D32), "Available")
                LegendItem(Color(0xFFD32F2F), "Unavailable")
            }

            ScrollableTabRow(selectedTabIndex = selectedIndex) {
                sortedDates.forEach { date ->
                    Tab(
                        selected = selectedDate == date,
                        onClick = { selectedDate = date },
                        text = {
                            val shortDate = date.substring(5).replace("-", "/")
                            Text(shortDate)
                        }
                    )
                }
            }

            LazyColumn(modifier = Modifier.padding(16.dp)) {
                val roomsForDay = weeklyData[selectedDate] ?: emptyList()
                items(roomsForDay) { room ->
                    RoomItem(room)
                }
            }
        }
    }
}

//Map ID Entity Number to Room Number
fun getRoomName(itemId: Int): String {
    return when (itemId) {
        61317 -> "Room 1753"
        61318 -> "Room 1754"
        61319 -> "Room 1732"
        61320 -> "Room 1734"
        else -> "Study Room $itemId"
    }
}

//The Actual Scraping of the Website
suspend fun fetchWeeklyAvailability(): WeeklyAvailability {
    val weeklyData = mutableMapOf<String, MutableMap<String, MutableList<TimeSlot>>>()

    return withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            //Look ahead 7 days
            val endDate = today.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)

            val response = Jsoup.connect("https://csuci.libcal.com/spaces/availability/grid")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                //Essential headers to get the full week data
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://csuci.libcal.com/reserve/spaces/first-floor")
                .data("lid", "8607")
                .data("gid", "15923")
                .data("start", startDate)
                .data("end", endDate)
                //600 = full week
                .data("pageSize", "600")
                .execute()

            val json = JSONObject(response.body())
            if (json.has("slots")) {
                val slotsArray = json.getJSONArray("slots")

                for (i in 0 until slotsArray.length()) {
                    val slot = slotsArray.getJSONObject(i)
                    val fullStart = slot.getString("start")
                    val date = fullStart.substring(0, 10)
                    val time = fullStart.substring(11, 16)
                    val name = getRoomName(slot.getInt("itemId"))

                    val className = slot.optString("className", "").lowercase()

                    //Blank return or "" = Green (Available)
                    //checkout or unavailable = red w/ cross out (booked/unavailable)
                    val isActuallyAvailable = if (className.isEmpty()) {
                        true
                    } else {
                        !className.contains("s-lc-eq-checkout") &&
                                !className.contains("unavailable")
                    }

                    android.util.Log.d("LIB_DEBUG", "Time: $time | Class: '$className' | Available: $isActuallyAvailable")
                    weeklyData.getOrPut(date) { mutableMapOf() }
                        .getOrPut(name) { mutableListOf() }
                        .add(TimeSlot(time, isActuallyAvailable))

                    //Testing with Logcat to see what class returned
                    //Mainly debug
                    //android.util.Log.d("LIB_DEBUG", "Time: $time | Class: $className")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Alphabetize rooms so don't jump around when switching tabs
        weeklyData.mapValues { (_, roomsMap) ->
            roomsMap.map { (name, slots) -> StudyRoom(name, slots) }.sortedBy { it.name }
        }

    }
}

//Legend for readability
@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}