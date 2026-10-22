package com.example.kairo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kairo.data.PreferencesManager
import com.example.kairo.data.remote.RetrofitClient
import com.example.kairo.data.repository.AuthRepository
import com.example.kairo.data.repository.CommuteRepositoryImpl
import com.example.kairo.domain.repository.CommuteRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserViewModel(
    application: Application,
    private val commuteRepository: CommuteRepository
) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val authRepository = AuthRepository(preferencesManager)

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

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

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
        viewModelScope.launch {
            authRepository.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = authRepository.currentUser
                        _currentUserEmail.value = user?.email
                        user?.id?.let { userId ->
                            authRepository.syncRemoteToLocalOrPush(userId)
                        }
                    }
                    else -> {
                        _currentUserEmail.value = null
                    }
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.signIn(email, pass)
            _isAuthLoading.value = false
            result.onSuccess {
                _validationResult.value = "Successfully signed in!"
            }.onFailure { ex ->
                _authError.value = ex.localizedMessage ?: "Sign in failed"
            }
        }
    }

    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.signUp(email, pass)
            _isAuthLoading.value = false
            result.onSuccess {
                _validationResult.value = "Account created! Please check your email or log in."
            }.onFailure { ex ->
                _authError.value = ex.localizedMessage ?: "Sign up failed"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            authRepository.signOut()
            _currentUserEmail.value = null
            _isAuthLoading.value = false
            _validationResult.value = "Signed out successfully"
        }
    }

    fun syncNow() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _isAuthLoading.value = true
            val res = authRepository.syncRemoteToLocalOrPush(user.id)
            _isAuthLoading.value = false
            res.onSuccess {
                _validationResult.value = "Settings synced with Supabase!"
            }.onFailure { ex ->
                _authError.value = "Sync failed: ${ex.localizedMessage}"
            }
        }
    }

    fun saveRoutine(wakeTime: String, sleepTime: String) {
        viewModelScope.launch {
            preferencesManager.saveWakeUpTime(wakeTime)
            preferencesManager.saveSleepTime(sleepTime)
            _validationResult.value = "Routine saved successfully!"
            authRepository.currentUser?.id?.let { userId ->
                authRepository.pushLocalToRemote(userId)
            }
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
            authRepository.currentUser?.id?.let { userId ->
                authRepository.pushLocalToRemote(userId)
            }
        }
    }

    fun clearValidationResult() {
        _validationResult.value = null
    }

    fun clearAuthError() {
        _authError.value = null
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
