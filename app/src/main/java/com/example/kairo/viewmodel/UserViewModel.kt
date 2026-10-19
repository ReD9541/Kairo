package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.PreferencesManager
import com.example.kairo.domain.repository.CommuteRepository
import com.example.kairo.data.repository.CommuteRepositoryImpl
import com.example.kairo.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class UserViewModel(
    application: Application,
    private val commuteRepository: CommuteRepository
) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()
    
    private val _wakeUpTime = MutableStateFlow("07:00")
    val wakeUpTime: StateFlow<String> = _wakeUpTime.asStateFlow()

    private val _sleepTime = MutableStateFlow("22:30")
    val sleepTime: StateFlow<String> = _sleepTime.asStateFlow()
    
    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()

    private val _validationResult = MutableStateFlow<String?>(null)
    val validationResult: StateFlow<String?> = _validationResult.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.wakeUpTimeFlow.collectLatest { time ->
                _wakeUpTime.value = time
            }
        }
        viewModelScope.launch {
            preferencesManager.sleepTimeFlow.collectLatest { time ->
                _sleepTime.value = time
            }
        }
        viewModelScope.launch {
            preferencesManager.userNameFlow.collectLatest { name ->
                _userName.value = name
            }
        }
    }

    fun saveRoutine(wakeTime: String, sleepTime: String) {
        viewModelScope.launch {
            preferencesManager.saveWakeUpTime(wakeTime)
            preferencesManager.saveSleepTime(sleepTime)
            _validationResult.value = "Routine saved successfully!"
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
        }
    }

    fun clearValidationResult() {
        _validationResult.value = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                        val commuteRepo = CommuteRepositoryImpl(RetrofitClient.tfNSWApiService)
                        return UserViewModel(application, commuteRepo) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}