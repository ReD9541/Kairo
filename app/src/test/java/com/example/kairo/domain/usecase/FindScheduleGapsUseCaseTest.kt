package com.example.kairo.domain.usecase

import com.example.kairo.domain.model.CalendarEvent
import com.example.kairo.domain.repository.ScheduleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FindScheduleGapsUseCaseTest {

    private val mockRepository = object : ScheduleRepository {
        var eventsToReturn = listOf<CalendarEvent>()
        override suspend fun getEvents(startTimeMillis: Long, endTimeMillis: Long): List<CalendarEvent> {
            return eventsToReturn
        }
    }

    private val useCase = FindScheduleGapsUseCase(mockRepository)

    @Test
    fun `finds gaps correctly with single event`() = runBlocking {
        // Day: 8 AM to 6 PM (10 hours = 36000000 ms)
        val dayStart = 0L
        val dayEnd = 36000000L

        // Event: 10 AM to 12 PM (from 2h to 4h)
        val eventStart = 2 * 3600000L
        val eventEnd = 4 * 3600000L

        mockRepository.eventsToReturn = listOf(
            CalendarEvent(1, "Meeting", eventStart, eventEnd, null)
        )

        val gaps = useCase(dayStart, dayEnd, 0L)

        assertEquals(2, gaps.size)
        // Gap 1: 8 AM to 10 AM
        assertEquals(0L, gaps[0].startTime)
        assertEquals(eventStart, gaps[0].endTime)

        // Gap 2: 12 PM to 6 PM
        assertEquals(eventEnd, gaps[1].startTime)
        assertEquals(dayEnd, gaps[1].endTime)
    }
    
    @Test
    fun `handles overlapping events correctly`() = runBlocking {
        val dayStart = 0L
        val dayEnd = 1000L

        // Event 1: 100 to 300
        // Event 2: 200 to 400
        mockRepository.eventsToReturn = listOf(
            CalendarEvent(1, "E1", 100L, 300L, null),
            CalendarEvent(2, "E2", 200L, 400L, null)
        )

        val gaps = useCase(dayStart, dayEnd, 0L)

        assertEquals(2, gaps.size)
        assertEquals(0L, gaps[0].startTime)
        assertEquals(100L, gaps[0].endTime)
        
        assertEquals(400L, gaps[1].startTime)
        assertEquals(1000L, gaps[1].endTime)
    }
}
