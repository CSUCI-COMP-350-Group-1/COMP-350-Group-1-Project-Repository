package com.example.cicompanion.calendar

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.calendar.model.CourseCatalogCourse
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.social.EventInvite
import com.example.cicompanion.social.SocialRepository
import com.example.cicompanion.social.UserAvatar
import com.example.cicompanion.social.UserProfile
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.utils.HtmlUtils
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

val CoralRed = Color(0xFFEF3347)
val CardOffWhite = Color(0xFFF6E6D8)
val SoftText = Color(0xFF6E5555)
val DateCellBorder = Color(0xFFE2BFB7)
val SharedEventBlue = Color(0xFF2196F3)
val CustomEventOrange = Color(0xFFFF9800)
val PinnedEventPurple = Color(0xFF9C27B0)
val DateCellWhite = Color(0xFFF7F4F8)
val EventCardGrey = Color(0xFFF2F2F2)

@Composable
fun CalendarHeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CoralRed)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            trailingContent?.invoke()
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )
}

@Composable
fun CompactTimeChip(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isError: Boolean = false
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isError) Color.Red.copy(alpha = 0.1f) else if (isSelected) CoralRed.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isError) Color.Red else if (isSelected) CoralRed else Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isError) Color.Red else if (isSelected) CoralRed else Color.Gray, fontSize = 10.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isError) Color.Red else if (isSelected) CoralRed else Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = time.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) Color.Red else if (isSelected) CoralRed else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun WheelTimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    var hour by remember {
        mutableIntStateOf(
            if (initialTime.hour == 0) 12
            else if (initialTime.hour > 12) initialTime.hour - 12
            else initialTime.hour
        )
    }
    var minute by remember { mutableIntStateOf(initialTime.minute) }
    var amPm by remember { mutableStateOf(if (initialTime.hour < 12) "AM" else "PM") }

    LaunchedEffect(initialTime) {
        val h = if (initialTime.hour == 0) 12
        else if (initialTime.hour > 12) initialTime.hour - 12
        else initialTime.hour
        hour = h
        minute = initialTime.minute
        amPm = if (initialTime.hour < 12) "AM" else "PM"
    }

    LaunchedEffect(hour, minute, amPm) {
        val h = when (amPm) {
            "AM" -> if (hour == 12) 0 else hour
            "PM" -> if (hour == 12) 12 else hour + 12
            else -> hour
        }
        val newTime = LocalTime.of(h, minute)
        if (newTime != initialTime) {
            onTimeChange(newTime)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = (1..12).toList(),
            initialIndex = hour - 1,
            onItemSelected = { hour = it },
            modifier = Modifier.weight(1f),
            isLooping = true
        )
        Text(":", style = MaterialTheme.typography.headlineMedium)
        WheelPicker(
            items = (0..59).toList(),
            initialIndex = minute,
            onItemSelected = { minute = it },
            format = { String.format(Locale.US, "%02d", it) },
            modifier = Modifier.weight(1f),
            isLooping = true
        )
        WheelPicker(
            items = listOf("AM", "PM"),
            initialIndex = if (amPm == "AM") 0 else 1,
            onItemSelected = { amPm = it },
            modifier = Modifier.weight(1f),
            isLooping = false
        )
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    format: (T) -> String = { it.toString() },
    isLooping: Boolean = false
) {
    if (items.isEmpty()) return
    
    val totalItemCount = if (isLooping) 10000 else items.size
    val safeInitialIndex = if (isLooping) {
        val mid = 5000
        mid - (mid % items.size) + initialIndex
    } else {
        initialIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 40.dp
    val selectedIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val actualIndex = selectedIndex % items.size
            if (actualIndex in items.indices) {
                onItemSelected(items[actualIndex])
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(CoralRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItemCount) { index ->
                val item = items[index % items.size]
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == index) CoralRed else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(initialTime: LocalTime, onDismiss: () -> Unit, onTimeSelected: (LocalTime) -> Unit) {
    var tempTime by remember { mutableStateOf(initialTime) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WheelTimePicker(initialTime = tempTime, onTimeChange = { tempTime = it })
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(tempTime) }, colors = ButtonDefaults.buttonColors(containerColor = CoralRed), shape = RoundedCornerShape(12.dp)) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ClassDetailsContent(
    selectedClass: SelectedClass,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "${selectedClass.courseCode} - ${selectedClass.courseTitle}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selectedClass.majorName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

        DetailRow(Icons.Outlined.Schedule, "${formatTimeForDisplay(selectedClass.startTime)} - ${formatTimeForDisplay(selectedClass.endTime)}")
        DetailRow(Icons.Outlined.CalendarToday, selectedClass.meetingPatternLabel())
        if (selectedClass.location.isNotBlank()) {
            DetailRow(Icons.Outlined.LocationOn, selectedClass.location)
        }

        if (selectedClass.hasSecondTimeRange) {
            Surface(
                color = CoralRed.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedClass.notes2.isNotBlank()) selectedClass.notes2 else "Secondary Meeting / Lab",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                    DetailRow(Icons.Outlined.Schedule, "${formatTimeForDisplay(selectedClass.startTime2)} - ${formatTimeForDisplay(selectedClass.endTime2)}", iconSize = 16.dp, textSize = 14.sp)
                    val days2 = selectedClass.daysOfWeek2.sorted().joinToString(" ") { day ->
                        when(day) {
                            1 -> "Mon"
                            2 -> "Tue"
                            3 -> "Wed"
                            4 -> "Thu"
                            5 -> "Fri"
                            6 -> "Sat"
                            7 -> "Sun"
                            else -> ""
                        }
                    }
                    DetailRow(Icons.Outlined.CalendarToday, days2, iconSize = 16.dp, textSize = 14.sp)
                    if (selectedClass.location2.isNotBlank()) {
                        DetailRow(Icons.Outlined.LocationOn, selectedClass.location2, iconSize = 16.dp, textSize = 14.sp)
                    }
                }
            }
        }

        if (selectedClass.termLabel.isNotBlank() || selectedClass.notes.isNotBlank()) {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            if (selectedClass.termLabel.isNotBlank()) {
                Text(text = "Term: ${selectedClass.termLabel}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            if (selectedClass.notes.isNotBlank()) {
                Text(text = selectedClass.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit")
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Remove")
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconSize: androidx.compose.ui.unit.Dp = 18.dp, textSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = Color.Gray)
        Text(text = text, fontSize = textSize)
    }
}

@Composable
fun AddClassContent(
    majors: List<CourseCatalogMajor>,
    editingClass: SelectedClass? = null,
    onDismiss: () -> Unit,
    onConfirm: (SelectedClass) -> Unit
) {
    var selectedMajor by remember { mutableStateOf<CourseCatalogMajor?>(null) }
    var selectedCourse by remember { mutableStateOf<CourseCatalogCourse?>(null) }
    val selectedDays = remember { mutableStateListOf<Int>() }

    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 15)) }

    var hasSecondTimeRange by remember { mutableStateOf(false) }
    val selectedDays2 = remember { mutableStateListOf<Int>() }
    var startTime2 by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime2 by remember { mutableStateOf(LocalTime.of(10, 15)) }
    var location2Text by rememberSaveable { mutableStateOf("") }
    var notes2Text by rememberSaveable { mutableStateOf("") }

    var startDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateText by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(4).toString()) }
    var locationText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var termLabelText by rememberSaveable { mutableStateOf("") }

    var showMajorPicker by remember { mutableStateOf(false) }
    var showCoursePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showStartTimePicker2 by remember { mutableStateOf(false) }
    var showEndTimePicker2 by remember { mutableStateOf(false) }

    LaunchedEffect(editingClass, majors) {
        if (editingClass != null && majors.isNotEmpty()) {
            val major = majors.firstOrNull { it.code == editingClass.majorCode }
            val course = major?.courses?.firstOrNull { it.code == editingClass.courseCode }
                ?: CourseCatalogCourse(editingClass.courseCode, editingClass.courseTitle, editingClass.typicallyOffered)

            selectedMajor = major
            selectedCourse = course
            selectedDays.clear()
            selectedDays.addAll(editingClass.daysOfWeek)
            startTime = LocalTime.parse(editingClass.startTime)
            endTime = LocalTime.parse(editingClass.endTime)

            hasSecondTimeRange = editingClass.hasSecondTimeRange
            selectedDays2.clear()
            selectedDays2.addAll(editingClass.daysOfWeek2)
            startTime2 = if (editingClass.startTime2.isNotBlank()) LocalTime.parse(editingClass.startTime2) else LocalTime.of(9, 0)
            endTime2 = if (editingClass.endTime2.isNotBlank()) LocalTime.parse(editingClass.endTime2) else LocalTime.of(10, 15)
            location2Text = editingClass.location2
            notes2Text = editingClass.notes2

            startDateText = editingClass.startDate
            endDateText = editingClass.endDate
            locationText = editingClass.location
            notesText = editingClass.notes
            termLabelText = editingClass.termLabel
        }
    }

    val timeError = startTime.isAfter(endTime) || startTime == endTime
    val timeError2 = hasSecondTimeRange && (startTime2.isAfter(endTime2) || startTime2 == endTime2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (editingClass == null) "New Class" else "Edit Class",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CoralRed
        )

        // Course Selection Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f).clickable { showMajorPicker = true }) {
                OutlinedTextField(
                    value = selectedMajor?.code ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Major", fontSize = 10.sp) },
                    placeholder = { Text("Select") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = CoralRed.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = Color.Gray
                    )
                )
            }
            Box(modifier = Modifier.weight(1.5f).clickable { if (selectedMajor != null) showCoursePicker = true }) {
                OutlinedTextField(
                    value = selectedCourse?.code ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Course", fontSize = 10.sp) },
                    placeholder = { Text("Select") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (selectedMajor != null) CoralRed.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = Color.Gray
                    )
                )
            }
        }

        selectedCourse?.let {
            Text(
                text = it.title,
                style = MaterialTheme.typography.bodySmall,
                color = CoralRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DayChipRows(
            selectedDays = selectedDays,
            onToggleDay = { dayValue ->
                if (dayValue in selectedDays) selectedDays.remove(dayValue)
                else selectedDays.add(dayValue)
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTimeChip(label = "Start", time = startTime, isSelected = true, onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
            CompactTimeChip(label = "End", time = endTime, isSelected = true, isError = timeError, onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
        }
        
        if (timeError) {
            Text("End time must be after start time.", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = termLabelText,
                onValueChange = { termLabelText = it },
                label = { Text("Term", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = locationText,
                onValueChange = { locationText = it },
                label = { Text("Location", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(label = "Start Date", value = startDateText, modifier = Modifier.weight(1f), onDateSelected = { startDateText = it })
            DatePickerField(label = "End Date", value = endDateText, modifier = Modifier.weight(1f), onDateSelected = { endDateText = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add Secondary/Lab Range", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = hasSecondTimeRange,
                onCheckedChange = { hasSecondTimeRange = it },
                colors = SwitchDefaults.colors(checkedTrackColor = CoralRed),
                modifier = Modifier.scale(0.8f)
            )
        }

        if (hasSecondTimeRange) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayChipRows(
                    selectedDays = selectedDays2,
                    onToggleDay = { dayValue ->
                        if (dayValue in selectedDays2) selectedDays2.remove(dayValue)
                        else selectedDays2.add(dayValue)
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactTimeChip(label = "Start 2", time = startTime2, isSelected = true, onClick = { showStartTimePicker2 = true }, modifier = Modifier.weight(1f))
                    CompactTimeChip(label = "End 2", time = endTime2, isSelected = true, isError = timeError2, onClick = { showEndTimePicker2 = true }, modifier = Modifier.weight(1f))
                }
                if (timeError2) {
                    Text("End time 2 must be after start time 2.", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = location2Text,
                        onValueChange = { location2Text = it },
                        label = { Text("2nd Loc", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes2Text,
                        onValueChange = { notes2Text = it },
                        label = { Text("2nd Notes", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("General Notes", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 1
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = Color.Gray)
            }
            Button(
                onClick = {
                    if (selectedMajor != null && selectedCourse != null && selectedDays.isNotEmpty()) {
                        val newClass = SelectedClass(
                            id = editingClass?.id ?: UUID.randomUUID().toString(),
                            majorCode = selectedMajor!!.code,
                            majorName = selectedMajor!!.name,
                            courseCode = selectedCourse!!.code,
                            courseTitle = selectedCourse!!.title,
                            typicallyOffered = selectedCourse!!.typicallyOffered,
                            daysOfWeek = selectedDays.sorted(),
                            startTime = startTime.toString(),
                            endTime = endTime.toString(),
                            hasSecondTimeRange = hasSecondTimeRange,
                            daysOfWeek2 = selectedDays2.sorted(),
                            startTime2 = startTime2.toString(),
                            endTime2 = endTime2.toString(),
                            location2 = location2Text.trim(),
                            notes2 = notes2Text.trim(),
                            startDate = startDateText,
                            endDate = endDateText,
                            location = locationText.trim(),
                            notes = notesText.trim(),
                            termLabel = termLabelText.trim(),
                            colorArgb = colorForCourse(selectedCourse!!.code),
                            reminderEnabled = false,
                            createdAt = editingClass?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onConfirm(newClass)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = selectedMajor != null && selectedCourse != null && selectedDays.isNotEmpty() && !timeError && (!hasSecondTimeRange || !timeError2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editingClass == null) "Add" else "Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showMajorPicker) {
        CatalogItemPickerDialog(
            title = "Select Major",
            items = majors,
            itemLabel = { item: CourseCatalogMajor -> "${item.code} - ${item.name}" },
            searchMatcher = { item: CourseCatalogMajor, query: String -> item.name.contains(query, true) || item.code.contains(query, true) },
            onDismiss = { showMajorPicker = false },
            onSelect = { item: CourseCatalogMajor ->
                selectedMajor = item
                selectedCourse = null
                showMajorPicker = false
            }
        )
    }

    if (showCoursePicker && selectedMajor != null) {
        CatalogItemPickerDialog(
            title = "Select Course",
            items = selectedMajor!!.courses,
            itemLabel = { item: CourseCatalogCourse -> "${item.code} - ${item.title}" },
            searchMatcher = { item: CourseCatalogCourse, query: String -> item.title.contains(query, true) || item.code.contains(query, true) },
            onDismiss = { showCoursePicker = false },
            onSelect = { item: CourseCatalogCourse ->
                selectedCourse = item
                showCoursePicker = false
            }
        )
    }

    if (showStartTimePicker) TimePickerDialog(startTime, { showStartTimePicker = false }, { startTime = it; showStartTimePicker = false })
    if (showEndTimePicker) TimePickerDialog(endTime, { showEndTimePicker = false }, { endTime = it; showEndTimePicker = false })
    if (showStartTimePicker2) TimePickerDialog(startTime2, { showStartTimePicker2 = false }, { startTime2 = it; showStartTimePicker2 = false })
    if (showEndTimePicker2) TimePickerDialog(endTime2, { showEndTimePicker2 = false }, { endTime2 = it; showEndTimePicker2 = false })
}

@Composable
fun <T> CatalogItemPickerDialog(
    title: String,
    items: List<T>,
    itemLabel: (T) -> String,
    searchMatcher: (T, String) -> Boolean,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items
        else items.filter { searchMatcher(it, searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (items.isEmpty()) {
                    Text("No items available.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(items = filteredItems) { item ->
                            Text(
                                text = itemLabel(item),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DayChipRows(
    selectedDays: List<Int>,
    onToggleDay: (Int) -> Unit
) {
    val dayOptions = listOf(
        1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayOptions.forEach { (value, label) ->
            val isSelected = value in selectedDays
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CoralRed else Color.Transparent)
                    .border(1.dp, if (isSelected) CoralRed else Color.LightGray.copy(alpha = 0.3f), CircleShape)
                    .clickable { onToggleDay(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.Gray,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.clickable {
        val parsedDate = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        }, parsedDate.year, parsedDate.monthValue - 1, parsedDate.dayOfMonth).show()
    }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = CoralRed.copy(alpha = 0.5f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = Color.Gray
            )
        )
    }
}

@Composable
fun SavedClassCard(
    selectedClass: SelectedClass,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${selectedClass.courseCode} - ${selectedClass.courseTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTimeForDisplay(selectedClass.startTime)} - ${formatTimeForDisplay(selectedClass.endTime)} | ${selectedClass.meetingPatternLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatTimeForDisplay(timeText: String): String = runCatching {
    val parsedTime = LocalTime.parse(timeText)
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    parsedTime.format(formatter)
}.getOrDefault(timeText)

fun colorForCourse(courseCode: String): Int {
    val palette = listOf(
        0xFFEF3347.toInt(), 0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(),
        0xFFFFA726.toInt(), 0xFF8E24AA.toInt(), 0xFF42A5F5.toInt()
    )
    val index = kotlin.math.abs(courseCode.hashCode()) % palette.size
    return palette[index]
}

@Composable
fun IncomingInvitesSection(invites: List<EventInvite>, onAccept: (EventInvite) -> Unit, onDecline: (EventInvite) -> Unit, onInviteClick: (EventInvite) -> Unit) {
    SectionCard(Modifier.border(1.dp, CoralRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))) {
        SectionHeading("Invitations (${invites.size})")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            invites.forEach { invite ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onInviteClick(invite) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(invite.eventTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("from ${invite.fromDisplayName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row {
                            IconButton(onClick = { onAccept(invite) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDecline(invite) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CoralRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendPickerDialog(
    eventId: String? = null,
    onDismiss: () -> Unit,
    onInvite: (UserProfile) -> Unit
) {
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(eventId) {
        if (currentUser != null) {
            SocialRepository.fetchNicknames(currentUser.uid, { nicknames = it }, {})
            
            SocialRepository.fetchAcceptedFriends(
                currentUserId = currentUser.uid,
                onSuccess = { allFriends ->
                    if (eventId != null) {
                        SocialRepository.fetchInvitedUserIds(eventId,
                            onSuccess = { invitedIds ->
                                friends = allFriends.filter { friend -> !invitedIds.contains(friend.uid) }
                                isLoading = false
                            },
                            onError = {
                                friends = allFriends
                                isLoading = false
                            }
                        )
                    } else {
                        friends = allFriends
                        isLoading = false
                    }
                },
                onError = { isLoading = false }
            )
        }
    }

    val filteredFriends = remember(friends, nicknames, searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter { friend ->
            val nickname = nicknames[friend.uid] ?: ""
            friend.displayName.contains(searchQuery, ignoreCase = true) ||
            friend.email.contains(searchQuery, ignoreCase = true) ||
            nickname.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        },
        title = { Text("Invite a Friend", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search friends...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoralRed)
                    }
                } else if (friends.isEmpty()) {
                    Text(if (eventId != null) "All eligible friends are already invited." else "You don't have any friends to invite.", 
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else if (filteredFriends.isEmpty()) {
                    Text("No friends match \"$searchQuery\"", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filteredFriends, key = { it.uid }) { friend ->
                            val nickname = nicknames[friend.uid]
                            val baseName = SocialRepository.displayNameOrEmail(friend)
                            val displayLabel = nickname ?: baseName

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onInvite(friend) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = friend.photoUrl)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (nickname != null) {
                                        Text(
                                            text = "($baseName)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = friend.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun EventMembersDialog(
    event: CalendarEvent,
    currentUserId: String,
    onDismiss: () -> Unit,
    onKick: (List<String>) -> Unit
) {
    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var nicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    val isOwner = event.ownerId == currentUserId

    val otherMembers = remember(members, event.ownerId) {
        members.filter { it.uid != event.ownerId }
    }

    LaunchedEffect(currentUserId) {
        SocialRepository.fetchNicknames(currentUserId, { nicknames = it }, {})
    }

    DisposableEffect(event.id) {
        val registration = SocialRepository.listenToEventMembers(
            eventId = event.id,
            onMembersChanged = { newMembers ->
                members = newMembers
                isLoading = false
            },
            onError = { isLoading = false }
        )
        onDispose { registration.remove() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Event Members",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandRedDark
                        )
                        Text(
                            text = "${members.size + 1} members total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandRedDark, strokeWidth = 3.dp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Leader Section
                        item {
                            MemberGroupHeader(text = "Host")
                            PremiumMemberRow(
                                userId = event.ownerId ?: "",
                                isOwner = true,
                                currentUserId = currentUserId,
                                eventId = event.id,
                                nickname = nicknames[event.ownerId]
                            )
                        }
                        
                        // Others Section
                        if (otherMembers.isNotEmpty() || !isOwner) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                MemberGroupHeader(text = "Participants")
                            }
                        }

                        if (otherMembers.isEmpty() && !isOwner) {
                            item { 
                                EmptyMembersPlaceholder()
                            }
                        } else {
                            items(otherMembers, key = { it.uid }) { member ->
                                PremiumMemberRow(
                                    userId = member.uid,
                                    displayName = SocialRepository.displayNameOrEmail(member),
                                    photoUrl = member.photoUrl,
                                    isOwner = false,
                                    canKick = isOwner,
                                    isSelected = selectedUsers.contains(member.uid),
                                    onToggleSelect = {
                                        selectedUsers = if (selectedUsers.contains(member.uid)) {
                                            selectedUsers - member.uid
                                        } else {
                                            selectedUsers + member.uid
                                        }
                                    },
                                    currentUserId = currentUserId,
                                    eventId = event.id,
                                    nickname = nicknames[member.uid]
                                )
                            }
                        }
                    }
                }

                if (isOwner && otherMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val canKickSelected = selectedUsers.isNotEmpty()
                    
                    Button(
                        onClick = { if (canKickSelected) onKick(selectedUsers.toList()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(if (canKickSelected) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canKickSelected) BrandRedDark else Color.Gray.copy(alpha = 0.2f),
                            contentColor = if (canKickSelected) Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canKickSelected
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canKickSelected) {
                                Icon(Icons.Default.PersonRemove, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Remove selected (${selectedUsers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            } else {
                                Text("Select members to remove", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberGroupHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyMembersPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Group, 
                contentDescription = null, 
                modifier = Modifier.size(48.dp), 
                tint = Color.Gray.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No other members yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PremiumMemberRow(
    userId: String,
    displayName: String = "Loading...",
    photoUrl: String = "",
    isOwner: Boolean,
    canKick: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    currentUserId: String,
    eventId: String,
    nickname: String? = null
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            SocialRepository.fetchUserProfile(userId, { profile = it }, {})
        }
    }

    val baseName = profile?.let { SocialRepository.displayNameOrEmail(it) } ?: displayName
    val displayLabel = nickname ?: baseName
    val photo = profile?.photoUrl ?: photoUrl
    val isSelectable = canKick && !isOwner && userId != currentUserId
    val isMe = userId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isSelectable) { onToggleSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandRedDark.copy(alpha = 0.05f) else Color.Transparent
        ),
        border = if (isSelected) BorderStroke(1.dp, BrandRedDark.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                UserAvatar(photoUrl = photo)
                if (isOwner) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(CustomEventOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "$displayLabel (You)" else displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isOwner || isMe) FontWeight.Bold else FontWeight.Medium,
                    color = if (isMe) BrandRedDark else Color.DarkGray
                )
                if (nickname != null && baseName != "Loading...") {
                    Text(
                        text = "($baseName)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (isOwner) {
                    Text("Event Host", style = MaterialTheme.typography.bodySmall, color = CustomEventOrange)
                } else if (isMe) {
                    Text("You", style = MaterialTheme.typography.bodySmall, color = BrandRedDark.copy(alpha = 0.7f))
                }
            }

            if (isSelectable) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandRedDark,
                        uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            } else if (isOwner) {
                Icon(
                    Icons.Default.Shield, 
                    contentDescription = "Admin", 
                    tint = CustomEventOrange.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AddEventContent(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime, List<UserProfile>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var invitedFriends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showFriendPicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val timeError = startTime.isAfter(endTime) || startTime == endTime

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("New Event", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CoralRed)
        
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTimeChip(label = "Start", time = startTime, isSelected = true, onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
            CompactTimeChip(label = "End", time = endTime, isSelected = true, isError = timeError, onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
        }
        if (timeError) {
            Text("End time must be after start time.", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
        }

        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 1)
        
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Invites (${invitedFriends.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showFriendPicker = true }) { Text("+ Add Friends", color = SharedEventBlue, fontSize = 12.sp) }
            }
            invitedFriends.forEach { friend ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    UserAvatar(photoUrl = friend.photoUrl)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(friend.displayName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { invitedFriends = invitedFriends.filter { it.uid != friend.uid } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = Color.Gray)
            }
            Button(
                onClick = { onConfirm(title, description, location, startTime, endTime, invitedFriends) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = title.isNotBlank() && !timeError,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Event")
            }
        }
    }

    if (showStartTimePicker) TimePickerDialog(startTime, { showStartTimePicker = false }, { startTime = it; showStartTimePicker = false })
    if (showEndTimePicker) TimePickerDialog(endTime, { showEndTimePicker = false }, { endTime = it; showEndTimePicker = false })
    if (showFriendPicker) {
        FriendPickerDialog(onDismiss = { showFriendPicker = false }, onInvite = { friend -> if (!invitedFriends.any { it.uid == friend.uid }) invitedFriends += friend; showFriendPicker = false })
    }
}

@Composable
fun EditEventContent(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, LocalTime, LocalTime) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var desc by remember { mutableStateOf(event.description ?: "") }
    var loc by remember { mutableStateOf(event.location ?: "") }
    var start by remember { mutableStateOf(event.start.toLocalTime()) }
    var end by remember { mutableStateOf(event.endExclusive.toLocalTime()) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    val timeError = start.isAfter(end) || start == end

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Edit Event", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CoralRed)
        
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTimeChip("Start", start, { showStart = true }, modifier = Modifier.weight(1f), isSelected = true)
            CompactTimeChip("End", end, { showEnd = true }, modifier = Modifier.weight(1f), isSelected = true, isError = timeError)
        }
        if (timeError) {
            Text("End time must be after start time.", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
        }

        OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = Color.Gray)
            }
            Button(
                onClick = { onConfirm(title, desc, loc, start, end) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = title.isNotBlank() && !timeError,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        }
    }

    if (showStart) TimePickerDialog(start, { showStart = false }, { start = it; showStart = false })
    if (showEnd) TimePickerDialog(end, { showEnd = false }, { end = it; showEnd = false })
}

@Composable
fun InviteDetailDialog(
    invite: EventInvite,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = CoralRed)
                Spacer(Modifier.width(12.dp))
                Text(invite.eventTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("From: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(invite.fromDisplayName, style = MaterialTheme.typography.bodyMedium, color = SharedEventBlue)
                }
                
                val start = ZonedDateTime.parse(invite.eventStart)
                val end = ZonedDateTime.parse(invite.eventEnd)
                val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text(start.format(dayFormatter), style = MaterialTheme.typography.bodySmall)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("${start.format(timeFormatter)} - ${end.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall)
                }
                
                if (!invite.eventLocation.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text(invite.eventLocation!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (!invite.eventDescription.isNullOrBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(HtmlUtils.stripHtml(invite.eventDescription!!), style = MaterialTheme.typography.bodyMedium)
                }
                
                if (invite.isPinnedByLeader) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = PinnedEventPurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Outlined.PushPin, contentDescription = null, tint = PinnedEventPurple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("The leader has this event pinned", style = MaterialTheme.typography.labelSmall, color = PinnedEventPurple, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)) {
                Text("Accept", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}
