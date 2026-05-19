package com.example.cicompanion.maps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.social.FirestoreManager
import com.example.cicompanion.social.SocialRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class MapViewModel : ViewModel() {
    var customPins by mutableStateOf<List<CustomPin>>(emptyList())
        private set

    var isPinMode by mutableStateOf(false)
        private set

    var editingPinId by mutableStateOf<String?>(null)
        private set

    var tempPinLocation by mutableStateOf<LatLng?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val editingPin: CustomPin? get() = editingPinId?.let { id -> customPins.find { it.id == id } }

    private var customPinsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        // Setup listener for auth changes to ensure pins are loaded when user signs in
        val auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            isLoggedIn = user != null
            
            // Clear existing listener on auth change
            customPinsListener?.remove()
            customPinsListener = null

            if (user != null) {
                loadCustomPins()
            } else {
                customPins = emptyList()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    fun loadCustomPins() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (customPinsListener != null) return // Already listening

        isLoading = true
        customPinsListener = SocialRepository.listenToCustomPins(user.uid,
            onPinsChanged = {
                customPins = it
                isLoading = false
            },
            onError = {
                errorMessage = it
                isLoading = false
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        customPinsListener?.remove()
    }

    fun togglePinMode() {
        if (!isLoggedIn) return
        isPinMode = !isPinMode
        if (!isPinMode) {
            tempPinLocation = null
            editingPinId = null
            errorMessage = null
        }
    }

    fun exitPinMode() {
        isPinMode = false
        tempPinLocation = null
        editingPinId = null
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }

    fun startEditingLocation(pinId: String) {
        val pin = customPins.find { it.id == pinId } ?: return
        isPinMode = true
        editingPinId = pinId
        tempPinLocation = pin.position
    }

    fun setTempPin(latLng: LatLng) {
        if (isPinMode) {
            tempPinLocation = latLng
        }
    }

    fun clearTempPin() {
        tempPinLocation = null
    }

    private fun isDuplicate(latitude: Double, longitude: Double, currentPinId: String?): Boolean {
        return customPins.any { 
            it.id != currentPinId && 
            abs(it.latitude - latitude) < 0.00001 && 
            abs(it.longitude - longitude) < 0.00001 
        }
    }

    fun savePin(name: String, description: String, color: Color, associatedEventId: String? = null) {
        val location = tempPinLocation ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        if (isDuplicate(location.latitude, location.longitude, editingPinId)) {
            errorMessage = "A pin already exists at this location."
            return
        }

        val pinToSave = if (editingPinId != null) {
            val existing = customPins.find { it.id == editingPinId }
            existing?.copy(
                name = name,
                description = description,
                latitude = location.latitude,
                longitude = location.longitude,
                colorArgb = color.toArgb(),
                associatedEventId = associatedEventId
            ) ?: return
        } else {
            CustomPin(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                description = description,
                colorArgb = color.toArgb(),
                associatedEventId = associatedEventId
            )
        }

        // Optimistic update
        val previousPins = customPins
        if (editingPinId != null) {
            customPins = customPins.map { if (it.id == editingPinId) pinToSave else it }
        } else {
            customPins = customPins + pinToSave
        }

        viewModelScope.launch {
            val success = if (editingPinId != null) {
                FirestoreManager.updateCustomPin(pinToSave)
            } else {
                FirestoreManager.saveCustomPin(pinToSave)
            }

            if (success) {
                isPinMode = false
                tempPinLocation = null
                editingPinId = null
                errorMessage = null
            } else {
                customPins = previousPins
                errorMessage = "Failed to save pin."
            }
        }
    }

    fun saveSharedPin(location: CampusLocation, onComplete: () -> Unit = {}) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val newPin = CustomPin(
            id = UUID.randomUUID().toString(),
            userId = user.uid,
            name = location.name,
            latitude = location.position.latitude,
            longitude = location.position.longitude,
            description = location.description,
            colorArgb = location.color.toArgb(),
            associatedEventId = location.associatedEventId
        )
        
        // Optimistic update
        val previousPins = customPins
        customPins = customPins + newPin
        onComplete() // Close sheet immediately for smoother experience
        
        viewModelScope.launch {
            val success = FirestoreManager.saveCustomPin(newPin)
            if (!success) {
                customPins = previousPins
                errorMessage = "Failed to save shared pin."
            }
        }
    }

    fun deletePin(pinId: String) {
        val previousPins = customPins
        customPins = customPins.filter { it.id != pinId }

        viewModelScope.launch {
            val success = FirestoreManager.deleteCustomPin(pinId)
            if (!success) {
                customPins = previousPins
                errorMessage = "Failed to delete pin."
            }
        }
    }

    fun togglePinLocation(location: CampusLocation) {
        if (!location.isCustom) return

        val existingPin = customPins.find { it.id == location.id } ?: return
        val targetStatus = !existingPin.isPinned
        val updatedPin = existingPin.copy(isPinned = targetStatus)

        val previousPins = customPins
        customPins = customPins.map { if (it.id == location.id) updatedPin else it }

        viewModelScope.launch {
            val success = FirestoreManager.updateCustomPin(updatedPin)
            if (!success) {
                customPins = previousPins
                errorMessage = "Failed to update pin status."
            }
        }
    }

    fun associateEvent(pinId: String, eventId: String?) {
        val pin = customPins.find { it.id == pinId } ?: return
        val updatedPin = pin.copy(associatedEventId = eventId)

        val previousPins = customPins
        customPins = customPins.map { if (it.id == pinId) updatedPin else it }

        viewModelScope.launch {
            val success = FirestoreManager.updateCustomPin(updatedPin)
            if (!success) {
                customPins = previousPins
                errorMessage = "Failed to associate event."
            }
        }
    }
}
