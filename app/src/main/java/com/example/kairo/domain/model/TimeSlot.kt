package com.example.kairo.domain.model

data class TimeSlot(
    val startTime: Long,
    val endTime: Long
) {
    val durationMillis: Long get() = endTime - startTime
}
