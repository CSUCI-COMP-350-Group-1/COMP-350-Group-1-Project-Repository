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
        isPinMode = !isPinMode
        if (!isPinMode) {
            tempPinLocation = null
            editingPinId = null
        }
    }

    fun exitPinMode() {
        isPinMode = false
        tempPinLocation = null
        editingPinId = null
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

    fun savePin(name: String, description: String, color: Color, associatedEventId: String? = null) {
        val location = tempPinLocation ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
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
        
        viewModelScope.launch {
            val success = if (editingPinId != null) {
                FirestoreManager.updateCustomPin(pinToSave)
            } else {
                FirestoreManager.saveCustomPin(pinToSave)
            }
            
            if (success) {
                // No need to manually reload, listener handles it
                isPinMode = false
                tempPinLocation = null
                editingPinId = null
            }
        }
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            FirestoreManager.deleteCustomPin(pinId)
            // No need to manually reload, listener handles it
        }
    }

    fun toggleFavorite(pin: CustomPin) {
        viewModelScope.launch {
            val updatedPin = pin.copy(isFavorited = !pin.isFavorited)
            FirestoreManager.updateCustomPin(updatedPin)
            // No need to manually reload, listener handles it
        }
    }
    
    fun associateEvent(pinId: String, eventId: String?) {
        viewModelScope.launch {
            val pin = customPins.find { it.id == pinId } ?: return@launch
            val updatedPin = pin.copy(associatedEventId = eventId)
            FirestoreManager.updateCustomPin(updatedPin)
            // No need to manually reload, listener handles it
        }
    }
}
