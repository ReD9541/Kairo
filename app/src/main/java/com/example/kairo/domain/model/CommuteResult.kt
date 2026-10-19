package com.example.kairo.domain.model

data class CommuteResult(
    val durationMillis: Long,
    val legs: List<TransitLeg>
)

data class TransitLeg(
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val transportationName: String,
    val transportationType: String, // "Train", "Bus", "Walk", etc.
    val durationMillis: Long,
    val originPlatform: String?,
    val destinationPlatform: String?
)
