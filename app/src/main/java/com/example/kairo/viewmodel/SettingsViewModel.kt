package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.PreferencesManager
import com.example.kairo.data.repository.AuthRepository
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

class SettingsViewModel(
    application: Application,
    private val commuteRepository: CommuteRepository
) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val authRepository = AuthRepository(preferencesManager)

    val loggedInUserEmail: StateFlow<String?> = MutableStateFlow<String?>(null).apply {
        viewModelScope.launch {
            authRepository.sessionStatus.collectLatest { status ->
                value = authRepository.currentUser?.email
            }
        }
    }.asStateFlow()

    private val _workCalendarUrl = MutableStateFlow("")
    val workCalendarUrl: StateFlow<String> = _workCalendarUrl.asStateFlow()

    private val _collegeCalendarUrl = MutableStateFlow("")
    val collegeCalendarUrl: StateFlow<String> = _collegeCalendarUrl.asStateFlow()

    private val _personalCalendarUrl = MutableStateFlow("")
    val personalCalendarUrl: StateFlow<String> = _personalCalendarUrl.asStateFlow()

    private val _homeLocation = MutableStateFlow("")
    val homeLocation: StateFlow<String> = _homeLocation.asStateFlow()

    private val _workLocation = MutableStateFlow("")
    val workLocation: StateFlow<String> = _workLocation.asStateFlow()

    private val _collegeLocation = MutableStateFlow("")
    val collegeLocation: StateFlow<String> = _collegeLocation.asStateFlow()

    private val _transportMode = MutableStateFlow("All Public Transport")
    val transportMode: StateFlow<String> = _transportMode.asStateFlow()

    private val _pomodoroWorkDuration = MutableStateFlow("90")
    val pomodoroWorkDuration: StateFlow<String> = _pomodoroWorkDuration.asStateFlow()

    private val _pomodoroBreakDuration = MutableStateFlow("15")
    val pomodoroBreakDuration: StateFlow<String> = _pomodoroBreakDuration.asStateFlow()

    private val _isValidatingWork = MutableStateFlow(false)
    val isValidatingWork: StateFlow<Boolean> = _isValidatingWork.asStateFlow()

    private val _isValidatingCollege = MutableStateFlow(false)
    val isValidatingCollege: StateFlow<Boolean> = _isValidatingCollege.asStateFlow()

    private val _validationResult = MutableStateFlow<String?>(null)
    val validationResult: StateFlow<String?> = _validationResult.asStateFlow()

    private val _workLocationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val workLocationSuggestions: StateFlow<List<String>> = _workLocationSuggestions.asStateFlow()

    private val _collegeLocationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val collegeLocationSuggestions: StateFlow<List<String>> = _collegeLocationSuggestions.asStateFlow()

    private val _homeLocationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val homeLocationSuggestions: StateFlow<List<String>> = _homeLocationSuggestions.asStateFlow()

    private var workSearchJob: Job? = null
    private var collegeSearchJob: Job? = null
    private var homeSearchJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesManager.workCalendarUrlFlow.collectLatest { url ->
                _workCalendarUrl.value = url
            }
        }
        viewModelScope.launch {
            preferencesManager.collegeCalendarUrlFlow.collectLatest { url ->
                _collegeCalendarUrl.value = url
            }
        }
        viewModelScope.launch {
            preferencesManager.homeLocationFlow.collectLatest { loc ->
                _homeLocation.value = loc
            }
        }
        viewModelScope.launch {
            preferencesManager.workLocationFlow.collectLatest { loc ->
                _workLocation.value = loc
            }
        }
        viewModelScope.launch {
            preferencesManager.homeLocationFlow.collectLatest { loc ->
                _homeLocation.value = loc
            }
        }
        viewModelScope.launch {
            preferencesManager.collegeLocationFlow.collectLatest { loc ->
                _collegeLocation.value = loc
            }
        }
        viewModelScope.launch {
            preferencesManager.personalCalendarUrlFlow.collectLatest { url ->
                _personalCalendarUrl.value = url
            }
        }
        viewModelScope.launch {
            preferencesManager.transportModeFlow.collectLatest { mode ->
                _transportMode.value = mode
            }
        }
        viewModelScope.launch {
            preferencesManager.pomodoroWorkDurationFlow.collectLatest { work ->
                _pomodoroWorkDuration.value = work
            }
        }
        viewModelScope.launch {
            preferencesManager.pomodoroBreakDurationFlow.collectLatest { brk ->
                _pomodoroBreakDuration.value = brk
            }
        }
    }

    fun saveAllSettings(
        workUrl: String,
        collegeUrl: String,
        personalUrl: String,
        homeLoc: String,
        workLoc: String,
        collegeLoc: String,
        mode: String,
        pomodoroWork: String,
        pomodoroBreak: String
    ) {
        viewModelScope.launch {
            _isValidatingWork.value = true
            _validationResult.value = null
            
            if (workLoc.isBlank() || collegeLoc.isBlank() || homeLoc.isBlank()) {
                _validationResult.value = "Error: Home, Work and College locations are mandatory."
                _isValidatingWork.value = false
                return@launch
            }
            
            val isHomeValid = commuteRepository.validateLocation(homeLoc)
            if (!isHomeValid) {
                _validationResult.value = "Error: Invalid Home Location according to TfNSW."
                _isValidatingWork.value = false
                return@launch
            }
            
            val isWorkValid = commuteRepository.validateLocation(workLoc)
            if (!isWorkValid) {
                _validationResult.value = "Error: Invalid Work Location according to TfNSW."
                _isValidatingWork.value = false
                return@launch
            }
            
            val isCollegeValid = commuteRepository.validateLocation(collegeLoc)
            if (!isCollegeValid) {
                _validationResult.value = "Error: Invalid College Location according to TfNSW."
                _isValidatingWork.value = false
                return@launch
            }
            
            preferencesManager.saveWorkCalendarUrl(workUrl)
            preferencesManager.saveCollegeCalendarUrl(collegeUrl)
            preferencesManager.savePersonalCalendarUrl(personalUrl)
            preferencesManager.saveHomeLocation(homeLoc)
            preferencesManager.saveWorkLocation(workLoc)
            preferencesManager.saveCollegeLocation(collegeLoc)
            preferencesManager.saveTransportMode(mode)
            preferencesManager.savePomodoroDurations(pomodoroWork, pomodoroBreak)
            
            authRepository.currentUser?.id?.let { userId ->
                authRepository.pushLocalToRemote(userId)
            }
            
            _validationResult.value = "All settings successfully saved!"
            _isValidatingWork.value = false
        }
    }
    
    fun clearValidationResult() {
        _validationResult.value = null
    }

    fun fetchHomeSuggestions(query: String) {
        homeSearchJob?.cancel()
        if (query.isBlank()) {
            _homeLocationSuggestions.value = emptyList()
            return
        }
        homeSearchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            _homeLocationSuggestions.value = commuteRepository.getLocationSuggestions(query)
        }
    }

    fun fetchWorkSuggestions(query: String) {
        workSearchJob?.cancel()
        if (query.isBlank()) {
            _workLocationSuggestions.value = emptyList()
            return
        }
        workSearchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            _workLocationSuggestions.value = commuteRepository.getLocationSuggestions(query)
        }
    }

    fun fetchCollegeSuggestions(query: String) {
        collegeSearchJob?.cancel()
        if (query.isBlank()) {
            _collegeLocationSuggestions.value = emptyList()
            return
        }
        collegeSearchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            _collegeLocationSuggestions.value = commuteRepository.getLocationSuggestions(query)
        }
    }

    fun clearHomeSuggestions() {
        _homeLocationSuggestions.value = emptyList()
    }

    fun clearWorkSuggestions() {
        _workLocationSuggestions.value = emptyList()
    }

    fun clearCollegeSuggestions() {
        _collegeLocationSuggestions.value = emptyList()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        val commuteRepo = CommuteRepositoryImpl(RetrofitClient.tfNSWApiService)
                        return SettingsViewModel(application, commuteRepo) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
