package com.example.kairo.domain.model

sealed interface ScheduleItem {
    val id: String
    val title: String
    val startTime: Long
    val endTime: Long

    data class Event(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val location: String?,
        val description: String?,
        val eventId: Long
    ) : ScheduleItem

    data class TransitTo(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val origin: String,
        val destination: String,
        val durationMillis: Long,
        val legs: List<TransitLeg> = emptyList()
    ) : ScheduleItem

    data class TransitHome(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val origin: String,
        val destination: String,
        val durationMillis: Long,
        val legs: List<TransitLeg> = emptyList()
    ) : ScheduleItem

    data class Decompression(
        override val id: String,
        override val title: String = "Post-Commute Decompression",
        override val startTime: Long,
        override val endTime: Long,
        val description: String = "Arrive home, shower, change & unwind"
    ) : ScheduleItem

    data class MorningRoutine(
        override val id: String,
        override val title: String = "Morning Routine",
        override val startTime: Long,
        override val endTime: Long,
        val activities: List<String> = listOf("Hygiene & Shower", "Healthy Breakfast", "Prepare for the day")
    ) : ScheduleItem

    data class MealPrepAndEat(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val mealType: String = "Meal"
    ) : ScheduleItem

    data class StudySetup(
        override val id: String,
        override val title: String = "Study Setup & Workspace Prep",
        override val startTime: Long,
        override val endTime: Long,
        val description: String = "Clear desk, open notes & grab water"
    ) : ScheduleItem

    data class DeepStudy(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val taskName: String,
        val suggestedActivities: List<String> = listOf("Deep focus work", "Milestone progress")
    ) : ScheduleItem

    data class MicroBreak(
        override val id: String,
        override val title: String = "Micro Break",
        override val startTime: Long,
        override val endTime: Long,
        val description: String = "Short break, stretch & hydrate"
    ) : ScheduleItem

    data class FocusBlock(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val suggestedActivities: List<String> = emptyList()
    ) : ScheduleItem

    data class Sleep(
        override val id: String,
        override val title: String = "Evening Wind Down & Sleep",
        override val startTime: Long,
        override val endTime: Long,
        val description: String = "Night rest & sleep"
    ) : ScheduleItem
}
