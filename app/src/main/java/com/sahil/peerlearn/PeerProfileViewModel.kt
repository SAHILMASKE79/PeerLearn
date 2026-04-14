package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PeerProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile.asStateFlow()

    private val _connectionDetails = MutableStateFlow(ConnectionDetails(ConnectionStatus.NOT_CONNECTED))
    val connectionDetails: StateFlow<ConnectionDetails> = _connectionDetails.asStateFlow()

    private val _uiState = MutableStateFlow<PeerProfileUiState>(PeerProfileUiState.Idle)
    val uiState: StateFlow<PeerProfileUiState> = _uiState.asStateFlow()

    fun fetchPeerProfile(peerUid: String) {
        viewModelScope.launch {
            _uiState.value = PeerProfileUiState.Loading
            userRepository.getUserProfile(peerUid).collectLatest { profile ->
                _peerProfile.value = profile
                _uiState.value = PeerProfileUiState.Idle
            }
        }
    }

    fun checkConnectionStatus(currentUid: String, peerUid: String) {
        observeConnectionStatus(currentUid, peerUid)
    }

    fun observeConnectionStatus(currentUid: String, peerUid: String) {
        viewModelScope.launch {
            userRepository.getConnectionStatus(currentUid, peerUid).collectLatest { details ->
                _connectionDetails.value = details
            }
        }
    }

    fun sendConnectionRequest(currentUid: String, currentUserName: String, peerUid: String) {
        viewModelScope.launch {
            userRepository.sendConnectionRequest(currentUid, currentUserName, peerUid).fold(
                onSuccess = {
                    // Status will be updated via observer
                },
                onFailure = { e ->
                    _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to send request")
                }
            )
        }
    }

    fun acceptConnection(currentUid: String, currentUserName: String, peerUid: String) {
        viewModelScope.launch {
            // notificationId is empty here because we are on profile, not notification screen
            userRepository.acceptConnection("", currentUid, currentUserName, peerUid).fold(
                onSuccess = { },
                onFailure = { e ->
                    _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to accept request")
                }
            )
        }
    }

    fun declineConnection(currentUid: String, peerUid: String) {
        viewModelScope.launch {
            userRepository.declineConnection("", currentUid, peerUid).fold(
                onSuccess = { },
                onFailure = { e ->
                    _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to decline request")
                }
            )
        }
    }
}

sealed class PeerProfileUiState {
    object Idle : PeerProfileUiState()
    object Loading : PeerProfileUiState()
    data class Error(val message: String) : PeerProfileUiState()
}
