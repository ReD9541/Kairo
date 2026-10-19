package com.example.kairo.domain.usecase

import com.example.kairo.domain.model.TimeSlot
import com.example.kairo.domain.repository.ScheduleRepository

class FindScheduleGapsUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    /**
     * Finds gaps in the schedule between [dayStartTime] and [dayEndTime].
     * @param dayStartTime The start of the day in milliseconds (e.g., 8:00 AM)
     * @param dayEndTime The end of the day in milliseconds (e.g., 10:00 PM)
     * @param minGapDurationMillis The minimum duration in milliseconds to be considered a valid gap
     */
    suspend operator fun invoke(
        dayStartTime: Long,
        dayEndTime: Long,
        minGapDurationMillis: Long = 15 * 60 * 1000 // default 15 mins
    ): List<TimeSlot> {
        val events = scheduleRepository.getEvents(dayStartTime, dayEndTime)
        
        // Filter events that actually overlap with our day window and sort by start time
        val sortedEvents = events
            .filter { it.startTime < dayEndTime && it.endTime > dayStartTime }
            .sortedBy { it.startTime }

        val gaps = mutableListOf<TimeSlot>()
        var currentFreeStartTime = dayStartTime

        for (event in sortedEvents) {
            val eventStart = maxOf(event.startTime, dayStartTime)
            val eventEnd = minOf(event.endTime, dayEndTime)

            if (currentFreeStartTime < eventStart) {
                val gapDuration = eventStart - currentFreeStartTime
                if (gapDuration >= minGapDurationMillis) {
                    gaps.add(TimeSlot(currentFreeStartTime, eventStart))
                }
            }
            
            // Advance the current free time to the end of the event, but only if it's further than current
            if (eventEnd > currentFreeStartTime) {
                currentFreeStartTime = eventEnd
            }
        }

        // Check for a gap between the last event and the end of the day
        if (currentFreeStartTime < dayEndTime) {
            val gapDuration = dayEndTime - currentFreeStartTime
            if (gapDuration >= minGapDurationMillis) {
                gaps.add(TimeSlot(currentFreeStartTime, dayEndTime))
            }
        }

        return gaps
    }
}
