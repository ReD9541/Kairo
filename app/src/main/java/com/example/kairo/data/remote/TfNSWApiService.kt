package com.example.kairo.data.remote

import com.example.kairo.data.model.TripResponse
import com.example.kairo.data.model.StopFinderResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TfNSWApiService {
    @GET("v1/tp/trip")
    suspend fun getTripPlan(
        @Header("Authorization") authHeader: String,
        @Query("outputFormat") outputFormat: String = "rapidJSON",
        @Query("coordOutputFormat") coordOutputFormat: String = "EPSG:4326",
        @Query("depArrMacro") depArrMacro: String = "dep",
        @Query("itdDate") date: String,
        @Query("itdTime") time: String,
        @Query("type_origin") typeOrigin: String = "any",
        @Query("name_origin") nameOrigin: String,
        @Query("type_destination") typeDestination: String = "any",
        @Query("name_destination") nameDestination: String,
        @Query("calcNumberOfTrips") calcNumberOfTrips: Int = 1,
        @Query("inclMOT_1") inclMOT1: String? = null,
        @Query("inclMOT_5") inclMOT5: String? = null,
        @Query("inclMOT_11") inclMOT11: String? = null
    ): TripResponse

    @GET("v1/tp/stop_finder")
    suspend fun findStop(
        @Header("Authorization") authHeader: String,
        @Query("outputFormat") outputFormat: String = "rapidJSON",
        @Query("type_sf") typeSf: String = "any",
        @Query("name_sf") nameSf: String
    ): StopFinderResponse
}
