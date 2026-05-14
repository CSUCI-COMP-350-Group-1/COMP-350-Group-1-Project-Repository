package com.example.cicompanion.home

import com.squareup.moshi.Json

data class WeatherResponse(
    val current: CurrentWeather,
    val location: WeatherLocation
)

data class WeatherLocation(
    val name: String
)

data class CurrentWeather(
    @Json(name = "temp_f") val tempF: Double,
    val condition: WeatherCondition,
    val last_updated: String = ""
)

data class WeatherCondition(
    val text: String,
    val icon: String // Returns "//cdn.weatherapi.com/..."
)