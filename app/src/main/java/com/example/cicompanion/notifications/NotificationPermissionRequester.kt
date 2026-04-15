package com.example.cicompanion.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class NotificationPermissionRequester(
    private val activity: ComponentActivity
) {

    //Handles Android 13+ notification permission requests.
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    //Public function called by MainActivity.
    fun requestIfNeeded() {
        if (!requiresRuntimePermission()) return
        if (alreadyGranted()) return

        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requiresRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    private fun alreadyGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}