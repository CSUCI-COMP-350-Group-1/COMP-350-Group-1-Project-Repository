package com.example.cicompanion.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UserNote(
    val content: String = "",
    val expiresAt: Long = 0, // -1 will now represent "Never"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        // If expiresAt is -1, it never expires
        if (expiresAt == -1L) return false
        return System.currentTimeMillis() > expiresAt
    }
}

private fun getTimeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

/**
 * A floating bubble that displays the user's current status note.
 * Tail circles are anchored to a fixed point, and the bubble grows up/left from it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserNoteBubble(
    note: UserNote?,
    userName: String?,
    modifier: Modifier = Modifier
) {

    var showFullNote by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    // The base Box is positioned by the caller (anchor point on the profile picture)
    if (note == null || note.isExpired()) return
    val labelText = if (!userName.isNullOrBlank()) "$userName's Note" else "User's Note"

    Box(modifier = modifier) {
        
        // Smallest tail circle
        Surface(
            modifier = Modifier
                .size(12.dp)
                .offset(x = 14.dp, y = 26.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {}

        // Medium tail circle
        Surface(
            modifier = Modifier
                .size(18.dp)
                .offset(x = (-3).dp, y = 15.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {}

        // Main Bubble Surface
        Layout(
            content = {
                Surface(
                    onClick = { showFullNote = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Text(
                        text = note.content,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .widthIn(min = 12.dp, max = 100.dp),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = Color.Black,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints)
            layout(0, 0) {
                placeable.place(
                    // Centered horizontally above the anchor, but constrained to not go too far left
                    x = (13.dp.roundToPx() - placeable.width / 2).coerceAtLeast((-16).dp.roundToPx()),
                    y = 16.dp.roundToPx() - placeable.height
                )
            }
        }

        if (showFullNote) {
            ModalBottomSheet(
                onDismissRequest = { showFullNote = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (note.expiresAt == -1L)
                            "Posted ${getTimeAgo(note.createdAt)} • Permanent"
                        else
                            "Posted ${getTimeAgo(note.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDialog(
    currentNote: UserNote?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
    onClear: () -> Unit
) {
    var text by remember { mutableStateOf(currentNote?.content ?: "") }
    var selectedDuration by remember {
        mutableLongStateOf(if (currentNote?.expiresAt == -1L) -1L else 60 * 60 * 1000L)
    }
    val charLimit = 100

    val durations = listOf(
        "15 minutes" to 15 * 60 * 1000L,
        "1 hour" to 60 * 60 * 1000L,
        "5 hours" to 5 * 60 * 60 * 1000L,
        "24 hours" to 24 * 60 * 60 * 1000L,
        "Forever" to -1L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentNote == null) "Add Status" else "Edit Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= charLimit) text = it },
                    label = { Text("Status") },
                    placeholder = { Text("What's on your mind?") },
                    supportingText = {
                        Text(
                            text = "${text.length} / $charLimit",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("How long should it last?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                durations.forEach { (label, durationMs) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedDuration == durationMs,
                            onClick = { selectedDuration = durationMs }
                        )
                        Text(label, fontSize = 14.sp)
                    }
                }
                
                if (currentNote != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF3347))
                    ) {
                        Text("Clear Current Status")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text, selectedDuration) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3347))
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
