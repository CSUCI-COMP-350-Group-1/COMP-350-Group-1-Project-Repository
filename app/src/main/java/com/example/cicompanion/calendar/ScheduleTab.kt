package com.example.cicompanion.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicompanion.calendar.data.repository.CourseCatalogRepository
import com.example.cicompanion.calendar.model.CourseCatalogMajor
import com.example.cicompanion.calendar.model.SelectedClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

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
    
    var showAddClassDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<SelectedClass?>(null) }
    var classToDelete by remember { mutableStateOf<SelectedClass?>(null) }
    var classToShowDetails by remember { mutableStateOf<SelectedClass?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            if (viewModel.selectedClasses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No classes added yet.", color = Color.Gray)
                        Text("Tap 'Add Class' to add your first class.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(viewModel.selectedClasses, key = { it.id }) { savedClass ->
                        SavedClassCard(
                            selectedClass = savedClass,
                            onClick = { classToShowDetails = savedClass }
                        )
                    }
                }
            }
        }
    }

    if (classToShowDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { classToShowDetails = null },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ClassDetailsContent(
                selectedClass = classToShowDetails!!,
                onEdit = {
                    classToEdit = classToShowDetails
                    classToShowDetails = null
                },
                onDelete = {
                    classToDelete = classToShowDetails
                    classToShowDetails = null
                }
            )
        }
    }

    if (showAddClassDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddClassDialog = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            AddClassContent(
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
    }

    classToEdit?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { classToEdit = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            AddClassContent(
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
    }

    classToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Remove Class") },
            text = { Text("Are you sure you want to remove ${item.courseCode} from your schedule?") },
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
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
