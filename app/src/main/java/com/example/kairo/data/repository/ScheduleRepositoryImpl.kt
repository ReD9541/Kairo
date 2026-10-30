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
        
        fun resolveEventLocation(rawLoc: String?, defaultLocSetting: String): String? {
            val trimmed = rawLoc?.trim()
            if (trimmed.isNullOrBlank()) return defaultLocSetting.ifBlank { null }
            if (trimmed.contains("Microsoft Teams Meeting", ignoreCase = true) ||
                trimmed.contains("Teams Meeting", ignoreCase = true) ||
                trimmed.equals("Teams", ignoreCase = true)) {
                return homeLocation.ifBlank { "Home" }
            }
            if (trimmed.equals("Home", ignoreCase = true)) return homeLocation.ifBlank { trimmed }
            if (trimmed.equals("Work", ignoreCase = true)) return workLocation.ifBlank { trimmed }
            if (trimmed.equals("College", ignoreCase = true) || trimmed.equals("Campus", ignoreCase = true)) return collegeLocation.ifBlank { trimmed }
            
            // Keep the specific original address if present
            return trimmed
        }

        val rawWorkEvents = if (workUrl.isNotBlank()) iCalDataSource.fetchEvents(workUrl) else emptyList()
        val rawCollegeEvents = if (collegeUrl.isNotBlank()) iCalDataSource.fetchEvents(collegeUrl) else emptyList()
        val rawPersonalEvents = if (personalUrl.isNotBlank()) iCalDataSource.fetchEvents(personalUrl) else emptyList()
        
        val workEvents = rawWorkEvents.map { 
            it.copy(location = resolveEventLocation(it.location, workLocation))
        }
        val collegeEvents = rawCollegeEvents.map {
            it.copy(location = resolveEventLocation(it.location, collegeLocation))
        }
        val personalEvents = rawPersonalEvents.map {
            it.copy(location = resolveEventLocation(it.location, homeLocation))
        }
        val mappedLocalEvents = localEvents.map {
            it.copy(location = resolveEventLocation(it.location, homeLocation))
        }

        fun isAssignmentOrDeadline(event: CalendarEvent): Boolean {
            val title = event.title.lowercase()
            val desc = event.description?.lowercase() ?: ""
            return title.contains("assignment") || title.contains("submission") ||
                   title.contains("due") || title.contains("prototype") ||
                   title.contains("quiz") || title.contains("deadline") ||
                   desc.contains("due date") || desc.contains("submission") ||
                   Regex("""\b(a[1-9]|m[1-9]|p[1-9])\b""").containsMatchIn(title)
        }

        val allEvents = (mappedLocalEvents + workEvents + collegeEvents + personalEvents).filter {
            it.endTime > startTimeMillis && it.startTime < endTimeMillis && !isAssignmentOrDeadline(it)
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
