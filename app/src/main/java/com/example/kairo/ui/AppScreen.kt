package com.example.kairo.ui

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class EventDetailRoute(val itemId: String)

@Serializable
object SettingsRoute

@Serializable
object UserRoute

@Serializable
object MilestoneRoute

@Serializable
object SplashRoute

@Serializable
object AddEventRoute
