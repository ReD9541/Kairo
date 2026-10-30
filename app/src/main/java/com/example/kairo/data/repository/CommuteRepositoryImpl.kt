package com.example.kairo.data.repository

import com.example.kairo.BuildConfig
import com.example.kairo.data.remote.TfNSWApiService
import com.example.kairo.domain.model.CommuteResult
import com.example.kairo.domain.model.TransitLeg
import com.example.kairo.domain.repository.CommuteRepository

class CommuteRepositoryImpl(
    private val apiService: TfNSWApiService
) : CommuteRepository {

    private val commuteCache = mutableMapOf<String, CommuteResult>()
    override suspend fun getCommuteDuration(
        originName: String,
        destinationName: String,
        date: String,
        time: String,
        depArrMacro: String,
        transportMode: String
    ): CommuteResult? {
        val cacheKey = "$originName|$destinationName|$date|$time|$depArrMacro|$transportMode"
        commuteCache[cacheKey]?.let { return it }

        return try {
            val originResponse = apiService.findStop(
                authHeader = "apikey ${BuildConfig.TFNSW_API_KEY}",
                nameSf = originName
            )
            val originId = originResponse.locations?.firstOrNull()?.id ?: originName

            val destResponse = apiService.findStop(
                authHeader = "apikey ${BuildConfig.TFNSW_API_KEY}",
                nameSf = destinationName
            )
            val destId = destResponse.locations?.firstOrNull()?.id ?: destinationName

            val (incl1, incl5, incl11) = when (transportMode) {
                "Train Only" -> Triple("on", "off", "off")
                "Bus Only" -> Triple("off", "on", "off")
                else -> Triple(null, null, null)
            }

            val response = apiService.getTripPlan(
                authHeader = "apikey ${BuildConfig.TFNSW_API_KEY}",
                date = date,
                time = time,
                depArrMacro = depArrMacro,
                nameOrigin = originId,
                nameDestination = destId,
                inclMOT1 = incl1,
                inclMOT5 = incl5,
                inclMOT11 = incl11
            )
            
            val firstJourney = response.journeys?.firstOrNull()
            if (firstJourney == null) {
                android.util.Log.e("CommuteRepo", "No journeys found in response: $response")
            }
            val totalDuration = firstJourney?.legs?.sumOf { it.duration ?: 0 } ?: 0
            
            val transitLegs = firstJourney?.legs?.mapNotNull { leg ->
                val originNameStr = leg.origin?.name ?: "Unknown"
                val destNameStr = leg.destination?.name ?: "Unknown"
                val depTime = leg.origin?.departureTimeEstimated ?: leg.origin?.departureTimePlanned ?: ""
                val arrTime = leg.destination?.arrivalTimeEstimated ?: leg.destination?.arrivalTimePlanned ?: ""
                val transportName = leg.transportation?.name ?: leg.transportation?.disassembledName ?: "Walk"
                val typeClass = leg.transportation?.product?.productClass
                val typeStr = when (typeClass) {
                    1 -> "Train"
                    5 -> "Bus"
                    7 -> "Coach"
                    9 -> "Ferry"
                    11 -> "Light Rail"
                    99 -> "Walk"
                    else -> "Transit"
                }
                
                val durationMillis = (leg.duration ?: 0) * 1000L
                val originPlatform = leg.origin?.properties?.platformName
                val destPlatform = leg.destination?.properties?.platformName

                TransitLeg(
                    origin = originNameStr,
                    destination = destNameStr,
                    departureTime = depTime,
                    arrivalTime = arrTime,
                    transportationName = transportName,
                    transportationType = typeStr,
                    durationMillis = durationMillis,
                    originPlatform = originPlatform,
                    destinationPlatform = destPlatform
                )
            } ?: emptyList()

            val commuteResult = CommuteResult(
                durationMillis = totalDuration * 1000L,
                legs = transitLegs
            )
            commuteCache[cacheKey] = commuteResult
            commuteResult
        } catch (e: Exception) {
            android.util.Log.e("CommuteRepo", "Error fetching trip", e)
            null
        }
    }

    override suspend fun validateLocation(locationName: String): Boolean {
        if (locationName.isBlank()) return false
        if (BuildConfig.TFNSW_API_KEY.isBlank()) {
            return true
        }
        return try {
            val response = apiService.findStop(
                authHeader = "apikey ${BuildConfig.TFNSW_API_KEY}",
                nameSf = locationName
            )
            val locations = response.locations
            !locations.isNullOrEmpty()
        } catch (e: Exception) {
            android.util.Log.w("CommuteRepo", "TfNSW API error during validation, allowing entry: ${e.message}")
            true
        }
    }

    override suspend fun getLocationSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        if (BuildConfig.TFNSW_API_KEY.isBlank()) {
            return emptyList()
        }
        return try {
            val response = apiService.findStop(
                authHeader = "apikey ${BuildConfig.TFNSW_API_KEY}",
                nameSf = query
            )
            response.locations?.mapNotNull { it.name }?.distinct() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.w("CommuteRepo", "TfNSW API error during suggestion search: ${e.message}")
            emptyList()
        }
    }
}
