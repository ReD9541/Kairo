package com.example.kairo.data.repository

import com.example.kairo.data.local.CalendarProviderDataSource
import com.example.kairo.data.remote.ICalDataSource
import com.example.kairo.data.PreferencesManager
import com.example.kairo.domain.model.CalendarEvent
import com.example.kairo.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.firstOrNull

class ScheduleRepositoryImpl(
    private val calendarProviderDataSource: CalendarProviderDataSource,
    private val iCalDataSource: ICalDataSource,
    private val preferencesManager: PreferencesManager
) : ScheduleRepository {
    override suspend fun getEvents(startTimeMillis: Long, endTimeMillis: Long): List<CalendarEvent> {
        val localEvents = calendarProviderDataSource.getEvents(startTimeMillis, endTimeMillis)
        
        val workUrl = preferencesManager.workCalendarUrlFlow.firstOrNull() ?: ""
        val collegeUrl = preferencesManager.collegeCalendarUrlFlow.firstOrNull() ?: ""
        val personalUrl = preferencesManager.personalCalendarUrlFlow.firstOrNull() ?: ""
        
        val homeLocation = preferencesManager.homeLocationFlow.firstOrNull() ?: ""
        val workLocation = preferencesManager.workLocationFlow.firstOrNull() ?: ""
        val collegeLocation = preferencesManager.collegeLocationFlow.firstOrNull() ?: ""
        
        fun resolveLoc(rawLoc: String?, defaultLoc: String = ""): String? {
            val target = rawLoc?.trim()
            if (target.isNullOrBlank()) return defaultLoc.ifBlank { null }
            return when {
                target.contains("Microsoft Teams Meeting", ignoreCase = true) ||
                target.contains("Teams Meeting", ignoreCase = true) ||
                target.equals("Teams", ignoreCase = true) -> homeLocation.ifBlank { "Home" }
                target.equals("Home", ignoreCase = true) -> homeLocation.ifBlank { rawLoc }
                target.equals("Work", ignoreCase = true) -> workLocation.ifBlank { rawLoc }
                target.equals("College", ignoreCase = true) -> collegeLocation.ifBlank { rawLoc }
                else -> rawLoc
            }
        }

        val rawWorkEvents = if (workUrl.isNotBlank()) iCalDataSource.fetchEvents(workUrl) else emptyList()
        val rawCollegeEvents = if (collegeUrl.isNotBlank()) iCalDataSource.fetchEvents(collegeUrl) else emptyList()
        val rawPersonalEvents = if (personalUrl.isNotBlank()) iCalDataSource.fetchEvents(personalUrl) else emptyList()
        
        val workEvents = rawWorkEvents.map { 
            val loc = it.location?.trim() ?: ""
            if (loc.contains("Microsoft Teams Meeting", ignoreCase = true) || loc.contains("Teams Meeting", ignoreCase = true)) {
                it.copy(location = homeLocation.ifBlank { "Home" })
            } else if (workLocation.isNotBlank()) {
                it.copy(location = workLocation)
            } else it
        }
        val collegeEvents = rawCollegeEvents.map {
            val loc = it.location?.trim() ?: ""
            if (loc.contains("Microsoft Teams Meeting", ignoreCase = true) || loc.contains("Teams Meeting", ignoreCase = true)) {
                it.copy(location = homeLocation.ifBlank { "Home" })
            } else if (collegeLocation.isNotBlank()) {
                it.copy(location = collegeLocation)
            } else it
        }
        val personalEvents = rawPersonalEvents.map {
            it.copy(location = resolveLoc(it.location))
        }
        val mappedLocalEvents = localEvents.map {
            it.copy(location = resolveLoc(it.location))
        }

        val allEvents = (mappedLocalEvents + workEvents + collegeEvents + personalEvents).filter {
            it.startTime >= startTimeMillis && it.startTime <= endTimeMillis
        }

        // Deduplicate events that have matching titles and start/end times
        val deduplicatedEvents = mutableListOf<CalendarEvent>()
        for (event in allEvents.sortedBy { it.startTime }) {
            val duplicateIndex = deduplicatedEvents.indexOfFirst { existing ->
                existing.title.trim().equals(event.title.trim(), ignoreCase = true) &&
                kotlin.math.abs(existing.startTime - event.startTime) < 5 * 60 * 1000L &&
                kotlin.math.abs(existing.endTime - event.endTime) < 5 * 60 * 1000L
            }

            if (duplicateIndex == -1) {
                deduplicatedEvents.add(event)
            } else {
                val existing = deduplicatedEvents[duplicateIndex]
                val existingLoc = existing.location ?: ""
                val newLoc = event.location ?: ""
                
                // If the new location perfectly matches our custom settings, prefer it
                if (newLoc.isNotBlank() && (newLoc == workLocation || newLoc == collegeLocation || newLoc == homeLocation)) {
                    deduplicatedEvents[duplicateIndex] = event
                } else if (existingLoc.isBlank() && newLoc.isNotBlank()) {
                    deduplicatedEvents[duplicateIndex] = event
                }
            }
        }
        
        return deduplicatedEvents.sortedBy { it.startTime }
    }
}
