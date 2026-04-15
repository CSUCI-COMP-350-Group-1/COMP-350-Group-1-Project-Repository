package com.example.cicompanion.startup

import android.content.Context
import com.example.cicompanion.calendar.CalendarReminderScheduler
import com.example.cicompanion.firebase.FcmTokenManager
import com.example.cicompanion.notifications.AppNotificationManager
import com.example.cicompanion.social.FriendRequestNotificationObserver
import com.google.firebase.auth.FirebaseAuth

class AppStartupCoordinator(
    context: Context
) {

    // Always use application context for non-UI startup work
    private val appContext = context.applicationContext

    //Observer that watches for incoming friend requests
    private val friendRequestObserver = FriendRequestNotificationObserver(appContext)

    //Tracks which signed-in user is currently being observed
    private var observedUserId: String? = null

    //Keeps auth-change handling out of MainActivity
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        handleAuthenticatedUser(auth.currentUser?.uid)
    }

    //Called once by MainActivity during startup
    fun start() {
        //Create all app notification channels.
        AppNotificationManager.createNotificationChannels(appContext)

        //Start calendar sync + reminder scheduling.
        CalendarReminderScheduler.start(appContext)
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        handleAuthenticatedUser(FirebaseAuth.getInstance().currentUser?.uid)
    }

    //Called once by MainActivity during teardown
    fun stop() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        friendRequestObserver.stop()
    }

    private fun handleAuthenticatedUser(currentUserId: String?) {
        if (userChanged(currentUserId)) {
            removeOldUserToken()
            friendRequestObserver.stop()
        }

        if (currentUserId == null) {
            observedUserId = null
            return
        }

        syncSignedInUserToken()
        startFriendRequestObserverIfNeeded(currentUserId)

        observedUserId = currentUserId
    }

    private fun userChanged(currentUserId: String?): Boolean {
        return observedUserId != null && observedUserId != currentUserId
    }

    private fun removeOldUserToken() {
        observedUserId?.let { previousUserId ->
            FcmTokenManager.removeCurrentTokenFromUser(previousUserId)
        }
    }

    private fun syncSignedInUserToken() {
        FcmTokenManager.syncCurrentTokenForSignedInUser()
    }

    private fun startFriendRequestObserverIfNeeded(currentUserId: String) {
        if (observedUserId == currentUserId) return
        friendRequestObserver.start(currentUserId)
    }
}