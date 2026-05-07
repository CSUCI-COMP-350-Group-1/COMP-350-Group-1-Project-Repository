package com.example.cicompanion.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showBioDialog by remember { mutableStateOf(false) }
    var showMajorDialog by remember { mutableStateOf(false) }
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

            OutlinedButton(
                onClick = { showNameDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Edit Display Name", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showBioDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Edit Bio", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showMajorDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Edit Major", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
            }

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
            }
        )
    }

    if (showBioDialog && userProfile != null) {
        BioDialog(
            currentBio = userProfile!!.bio,
            onDismiss = { showBioDialog = false },
            onUpdate = { newBio ->
                SocialRepository.updateBio(
                    userId = userProfile!!.uid,
                    newBio = newBio,
                    onSuccess = {
                        showBioDialog = false
                        userProfile = userProfile?.copy(bio = newBio)
                    },
                    onError = { msg -> errorMessage = msg }
                )
            }
        )
    }

    if (showMajorDialog && userProfile != null) {
        MajorDialog(
            currentMajor = userProfile!!.major,
            onDismiss = { showMajorDialog = false },
            onUpdate = { newMajor ->
                SocialRepository.updateMajor(
                    userId = userProfile!!.uid,
                    newMajor = newMajor,
                    onSuccess = {
                        showMajorDialog = false
                        userProfile = userProfile?.copy(major = newMajor)
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
    onUpdate: (String) -> Unit
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
fun BioDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var newBio by remember { mutableStateOf(currentBio) }
    val charLimit = 150

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bio") },
        text = {
            Column {
                OutlinedTextField(
                    value = newBio,
                    onValueChange = { if (it.length <= charLimit) newBio = it },
                    label = { Text("Bio") },
                    supportingText = {
                        Text(
                            text = "${newBio.length} / $charLimit",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = if (newBio.length >= charLimit) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(newBio) },
                enabled = newBio != currentBio,
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
fun MajorDialog(
    currentMajor: String,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var newMajor by remember { mutableStateOf(currentMajor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Major") },
        text = {
            Column {
                OutlinedTextField(
                    value = newMajor,
                    onValueChange = { newMajor = it },
                    label = { Text("Major") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(newMajor) },
                enabled = newMajor != currentMajor,
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
