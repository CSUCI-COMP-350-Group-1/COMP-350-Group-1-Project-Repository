package com.example.googlecalendarviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.googlecalendarviewer.auth.GoogleCalendarAuthManager
import com.example.googlecalendarviewer.CalendarApp

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var authManager: GoogleCalendarAuthManager

    private val authorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            authManager.extractAccessToken(result.data)
                .onSuccess { token ->
                    viewModel.onAuthorized(token)
                }
                .onFailure { error ->
                    viewModel.setError(error.message ?: "Authorization failed.")
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = GoogleCalendarAuthManager(this)

        setContent {
            CalendarApp(
                viewModel = viewModel,
                onConnectCalendar = ::connectGoogleCalendar
            )
        }
    }

    private fun connectGoogleCalendar() {
        authManager.requestCalendarAccess(
            onResolutionRequired = { pendingIntent ->
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            },
            onAccessToken = { token ->
                viewModel.onAuthorized(token)
            },
            onError = { message ->
                viewModel.setError(message)
            }
        )
    }
}