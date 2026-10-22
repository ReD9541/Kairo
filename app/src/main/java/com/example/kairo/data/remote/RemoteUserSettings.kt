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
    @SerialName("custom_milestones") val customMilestones: String = "",
    @SerialName("completed_milestones") val completedMilestones: String = "",
    @SerialName("main_goal_milestones") val mainGoalMilestones: String = "",
    @SerialName("manual_events") val manualEvents: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
)
