package com.example.calendarexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val location: String?,
    val allDay: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarViewerScreen()
                }
            }
        }
    }
}

@Composable
private fun CalendarViewerScreen() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasPermission = granted
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Simple header (no TopAppBar)
            Text(
                text = "My Calendar (simple)",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    PermissionRequestUi(
                        onRequest = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        onOpenCalendarApp = { openCalendarAppAt(context, System.currentTimeMillis()) }
                    )
                } else {
                    EventsListUi(context = context)
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestUi(
    onRequest: () -> Unit,
    onOpenCalendarApp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "To show your calendar events in this app, allow Calendar access.\n\n" +
                    "If your Google account is synced on this device, events will appear."
        )
        Button(onClick = onRequest) { Text("Grant Calendar Permission") }
        OutlinedButton(onClick = onOpenCalendarApp) { Text("Open Calendar app") }
    }
}

@Composable
private fun EventsListUi(context: Context) {
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            events = withContext(Dispatchers.IO) {
                queryUpcomingInstances(context, daysAhead = 30)
            }
        } catch (t: Throwable) {
            error = t.message ?: t.javaClass.simpleName
        } finally {
            loading = false
        }
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error reading calendar: $error")
        }

        events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No events found.\n(If this is an emulator, add events or enable Google sync.)")
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(events) { e ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openEventInCalendarApp(context, e.eventId) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(formatEventTime(e.beginMillis, e.endMillis, e.allDay))
                        if (!e.location.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("📍 ${e.location}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission") // Safe: called only after permission granted
private fun queryUpcomingInstances(context: Context, daysAhead: Int): List<CalendarEvent> {
    val now = System.currentTimeMillis()
    val end = now + daysAhead * 24L * 60L * 60L * 1000L

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, end)
    val uri = builder.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.EVENT_LOCATION,
        CalendarContract.Instances.ALL_DAY
    )

    val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

    val out = mutableListOf<CalendarEvent>()
    context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { c ->
        val idCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val titleCol = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endCol = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val locCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
        val allDayCol = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

        while (c.moveToNext()) {
            out += CalendarEvent(
                eventId = c.getLong(idCol),
                title = c.getString(titleCol) ?: "(No title)",
                beginMillis = c.getLong(beginCol),
                endMillis = c.getLong(endCol),
                location = c.getString(locCol),
                allDay = c.getInt(allDayCol) != 0
            )
        }
    }
    return out
}

private fun formatEventTime(beginMillis: Long, endMillis: Long, allDay: Boolean): String {
    val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    val begin = Date(beginMillis)
    val end = Date(endMillis)

    return if (allDay) {
        "${dateFmt.format(begin)} (All day)"
    } else {
        "${dateFmt.format(begin)} • ${timeFmt.format(begin)} – ${timeFmt.format(end)}"
    }
}

private fun openEventInCalendarApp(context: Context, eventId: Long) {
    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No calendar app available
    }
}

private fun openCalendarAppAt(context: Context, timeMillis: Long) {
    val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
    ContentUris.appendId(builder, timeMillis)
    val intent = Intent(Intent.ACTION_VIEW, builder.build())
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No calendar app available
    }
}