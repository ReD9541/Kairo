package com.example.kairo

import com.example.kairo.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TfNSWTest {
    @Test
    fun testTripPlan() = runBlocking {
        try {
            val apiService = RetrofitClient.tfNSWApiService
            val response = apiService.getTripPlan(
                authHeader = "apikey eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJHTVpSTzUxZjh3bVp1QkRPaDFxM1luLTVRclp2ckRzSkZ4MVZERnlKdkNNIiwiaWF0IjoxNzkxNzc4MjY0fQ.tDfxSwxBUSeYY9_jOp9URY23Hox-g6n6xrYnAbCE41k",
                date = "20261016",
                time = "1700",
                nameOrigin = "Central Station",
                nameDestination = "Parramatta Station",
                depArrMacro = "dep"
            )
            println("Response: $response")
            
            val firstJourney = response.journeys?.firstOrNull()
            val totalDuration = firstJourney?.legs?.sumOf { it.duration ?: 0 }
            println("Duration: $totalDuration")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
