package com.example.cicompanion.studyRoom

// Imports for VIEWMODEL
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// *******IMPORTANT NOTE********
// This is the VIEWMODEL layer of this application
// Acts as a translation layer between the VIEW and MODEL layers

class RoomViewModel : ViewModel() {
    private val scraper = WebScrape()

    private val _weeklyData = MutableStateFlow<Map<String, List<StudyRoom>>>(emptyMap())
    val weeklyData: StateFlow<Map<String, List<StudyRoom>>> = _weeklyData

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val rawSlots = scraper.fetchRawSlots()
            val processedData = mutableMapOf<String, MutableMap<String, MutableList<TimeSlot>>>()

            rawSlots.forEach { slot ->
                val fullStart = slot.getString("start")
                val date = fullStart.substring(0, 10)
                val time = fullStart.substring(11, 16)
                val name = getRoomName(slot.getInt("itemId"))
                val className = slot.optString("className", "").lowercase()

                val isAvailable = if (className.isEmpty()) true
                //
                else !className.contains("s-lc-eq-checkout") && !className.contains("unavailable")

                processedData.getOrPut(date) { mutableMapOf() }
                    .getOrPut(name) { mutableListOf() }
                    .add(TimeSlot(time, isAvailable))
            }

            // Sort by name
            _weeklyData.value = processedData.mapValues { (_, roomsMap) ->
                roomsMap.map { (name, slots) -> StudyRoom(name, slots) }.sortedBy { it.name }
            }
            _isLoading.value = false
        }
    }

    // Map entity ID from website to room number
    private fun getRoomName(itemId: Int): String {
        return when (itemId) {
            61317 -> "Room 1753"
            61318 -> "Room 1754"
            61319 -> "Room 1732"
            61320 -> "Room 1734"
            else -> "Study Room $itemId"
        }
    }
}