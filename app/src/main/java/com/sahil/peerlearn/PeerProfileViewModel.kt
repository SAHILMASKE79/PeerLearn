package com.sahil.peerlearn

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PeerProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val db = Firebase.firestore

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile.asStateFlow()

    private val _connectionDetails = MutableStateFlow(ConnectionDetails(ConnectionStatus.NOT_CONNECTED))
    val connectionDetails: StateFlow<ConnectionDetails> = _connectionDetails.asStateFlow()

    private val _uiState = MutableStateFlow<PeerProfileUiState>(PeerProfileUiState.Idle)
    val uiState: StateFlow<PeerProfileUiState> = _uiState.asStateFlow()

    fun fetchPeerProfile(peerUid: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PeerProfileUiState.Loading
                userRepository.getUserProfile(peerUid).collectLatest { profile ->
                    _peerProfile.value = profile
                    _uiState.value = PeerProfileUiState.Idle
                }
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "fetchPeerProfile failed: ${e.message}")
                _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun checkConnectionStatus(currentUid: String, peerUid: String) {
        viewModelScope.launch {
            try {
                userRepository.getConnectionStatus(currentUid, peerUid).collectLatest { details ->
                    _connectionDetails.value = details
                }
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "checkConnectionStatus failed: ${e.message}")
            }
        }
    }

    fun sendConnectionRequest(currentUid: String, currentUserName: String, peerUid: String) {
        viewModelScope.launch {
            try {
                userRepository.sendConnectionRequest(currentUid, currentUserName, peerUid).fold(
                    onSuccess = {
                        // Status will be updated via observer
                    },
                    onFailure = { e ->
                        _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to send request")
                    }
                )
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "sendConnectionRequest failed: ${e.message}")
                _uiState.value = PeerProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun acceptConnection(currentUid: String, currentUserName: String, fromUid: String) {
        viewModelScope.launch {
            try {
                val notificationId = findNotificationId(currentUid, fromUid)
                userRepository.acceptConnection(notificationId, currentUid, currentUserName, fromUid).fold(
                    onSuccess = { },
                    onFailure = { e ->
                        _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to accept request")
                    }
                )
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "acceptConnection failed: ${e.message}")
                _uiState.value = PeerProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun declineConnection(currentUid: String, fromUid: String) {
        viewModelScope.launch {
            try {
                val notificationId = findNotificationId(currentUid, fromUid)
                userRepository.declineConnection(notificationId, currentUid, fromUid).fold(
                    onSuccess = { },
                    onFailure = { e ->
                        _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to decline request")
                    }
                )
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "declineConnection failed: ${e.message}")
                _uiState.value = PeerProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private suspend fun findNotificationId(toUid: String, fromUid: String): String {
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("toUid", toUid)
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("type", "connection_request")
                .get()
                .await()
            snapshot.documents.firstOrNull()?.id ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun disconnectPeer(currentUid: String, peerUid: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.disconnectPeer(currentUid, peerUid).fold(
                    onSuccess = {
                        onComplete()
                    },
                    onFailure = { e ->
                        _uiState.value = PeerProfileUiState.Error(e.message ?: "Failed to disconnect")
                    }
                )
            } catch (e: Exception) {
                Log.e("PeerProfileVM", "disconnectPeer failed: ${e.message}")
                _uiState.value = PeerProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }
}

sealed class PeerProfileUiState {
    object Idle : PeerProfileUiState()
    object Loading : PeerProfileUiState()
    data class Error(val message: String) : PeerProfileUiState()
}
