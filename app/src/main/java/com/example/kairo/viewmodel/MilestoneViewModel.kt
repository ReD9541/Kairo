package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.PreferencesManager
import com.example.kairo.data.remote.ICalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class Milestone(
    val id: String,
    val title: String,
    val dateStr: String = "",
    val isCompleted: Boolean = false,
    val isCustom: Boolean = false,
    val isMainGoal: Boolean = false
)

class MilestoneViewModel(
    application: Application,
    private val iCalDataSource: ICalDataSource
) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _milestones = MutableStateFlow<List<Milestone>>(emptyList())
    val milestones: StateFlow<List<Milestone>> = _milestones.asStateFlow()

    private var customMilestonesRaw = listOf<Milestone>()
    private var completedIdsRaw = setOf<String>()
    private var mainGoalIdsRaw = setOf<String>()

    init {
        viewModelScope.launch {
            // Load state
            preferencesManager.customMilestonesFlow.collectLatest { json ->
                customMilestonesRaw = try {
                    if (json.isNotBlank()) Json.decodeFromString(json) else emptyList()
                } catch (e: Exception) { emptyList() }
                refreshList()
            }
        }
        viewModelScope.launch {
            preferencesManager.completedMilestonesFlow.collectLatest { json ->
                completedIdsRaw = try {
                    if (json.isNotBlank()) Json.decodeFromString<List<String>>(json).toSet() else emptySet()
                } catch (e: Exception) { emptySet() }
                refreshList()
            }
        }
        viewModelScope.launch {
            preferencesManager.mainGoalMilestonesFlow.collectLatest { json ->
                mainGoalIdsRaw = try {
                    if (json.isNotBlank()) Json.decodeFromString<List<String>>(json).toSet() else emptySet()
                } catch (e: Exception) { emptySet() }
                refreshList()
            }
        }
        
        // Fetch college events
        viewModelScope.launch {
            val url = preferencesManager.collegeCalendarUrlFlow.firstOrNull() ?: ""
            if (url.isNotBlank()) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val start = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, 35) // 4 weeks
                val end = cal.timeInMillis
                
                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val events = iCalDataSource.fetchEvents(url).filter { it.startTime in start..end }
                val eventMilestones = events.map { 
                    Milestone(it.id.toString(), it.title, formatter.format(java.util.Date(it.startTime)), isCustom = false) 
                }
                
                _milestones.value = eventMilestones + customMilestonesRaw
                applyState()
            }
        }
    }

    private fun refreshList() {
        val current = _milestones.value.filter { !it.isCustom }
        _milestones.value = current + customMilestonesRaw
        applyState()
    }

    private fun applyState() {
        _milestones.value = _milestones.value.map { 
            it.copy(
                isCompleted = completedIdsRaw.contains(it.id),
                isMainGoal = mainGoalIdsRaw.contains(it.id)
            ) 
        }
    }

    fun toggleMilestone(id: String) {
        val newCompleted = if (completedIdsRaw.contains(id)) {
            completedIdsRaw - id
        } else {
            completedIdsRaw + id
        }
        
        viewModelScope.launch {
            val json = Json.encodeToString(newCompleted.toList())
            preferencesManager.saveCompletedMilestones(json)
        }
    }

    fun toggleMainGoal(id: String) {
        val newGoals = if (mainGoalIdsRaw.contains(id)) {
            mainGoalIdsRaw - id
        } else {
            mainGoalIdsRaw + id
        }
        
        viewModelScope.launch {
            val json = Json.encodeToString(newGoals.toList())
            preferencesManager.saveMainGoalMilestones(json)
        }
    }

    fun addCustomMilestone(title: String) {
        val newMilestone = Milestone(id = "custom_${System.currentTimeMillis()}", title = title, isCustom = true)
        val updatedCustom = customMilestonesRaw + newMilestone
        
        viewModelScope.launch {
            val json = Json.encodeToString(updatedCustom)
            preferencesManager.saveCustomMilestones(json)
        }
    }

    fun deleteCustomMilestone(id: String) {
        customMilestonesRaw = customMilestonesRaw.filter { it.id != id }
        val updatedCompleted = completedIdsRaw - id
        val updatedMain = mainGoalIdsRaw - id
        
        viewModelScope.launch {
            val jsonCustom = Json.encodeToString(customMilestonesRaw)
            preferencesManager.saveCustomMilestones(jsonCustom)
            
            val jsonCompleted = Json.encodeToString(updatedCompleted.toList())
            preferencesManager.saveCompletedMilestones(jsonCompleted)
            
            val jsonMain = Json.encodeToString(updatedMain.toList())
            preferencesManager.saveMainGoalMilestones(jsonMain)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MilestoneViewModel::class.java)) {
                        return MilestoneViewModel(application, ICalDataSource()) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}