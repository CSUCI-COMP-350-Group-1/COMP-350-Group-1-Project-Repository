package com.example.cicompanion.calendar.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class GoogleCalendarAuthManager(
    context: Context
) {
    private val authorizationClient: AuthorizationClient =
        Identity.getAuthorizationClient(context)

    fun requestCalendarAccess(
        onResolutionRequired: (PendingIntent) -> Unit,
        onAccessToken: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestedScopes: List<Scope> = listOf(
            Scope(CALENDAR_EVENTS_SCOPE),
            Scope(CALENDAR_LIST_READONLY_SCOPE)
        )

        val request: AuthorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        authorizationClient.authorize(request)
            .addOnSuccessListener { authorizationResult: AuthorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent: PendingIntent? = authorizationResult.pendingIntent
                    if (pendingIntent != null) {
                        onResolutionRequired(pendingIntent)
                    } else {
                        onError("Authorization requires user action, but no PendingIntent was returned.")
                    }
                } else {
                    val accessToken: String? = authorizationResult.accessToken
                    if (accessToken.isNullOrBlank()) {
                        onError("No access token was returned.")
                    } else {
                        onAccessToken(accessToken)
                    }
                }
            }
            .addOnFailureListener { exception: Exception ->
                onError(exception.message ?: "Failed to start authorization.")
            }
    }

    fun extractAccessToken(data: Intent?): Result<String> {
        return try {
            val result: AuthorizationResult =
                authorizationClient.getAuthorizationResultFromIntent(data)

            val accessToken: String? = result.accessToken
            if (accessToken.isNullOrBlank()) {
                Result.failure(IllegalStateException("No access token was returned."))
            } else {
                Result.success(accessToken)
            }
        } catch (exception: ApiException) {
            Result.failure(
                IllegalStateException(
                    "Authorization failed: ${exception.statusCode}",
                    exception
                )
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    companion object {
        const val CALENDAR_EVENTS_SCOPE =
            "https://www.googleapis.com/auth/calendar.events"

        const val CALENDAR_LIST_READONLY_SCOPE =
            "https://www.googleapis.com/auth/calendar.calendarlist.readonly"
    }
}
