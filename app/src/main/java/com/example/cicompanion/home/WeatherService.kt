package com.example.cicompanion.home

import com.example.cicompanion.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherService {
    @GET("v1/current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String = BuildConfig.WEATHER_API_KEY,
        @Query("q") location: String // Pass "lat,long" or a City name
    ): WeatherResponse
}