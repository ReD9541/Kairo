package com.example.kairo.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripResponse(
    @Json(name = "journeys") val journeys: List<Journey>?
)

@JsonClass(generateAdapter = true)
data class Journey(
    @Json(name = "legs") val legs: List<Leg>?
)

@JsonClass(generateAdapter = true)
data class Leg(
    @Json(name = "duration") val duration: Int?,
    @Json(name = "origin") val origin: LegStop?,
    @Json(name = "destination") val destination: LegStop?,
    @Json(name = "transportation") val transportation: Transportation?
)

@JsonClass(generateAdapter = true)
data class LegStop(
    @Json(name = "name") val name: String?,
    @Json(name = "departureTimeEstimated") val departureTimeEstimated: String?,
    @Json(name = "departureTimePlanned") val departureTimePlanned: String?,
    @Json(name = "arrivalTimeEstimated") val arrivalTimeEstimated: String?,
    @Json(name = "arrivalTimePlanned") val arrivalTimePlanned: String?,
    @Json(name = "properties") val properties: LegProperties?
)

@JsonClass(generateAdapter = true)
data class LegProperties(
    @Json(name = "platformName") val platformName: String?
)

@JsonClass(generateAdapter = true)
data class Transportation(
    @Json(name = "name") val name: String?,
    @Json(name = "disassembledName") val disassembledName: String?,
    @Json(name = "product") val product: Product?
)

@JsonClass(generateAdapter = true)
data class Product(
    @Json(name = "class") val productClass: Int?
)

@JsonClass(generateAdapter = true)
data class StopFinderResponse(
    @Json(name = "locations") val locations: List<TfNSWLocation>?
)

@JsonClass(generateAdapter = true)
data class TfNSWLocation(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?
)
