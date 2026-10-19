package com.example.kairo.domain.repository

import com.example.kairo.domain.model.CommuteResult

interface CommuteRepository {
    suspend fun getCommuteDuration(
        originName: String,
        destinationName: String,
        date: String, // yyyymmdd
        time: String,  // hhmm
        depArrMacro: String = "dep",
        transportMode: String = "All Public Transport"
    ): CommuteResult?

    suspend fun validateLocation(locationName: String): Boolean
    
    suspend fun getLocationSuggestions(query: String): List<String>
}
