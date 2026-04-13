package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authManager: AuthManager,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authManager.signupWithEmail(email, password, name).fold(
                onSuccess = { user ->
                    createUserProfile(user, name, email)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Signup Failed")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authManager.loginWithEmail(email, password).fold(
                onSuccess = { user ->
                    // Check and create profile if missing (Requirement 5)
                    userRepository.checkAndCreateProfile(user.uid, user.displayName ?: "", user.email ?: "")
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Login Failed")
                }
            )
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authManager.signInWithGoogle().fold(
                onSuccess = { user ->
                    userRepository.checkAndCreateProfile(user.uid, user.displayName ?: "", user.email ?: "")
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Google Sign In Failed")
                }
            )
        }
    }

    private suspend fun createUserProfile(user: FirebaseUser, name: String, email: String) {
        userRepository.createUserProfile(user.uid, email, name).fold(
            onSuccess = {
                _uiState.value = AuthUiState.Success(user)
            },
            onFailure = { e ->
                _uiState.value = AuthUiState.Error("Profile creation failed: ${e.message}")
            }
        )
    }

    fun updateProfile(
        uid: String,
        name: String,
        college: String,
        year: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            userRepository.updateProfileFields(uid, name, college, year, bio, teachSkills, learnSkills).fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Idle
                    onComplete()
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error("Update failed: ${e.message}")
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
