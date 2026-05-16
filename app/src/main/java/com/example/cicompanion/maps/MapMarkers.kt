package com.example.cicompanion.maps

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.cicompanion.ui.theme.BrandOrange
import com.example.cicompanion.ui.theme.CoralRed
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun CustomPinMarkerIcon(color: Color, isPinned: Boolean, eventCount: Int, isEditing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEditing) 1.6f else 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        // Pulsing background ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                .background(color.copy(alpha = pulseAlpha), CircleShape)
        )

        // Pin body (Teardrop shape)
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer(rotationZ = 45f)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp))
                .background(color, RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp))
                .border(
                    width = 2.dp,
                    color = if (isPinned) Color(0xFF9C27B0) else Color.White,
                    shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 17.dp, bottomEnd = 2.dp)
                )
        )

        // Inner white circle to make it pop
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, CircleShape)
                .shadow(2.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }

        // Badge for associated events
        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(18.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
            }
        }
    }
}

@Composable
fun SelectedPointerIcon(
    location: CampusLocation, 
    eventCount: Int = 0, 
    hasPinnedEvent: Boolean = false,
    hasBookmarkedEvent: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f, 
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(70.dp) 
            .graphicsLayer(translationY = bounce)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = location.color,
            modifier = Modifier.fillMaxSize().shadow(4.dp, CircleShape)
        )
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp).size(32.dp),
            border = BorderStroke(2.5.dp, when {
                location.isPinned -> Color(0xFF9C27B0) // Purple for pinned custom pins
                hasPinnedEvent -> Color(0xFF9C27B0) // Purple for pinned
                eventCount > 0 -> BrandOrange // Orange for regular events
                else -> location.color
            }),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (location.isCustom) Icons.Default.PushPin else location.icon,
                    contentDescription = null,
                    tint = location.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(18.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
            }
        }
    }
}

@Composable
fun LandmarkIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    eventCount: Int = 0, 
    hasPinnedEvent: Boolean = false,
    hasBookmarkedEvent: Boolean = false,
    isCustom: Boolean = false,
    isPinned: Boolean = false
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color, CircleShape)
                .border(2.5.dp, when {
                    isPinned -> Color(0xFF9C27B0) // Purple border for pinned
                    hasPinnedEvent -> Color(0xFF9C27B0) // Purple for pinned
                    eventCount > 0 -> BrandOrange // Orange for regular events
                    else -> Color.White
                }, CircleShape)
                .shadow(4.dp, CircleShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCustom) Icons.Default.PushPin else icon,
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(18.dp)
            )
        }

        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(16.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
            }
        }
    }
}

@Composable
fun UserLocationMarker(position: LatLng) {
    MarkerComposable(state = rememberMarkerState(position = position)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PersonPinCircle, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(40.dp))
            Box(modifier = Modifier.size(12.dp).offset(y = (-2).dp).background(Color.White, CircleShape))
            Box(modifier = Modifier.size(8.dp).offset(y = (-2).dp).background(Color(0xFF1A73E8), CircleShape))
        }
    }
    Circle(center = position, radius = 25.0, fillColor = Color(0xFF1A73E8).copy(alpha = 0.15f), strokeColor = Color(0xFF1A73E8).copy(alpha = 0.4f), strokeWidth = 2f)
}
