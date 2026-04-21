package com.sahil.peerlearn

import com.google.firebase.auth.FirebaseAuth
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

    constructor() : this(UserRepository())

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val peersCount: Int = 0 // Hardcoded for now

    fun fetchProfile(uid: String?) {
        val resolvedUid = uid?.takeIf { it.isNotBlank() } ?: FirebaseAuth.getInstance().currentUser?.uid
        if (resolvedUid.isNullOrBlank()) {
            _uiState.value = ProfileUiState.Error("User ID is null")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            userRepository.getUserProfile(resolvedUid).collectLatest { profile ->
                _userProfile.value = profile
                _uiState.value = if (profile == null) {
                    ProfileUiState.Error("Profile not found")
                } else {
                    ProfileUiState.Idle
                }
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

    fun uploadProfileImage(uid: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.uploadProfileImage(uid, imageUri)
            result.fold(
                onSuccess = {
                    fetchProfile(uid)
                    _uiState.value = ProfileUiState.Success("Image uploaded successfully")
                },
                onFailure = { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to upload image")
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
