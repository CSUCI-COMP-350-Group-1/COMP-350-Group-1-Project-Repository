package com.example.cicompanion.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

private val BrandRed = Color(0xFFEF3347)

@Composable
fun EditProfileScreen(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            SocialRepository.fetchUserProfile(
                userId = uid,
                onSuccess = { profile -> userProfile = profile },
                onError = { msg -> errorMessage = msg }
            )
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Profile Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SettingsItem(
                label = "Edit Display Name",
                icon = Icons.Default.Person,
                onClick = { showNameDialog = true }
            )
            SettingsItem(
                label = "Edit Profile Picture",
                icon = Icons.Default.Image,
                onClick = { /* TODO: Implement */ }
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = BrandRed, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                Text("Go Back", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNameDialog && userProfile != null) {
        DisplayNameDialog(
            currentProfile = userProfile!!,
            onDismiss = { showNameDialog = false },
            onUpdate = { newName ->
                SocialRepository.updateDisplayName(
                    userId = userProfile!!.uid,
                    newDisplayName = newName,
                    onSuccess = {
                        showNameDialog = false
                        userProfile = userProfile?.copy(displayName = newName)
                    },
                    onError = { msg -> errorMessage = msg }
                )
            },
            onRevert = {
                SocialRepository.revertToOriginalDisplayName(
                    userId = userProfile!!.uid,
                    originalDisplayName = userProfile!!.originalDisplayName,
                    onSuccess = {
                        showNameDialog = false
                        userProfile = userProfile?.copy(displayName = userProfile!!.originalDisplayName)
                    },
                    onError = { msg -> errorMessage = msg }
                )
            }
        )
    }
}

@Composable
fun DisplayNameDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit,
    onRevert: () -> Unit
) {
    var newName by remember { mutableStateOf(currentProfile.displayName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (currentProfile.originalDisplayName.isNotBlank() && 
                    currentProfile.displayName != currentProfile.originalDisplayName) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onRevert,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Revert to '${currentProfile.originalDisplayName}'", color = BrandRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(newName) },
                enabled = newName.isNotBlank() && newName != currentProfile.displayName,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                Text("Update")
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
fun SettingsItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.LightGray.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
