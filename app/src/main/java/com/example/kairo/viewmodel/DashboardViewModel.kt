package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.PreferencesManager
import com.example.kairo.data.local.CalendarProviderDataSource
import com.example.kairo.data.remote.ICalDataSource
import com.example.kairo.data.remote.RetrofitClient
import com.example.kairo.data.repository.CommuteRepositoryImpl
import com.example.kairo.data.repository.ScheduleRepositoryImpl
import com.example.kairo.domain.model.CalendarEvent
import com.example.kairo.domain.model.ScheduleItem
import com.example.kairo.domain.repository.CommuteRepository
import com.example.kairo.domain.usecase.CalculateCommuteUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
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

    private var loadJob: Job? = null
    
    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _isAddingEvent = MutableStateFlow(false)
    val isAddingEvent: StateFlow<Boolean> = _isAddingEvent.asStateFlow()

    private val _addEventError = MutableStateFlow<String?>(null)
    val addEventError: StateFlow<String?> = _addEventError.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val locationSuggestions: StateFlow<List<String>> = _locationSuggestions.asStateFlow()

    private val _quickMealActive = MutableStateFlow(false)
    val quickMealActive: StateFlow<Boolean> = _quickMealActive.asStateFlow()

    private val _runningLateMins = MutableStateFlow(0)
    val runningLateMins: StateFlow<Int> = _runningLateMins.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesManager.quickMealModeFlow.collectLatest {
                _quickMealActive.value = it
            }
        }
        viewModelScope.launch {
            preferencesManager.runningLateMinsFlow.collectLatest {
                _runningLateMins.value = it
            }
        }
        viewModelScope.launch {
            preferencesManager.preferencesFlow
                .debounce(300)
                .collectLatest {
                    loadSchedule()
                }
        }
    }

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
            delay(300)
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

    fun toggleQuickMealMode() {
        viewModelScope.launch {
            val newMode = !_quickMealActive.value
            _quickMealActive.value = newMode
            preferencesManager.toggleQuickMealMode(newMode)
            loadSchedule()
        }
    }

    fun addRunningLate15Mins() {
        viewModelScope.launch {
            val newMins = _runningLateMins.value + 15
            _runningLateMins.value = newMins
            preferencesManager.setRunningLateMins(newMins)
            loadSchedule()
        }
    }

    fun resetRunningLate() {
        viewModelScope.launch {
            _runningLateMins.value = 0
            preferencesManager.setRunningLateMins(0)
            loadSchedule()
        }
    }

    fun loadSchedule() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val wakeStr = preferencesManager.wakeUpTimeFlow.firstOrNull() ?: "07:00"
                val sleepStr = preferencesManager.sleepTimeFlow.firstOrNull() ?: "22:30"
                
                val morningMinsVal = preferencesManager.morningRoutineMinsFlow.firstOrNull()?.toLongOrNull() ?: 40L
                val decompressMinsVal = preferencesManager.postCommuteDecompressMinsFlow.firstOrNull()?.toLongOrNull() ?: 35L
                val studyPrepMinsVal = preferencesManager.studyPrepBufferMinsFlow.firstOrNull()?.toLongOrNull() ?: 15L
                val lunchMinsVal = preferencesManager.lunchDurationMinsFlow.firstOrNull()?.toLongOrNull() ?: 45L
                val isQuickMeal = preferencesManager.quickMealModeFlow.firstOrNull() ?: false
                val dinnerMinsVal = if (isQuickMeal) 25L else (preferencesManager.dinnerPrepAndEatMinsFlow.firstOrNull()?.toLongOrNull() ?: 60L)
                val minStudySlotMinsVal = preferencesManager.minStudySlotMinsFlow.firstOrNull()?.toLongOrNull() ?: 35L
                val runningLate = preferencesManager.runningLateMinsFlow.firstOrNull() ?: 0

                val wakeParts = wakeStr.split(":").map { it.toIntOrNull() ?: 0 }
                val sleepParts = sleepStr.split(":").map { it.toIntOrNull() ?: 0 }

                // Selected Day Start (Wake Up)
                val cal = Calendar.getInstance()
                cal.timeInMillis = _selectedDateMillis.value
                cal.set(Calendar.HOUR_OF_DAY, wakeParts.getOrElse(0) { 7 })
                cal.set(Calendar.MINUTE, wakeParts.getOrElse(1) { 0 })
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                val baseDayStart = cal.timeInMillis
                val dayStart = baseDayStart + (runningLate * 60 * 1000L)

                // Next Day Start (Strict Day Boundary)
                val nextDayCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val nextDayStart = nextDayCal.timeInMillis

                // Base Sleep Time on Selected Day
                cal.set(Calendar.HOUR_OF_DAY, sleepParts.getOrElse(0) { 22 })
                cal.set(Calendar.MINUTE, sleepParts.getOrElse(1) { 30 })
                var dayEnd = cal.timeInMillis

                if (dayEnd < baseDayStart) {
                    dayEnd += 24 * 60 * 60 * 1000L
                }

                val queryStart = baseDayStart - (4 * 60 * 60 * 1000L)
                val queryEnd = nextDayStart + (12 * 60 * 60 * 1000L)

                val events = scheduleRepository.getEvents(queryStart, queryEnd)
                
                val manualEventsJson = preferencesManager.manualEventsFlow.firstOrNull() ?: ""
                val manualEvents = try {
                    if (manualEventsJson.isNotBlank()) kotlinx.serialization.json.Json.decodeFromString<List<CalendarEvent>>(manualEventsJson) else emptyList()
                } catch(e: Exception) { emptyList() }
                
                val dayEvents = (events + manualEvents).filter {
                    it.startTime >= dayStart && it.startTime < nextDayStart
                }.sortedBy { it.startTime }

                val maxEventEnd = dayEvents.maxOfOrNull { it.endTime } ?: 0L
                if (maxEventEnd > dayEnd) {
                    dayEnd = minOf(nextDayStart, maxEventEnd + (2 * 60 * 60 * 1000L))
                }

                val cal21 = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 21)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                val lateShiftEvent = dayEvents.firstOrNull { it.endTime >= cal21 || (it.startTime >= cal21 - (4 * 60 * 60 * 1000L) && it.title.contains("Work|Shift|Class", ignoreCase = true)) }
                val hasLateShift = lateShiftEvent != null
                val lateShiftStart = lateShiftEvent?.startTime ?: Long.MAX_VALUE

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

                val mappedEvents = dayEvents.map { event ->
                    val resolvedLoc = if (isTeamsMeeting(event.location)) homeLocation else event.location
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
                
                val pomodoroWorkMillis = maxOf(1L, preferencesManager.pomodoroWorkDurationFlow.firstOrNull()?.toLongOrNull() ?: 90L) * 60 * 1000L
                val pomodoroBreakMillis = maxOf(0L, preferencesManager.pomodoroBreakDurationFlow.firstOrNull()?.toLongOrNull() ?: 15L) * 60 * 1000L

                val items = mutableListOf<ScheduleItem>()
                var lastLocation = homeLocation
                var prevOccupiedEndTime = dayStart

                fun parseIsoTime(isoStr: String?): Long? {
                    if (isoStr.isNullOrBlank()) return null
                    return try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        sdf.parse(isoStr)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

                for (i in mappedEvents.indices) {
                    val event = mappedEvents[i]
                    val eventLoc = event.location

                    if (!eventLoc.isNullOrBlank() && eventLoc != lastLocation) {
                        val commuteResult = calculateCommuteUseCase(
                            originName = lastLocation,
                            destinationName = eventLoc,
                            timeMillis = event.startTime,
                            depArrMacro = "arr",
                            transportMode = transportMode
                        )

                        if (commuteResult != null && commuteResult.durationMillis > 0) {
                            val firstLegStart = commuteResult.legs.firstOrNull()?.departureTime?.let { parseIsoTime(it) }
                            val lastLegEnd = commuteResult.legs.lastOrNull()?.arrivalTime?.let { parseIsoTime(it) }

                            val transitStart = firstLegStart ?: (event.startTime - commuteResult.durationMillis)
                            val transitEnd = lastLegEnd ?: event.startTime

                            if (lastLocation == homeLocation) {
                                val isWorkOrCollege = (workLocation != null && eventLoc == workLocation) ||
                                                      (collegeLocation != null && eventLoc == collegeLocation) ||
                                                      event.title.contains("Work", ignoreCase = true) ||
                                                      event.title.contains("College", ignoreCase = true) ||
                                                      event.title.contains("Class", ignoreCase = true)

                                val desiredGetReadyDuration = if (isWorkOrCollege) 45 * 60 * 1000L else 15 * 60 * 1000L
                                val getReadyTitle = if (isWorkOrCollege) "Get Ready to Leave" else "Quick Prep"

                                val availablePrepTime = transitStart - maxOf(prevOccupiedEndTime, dayStart)
                                if (availablePrepTime > 0) {
                                    val actualPrepDuration = minOf(desiredGetReadyDuration, availablePrepTime)
                                    val getReadyStart = transitStart - actualPrepDuration
                                    items.add(
                                        ScheduleItem.FocusBlock(
                                            id = "get_ready_${event.eventId}",
                                            title = getReadyTitle,
                                            startTime = getReadyStart,
                                            endTime = transitStart,
                                            suggestedActivities = listOf("Shower & dress", "Pack bag", "Grab essentials")
                                        )
                                    )
                                    prevOccupiedEndTime = transitStart
                                }
                            } else {
                                val availableBufferTime = transitStart - maxOf(prevOccupiedEndTime, dayStart)
                                if (availableBufferTime > 0) {
                                    val actualBuffer = minOf(15 * 60 * 1000L, availableBufferTime)
                                    val bufferStart = transitStart - actualBuffer
                                    items.add(
                                        ScheduleItem.FocusBlock(
                                            id = "buffer_${event.eventId}",
                                            title = "Buffer to Transit",
                                            startTime = bufferStart,
                                            endTime = transitStart,
                                            suggestedActivities = listOf("Walk to transit stop")
                                        )
                                    )
                                    prevOccupiedEndTime = transitStart
                                }
                            }

                            items.add(
                                ScheduleItem.TransitTo(
                                    id = "transit_to_${event.eventId}",
                                    title = "Commute to ${eventLoc.take(15)}",
                                    startTime = transitStart,
                                    endTime = transitEnd,
                                    origin = lastLocation,
                                    destination = eventLoc,
                                    durationMillis = maxOf(commuteResult.durationMillis, transitEnd - transitStart),
                                    legs = commuteResult.legs
                                )
                            )
                            prevOccupiedEndTime = maxOf(prevOccupiedEndTime, transitEnd)
                        }
                        lastLocation = eventLoc
                    }

                    val eventStart = maxOf(event.startTime, prevOccupiedEndTime)
                    if (event.endTime > eventStart) {
                        items.add(
                            ScheduleItem.Event(
                                id = event.id,
                                eventId = event.eventId,
                                title = event.title,
                                startTime = eventStart,
                                endTime = event.endTime,
                                location = event.location,
                                description = event.description
                            )
                        )
                        prevOccupiedEndTime = event.endTime
                    }

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
                            val homeFirstLegStart = commuteHomeResult.legs.firstOrNull()?.departureTime?.let { parseIsoTime(it) }
                            val homeLastLegEnd = commuteHomeResult.legs.lastOrNull()?.arrivalTime?.let { parseIsoTime(it) }

                            val commuteHomeStart = homeFirstLegStart ?: event.endTime
                            val commuteHomeEnd = homeLastLegEnd ?: (commuteHomeStart + commuteHomeResult.durationMillis)

                            items.add(
                                ScheduleItem.TransitHome(
                                    id = "transit_home_${event.eventId}",
                                    title = "Commute Home",
                                    startTime = commuteHomeStart,
                                    endTime = commuteHomeEnd,
                                    origin = eventLoc,
                                    destination = homeLocation,
                                    durationMillis = maxOf(commuteHomeResult.durationMillis, commuteHomeEnd - commuteHomeStart),
                                    legs = commuteHomeResult.legs
                                )
                            )
                            prevOccupiedEndTime = commuteHomeEnd

                            val availableDecompressTime = maxOf(0L, nextStartTime - commuteHomeEnd)
                            val decompressDuration = if (nextEventWithLoc == null) {
                                decompressMinsVal * 60 * 1000L
                            } else {
                                minOf(decompressMinsVal * 60 * 1000L, availableDecompressTime)
                            }

                            if (decompressDuration >= 5 * 60 * 1000L) {
                                items.add(
                                    ScheduleItem.Decompression(
                                        id = "decompress_${event.eventId}",
                                        title = "Post-Commute Decompression",
                                        startTime = commuteHomeEnd,
                                        endTime = commuteHomeEnd + decompressDuration,
                                        description = "Arrive home, shower, change clothes & unwind"
                                    )
                                )
                                prevOccupiedEndTime = commuteHomeEnd + decompressDuration
                            }
                            lastLocation = homeLocation

                            if (nextEventWithLoc == null && prevOccupiedEndTime > dayEnd) {
                                dayEnd = prevOccupiedEndTime + (30 * 60 * 1000L)
                            }
                        }
                    }
                }

                items.sortBy { it.startTime }

                val firstItemStart = items.firstOrNull()?.startTime ?: dayEnd
                val morningRoutineDuration = morningMinsVal * 60 * 1000L
                val availableMorningTime = maxOf(0L, firstItemStart - dayStart)

                if (availableMorningTime >= 10 * 60 * 1000L) {
                    val actualMorningDuration = minOf(morningRoutineDuration, availableMorningTime)
                    items.add(
                        0,
                        ScheduleItem.MorningRoutine(
                            id = "morning_routine",
                            title = "Morning Routine & Breakfast",
                            startTime = dayStart,
                            endTime = dayStart + actualMorningDuration,
                            activities = listOf("Shower & hygiene", "Healthy breakfast", "Hydrate & plan day")
                        )
                    )
                }

                items.sortBy { it.startTime }

                val gapTargets = items.toMutableList()
                val windDownStart = maxOf(dayStart, dayEnd - (60 * 60 * 1000L))
                if (gapTargets.isEmpty() || gapTargets.last().endTime < windDownStart) {
                    gapTargets.add(
                        ScheduleItem.FocusBlock(
                            id = "day_end_anchor",
                            title = "Day End Anchor",
                            startTime = windDownStart,
                            endTime = windDownStart
                        )
                    )
                }

                val gapFilledItems = mutableListOf<ScheduleItem>()
                var gapIndex = 0

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
                
                val collegeUrl = preferencesManager.collegeCalendarUrlFlow.firstOrNull() ?: ""
                val collegeEvents = if (collegeUrl.isNotBlank()) ICalDataSource().fetchEvents(collegeUrl) else emptyList()
                val allMilestones = customMilestones + collegeEvents.map { Milestone(it.id.toString(), it.title) }

                val bookmarkedTasks = allMilestones
                    .filter { mainGoalIds.contains(it.id) && !completedIds.contains(it.id) }
                    .map { it.title }

                var bookmarkedIndex = 0

                val calLunchStart = Calendar.getInstance().apply {
                    timeInMillis = _selectedDateMillis.value
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                val calDinnerStart = Calendar.getInstance().apply {
                    timeInMillis = _selectedDateMillis.value
                    set(Calendar.HOUR_OF_DAY, 18)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                var lunchScheduled = false
                var dinnerScheduled = false

                var cursor = dayStart

                for (item in gapTargets) {
                    if (item.startTime > cursor) {
                        var chunkStart = cursor
                        val gapEnd = item.startTime

                        while (chunkStart < gapEnd) {
                            val remainingGap = gapEnd - chunkStart
                            val minStudyMillis = minStudySlotMinsVal * 60 * 1000L

                            // Lunch Anchor Check
                            if (!lunchScheduled && chunkStart >= calLunchStart - (60 * 60 * 1000L) && chunkStart <= calLunchStart + (90 * 60 * 1000L)) {
                                val lunchMillis = minOf(lunchMinsVal * 60 * 1000L, remainingGap)
                                val lunchEnd = chunkStart + lunchMillis
                                gapFilledItems.add(
                                    ScheduleItem.MealPrepAndEat(
                                        id = "lunch_anchor_${gapIndex++}",
                                        title = "Lunch & Reset",
                                        startTime = chunkStart,
                                        endTime = lunchEnd,
                                        mealType = "Lunch"
                                    )
                                )
                                chunkStart = lunchEnd
                                lunchScheduled = true
                                continue
                            }

                            // Dinner Anchor Check (MUST be before late shift starts!)
                            if (!dinnerScheduled && chunkStart < lateShiftStart && (hasLateShift || (chunkStart >= calDinnerStart - (30 * 60 * 1000L) && chunkStart <= calDinnerStart + (120 * 60 * 1000L)))) {
                                val dinnerMillis = minOf(dinnerMinsVal * 60 * 1000L, remainingGap)
                                val dinnerEnd = chunkStart + dinnerMillis
                                val dinnerTitle = if (hasLateShift) "Dinner & Meal Prep before Shift" else if (isQuickMeal) "Quick Dinner / Leftovers" else "Meal Prep, Dinner & Cleanup"
                                gapFilledItems.add(
                                    ScheduleItem.MealPrepAndEat(
                                        id = "dinner_anchor_${gapIndex++}",
                                        title = dinnerTitle,
                                        startTime = chunkStart,
                                        endTime = dinnerEnd,
                                        mealType = "Dinner"
                                    )
                                )
                                chunkStart = dinnerEnd
                                dinnerScheduled = true
                                continue
                            }

                            // IF HAS LATE SHIFT AND chunkStart < lateShiftStart: Pre-Shift Rest & Prep
                            if (hasLateShift && chunkStart < lateShiftStart) {
                                gapFilledItems.add(
                                    ScheduleItem.MicroBreak(
                                        id = "pre_shift_rest_${gapIndex++}",
                                        title = "Pre-Shift Rest & Prep",
                                        startTime = chunkStart,
                                        endTime = gapEnd,
                                        description = "Unwind, hydrate & prepare for evening work"
                                    )
                                )
                                chunkStart = gapEnd
                                break
                            }

                            if (remainingGap < minStudyMillis) {
                                gapFilledItems.add(
                                    ScheduleItem.MicroBreak(
                                        id = "micro_break_${gapIndex++}",
                                        title = "Micro Break",
                                        startTime = chunkStart,
                                        endTime = gapEnd,
                                        description = "Short break, stretch & hydrate"
                                    )
                                )
                                chunkStart = gapEnd
                                break
                            } else {
                                val studyPrepMillis = studyPrepMinsVal * 60 * 1000L
                                if (remainingGap >= studyPrepMillis + minStudyMillis) {
                                    val setupEnd = chunkStart + studyPrepMillis
                                    gapFilledItems.add(
                                        ScheduleItem.StudySetup(
                                            id = "study_setup_${gapIndex++}",
                                            title = "Workspace Prep",
                                            startTime = chunkStart,
                                            endTime = setupEnd,
                                            description = "Clear workspace, open notes & grab water"
                                        )
                                    )
                                    chunkStart = setupEnd
                                }

                                val availableStudyTime = gapEnd - chunkStart
                                val studyDuration = minOf(pomodoroWorkMillis, availableStudyTime)
                                val studyEnd = chunkStart + studyDuration

                                val nextTask = if (bookmarkedTasks.isNotEmpty()) {
                                    val t = bookmarkedTasks[bookmarkedIndex % bookmarkedTasks.size]
                                    bookmarkedIndex++
                                    t
                                } else "Deep Study & Assignment Work"

                                gapFilledItems.add(
                                    ScheduleItem.DeepStudy(
                                        id = "deep_study_${gapIndex++}",
                                        title = nextTask,
                                        startTime = chunkStart,
                                        endTime = studyEnd,
                                        taskName = nextTask
                                    )
                                )
                                chunkStart = studyEnd

                                val breakRemaining = gapEnd - chunkStart
                                if (breakRemaining > 0 && pomodoroBreakMillis > 0) {
                                    val breakDuration = minOf(pomodoroBreakMillis, breakRemaining)
                                    val breakEnd = chunkStart + breakDuration
                                    gapFilledItems.add(
                                        ScheduleItem.MicroBreak(
                                            id = "break_${gapIndex++}",
                                            title = "Break",
                                            startTime = chunkStart,
                                            endTime = breakEnd,
                                            description = "Rest eyes, walk around & hydrate"
                                        )
                                    )
                                    chunkStart = breakEnd
                                }
                            }
                        }
                    }

                    if (item.id != "day_end_anchor") {
                        gapFilledItems.add(item)
                    }
                    if (item.endTime > cursor) {
                        cursor = item.endTime
                    }
                }

                // Evening Wind Down
                val finalWindDownStart = maxOf(cursor, windDownStart)
                if (finalWindDownStart < dayEnd) {
                    gapFilledItems.add(
                        ScheduleItem.Sleep(
                            id = "evening_wind_down",
                            title = "Evening Wind Down",
                            startTime = finalWindDownStart,
                            endTime = dayEnd,
                            description = "Unwind, prepare for tomorrow, no screens"
                        )
                    )
                }

                // Final Pass: Ensure strict non-overlap invariant
                gapFilledItems.sortBy { it.startTime }
                val finalItems = mutableListOf<ScheduleItem>()
                var timelineCursor = dayStart

                for (item in gapFilledItems) {
                    if (item.startTime < timelineCursor) {
                        val newStart = timelineCursor
                        if (item.endTime <= newStart) continue

                        val adjustedItem = when (item) {
                            is ScheduleItem.FocusBlock -> item.copy(startTime = newStart)
                            is ScheduleItem.MorningRoutine -> item.copy(startTime = newStart)
                            is ScheduleItem.Decompression -> item.copy(startTime = newStart)
                            is ScheduleItem.MealPrepAndEat -> item.copy(startTime = newStart)
                            is ScheduleItem.StudySetup -> item.copy(startTime = newStart)
                            is ScheduleItem.DeepStudy -> item.copy(startTime = newStart)
                            is ScheduleItem.MicroBreak -> item.copy(startTime = newStart)
                            else -> item
                        }
                        finalItems.add(adjustedItem)
                        timelineCursor = adjustedItem.endTime
                    } else {
                        finalItems.add(item)
                        timelineCursor = item.endTime
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    scheduleItems = finalItems.sortedBy { it.startTime }
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
