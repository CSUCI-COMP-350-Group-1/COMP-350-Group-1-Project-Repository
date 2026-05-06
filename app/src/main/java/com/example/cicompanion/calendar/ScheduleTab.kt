package com.example.cicompanion.calendar

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cicompanion.calendar.data.repository.CourseCatalogRepository
import com.example.cicompanion.calendar.model.CourseCatalogCourse
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.SelectedClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
private fun rememberScheduleAuthUser(): FirebaseUser? {
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    return currentUser
}

@Composable
fun ScheduleTab(viewModel: CalendarViewModel) {
    val currentUser = rememberScheduleAuthUser()
    val context = LocalContext.current
    val repository = remember(context) { CourseCatalogRepository(context.applicationContext) }

    var majors by remember { mutableStateOf<List<CourseCatalogMajor>>(emptyList()) }
    
    var showAddClassDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<SelectedClass?>(null) }
    var classToDelete by remember { mutableStateOf<SelectedClass?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            repository.loadMajors()
        }.onSuccess { loadedMajors ->
            majors = loadedMajors.sortedBy { it.code }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.loadSelectedClasses()
        }
    }

    if (currentUser == null) {
        @Suppress("DEPRECATION")
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("Please sign in to manage your schedule.")
        }
        return
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddClassDialog = true },
                containerColor = CoralRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp),
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                                .padding(1.dp)
                                .background(CoralRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                },
                text = { Text("Add Class", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CalendarHeroHeader(
                title = "Class Schedule",
                subtitle = "Manage your weekly classes and semester schedule.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.selectedClasses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    @Suppress("DEPRECATION")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No classes added yet.", color = Color.Gray)
                        Text("Tap '+' to add your first class.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(viewModel.selectedClasses, key = { it.id }) { savedClass ->
                        SavedClassCard(
                            selectedClass = savedClass,
                            onEdit = { classToEdit = savedClass },
                            onDelete = { classToDelete = savedClass }
                        )
                    }
                }
            }
        }
    }

    if (showAddClassDialog) {
        AddClassDialog(
            majors = majors,
            onDismiss = { showAddClassDialog = false },
            onConfirm = { selectedClass ->
                viewModel.saveSelectedClass(selectedClass) { success ->
                    if (success) {
                        showAddClassDialog = false
                        Toast.makeText(context, "Class added successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to add class.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    classToEdit?.let { item ->
        AddClassDialog(
            majors = majors,
            editingClass = item,
            onDismiss = { classToEdit = null },
            onConfirm = { updatedClass ->
                viewModel.saveSelectedClass(updatedClass) { success ->
                    if (success) {
                        classToEdit = null
                        Toast.makeText(context, "Class updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update class.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    classToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { @Suppress("DEPRECATION") Text("Remove Class") },
            text = { @Suppress("DEPRECATION") Text("Are you sure you want to remove ${item.courseCode} from your schedule?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedClass(item.id) { success ->
                            if (success) {
                                classToDelete = null
                                Toast.makeText(context, "Class removed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    @Suppress("DEPRECATION")
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    @Suppress("DEPRECATION")
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AddClassDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            @Suppress("DEPRECATION")
            Text(
                text = if (editingClass == null) "New Class" else "Edit Class",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CoralRed
            )
        },
        text = {
            @Suppress("DEPRECATION")
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showMajorPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = selectedMajor?.let { "${it.code} - ${it.name}" } ?: "Select Major",
                            color = if (selectedMajor == null) Color.Gray else CoralRed,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { showCoursePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedMajor != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed, disabledContentColor = Color.Gray),
                        border = BorderStroke(1.dp, if (selectedMajor != null) CoralRed.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text(
                                text = selectedCourse?.let { "${it.code} - ${it.title}" } ?: "Select Course",
                                color = if (selectedCourse == null) Color.Gray else CoralRed,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                maxLines = 1
                            )
                        }
                    }

                    if (selectedCourse != null) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = buildString {
                                append("Information: ")
                                append(selectedCourse!!.typicallyOffered)
                                if (locationText.isNotBlank()) {
                                    append(" - ")
                                    append(locationText)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Column {
                    @Suppress("DEPRECATION")
                    Text("Meeting Days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DayChipRows(
                        selectedDays = selectedDays,
                        onToggleDay = { dayValue ->
                            if (dayValue in selectedDays) selectedDays.remove(dayValue)
                            else selectedDays.add(dayValue)
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    @Suppress("DEPRECATION")
                    Text("Time Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = startTime, onTimeChange = { startTime = it }) }
                        @Suppress("DEPRECATION")
                        Text("to", Modifier.padding(horizontal = 8.dp))
                        Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = endTime, onTimeChange = { endTime = it }) }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    @Suppress("DEPRECATION")
                    Text("Add 2nd Time Range (e.g. Lab)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = hasSecondTimeRange, 
                        onCheckedChange = { hasSecondTimeRange = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralRed,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    )
                }

                if (hasSecondTimeRange) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        @Suppress("DEPRECATION")
                        Text("2nd Meeting Days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        DayChipRows(
                            selectedDays = selectedDays2,
                            onToggleDay = { dayValue ->
                                if (dayValue in selectedDays2) selectedDays2.remove(dayValue)
                                else selectedDays2.add(dayValue)
                            }
                        )
                        
                        @Suppress("DEPRECATION")
                        Text("2nd Time Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = startTime2, onTimeChange = { startTime2 = it }) }
                            @Suppress("DEPRECATION")
                            Text("to", Modifier.padding(horizontal = 8.dp))
                            Box(Modifier.weight(1f)) { WheelTimePicker(initialTime = endTime2, onTimeChange = { endTime2 = it }) }
                        }
                        
                        OutlinedTextField(
                            value = location2Text,
                            onValueChange = { location2Text = it },
                            label = { Text("2nd Location (e.g. Lab Room)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (selectedCourse != null) {
                            @Suppress("DEPRECATION")
                            Text(
                                text = buildString {
                                    append("Information: ")
                                    append(selectedCourse!!.typicallyOffered)
                                    if (location2Text.isNotBlank()) {
                                        append(" - ")
                                        append(location2Text)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = notes2Text,
                            onValueChange = { notes2Text = it },
                            label = { Text("Notes for 2nd Time (e.g. Lab Section)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePickerField(
                        label = "Start Date",
                        value = startDateText,
                        modifier = Modifier.weight(1f),
                        onDateSelected = { startDateText = it }
                    )
                    DatePickerField(
                        label = "End Date",
                        value = endDateText,
                        modifier = Modifier.weight(1f),
                        onDateSelected = { endDateText = it }
                    )
                }

                OutlinedTextField(
                    value = termLabelText,
                    onValueChange = { termLabelText = it },
                    label = { Text("Term (e.g. Fall 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("General Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            @Suppress("DEPRECATION")
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
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = selectedMajor != null && selectedCourse != null && selectedDays.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                @Suppress("DEPRECATION")
                Text(if (editingClass == null) "Add Class" else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            @Suppress("DEPRECATION")
            TextButton(onClick = onDismiss) {
                @Suppress("DEPRECATION")
                Text("Back", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showMajorPicker) {
        CatalogItemPickerDialog(
            title = "Select Major",
            items = majors,
            itemLabel = { "${it.code} - ${it.name}" },
            searchMatcher = { item, query -> item.name.contains(query, true) || item.code.contains(query, true) },
            onDismiss = { showMajorPicker = false },
            onSelect = {
                selectedMajor = it
                selectedCourse = null
                showMajorPicker = false
            }
        )
    }

    if (showCoursePicker && selectedMajor != null) {
        CatalogItemPickerDialog(
            title = "Select Course",
            items = selectedMajor!!.courses,
            itemLabel = { "${it.code} - ${it.title}" },
            searchMatcher = { item, query -> item.title.contains(query, true) || item.code.contains(query, true) },
            onDismiss = { showCoursePicker = false },
            onSelect = {
                selectedCourse = it
                showCoursePicker = false
            }
        )
    }
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
        title = { @Suppress("DEPRECATION") Text(title, fontWeight = FontWeight.Bold) },
        text = {
            @Suppress("DEPRECATION")
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (items.isEmpty()) {
                    @Suppress("DEPRECATION")
                    Text("No items available.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(filteredItems) { item ->
                            @Suppress("DEPRECATION")
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
            @Suppress("DEPRECATION")
            TextButton(onClick = onDismiss) {
                @Suppress("DEPRECATION")
                Text("Cancel", color = CoralRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DayChipRows(
    selectedDays: List<Int>,
    onToggleDay: (Int) -> Unit
) {
    val dayOptions = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
    )
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayOptions.forEach { (value, label) ->
            val isSelected = value in selectedDays
            FilterChip(
                selected = isSelected,
                onClick = { onToggleDay(value) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoralRed,
                    selectedLabelColor = Color.White,
                    labelColor = CoralRed
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = CoralRed.copy(alpha = 0.5f),
                    selectedBorderColor = CoralRed
                )
            )
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    Column(modifier = modifier) {
        @Suppress("DEPRECATION")
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SoftText)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                val parsedDate = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
                }, parsedDate.year, parsedDate.monthValue - 1, parsedDate.dayOfMonth).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
            border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f))
        ) {
            Text(value, color = CoralRed)
        }
    }
}

@Composable
private fun SavedClassCard(
    selectedClass: SelectedClass,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        @Suppress("DEPRECATION")
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "${selectedClass.courseCode} - ${selectedClass.courseTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    @Suppress("DEPRECATION")
                    Text(
                        text = selectedClass.majorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed)
                    }
                }
            }
            
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = "${formatTimeForDisplay(selectedClass.startTime)} - ${formatTimeForDisplay(selectedClass.endTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = selectedClass.meetingPatternLabel(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (selectedClass.hasSecondTimeRange) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoralRed.copy(alpha = 0.05f))
                        .border(1.dp, CoralRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(14.dp), tint = CoralRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = if (selectedClass.notes2.isNotBlank()) selectedClass.notes2 else "Secondary Meeting / Lab",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = selectedClass.daysOfWeek2.sorted().joinToString(" ") { day ->
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
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = "${formatTimeForDisplay(selectedClass.startTime2)} - ${formatTimeForDisplay(selectedClass.endTime2)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (selectedClass.location2.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(6.dp))
                            @Suppress("DEPRECATION")
                            Text(text = selectedClass.location2, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (selectedClass.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    @Suppress("DEPRECATION")
                    Text(text = selectedClass.location, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedClass.termLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoralRed.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        @Suppress("DEPRECATION")
                        Text(selectedClass.termLabel, color = CoralRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedClass.notes.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = selectedClass.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeForDisplay(timeText: String): String {
    return runCatching {
        val parsedTime = LocalTime.parse(timeText)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        parsedTime.format(formatter)
    }.getOrDefault(timeText)
}

private fun colorForCourse(courseCode: String): Int {
    val palette = listOf(
        0xFFEF3347.toInt(), 0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(),
        0xFFFFA726.toInt(), 0xFF8E24AA.toInt(), 0xFF42A5F5.toInt()
    )
    val index = kotlin.math.abs(courseCode.hashCode()) % palette.size
    return palette[index]
}
