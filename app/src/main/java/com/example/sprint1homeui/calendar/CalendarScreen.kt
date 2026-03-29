package com.example.sprint1homeui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sprint1homeui.calendar.auth.GoogleCalendarAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    val context = LocalContext.current
    val calendarViewModel: CalendarViewModel = viewModel()
    val authManager = GoogleCalendarAuthManager(context)

    val authorizationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            authManager.extractAccessToken(result.data)
                .onSuccess { token ->
                    calendarViewModel.onAuthorized(token)
                }
                .onFailure { error ->
                    calendarViewModel.setError(error.message ?: "Authorization failed.")
                }
        }

    fun connectGoogleCalendar() {
        authManager.requestCalendarAccess(
            onResolutionRequired = { pendingIntent ->
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            },
            onAccessToken = { token ->
                calendarViewModel.onAuthorized(token)
            },
            onError = { message ->
                calendarViewModel.setError(message)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontSize = 20.sp) }
            )
        },
        content = { paddingValues ->
            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // apply scaffold padding
                contentAlignment = Alignment.Center
            ) {
                // calender ui
                CalendarApp(
                    viewModel = calendarViewModel,
                    onConnectCalendar = ::connectGoogleCalendar
                )

                // navigation at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = { navController.navigate("home") }) {
                        Text("Back to Home")
                    }
                }
            }
        }
    )
}
