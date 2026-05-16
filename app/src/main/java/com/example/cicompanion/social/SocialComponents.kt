package com.example.cicompanion.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cicompanion.ui.theme.CoralRed

@Composable
fun UserAvatar(photoUrl: String, modifier: Modifier = Modifier) {
    if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = modifier
                .clip(CircleShape)
                .background(Color.LightGray)
        )
    } else {
        Box(
            modifier = modifier
                .background(CoralRed.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person, 
                contentDescription = null, 
                tint = CoralRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
