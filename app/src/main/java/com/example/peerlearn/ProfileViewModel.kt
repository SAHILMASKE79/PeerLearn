package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val peersCount: Int = 0 // Hardcoded for now

    fun fetchProfile(uid: String?) {
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("User ID is null")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            userRepository.getUserProfile(uid).collectLatest { profile ->
                _userProfile.value = profile
                _uiState.value = ProfileUiState.Idle
            }
        }
    }

    fun updateProfile(
        uid: String,
        name: String,
        bio: String,
        college: String,
        year: String,
        githubLink: String,
        linkedinLink: String,
        teachSkills: List<String>,
        learnSkills: List<String>
    ) {
        if (name.isBlank()) {
            _uiState.value = ProfileUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.updateUserProfile(
                uid, name, bio, college, year, githubLink, linkedinLink, teachSkills, learnSkills
            )
            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Success("Profile updated!")
                },
                onFailure = { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Update failed. Try again.")
                }
            )
        }
    }

    fun clearState() {
        _uiState.value = ProfileUiState.Idle
    }
}

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
