package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MatchViewModel(
    private val peerRepository: PeerRepository,
    private val uid: String
) : ViewModel() {

    private val userRepository = UserRepository()

    // Peers with match percentage
    private val _recommendedPeersWithMatch = MutableStateFlow<List<Pair<UserProfile, Int>>>(emptyList())
    val recommendedPeersWithMatch: StateFlow<List<Pair<UserProfile, Int>>> = _recommendedPeersWithMatch

    // UI feedback (snackbar messages)
    private val _uiState = MutableStateFlow<String?>(null)
    val uiState: StateFlow<String?> = _uiState

    // Track which peers we've already sent requests to (in this session)
    private val sentRequests = mutableSetOf<String>()

    init {
        loadPeers()
    }

    private fun loadPeers() {
        viewModelScope.launch {
            try {
                peerRepository.getRecommendedPeersWithMatch(uid).collect { peers ->
                    // Filter out anonymous users
                    val filtered = peers.filter { (peer, _) ->
                        peer.name.isNotEmpty() && peer.uid != uid
                    }
                    _recommendedPeersWithMatch.value = filtered
                }
            } catch (e: Exception) {
                _uiState.value = "Failed to load peers: ${e.message}"
            }
        }
    }

    // ✅ FIXED: Actually sends connection request
    fun sendConnectionRequest(peerUid: String) {
        if (sentRequests.contains(peerUid)) {
            _uiState.value = "Request already sent!"
            return
        }

        viewModelScope.launch {
            try {
                // Get current user's name from Firebase Auth
                val currentUserName = Firebase.auth.currentUser?.displayName ?: ""

                val result = userRepository.sendConnectionRequest(
                    currentUid = uid,
                    currentUserName = currentUserName,
                    peerUid = peerUid
                )

                if (result.isSuccess) {
                    sentRequests.add(peerUid)
                    _uiState.value = "Connection request sent! 🤝"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    if (errorMsg.contains("already exists")) {
                        _uiState.value = "Already connected or request pending!"
                    } else {
                        _uiState.value = "Failed to send request. Try again."
                    }
                }
            } catch (e: Exception) {
                _uiState.value = "Error: ${e.message}"
            }
        }
    }

    fun clearUiState() {
        _uiState.value = null
    }
}