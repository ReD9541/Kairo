package com.example.kairo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val WORK_CALENDAR_URL = stringPreferencesKey("work_calendar_url")
        val COLLEGE_CALENDAR_URL = stringPreferencesKey("college_calendar_url")
        val PERSONAL_CALENDAR_URL = stringPreferencesKey("personal_calendar_url")
        val HOME_LOCATION = stringPreferencesKey("home_location")
        val WORK_LOCATION = stringPreferencesKey("work_location")
        val COLLEGE_LOCATION = stringPreferencesKey("college_location")
        val TRANSPORT_MODE = stringPreferencesKey("transport_mode")
        val WAKE_UP_TIME = stringPreferencesKey("wake_up_time")
        val SLEEP_TIME = stringPreferencesKey("sleep_time")
        val CUSTOM_MILESTONES = stringPreferencesKey("custom_milestones")
        val COMPLETED_MILESTONES = stringPreferencesKey("completed_milestones")
        val MAIN_GOAL_MILESTONES = stringPreferencesKey("main_goal_milestones")
        val MANUAL_EVENTS = stringPreferencesKey("manual_events")
        val USER_NAME = stringPreferencesKey("user_name")
        val POMODORO_WORK_DURATION = stringPreferencesKey("pomodoro_work_duration")
        val POMODORO_BREAK_DURATION = stringPreferencesKey("pomodoro_break_duration")

        val MORNING_ROUTINE_MINS = stringPreferencesKey("morning_routine_mins")
        val POST_COMMUTE_DECOMPRESS_MINS = stringPreferencesKey("post_commute_decompress_mins")
        val STUDY_PREP_BUFFER_MINS = stringPreferencesKey("study_prep_buffer_mins")
        val LUNCH_DURATION_MINS = stringPreferencesKey("lunch_duration_mins")
        val DINNER_PREP_AND_EAT_MINS = stringPreferencesKey("dinner_prep_and_eat_mins")
        val MIN_STUDY_SLOT_MINS = stringPreferencesKey("min_study_slot_mins")
        val QUICK_MEAL_MODE = stringPreferencesKey("quick_meal_mode")
        val RUNNING_LATE_MINS = stringPreferencesKey("running_late_mins")
    }

    val preferencesFlow: Flow<Preferences> = context.dataStore.data

    val workCalendarUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[WORK_CALENDAR_URL] ?: "" }

    val collegeCalendarUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[COLLEGE_CALENDAR_URL] ?: "" }

    val personalCalendarUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PERSONAL_CALENDAR_URL] ?: "" }

    val transportModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[TRANSPORT_MODE] ?: "All Public Transport" }

    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] ?: "" }

    val pomodoroWorkDurationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[POMODORO_WORK_DURATION] ?: "90" }

    val pomodoroBreakDurationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[POMODORO_BREAK_DURATION] ?: "15" }

    val morningRoutineMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MORNING_ROUTINE_MINS] ?: "40" }

    val postCommuteDecompressMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[POST_COMMUTE_DECOMPRESS_MINS] ?: "35" }

    val studyPrepBufferMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[STUDY_PREP_BUFFER_MINS] ?: "15" }

    val lunchDurationMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[LUNCH_DURATION_MINS] ?: "45" }

    val dinnerPrepAndEatMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DINNER_PREP_AND_EAT_MINS] ?: "60" }

    val minStudySlotMinsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MIN_STUDY_SLOT_MINS] ?: "35" }

    val quickMealModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[QUICK_MEAL_MODE]?.toBoolean() ?: false }

    val runningLateMinsFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[RUNNING_LATE_MINS]?.toIntOrNull() ?: 0 }

    suspend fun saveWorkCalendarUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[WORK_CALENDAR_URL] = url }
    }

    suspend fun saveCollegeCalendarUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[COLLEGE_CALENDAR_URL] = url }
    }

    suspend fun savePersonalCalendarUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[PERSONAL_CALENDAR_URL] = url }
    }

    val homeLocationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[HOME_LOCATION] ?: "" }

    val workLocationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[WORK_LOCATION] ?: "" }

    val collegeLocationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[COLLEGE_LOCATION] ?: "" }

    val wakeUpTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[WAKE_UP_TIME] ?: "07:00" }

    val sleepTimeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SLEEP_TIME] ?: "22:30" }

    suspend fun saveHomeLocation(location: String) {
        context.dataStore.edit { preferences -> preferences[HOME_LOCATION] = location }
    }

    suspend fun saveWorkLocation(location: String) {
        context.dataStore.edit { preferences -> preferences[WORK_LOCATION] = location }
    }

    suspend fun saveCollegeLocation(location: String) {
        context.dataStore.edit { preferences -> preferences[COLLEGE_LOCATION] = location }
    }

    suspend fun saveTransportMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[TRANSPORT_MODE] = mode }
    }

    suspend fun saveWakeUpTime(time: String) {
        context.dataStore.edit { preferences -> preferences[WAKE_UP_TIME] = time }
    }

    suspend fun saveSleepTime(time: String) {
        context.dataStore.edit { preferences -> preferences[SLEEP_TIME] = time }
    }

    val customMilestonesFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CUSTOM_MILESTONES] ?: "" }

    val completedMilestonesFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[COMPLETED_MILESTONES] ?: "" }

    val mainGoalMilestonesFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MAIN_GOAL_MILESTONES] ?: "" }

    val manualEventsFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MANUAL_EVENTS] ?: "" }

    suspend fun saveCustomMilestones(milestonesJson: String) {
        context.dataStore.edit { preferences -> preferences[CUSTOM_MILESTONES] = milestonesJson }
    }

    suspend fun saveCompletedMilestones(completedJson: String) {
        context.dataStore.edit { preferences -> preferences[COMPLETED_MILESTONES] = completedJson }
    }

    suspend fun saveMainGoalMilestones(mainGoalsJson: String) {
        context.dataStore.edit { preferences -> preferences[MAIN_GOAL_MILESTONES] = mainGoalsJson }
    }

    suspend fun saveManualEvents(eventsJson: String) {
        context.dataStore.edit { preferences -> preferences[MANUAL_EVENTS] = eventsJson }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences -> preferences[USER_NAME] = name }
    }

    suspend fun savePomodoroDurations(work: String, breakTime: String) {
        context.dataStore.edit { preferences -> 
            preferences[POMODORO_WORK_DURATION] = work
            preferences[POMODORO_BREAK_DURATION] = breakTime
        }
    }

    suspend fun saveHumanRoutineBuffers(
        morning: String,
        decompress: String,
        studyPrep: String,
        lunch: String,
        dinner: String,
        minStudy: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[MORNING_ROUTINE_MINS] = morning
            preferences[POST_COMMUTE_DECOMPRESS_MINS] = decompress
            preferences[STUDY_PREP_BUFFER_MINS] = studyPrep
            preferences[LUNCH_DURATION_MINS] = lunch
            preferences[DINNER_PREP_AND_EAT_MINS] = dinner
            preferences[MIN_STUDY_SLOT_MINS] = minStudy
        }
    }

    suspend fun toggleQuickMealMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QUICK_MEAL_MODE] = enabled.toString()
        }
    }

    suspend fun setRunningLateMins(mins: Int) {
        context.dataStore.edit { preferences ->
            preferences[RUNNING_LATE_MINS] = mins.toString()
        }
    }

    suspend fun exportRemoteUserSettings(userId: String): com.example.kairo.data.remote.RemoteUserSettings {
        val preferences = context.dataStore.data.first()
        return com.example.kairo.data.remote.RemoteUserSettings(
            id = userId,
            userName = preferences[USER_NAME] ?: "",
            wakeUpTime = preferences[WAKE_UP_TIME] ?: "07:00",
            sleepTime = preferences[SLEEP_TIME] ?: "22:30",
            workCalendarUrl = preferences[WORK_CALENDAR_URL] ?: "",
            collegeCalendarUrl = preferences[COLLEGE_CALENDAR_URL] ?: "",
            personalCalendarUrl = preferences[PERSONAL_CALENDAR_URL] ?: "",
            homeLocation = preferences[HOME_LOCATION] ?: "",
            workLocation = preferences[WORK_LOCATION] ?: "",
            collegeLocation = preferences[COLLEGE_LOCATION] ?: "",
            transportMode = preferences[TRANSPORT_MODE] ?: "All Public Transport",
            pomodoroWorkDuration = preferences[POMODORO_WORK_DURATION] ?: "90",
            pomodoroBreakDuration = preferences[POMODORO_BREAK_DURATION] ?: "15",
            morningRoutineMins = preferences[MORNING_ROUTINE_MINS] ?: "40",
            postCommuteDecompressMins = preferences[POST_COMMUTE_DECOMPRESS_MINS] ?: "35",
            studyPrepBufferMins = preferences[STUDY_PREP_BUFFER_MINS] ?: "15",
            lunchDurationMins = preferences[LUNCH_DURATION_MINS] ?: "45",
            dinnerPrepAndEatMins = preferences[DINNER_PREP_AND_EAT_MINS] ?: "60",
            minStudySlotMins = preferences[MIN_STUDY_SLOT_MINS] ?: "35",
            customMilestones = preferences[CUSTOM_MILESTONES] ?: "",
            completedMilestones = preferences[COMPLETED_MILESTONES] ?: "",
            mainGoalMilestones = preferences[MAIN_GOAL_MILESTONES] ?: "",
            manualEvents = preferences[MANUAL_EVENTS] ?: ""
        )
    }

    suspend fun importRemoteUserSettings(remote: com.example.kairo.data.remote.RemoteUserSettings) {
        context.dataStore.edit { preferences ->
            if (remote.userName.isNotBlank()) preferences[USER_NAME] = remote.userName
            if (remote.wakeUpTime.isNotBlank()) preferences[WAKE_UP_TIME] = remote.wakeUpTime
            if (remote.sleepTime.isNotBlank()) preferences[SLEEP_TIME] = remote.sleepTime
            if (remote.workCalendarUrl.isNotBlank()) preferences[WORK_CALENDAR_URL] = remote.workCalendarUrl
            if (remote.collegeCalendarUrl.isNotBlank()) preferences[COLLEGE_CALENDAR_URL] = remote.collegeCalendarUrl
            if (remote.personalCalendarUrl.isNotBlank()) preferences[PERSONAL_CALENDAR_URL] = remote.personalCalendarUrl
            if (remote.homeLocation.isNotBlank()) preferences[HOME_LOCATION] = remote.homeLocation
            if (remote.workLocation.isNotBlank()) preferences[WORK_LOCATION] = remote.workLocation
            if (remote.collegeLocation.isNotBlank()) preferences[COLLEGE_LOCATION] = remote.collegeLocation
            if (remote.transportMode.isNotBlank()) preferences[TRANSPORT_MODE] = remote.transportMode
            if (remote.pomodoroWorkDuration.isNotBlank()) preferences[POMODORO_WORK_DURATION] = remote.pomodoroWorkDuration
            if (remote.pomodoroBreakDuration.isNotBlank()) preferences[POMODORO_BREAK_DURATION] = remote.pomodoroBreakDuration
            if (remote.morningRoutineMins.isNotBlank()) preferences[MORNING_ROUTINE_MINS] = remote.morningRoutineMins
            if (remote.postCommuteDecompressMins.isNotBlank()) preferences[POST_COMMUTE_DECOMPRESS_MINS] = remote.postCommuteDecompressMins
            if (remote.studyPrepBufferMins.isNotBlank()) preferences[STUDY_PREP_BUFFER_MINS] = remote.studyPrepBufferMins
            if (remote.lunchDurationMins.isNotBlank()) preferences[LUNCH_DURATION_MINS] = remote.lunchDurationMins
            if (remote.dinnerPrepAndEatMins.isNotBlank()) preferences[DINNER_PREP_AND_EAT_MINS] = remote.dinnerPrepAndEatMins
            if (remote.minStudySlotMins.isNotBlank()) preferences[MIN_STUDY_SLOT_MINS] = remote.minStudySlotMins
            if (remote.customMilestones.isNotBlank()) preferences[CUSTOM_MILESTONES] = remote.customMilestones
            if (remote.completedMilestones.isNotBlank()) preferences[COMPLETED_MILESTONES] = remote.completedMilestones
            if (remote.mainGoalMilestones.isNotBlank()) preferences[MAIN_GOAL_MILESTONES] = remote.mainGoalMilestones
            if (remote.manualEvents.isNotBlank()) preferences[MANUAL_EVENTS] = remote.manualEvents
        }
    }
}
