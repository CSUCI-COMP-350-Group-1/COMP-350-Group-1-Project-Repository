package com.example.cicompanion.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicompanion.calendar.data.repository.CourseCatalogRepository
import com.example.cicompanion.calendar.model.CourseCatalogCourse
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.SelectedClass
import com.example.cicompanion.ui.theme.AppBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

// UPDATED FILE:
// - Better dropdown menus using ExposedDropdownMenuBox
// - Easier native date/time pickers
// - Removed reminder placeholder text
// - Keeps schedule save/edit/delete behavior

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTab(viewModel: CalendarViewModel) {
    val currentUser = rememberScheduleAuthUser()
    val context = LocalContext.current
    val repository = remember(context) { CourseCatalogRepository(context.applicationContext) }

    var majors by remember { mutableStateOf<List<CourseCatalogMajor>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(true) }
    var catalogError by remember { mutableStateOf<String?>(null) }

    var selectedMajor by remember { mutableStateOf<CourseCatalogMajor?>(null) }
    var selectedCourse by remember { mutableStateOf<CourseCatalogCourse?>(null) }

    val selectedDays = remember { mutableStateListOf<Int>() }

    // CALENDAR SCHEDULE CHANGE:
    // Keep persisted values as strings, but edit them through easy native pickers.
    var startTimeText by rememberSaveable { mutableStateOf("09:00") }
    var endTimeText by rememberSaveable { mutableStateOf("10:15") }
    var startDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateText by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(4).toString()) }
    var locationText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var termLabelText by rememberSaveable { mutableStateOf("") }

    var editingClassId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCreatedAt by rememberSaveable { mutableStateOf(0L) }

    var formError by remember { mutableStateOf<String?>(null) }
    var formMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var majorMenuExpanded by remember { mutableStateOf(false) }
    var courseMenuExpanded by remember { mutableStateOf(false) }

    val availableCourses = selectedMajor?.courses.orEmpty()

    fun clearForm() {
        selectedMajor = null
        selectedCourse = null
        selectedDays.clear()
        startTimeText = "09:00"
        endTimeText = "10:15"
        startDateText = LocalDate.now().toString()
        endDateText = LocalDate.now().plusMonths(4).toString()
        locationText = ""
        notesText = ""
        termLabelText = ""
        editingClassId = null
        editingCreatedAt = 0L
        formError = null
        formMessage = null
    }

    fun populateForm(item: SelectedClass) {
        val major = majors.firstOrNull { it.code == item.majorCode }
        val course = major?.courses?.firstOrNull { it.code == item.courseCode }
            ?: CourseCatalogCourse(
                code = item.courseCode,
                title = item.courseTitle,
                typicallyOffered = item.typicallyOffered
            )

        selectedMajor = major ?: CourseCatalogMajor(
            code = item.majorCode,
            name = item.majorName,
            courses = listOf(course)
        )
        selectedCourse = course

        selectedDays.clear()
        selectedDays.addAll(item.daysOfWeek.sorted())

        startTimeText = item.startTime
        endTimeText = item.endTime
        startDateText = item.startDate
        endDateText = item.endDate
        locationText = item.location
        notesText = item.notes
        termLabelText = item.termLabel
        editingClassId = item.id
        editingCreatedAt = item.createdAt
        formError = null
        formMessage = null
    }

    LaunchedEffect(Unit) {
        isLoadingCatalog = true
        catalogError = null

        runCatching {
            repository.loadMajors()
        }.onSuccess { loadedMajors ->
            majors = loadedMajors.sortedBy { it.code }
        }.onFailure { error ->
            catalogError = error.message ?: "Failed to load course catalog."
        }

        isLoadingCatalog = false
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.loadSelectedClasses()
        } else {
            clearForm()
        }
    }

    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Please sign in to manage your schedule.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Class Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (catalogError != null) {
            item {
                ScheduleMessageCard(
                    text = catalogError!!,
                    isError = true
                )
            }
        }

        if (formError != null) {
            item {
                ScheduleMessageCard(
                    text = formError!!,
                    isError = true
                )
            }
        }

        if (formMessage != null) {
            item {
                ScheduleMessageCard(
                    text = formMessage!!,
                    isError = false
                )
            }
        }

        item {
            Text(
                text = "Saved Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (viewModel.selectedClasses.isEmpty()) {
            item {
                ScheduleSectionCard {
                    Text("No saved classes yet.")
                }
            }
        } else {
            items(viewModel.selectedClasses, key = { it.id }) { savedClass ->
                SavedClassCard(
                    selectedClass = savedClass,
                    onEdit = { populateForm(savedClass) },
                    onDelete = {
                        viewModel.deleteSelectedClass(savedClass.id) { success ->
                            formMessage = if (success) {
                                "Class removed."
                            } else {
                                "Could not remove class."
                            }
                        }
                    }
                )
            }
        }

        item {
            Text(
                text = if (editingClassId == null) "Add Class" else "Edit Class",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ScheduleSectionCard {
                if (isLoadingCatalog) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // CALENDAR SCHEDULE CHANGE:
                    // Better dropdown for majors.
                    ExposedDropdownMenuBox(
                        expanded = majorMenuExpanded,
                        onExpandedChange = { majorMenuExpanded = !majorMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMajor?.let { "${it.code} - ${it.name}" } ?: "",
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            label = { Text("Major") },
                            placeholder = { Text("Select major") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorMenuExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = majorMenuExpanded,
                            onDismissRequest = { majorMenuExpanded = false }
                        ) {
                            majors.forEach { major ->
                                DropdownMenuItem(
                                    text = { Text("${major.code} - ${major.name}") },
                                    onClick = {
                                        selectedMajor = major
                                        selectedCourse = null
                                        majorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CALENDAR SCHEDULE CHANGE:
                    // Better dropdown for courses.
                    ExposedDropdownMenuBox(
                        expanded = courseMenuExpanded,
                        onExpandedChange = {
                            if (selectedMajor != null) {
                                courseMenuExpanded = !courseMenuExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse?.let { "${it.code} - ${it.title}" } ?: "",
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            enabled = selectedMajor != null,
                            label = { Text("Course") },
                            placeholder = { Text("Select course") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseMenuExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = courseMenuExpanded,
                            onDismissRequest = { courseMenuExpanded = false }
                        ) {
                            availableCourses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code} - ${course.title}") },
                                    onClick = {
                                        selectedCourse = course
                                        courseMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Meeting Days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DayChipRows(
                        selectedDays = selectedDays,
                        onToggleDay = { dayValue ->
                            if (dayValue in selectedDays) {
                                selectedDays.remove(dayValue)
                            } else {
                                selectedDays.add(dayValue)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // CALENDAR SCHEDULE CHANGE:
                    // Easier time selection using native time pickers.
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimePickerField(
                            label = "Start Time",
                            value = startTimeText,
                            modifier = Modifier.weight(1f),
                            onTimeSelected = { startTimeText = it }
                        )

                        TimePickerField(
                            label = "End Time",
                            value = endTimeText,
                            modifier = Modifier.weight(1f),
                            onTimeSelected = { endTimeText = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CALENDAR SCHEDULE CHANGE:
                    // Easier date selection using native date pickers.
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = termLabelText,
                        onValueChange = { termLabelText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Term Label") },
                        placeholder = { Text("Fall 2026") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location") },
                        placeholder = { Text("Broome 2480") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes") },
                        placeholder = { Text("Optional") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                formError = null
                                formMessage = null

                                val major = selectedMajor
                                val course = selectedCourse

                                if (major == null || course == null) {
                                    formError = "Choose a major and a course."
                                    return@Button
                                }

                                if (selectedDays.isEmpty()) {
                                    formError = "Choose at least one meeting day."
                                    return@Button
                                }

                                val startDate = runCatching { LocalDate.parse(startDateText) }.getOrNull()
                                val endDate = runCatching { LocalDate.parse(endDateText) }.getOrNull()
                                val startTime = runCatching { LocalTime.parse(startTimeText) }.getOrNull()
                                val endTime = runCatching { LocalTime.parse(endTimeText) }.getOrNull()

                                if (startDate == null || endDate == null) {
                                    formError = "Choose valid dates."
                                    return@Button
                                }

                                if (startTime == null || endTime == null) {
                                    formError = "Choose valid times."
                                    return@Button
                                }

                                if (endDate.isBefore(startDate)) {
                                    formError = "End date must be on or after start date."
                                    return@Button
                                }

                                if (!endTime.isAfter(startTime)) {
                                    formError = "End time must be after start time."
                                    return@Button
                                }

                                isSaving = true
                                val now = System.currentTimeMillis()

                                val selectedClass = SelectedClass(
                                    id = editingClassId ?: UUID.randomUUID().toString(),
                                    majorCode = major.code,
                                    majorName = major.name,
                                    courseCode = course.code,
                                    courseTitle = course.title,
                                    typicallyOffered = course.typicallyOffered,
                                    daysOfWeek = selectedDays.sorted(),
                                    startTime = startTimeText,
                                    endTime = endTimeText,
                                    startDate = startDateText,
                                    endDate = endDateText,
                                    location = locationText.trim(),
                                    notes = notesText.trim(),
                                    termLabel = termLabelText.trim(),
                                    colorArgb = colorForCourse(course.code),
                                    reminderEnabled = false,
                                    reminderMinutesBefore = null,
                                    createdAt = if (editingClassId == null) now else editingCreatedAt,
                                    updatedAt = now
                                )

                                viewModel.saveSelectedClass(selectedClass) { success ->
                                    isSaving = false
                                    if (success) {
                                        formMessage = if (editingClassId == null) {
                                            "Class saved."
                                        } else {
                                            "Class updated."
                                        }
                                        clearForm()
                                    } else {
                                        formError = "Could not save class."
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFEF3347)
                            )
                        ) {
                            Text(if (editingClassId == null) "Save Class" else "Update Class")
                        }

                        if (editingClassId != null) {
                            OutlinedButton(
                                onClick = { clearForm() },
                                enabled = !isSaving
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChipRows(
    selectedDays: List<Int>,
    onToggleDay: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DAY_OPTIONS.take(4).forEach { day ->
                FilterChip(
                    selected = day.value in selectedDays,
                    onClick = { onToggleDay(day.value) },
                    label = { Text(day.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DAY_OPTIONS.drop(4).forEach { day ->
                FilterChip(
                    selected = day.value in selectedDays,
                    onClick = { onToggleDay(day.value) },
                    label = { Text(day.label) }
                )
            }
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

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = {
                showNativeDatePicker(
                    context = context,
                    initialDate = value,
                    onDatePicked = onDateSelected
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (value.isBlank()) "Select date" else value)
        }
    }
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = {
                showNativeTimePicker(
                    context = context,
                    initialTime = value,
                    onTimePicked = onTimeSelected
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (value.isBlank()) "Select time" else value)
        }
    }
}

private fun showNativeDatePicker(
    context: Context,
    initialDate: String,
    onDatePicked: (String) -> Unit
) {
    val parsedDate = runCatching { LocalDate.parse(initialDate) }.getOrElse { LocalDate.now() }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            onDatePicked(selectedDate.toString())
        },
        parsedDate.year,
        parsedDate.monthValue - 1,
        parsedDate.dayOfMonth
    ).show()
}

private fun showNativeTimePicker(
    context: Context,
    initialTime: String,
    onTimePicked: (String) -> Unit
) {
    val parsedTime = runCatching { LocalTime.parse(initialTime) }.getOrElse { LocalTime.of(9, 0) }

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimePicked(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
        },
        parsedTime.hour,
        parsedTime.minute,
        false
    ).show()
}

@Composable
private fun ScheduleSectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun ScheduleMessageCard(
    text: String,
    isError: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

@Composable
private fun SavedClassCard(
    selectedClass: SelectedClass,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ScheduleSectionCard {
        Text(
            text = "${selectedClass.courseCode} - ${selectedClass.courseTitle}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text("${selectedClass.majorCode} - ${selectedClass.majorName}")
        Text("Days: ${selectedClass.meetingPatternLabel()}")
        Text(
            "Time: ${formatTimeForDisplay(selectedClass.startTime)} - ${formatTimeForDisplay(selectedClass.endTime)}"
        )
        Text("Dates: ${selectedClass.startDate} to ${selectedClass.endDate}")

        if (selectedClass.location.isNotBlank()) {
            Text("Location: ${selectedClass.location}")
        }

        if (selectedClass.termLabel.isNotBlank()) {
            Text("Term: ${selectedClass.termLabel}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFEF3347)
                )
            ) {
                Text("Edit")
            }

            OutlinedButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

private data class DayOption(
    val value: Int,
    val label: String
)

private val DAY_OPTIONS = listOf(
    DayOption(1, "Mon"),
    DayOption(2, "Tue"),
    DayOption(3, "Wed"),
    DayOption(4, "Thu"),
    DayOption(5, "Fri"),
    DayOption(6, "Sat"),
    DayOption(7, "Sun")
)

// Convert stored HH:mm values to a 12-hour display like 9:00 AM.
private fun formatTimeForDisplay(timeText: String): String {
    return runCatching {
        val parsedTime = java.time.LocalTime.parse(timeText)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        parsedTime.format(formatter)
    }.getOrDefault(timeText)
}
private fun colorForCourse(courseCode: String): Int {
    val palette = listOf(
        0xFFEF3347.toInt(),
        0xFF5C6BC0.toInt(),
        0xFF26A69A.toInt(),
        0xFFFFA726.toInt(),
        0xFF8E24AA.toInt(),
        0xFF42A5F5.toInt()
    )

    val index = kotlin.math.abs(courseCode.hashCode()) % palette.size
    return palette[index]
}