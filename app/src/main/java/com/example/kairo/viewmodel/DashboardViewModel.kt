package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.local.CalendarProviderDataSource
import com.example.kairo.data.remote.RetrofitClient
import com.example.kairo.data.repository.CommuteRepositoryImpl
import com.example.kairo.data.repository.ScheduleRepositoryImpl
import com.example.kairo.domain.model.ScheduleItem
import com.example.kairo.domain.usecase.CalculateCommuteUseCase
import com.example.kairo.domain.model.CalendarEvent
import com.example.kairo.data.remote.ICalDataSource
import com.example.kairo.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import com.example.kairo.viewmodel.Milestone
import kotlinx.coroutines.flow.StateFlow
import com.example.kairo.domain.repository.CommuteRepository
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val isLoading: Boolean = false,
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val error: String? = null
)

@OptIn(FlowPreview::class)
class DashboardViewModel(
    application: Application,
    private val calculateCommuteUseCase: CalculateCommuteUseCase,
    private val scheduleRepository: ScheduleRepositoryImpl,
    private val preferencesManager: PreferencesManager,
    private val commuteRepository: CommuteRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        viewModelScope.launch {
            // Observe preferences with debounce to avoid firing multiple concurrent network calls on startup
            preferencesManager.preferencesFlow
                .debounce(300)
                .collectLatest {
                    loadSchedule()
                }
        }
    }

    private var loadJob: Job? = null
    
    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _isAddingEvent = MutableStateFlow(false)
    val isAddingEvent: StateFlow<Boolean> = _isAddingEvent.asStateFlow()

    private val _addEventError = MutableStateFlow<String?>(null)
    val addEventError: StateFlow<String?> = _addEventError.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val locationSuggestions: StateFlow<List<String>> = _locationSuggestions.asStateFlow()

    private var searchJob: Job? = null

    fun clearAddEventError() {
        _addEventError.value = null
    }

    fun fetchSuggestions(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _locationSuggestions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Debounce typing
            _locationSuggestions.value = commuteRepository.getLocationSuggestions(query)
        }
    }

    fun clearSuggestions() {
        _locationSuggestions.value = emptyList()
    }
    
    fun setSelectedDate(timeInMillis: Long) {
        _selectedDateMillis.value = timeInMillis
        loadSchedule()
    }

    fun resetToCurrentWeek() {
        _selectedDateMillis.value = System.currentTimeMillis()
        loadSchedule()
    }

    fun loadSchedule() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val wakeStr = preferencesManager.wakeUpTimeFlow.firstOrNull() ?: "07:00"
                val sleepStr = preferencesManager.sleepTimeFlow.firstOrNull() ?: "22:30"
                
                val wakeParts = wakeStr.split(":").map { it.toIntOrNull() ?: 0 }
                val sleepParts = sleepStr.split(":").map { it.toIntOrNull() ?: 0 }

                // Determine time window for the selected date
                val cal = Calendar.getInstance()
                cal.timeInMillis = _selectedDateMillis.value
                
                cal.set(Calendar.HOUR_OF_DAY, wakeParts.getOrElse(0) { 7 })
                cal.set(Calendar.MINUTE, wakeParts.getOrElse(1) { 0 })
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayStart = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, sleepParts.getOrElse(0) { 22 })
                cal.set(Calendar.MINUTE, sleepParts.getOrElse(1) { 30 })
                var dayEnd = cal.timeInMillis

                // If sleep time is early morning (e.g. 02:00) and wake time is morning (07:00), 
                // the sleep time actually refers to the next day.
                if (dayEnd < dayStart) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    dayEnd = cal.timeInMillis
                }

                // Fetch events
                val events = scheduleRepository.getEvents(dayStart, dayEnd)

                val items = mutableListOf<ScheduleItem>()
                
                // Add manual events from Preferences
                val manualEventsJson = preferencesManager.manualEventsFlow.firstOrNull() ?: ""
                val manualEvents = try {
                    if (manualEventsJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<CalendarEvent>>(manualEventsJson) else emptyList()
                } catch(e: Exception) { emptyList() }
                
                val allEvents = (events + manualEvents).filter {
                    it.startTime >= dayStart && it.endTime <= dayEnd
                }.sortedBy { it.startTime }

                val homeLocation = preferencesManager.homeLocationFlow.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Home"
                val workLocation = preferencesManager.workLocationFlow.firstOrNull()?.takeIf { it.isNotBlank() }
                val collegeLocation = preferencesManager.collegeLocationFlow.firstOrNull()?.takeIf { it.isNotBlank() }
                val transportMode = preferencesManager.transportModeFlow.firstOrNull() ?: "All Public Transport"

                fun isTeamsMeeting(loc: String?): Boolean {
                    if (loc.isNullOrBlank()) return false
                    val trimmed = loc.trim()
                    return trimmed.contains("Microsoft Teams Meeting", ignoreCase = true) ||
                           trimmed.contains("Teams Meeting", ignoreCase = true) ||
                           trimmed.equals("Teams", ignoreCase = true)
                }

                // Convert CalendarEvent to ScheduleItem.Event
                val mappedEvents = allEvents.map { event ->
                    val resolvedLoc = if (isTeamsMeeting(event.location)) {
                        homeLocation
                    } else {
                        event.location
                    }
                    ScheduleItem.Event(
                        id = "event_${event.id}",
                        eventId = event.id,
                        title = event.title,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        location = resolvedLoc,
                        description = event.description
                    )
                }.sortedBy { it.startTime }
                
                val pomodoroWork = preferencesManager.pomodoroWorkDurationFlow.firstOrNull()?.toLongOrNull() ?: 90L
                val pomodoroBreak = preferencesManager.pomodoroBreakDurationFlow.firstOrNull()?.toLongOrNull() ?: 15L
                
                val pomodoroWorkMillis = maxOf(1L, pomodoroWork) * 60 * 1000L
                val pomodoroBreakMillis = maxOf(0L, pomodoroBreak) * 60 * 1000L

                // Inject Transit items before events if they have a location
                var lastLocation = homeLocation // Start location is home

                for (i in mappedEvents.indices) {
                    val event = mappedEvents[i]
                    val eventLoc = event.location
                    if (!eventLoc.isNullOrBlank() && eventLoc != lastLocation) {
                        // Calculate commute from lastLocation to eventLoc
                        val commuteResult = calculateCommuteUseCase(
                            originName = lastLocation,
                            destinationName = eventLoc,
                            timeMillis = event.startTime, // We want to arrive by the event start time
                            depArrMacro = "arr",
                            transportMode = transportMode
                        )
                        if (commuteResult != null && commuteResult.durationMillis > 0) {
                            val transitStart = event.startTime - commuteResult.durationMillis
                            
                            // Add 'Get Ready' block if leaving from home
                            if (lastLocation == homeLocation) {
                                val isWorkOrCollege = (workLocation != null && eventLoc == workLocation) ||
                                                      (collegeLocation != null && eventLoc == collegeLocation) ||
                                                      event.title.contains("Work", ignoreCase = true) ||
                                                      event.title.contains("College", ignoreCase = true) ||
                                                      event.title.contains("Class", ignoreCase = true) ||
                                                      event.title.contains("Shift", ignoreCase = true) ||
                                                      event.title.contains("Lecture", ignoreCase = true) ||
                                                      event.title.contains("AIT", ignoreCase = true)

                                val getReadyDuration = if (isWorkOrCollege) 45 * 60 * 1000L else 5 * 60 * 1000L
                                val getReadyTitle = if (isWorkOrCollege) "Get Ready to Leave" else "Quick Prep"
                                val getReadyActivities = if (isWorkOrCollege) listOf("Shower", "Pack bag", "Breakfast") else listOf("Grab keys", "Lock door")

                                val getReadyStart = transitStart - getReadyDuration
                                items.add(
                                    ScheduleItem.FocusBlock(
                                        id = "get_ready_${event.eventId}",
                                        title = getReadyTitle,
                                        startTime = getReadyStart,
                                        endTime = transitStart,
                                        suggestedActivities = getReadyActivities
                                    )
                                )
                            } else {
                                // Provide buffer to reach transit if already out
                                val bufferStart = transitStart - (15 * 60 * 1000L)
                                items.add(
                                    ScheduleItem.FocusBlock(
                                        id = "buffer_${event.eventId}",
                                        title = "Buffer to Transit",
                                        startTime = bufferStart,
                                        endTime = transitStart,
                                        suggestedActivities = listOf("Walk to transit stop")
                                    )
                                )
                            }
                            
                            items.add(
                                ScheduleItem.Transit(
                                    id = "transit_to_${event.eventId}",
                                    title = "Commute to ${eventLoc.take(15)}",
                                    startTime = transitStart,
                                    endTime = event.startTime,
                                    origin = lastLocation,
                                    destination = eventLoc,
                                    durationMillis = commuteResult.durationMillis,
                                    legs = commuteResult.legs
                                )
                            )
                        } else {
                            android.util.Log.e("DashboardViewModel", "Commute failed from $lastLocation to $eventLoc")
                        }
                        lastLocation = eventLoc
                    }
                    items.add(event)

                    // Check if returning home after this event is feasible
                    if (!eventLoc.isNullOrBlank() && eventLoc != homeLocation) {
                        val nextEventWithLoc = mappedEvents.drop(i + 1).firstOrNull { !it.location.isNullOrBlank() }
                        val nextStartTime = nextEventWithLoc?.startTime ?: dayEnd

                        val commuteHomeResult = calculateCommuteUseCase(
                            originName = eventLoc,
                            destinationName = homeLocation,
                            timeMillis = event.endTime,
                            depArrMacro = "dep",
                            transportMode = transportMode
                        )

                        if (commuteHomeResult != null && commuteHomeResult.durationMillis > 0) {
                            val commuteHomeEnd = event.endTime + commuteHomeResult.durationMillis
                            val estNextTransitDuration = if (nextEventWithLoc != null && !nextEventWithLoc.location.isNullOrBlank()) {
                                calculateCommuteUseCase(
                                    originName = homeLocation,
                                    destinationName = nextEventWithLoc.location,
                                    timeMillis = nextEventWithLoc.startTime,
                                    depArrMacro = "arr",
                                    transportMode = transportMode
                                )?.durationMillis ?: (30 * 60 * 1000L)
                            } else 0L

                            val requiredBuffer = 15 * 60 * 1000L
                            val departureForNext = nextStartTime - estNextTransitDuration

                            if (commuteHomeEnd + requiredBuffer <= departureForNext || nextEventWithLoc == null) {
                                items.add(
                                    ScheduleItem.Transit(
                                        id = "transit_home_${event.eventId}",
                                        title = "Commute to Home",
                                        startTime = event.endTime,
                                        endTime = commuteHomeEnd,
                                        origin = eventLoc,
                                        destination = homeLocation,
                                        durationMillis = commuteHomeResult.durationMillis,
                                        legs = commuteHomeResult.legs
                                    )
                                )
                                lastLocation = homeLocation
                            }
                        }
                    }
                }

                // Sort occupied items chronologically
                items.sortBy { it.startTime }

                val finalItems = mutableListOf<ScheduleItem>()
                var gapIndex = 0

                // 1. Inject Morning Routine: Clean up + Breakfast starting at dayStart (Wake Up Time)
                val cleanUpEnd = dayStart + (30 * 60 * 1000L) // 30 mins
                val breakfastEnd = cleanUpEnd + (30 * 60 * 1000L) // 30 mins
                
                // Check if any items conflict with morning routine (between dayStart and breakfastEnd)
                val morningConflict = items.any { it.startTime < breakfastEnd && it.endTime > dayStart }

                if (!morningConflict) {
                    finalItems.add(
                        ScheduleItem.FocusBlock(
                            id = "morning_cleanup",
                            title = "Morning Clean Up",
                            startTime = dayStart,
                            endTime = cleanUpEnd,
                            suggestedActivities = listOf("Make bed", "Shower", "Get dressed")
                        )
                    )
                    finalItems.add(
                        ScheduleItem.FocusBlock(
                            id = "morning_breakfast",
                            title = "Breakfast",
                            startTime = cleanUpEnd,
                            endTime = breakfastEnd,
                            suggestedActivities = listOf("Eat healthy breakfast", "Check emails")
                        )
                    )
                }

                // 2. Process Gaps with Pomodoro logic (STRICTLY starting from maxOf(dayStart, breakfastEnd))
                var currentFreeStart = if (!morningConflict) breakfastEnd else dayStart
                
                // Read pending milestones to use as focus block titles
                val customMilestonesJson = preferencesManager.customMilestonesFlow.firstOrNull() ?: ""
                val completedIdsJson = preferencesManager.completedMilestonesFlow.firstOrNull() ?: ""
                val mainGoalsIdsJson = preferencesManager.mainGoalMilestonesFlow.firstOrNull() ?: ""
                
                val customMilestones = try {
                    if (customMilestonesJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<Milestone>>(customMilestonesJson) else emptyList()
                } catch(e: Exception) { emptyList() }
                
                val completedIds = try {
                    if (completedIdsJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<String>>(completedIdsJson).toSet() else emptySet()
                } catch(e: Exception) { emptySet() }
                
                val mainGoalIds = try {
                    if (mainGoalsIdsJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<String>>(mainGoalsIdsJson).toSet() else emptySet()
                } catch(e: Exception) { emptySet() }
                
                // Also pull from college events to merge pending tasks
                val collegeUrl = preferencesManager.collegeCalendarUrlFlow.firstOrNull() ?: ""
                val collegeEvents = if (collegeUrl.isNotBlank()) {
                    ICalDataSource().fetchEvents(collegeUrl)
                } else emptyList()
                
                val allMilestones = customMilestones + collegeEvents.map { Milestone(it.id.toString(), it.title) }

                // Only get bookmarked (mainGoalIds) AND uncompleted (!completedIds) tasks
                val bookmarkedTasks = allMilestones
                    .filter { mainGoalIds.contains(it.id) && !completedIds.contains(it.id) }
                    .map { it.title }

                var bookmarkedIndex = 0

                for (item in items) {
                    val itemStart = item.startTime
                    val itemEnd = item.endTime

                    // Skip gap calculation for items ending before/at currentFreeStart (e.g. early/past events)
                    if (itemEnd <= currentFreeStart) {
                        finalItems.add(item)
                        continue
                    }

                    val effectiveGapStart = maxOf(currentFreeStart, dayStart)
                    if (effectiveGapStart < itemStart) {
                        var chunkStart = effectiveGapStart
                        // Generate 90 min work + 15 min break chunks until we hit itemStart
                        while (chunkStart < itemStart) {
                            val remainingGap = itemStart - chunkStart
                            if (remainingGap < 30 * 60 * 1000L) {
                                // Small gap, just add a generic block
                                finalItems.add(
                                    ScheduleItem.FocusBlock(
                                        id = "gap_${gapIndex++}",
                                        title = "Free Time",
                                        startTime = chunkStart,
                                        endTime = itemStart,
                                        suggestedActivities = listOf("Relax", "Stretch", "Check phone")
                                    )
                                )
                                break
                            } else {
                                // We have at least 30 mins. Try to allocate pomodoro work.
                                val workDuration = minOf(pomodoroWorkMillis, remainingGap)
                                val workEnd = chunkStart + workDuration
                                
                                val nextTask = if (bookmarkedTasks.isNotEmpty()) {
                                    val task = bookmarkedTasks[bookmarkedIndex % bookmarkedTasks.size]
                                    bookmarkedIndex++
                                    task
                                } else {
                                    "Study/Assignment Work"
                                }
                                
                                finalItems.add(
                                    ScheduleItem.FocusBlock(
                                        id = "work_${gapIndex++}",
                                        title = nextTask,
                                        startTime = chunkStart,
                                        endTime = workEnd,
                                        suggestedActivities = listOf("Deep work", "Focus on top priority task")
                                    )
                                )
                                
                                chunkStart = workEnd
                                
                                // If there's still time left, try to allocate a break
                                val breakRemaining = itemStart - chunkStart
                                if (breakRemaining > 0 && pomodoroBreakMillis > 0) {
                                    val breakDuration = minOf(pomodoroBreakMillis, breakRemaining)
                                    val breakEnd = chunkStart + breakDuration
                                    finalItems.add(
                                        ScheduleItem.FocusBlock(
                                            id = "break_${gapIndex++}",
                                            title = "Break",
                                            startTime = chunkStart,
                                            endTime = breakEnd,
                                            suggestedActivities = listOf("Walk around", "Drink water", "Rest eyes")
                                        )
                                    )
                                    chunkStart = breakEnd
                                }
                            }
                        }
                    }
                    finalItems.add(item)
                    if (itemEnd > currentFreeStart) {
                        currentFreeStart = itemEnd
                    }
                }


                // 3. Process remaining time until Wind Down
                val windDownStart = dayEnd - (60 * 60 * 1000L) // 1 hour before sleep
                
                if (currentFreeStart < windDownStart) {
                    var chunkStart = currentFreeStart
                    while (chunkStart < windDownStart) {
                        val remainingGap = windDownStart - chunkStart
                        if (remainingGap < 30 * 60 * 1000L) {
                            finalItems.add(
                                ScheduleItem.FocusBlock(
                                    id = "gap_${gapIndex++}",
                                    title = "Free Time",
                                    startTime = chunkStart,
                                    endTime = windDownStart,
                                    suggestedActivities = listOf("Relax", "Stretch")
                                )
                            )
                            break
                        } else {
                            val workDuration = minOf(pomodoroWorkMillis, remainingGap)
                            val workEnd = chunkStart + workDuration
                            
                            val nextTask = if (bookmarkedTasks.isNotEmpty()) {
                                val task = bookmarkedTasks[bookmarkedIndex % bookmarkedTasks.size]
                                bookmarkedIndex++
                                task
                            } else {
                                "Study/Assignment Work"
                            }
                            
                            finalItems.add(
                                ScheduleItem.FocusBlock(
                                    id = "work_${gapIndex++}",
                                    title = nextTask,
                                    startTime = chunkStart,
                                    endTime = workEnd,
                                    suggestedActivities = listOf("Deep work")
                                )
                            )
                            chunkStart = workEnd
                            
                            val breakRemaining = windDownStart - chunkStart
                            if (breakRemaining > 0 && pomodoroBreakMillis > 0) {
                                val breakDuration = minOf(pomodoroBreakMillis, breakRemaining)
                                val breakEnd = chunkStart + breakDuration
                                finalItems.add(
                                    ScheduleItem.FocusBlock(
                                        id = "break_${gapIndex++}",
                                        title = "Break",
                                        startTime = chunkStart,
                                        endTime = breakEnd,
                                        suggestedActivities = listOf("Rest")
                                    )
                                )
                                chunkStart = breakEnd
                            }
                        }
                    }
                }
                
                // 4. Inject Evening Routine: Wind Down
                val finalWindDownStart = maxOf(currentFreeStart, windDownStart)
                if (finalWindDownStart < dayEnd) {
                    finalItems.add(
                        ScheduleItem.FocusBlock(
                            id = "evening_wind_down",
                            title = "Wind Down",
                            startTime = finalWindDownStart,
                            endTime = dayEnd,
                            suggestedActivities = listOf("Read a book", "Prepare for tomorrow", "No screens")
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    scheduleItems = finalItems
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error occurred"
                )
            }
        }
    }

    fun addManualEvent(
        title: String, 
        location: String, 
        description: String, 
        startTime: Long, 
        endTime: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isAddingEvent.value = true
            _addEventError.value = null

            val trimmedLoc = location.trim()
            val isTeamsMeeting = trimmedLoc.contains("Microsoft Teams Meeting", ignoreCase = true) ||
                                 trimmedLoc.contains("Teams Meeting", ignoreCase = true) ||
                                 trimmedLoc.equals("Teams", ignoreCase = true)

            val homeLoc = preferencesManager.homeLocationFlow.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Home"
            val finalLocation = if (isTeamsMeeting) homeLoc else trimmedLoc

            if (!isTeamsMeeting && finalLocation.isNotBlank() && !finalLocation.equals("Home", ignoreCase = true)) {
                val isValid = commuteRepository.validateLocation(finalLocation)
                if (!isValid) {
                    _addEventError.value = "Error: Invalid Location according to TfNSW."
                    _isAddingEvent.value = false
                    return@launch
                }
            }

            val currentJson = preferencesManager.manualEventsFlow.firstOrNull() ?: ""
            val currentList = try {
                if (currentJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<CalendarEvent>>(currentJson) else emptyList()
            } catch(e: Exception) { emptyList() }
            
            val newEvent = CalendarEvent(
                id = System.currentTimeMillis(),
                title = title,
                startTime = startTime,
                endTime = endTime,
                location = finalLocation.ifBlank { null },
                description = description.ifBlank { null }
            )
            val newList = currentList + newEvent
            preferencesManager.saveManualEvents(kotlinx.serialization.json.Json.encodeToString(newList))

            _isAddingEvent.value = false
            onSuccess()
            loadSchedule()
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val calendarProvider = CalendarProviderDataSource(application)
                val preferencesManager = com.example.kairo.data.PreferencesManager(application)
                val iCalDataSource = com.example.kairo.data.remote.ICalDataSource()
                val scheduleRepo = ScheduleRepositoryImpl(calendarProvider, iCalDataSource, preferencesManager)
                val commuteRepo = CommuteRepositoryImpl(RetrofitClient.tfNSWApiService)
                val calcCommute = CalculateCommuteUseCase(commuteRepo)
                return DashboardViewModel(application, calcCommute, scheduleRepo, preferencesManager, commuteRepo) as T
            }
        }
    }
}
