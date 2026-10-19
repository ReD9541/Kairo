package com.example.kairo.domain.model

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val description: String? = null
)
