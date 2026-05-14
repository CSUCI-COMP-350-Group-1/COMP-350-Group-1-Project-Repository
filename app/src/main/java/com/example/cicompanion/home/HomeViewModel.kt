package com.example.cicompanion.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cicompanion.appNavigation.FeatureItem
import com.example.cicompanion.appNavigation.allAvailableFeatures
import com.example.cicompanion.appNavigation.defaultFeatureItems
import com.example.cicompanion.social.FirestoreManager
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // SharedPreferences for the saved settings across app restarts
    private val sharedPrefs = application.getSharedPreferences("quick_access_prefs", Context.MODE_PRIVATE)

    var displayedFeatures by mutableStateOf<List<FeatureItem>>(defaultFeatureItems)
        private set

    var isLoadingCustomization by mutableStateOf(false)
        private set

    private var loadedUserId: String? = null
    private var isFetching = false

    var weatherState by mutableStateOf<WeatherResponse?>(null)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val weatherService = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(WeatherService::class.java)

    fun fetchWeather(location: String) {
        viewModelScope.launch {
            try {
                weatherState = weatherService.getCurrentWeather(location = location)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching weather: ${e.message}", e)
            }
        }
    }

    init {
        // load from local storage if user is already signed in
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid ?: return
        
        val savedString = sharedPrefs.getString("buttons_$currentUid", null)
        if (savedString != null) {
            val routes = savedString.split(",").filter { it.isNotEmpty() }
            // preserves the saved order
            displayedFeatures = routes.mapNotNull { route -> 
                allAvailableFeatures.find { it.route == route } 
            }
        }
    }

    private fun saveToPrefs(routes: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid ?: return
        sharedPrefs.edit().putString("buttons_$currentUid", routes.joinToString(",")).apply()
    }

    fun loadCustomization() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid

        if (currentUser == null) {
            // Reset to defaults for guest users
            displayedFeatures = defaultFeatureItems
            loadedUserId = null
            isLoadingCustomization = false
            return
        }

        // Load from local storage first for immediate UI update
        loadFromPrefs()

        // avoid fetching again, a duplicate
        if (loadedUserId == currentUid || isFetching) return

        viewModelScope.launch {
            isFetching = true
            isLoadingCustomization = true
            try {
                val savedRoutes = FirestoreManager.fetchQuickAccessButtons()
                if (savedRoutes != null) {
                    // firestore the saved selections
                    val features = savedRoutes.mapNotNull { route -> 
                        allAvailableFeatures.find { it.route == route } 
                    }
                    displayedFeatures = features
                    // Sync local storage with Firestore data
                    saveToPrefs(savedRoutes)
                } else {
                    // keep defaults if no data in firestore
                    if (sharedPrefs.getString("buttons_$currentUid", null) == null) {
                        displayedFeatures = defaultFeatureItems
                    }
                }
                loadedUserId = currentUid
            } catch (e: Exception) {
                // error check
            } finally {
                isLoadingCustomization = false
                isFetching = false
            }
        }
    }

    fun updateCustomization(selectedRoutes: List<String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid
        
        // Updated the local state (the refresh)
        displayedFeatures = selectedRoutes.mapNotNull { route -> 
            allAvailableFeatures.find { it.route == route } 
        }
        loadedUserId = currentUid
        
        // local persist
        saveToPrefs(selectedRoutes)
        
        // Sync to cloud
        if (currentUid != null) {
            viewModelScope.launch {
                FirestoreManager.saveQuickAccessButtons(selectedRoutes)
            }
        }
    }
}
