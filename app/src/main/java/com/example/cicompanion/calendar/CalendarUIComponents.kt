package com.example.cicompanion.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cicompanion.ui.theme.BrandRedDark
import com.example.cicompanion.ui.theme.BrandRedLighter
import java.time.LocalTime
import java.util.Locale

val CoralRed = Color(0xFFEF3347)
val CardOffWhite = Color(0xFFF6E6D8)
val SoftText = Color(0xFF6E5555)

@Composable
fun CalendarHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandRedLighter, BrandRedDark)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            trailingContent()
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardOffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun WheelTimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    // EDIT CLASS TIME FIX:
    // These values must update when initialTime changes.
    // Without this, editing a saved class can keep the default 9:00 AM / 10:15 AM wheel state.
    var hour by remember {
        mutableIntStateOf(
            if (initialTime.hour == 0) 12
            else if (initialTime.hour > 12) initialTime.hour - 12
            else initialTime.hour
        )
    }
    var minute by remember { mutableIntStateOf(initialTime.minute) }
    var amPm by remember { mutableStateOf(if (initialTime.hour < 12) "AM" else "PM") }

    // EDIT CLASS TIME FIX:
    // When AddClassDialog loads an existing class, initialTime changes from the default
    // to the saved value. Sync the picker state to that saved value.
    LaunchedEffect(initialTime) {
        hour = if (initialTime.hour == 0) {
            12
        } else if (initialTime.hour > 12) {
            initialTime.hour - 12
        } else {
            initialTime.hour
        }

        minute = initialTime.minute
        amPm = if (initialTime.hour < 12) "AM" else "PM"
    }

    LaunchedEffect(hour, minute, amPm) {
        val h = when (amPm) {
            "AM" -> if (hour == 12) 0 else hour
            "PM" -> if (hour == 12) 12 else hour + 12
            else -> hour
        }

        val newTime = LocalTime.of(h, minute)

        // EDIT CLASS TIME FIX:
        // Avoid re-sending the same value repeatedly while syncing picker state.
        if (newTime != initialTime) {
            onTimeChange(newTime)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = (1..12).toList(),
            initialIndex = hour - 1,
            onItemSelected = { selectedHour -> hour = selectedHour },
            modifier = Modifier.weight(1f)
        )

        Text(":", style = MaterialTheme.typography.headlineMedium)

        WheelPicker(
            items = (0..59).toList(),
            initialIndex = minute,
            onItemSelected = { selectedMinute -> minute = selectedMinute },
            format = { value -> String.format(Locale.US, "%02d", value) },
            modifier = Modifier.weight(1f)
        )

        WheelPicker(
            items = listOf("AM", "PM"),
            initialIndex = if (amPm == "AM") 0 else 1,
            onItemSelected = { selectedAmPm -> amPm = selectedAmPm },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    format: (T) -> String = { it.toString() }
) {
    val safeInitialIndex = initialIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 40.dp

    val selectedIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    // EDIT CLASS TIME FIX:
    // If the parent changes initialIndex while editing an existing class,
    // move the wheel to the saved value instead of leaving it at the old default.
    LaunchedEffect(safeInitialIndex, items.size) {
        if (items.isNotEmpty() && listState.firstVisibleItemIndex != safeInitialIndex) {
            listState.scrollToItem(safeInitialIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            if (selectedIndex in items.indices) {
                onItemSelected(items[selectedIndex])
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(CoralRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(items[index]),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == index) CoralRed else Color.Gray
                    )
                }
            }
        }
    }
}