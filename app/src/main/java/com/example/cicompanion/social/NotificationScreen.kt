package com.example.cicompanion.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.cicompanion.notifications.AppNotificationItem
import com.example.cicompanion.notifications.NotificationRepository
import com.example.cicompanion.ui.theme.AppBackground
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val notifications by NotificationRepository.notifications.collectAsState()

    LaunchedEffect(Unit) {
        NotificationRepository.initialize(context)
        NotificationRepository.markAllAsRead(context)
    }

    Scaffold(
        containerColor = AppBackground
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No notifications yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(notifications, key = { _, item -> item.id }) { index, item ->
                    NotificationListItem(item)

                    if (index < notifications.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationListItem(item: AppNotificationItem) {
    val formattedTime = DateFormat.getDateTimeInstance().format(Date(item.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}