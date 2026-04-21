package com.example.cicompanion.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.appNavigation.FeatureItem
import com.example.cicompanion.appNavigation.allAvailableFeatures
import com.example.cicompanion.appNavigation.defaultFeatureItems
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var displayedFeatures by mutableStateOf<List<FeatureItem>>(defaultFeatureItems)
        private set

    var isLoadingCustomization by mutableStateOf(false)
        private set

    private var loadedUserId: String? = null

    fun loadCustomization() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid

        if (currentUser == null) {
            displayedFeatures = defaultFeatureItems
            loadedUserId = null
            return
        }

        // Avoid re-fetching if we already have data in memory for this specific user
        if (loadedUserId == currentUid) return

        viewModelScope.launch {
            isLoadingCustomization = true
            val savedRoutes = FirestoreManager.fetchQuickAccessButtons()
            if (savedRoutes != null) {
                // Respect empty list if that's what the user saved
                displayedFeatures = allAvailableFeatures.filter { it.route in savedRoutes }
            } else {
                displayedFeatures = defaultFeatureItems
            }
            isLoadingCustomization = false
            loadedUserId = currentUid
        }
    }

    fun updateCustomization(selectedRoutes: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        displayedFeatures = allAvailableFeatures.filter { it.route in selectedRoutes }
        loadedUserId = currentUser?.uid
        viewModelScope.launch {
            FirestoreManager.saveQuickAccessButtons(selectedRoutes)
        }
    }
}
