package com.example.kairo.domain.usecase

import com.example.kairo.domain.repository.CommuteRepository
import com.example.kairo.domain.model.CommuteResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculateCommuteUseCase(
    private val commuteRepository: CommuteRepository
) {
    /**
     * Calculates the commute duration between two locations at a given time.
     * @param originName The starting location
     * @param destinationName The destination location
     * @param timeMillis The time of departure in milliseconds
     * @return Commute duration in milliseconds, or null if it cannot be calculated
     */
    suspend operator fun invoke(
        originName: String,
        destinationName: String,
        timeMillis: Long,
        depArrMacro: String = "dep",
        transportMode: String = "All Public Transport"
    ): CommuteResult? {
        val dateObj = Date(timeMillis)
        val tz = java.util.TimeZone.getTimeZone("Australia/Sydney")
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply { timeZone = tz }
        val timeFormat = SimpleDateFormat("HHmm", Locale.getDefault()).apply { timeZone = tz }
        
        val dateString = dateFormat.format(dateObj)
        val timeString = timeFormat.format(dateObj)

        val result = commuteRepository.getCommuteDuration(
            originName = originName,
            destinationName = destinationName,
            date = dateString,
            time = timeString,
            depArrMacro = depArrMacro,
            transportMode = transportMode
        )

        return result
    }
}
