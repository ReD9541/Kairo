package com.example.kairo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteUserSettings(
    @SerialName("id") val id: String,
    @SerialName("user_name") val userName: String = "",
    @SerialName("wake_up_time") val wakeUpTime: String = "07:00",
    @SerialName("sleep_time") val sleepTime: String = "22:30",
    @SerialName("work_calendar_url") val workCalendarUrl: String = "",
    @SerialName("college_calendar_url") val collegeCalendarUrl: String = "",
    @SerialName("personal_calendar_url") val personalCalendarUrl: String = "",
    @SerialName("home_location") val homeLocation: String = "",
    @SerialName("work_location") val workLocation: String = "",
    @SerialName("college_location") val collegeLocation: String = "",
    @SerialName("transport_mode") val transportMode: String = "All Public Transport",
    @SerialName("pomodoro_work_duration") val pomodoroWorkDuration: String = "90",
    @SerialName("pomodoro_break_duration") val pomodoroBreakDuration: String = "15",
    @SerialName("morning_routine_mins") val morningRoutineMins: String = "40",
    @SerialName("post_commute_decompress_mins") val postCommuteDecompressMins: String = "35",
    @SerialName("study_prep_buffer_mins") val studyPrepBufferMins: String = "15",
    @SerialName("lunch_duration_mins") val lunchDurationMins: String = "45",
    @SerialName("dinner_prep_and_eat_mins") val dinnerPrepAndEatMins: String = "60",
    @SerialName("min_study_slot_mins") val minStudySlotMins: String = "35",
    @SerialName("custom_milestones") val customMilestones: String = "",
    @SerialName("completed_milestones") val completedMilestones: String = "",
    @SerialName("main_goal_milestones") val mainGoalMilestones: String = "",
    @SerialName("manual_events") val manualEvents: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
)
