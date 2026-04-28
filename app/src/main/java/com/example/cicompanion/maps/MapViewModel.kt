package com.example.cicompanion.maps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class MapViewModel : ViewModel() {
    var customPins by mutableStateOf<List<CustomPin>>(emptyList())
        private set

    var isPinMode by mutableStateOf(false)
        private set

    var tempPinLocation by mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        // Setup listener for auth changes to ensure pins are loaded when user signs in
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadCustomPins()
            } else {
                customPins = emptyList()
            }
        }
        
        // Initial load if already signed in
        if (auth.currentUser != null) {
            loadCustomPins()
        }
    }

    fun loadCustomPins() {
        viewModelScope.launch {
            isLoading = true
            customPins = FirestoreManager.fetchCustomPins()
            isLoading = false
        }
    }

    fun togglePinMode() {
        isPinMode = !isPinMode
        if (!isPinMode) {
            tempPinLocation = null
        }
    }

    fun setTempPin(latLng: com.google.android.gms.maps.model.LatLng) {
        if (isPinMode) {
            tempPinLocation = latLng
        }
    }

    fun savePin(name: String, description: String, color: androidx.compose.ui.graphics.Color) {
        val location = tempPinLocation ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val newPin = CustomPin(
            id = UUID.randomUUID().toString(),
            userId = user.uid,
            name = name,
            latitude = location.latitude,
            longitude = location.longitude,
            description = description,
            colorArgb = color.toArgb()
        )
        
        viewModelScope.launch {
            val success = FirestoreManager.saveCustomPin(newPin)
            if (success) {
                // Force reload from Firestore to ensure synchronization
                loadCustomPins()
                isPinMode = false
                tempPinLocation = null
            }
        }
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            val success = FirestoreManager.deleteCustomPin(pinId)
            if (success) {
                loadCustomPins()
            }
        }
    }

    fun toggleFavorite(pin: CustomPin) {
        viewModelScope.launch {
            val updatedPin = pin.copy(isFavorited = !pin.isFavorited)
            val success = FirestoreManager.updateCustomPin(updatedPin)
            if (success) {
                loadCustomPins()
            }
        }
    }
    
    fun associateEvent(pinId: String, eventId: String) {
        viewModelScope.launch {
            val pin = customPins.find { it.id == pinId } ?: return@launch
            val updatedPin = pin.copy(associatedEventId = eventId)
            val success = FirestoreManager.updateCustomPin(updatedPin)
            if (success) {
                loadCustomPins()
            }
        }
    }
}
