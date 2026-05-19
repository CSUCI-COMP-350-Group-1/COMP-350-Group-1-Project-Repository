package com.example.cicompanion.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.CoralRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    var showReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Account") {
                SettingsClickableItem(
                    title = "Edit Profile",
                    subtitle = "Change your display name, bio, and photo",
                    icon = Icons.Default.Person,
                    onClick = { navController.navigate(Routes.EDIT_PROFILE) }
                )
            }

            SettingsSection(title = "Appearance") {
                SettingsSwitchItem(
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark themes (Experimental)",
                    icon = Icons.Default.DarkMode,
                    checked = viewModel.isDarkMode,
                    onCheckedChange = { viewModel.updateDarkMode(it) }
                )
            }

            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    title = "Push Notifications",
                    subtitle = "Receive alerts for events and messages",
                    icon = Icons.Default.Notifications,
                    checked = viewModel.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotifications(it) }
                )

                if (viewModel.notificationsEnabled) {
                    SettingsClickableItem(
                        title = "Default Reminder Time",
                        subtitle = "${viewModel.defaultReminderMinutes} minutes before event",
                        icon = Icons.Default.Timer,
                        onClick = { showReminderDialog = true }
                    )
                }
            }

            SettingsSection(title = "About") {
                SettingsClickableItem(
                    title = "App Version",
                    subtitle = "1.0.0 (Stable)",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showReminderDialog) {
        ReminderTimePickerDialog(
            currentMinutes = viewModel.defaultReminderMinutes,
            onDismiss = { showReminderDialog = false },
            onConfirm = { 
                viewModel.updateDefaultReminder(it)
                showReminderDialog = false
            }
        )
    }
}

@Composable
fun ReminderTimePickerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(0, 5, 10, 15, 30, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Reminder Time") },
        text = {
            Column {
                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(minutes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = minutes == currentMinutes,
                            onClick = { onConfirm(minutes) },
                            colors = RadioButtonDefaults.colors(selectedColor = CoralRed)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = if (minutes == 0) "At time of event" else "$minutes minutes before")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = CoralRed,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = CoralRed)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
}
