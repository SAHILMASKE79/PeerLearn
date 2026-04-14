package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: HomeRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _recommendedPeers = MutableStateFlow<List<UserProfile>>(emptyList())
    val recommendedPeers: StateFlow<List<UserProfile>> = _recommendedPeers.asStateFlow()

    private val _allPeers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allPeers: StateFlow<List<UserProfile>> = _allPeers.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun initHome(uid: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            // Observe unread notifications
            userRepository.getNotifications(uid).collect { notifications ->
                _unreadCount.value = notifications.count { !it.isRead }
            }
        }
        
        viewModelScope.launch {
            try {
                // Fetch current user in real-time
                repository.getCurrentUser(uid).collect { user ->
                    _currentUser.value = user
                    if (user != null) {
                        fetchPeers(user)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchPeers(user: UserProfile) {
        val recommended = repository.getRecommendedPeers(user.learnSkills, user.uid)
        val all = repository.getAllPeers(user.uid)
        
        _recommendedPeers.value = recommended
        _allPeers.value = all
        _uiState.value = HomeUiState.Success
    }
}
