package com.example.myapplication3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//Data Model For Study Room Availability For 1 Week
data class TimeSlot(val startTime: String, val isAvailable: Boolean)
data class StudyRoom(val name: String, val slots: List<TimeSlot>)

// *******IMPORTANT NOTE********
// This is the MODEL layer for application
// Scrapes CSUCI Library website for study room availability

class WebScrape {
    suspend fun fetchRawSlots(): List<JSONObject> = withContext(Dispatchers.IO) {
        val slotsList = mutableListOf<JSONObject>()
        try {
            val today = LocalDate.now()
            val startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = today.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Header for request
            // receive json of data
            val response = Jsoup.connect("https://csuci.libcal.com/spaces/availability/grid")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://csuci.libcal.com/reserve/spaces/first-floor")
                .data("lid", "8607")
                .data("gid", "15923")
                .data("start", startDate)
                .data("end", endDate)
                // Page size to select full weeks worth of data
                .data("pageSize", "600")
                .execute()

            val json = JSONObject(response.body())
            if (json.has("slots")) {
                val slotsArray = json.getJSONArray("slots")
                for (i in 0 until slotsArray.length()) {
                    slotsList.add(slotsArray.getJSONObject(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        slotsList
    }
}