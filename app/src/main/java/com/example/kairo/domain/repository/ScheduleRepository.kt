package com.example.kairo.domain.repository

import com.example.kairo.domain.model.CalendarEvent

interface ScheduleRepository {
    suspend fun getEvents(startTimeMillis: Long, endTimeMillis: Long): List<CalendarEvent>
}
