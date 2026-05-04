package com.example.cicompanion.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Represents a short status note that a user can set on their profile.
 */
data class UserNote(
    val content: String = "",
    val expiresAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the note has reached its expiration time.
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
}

/**
 * A floating bubble that displays the user's current status note.
 * Tail circles are anchored to a fixed point, and the bubble grows up/left from it.
 */
@Composable
fun UserNoteBubble(note: UserNote?, modifier: Modifier = Modifier) {
    if (note == null || note.isExpired()) return

    // The base Box is positioned by the caller (anchor point on the profile picture)
    Box(modifier = modifier) {
        
        // 1. Smallest tail circle - stays pinned to the anchor point
        Surface(
            modifier = Modifier
                .size(10.dp)
                .offset(x = (12).dp, y = (44).dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {}

        // 2. Medium tail circle - stays pinned slightly further top-left
        Surface(
            modifier = Modifier
                .size(16.dp)
                .offset(x = (2).dp, y = (28).dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {}

        // 3. Main Bubble Surface
        // wrapContentSize with Alignment.BottomEnd makes the bubble grow UP and LEFT
        // as the text length increases, keeping the bottom-right corner fixed near the tails.
        Box(
            modifier = Modifier
                .offset(x = (0).dp, y = (0).dp) // Position of the bubble's bottom-right corner
                .wrapContentSize(unbounded = true, align = Alignment.BottomEnd)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Text(
                    text = note.content,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 120.dp),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = Color.Black,
                    maxLines = 4,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Dialog for adding or editing a profile status note.
 */
@Composable
fun StatusDialog(
    currentNote: UserNote?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var text by remember { mutableStateOf(currentNote?.content ?: "") }
    var selectedDuration by remember { mutableLongStateOf(3600000L) } // Default to 1 hour
    val charLimit = 50

    val durations = listOf(
        "15 minutes" to 15 * 60 * 1000L,
        "1 hour" to 60 * 60 * 1000L,
        "5 hours" to 5 * 60 * 60 * 1000L,
        "24 hours" to 24 * 60 * 60 * 1000L
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
