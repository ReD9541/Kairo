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

    data class Transit(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val origin: String,
        val destination: String,
        val durationMillis: Long,
        val legs: List<TransitLeg> = emptyList()
    ) : ScheduleItem

    data class FocusBlock(
        override val id: String,
        override val title: String,
        override val startTime: Long,
        override val endTime: Long,
        val suggestedActivities: List<String> = emptyList()
    ) : ScheduleItem
}
