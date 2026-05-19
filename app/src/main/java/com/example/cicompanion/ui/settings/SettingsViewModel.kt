package com.example.cicompanion.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
    var defaultReminderMinutes by mutableStateOf(15)
    
    var isLoading by mutableStateOf(false)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            isLoading = true
            val settings = FirestoreManager.fetchGeneralSettings()
            if (settings.isNotEmpty()) {
                isDarkMode = settings["isDarkMode"] as? Boolean ?: false
                notificationsEnabled = settings["notificationsEnabled"] as? Boolean ?: true
                defaultReminderMinutes = (settings["defaultReminderMinutes"] as? Long)?.toInt() ?: 15
            }
            isLoading = false
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        viewModelScope.launch {
            FirestoreManager.saveGeneralSetting("isDarkMode", enabled)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        viewModelScope.launch {
            FirestoreManager.saveGeneralSetting("notificationsEnabled", enabled)
        }
    }

    fun updateDefaultReminder(minutes: Int) {
        defaultReminderMinutes = minutes
        viewModelScope.launch {
            FirestoreManager.saveGeneralSetting("defaultReminderMinutes", minutes)
        }
    }
}
