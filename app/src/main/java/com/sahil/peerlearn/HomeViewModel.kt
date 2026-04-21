package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel(), ConnectionViewModel {
    private val firestore = Firebase.firestore
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

    private val _recommendedPeersWithMatch = MutableStateFlow<List<Pair<UserProfile, Int>>>(emptyList())
    val recommendedPeersWithMatch: StateFlow<List<Pair<UserProfile, Int>>> = _recommendedPeersWithMatch.asStateFlow()

    fun initHome(uid: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            // Observe current user and all users
            combine(
                repository.getCurrentUser(uid),
                repository.getAllUsersStream()
            ) { currentUser, allUsers ->
                val realUsers = allUsers.filter { it.name.isNotBlank() && it.name != "Unknown" && it.name.length > 1 }
                _currentUser.value = currentUser
                _allPeers.value = realUsers // Update _allPeers with only real users

                if (currentUser != null) {
                    val recommended = realUsers
                        .filter { it.uid != currentUser.uid }
                        .map { peer ->
                            val matchCount = peer.teachSkills.count { it in currentUser.learnSkills }
                            val totalPossibleMatches = currentUser.learnSkills.size.coerceAtLeast(1)
                            val matchPercentage = ((matchCount.toFloat() / totalPossibleMatches) * 100).toInt()
                            peer to matchPercentage
                        }
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                        .take(5)
                    
                    _recommendedPeersWithMatch.value = recommended
                    _recommendedPeers.value = recommended.map { it.first }
                } else {
                    _recommendedPeersWithMatch.value = emptyList()
                    _recommendedPeers.value = emptyList()
                }
                _uiState.value = HomeUiState.Success
            }.collect()
        }
    }

    override fun sendConnectionRequest(currentUid: String, peerUid: String) {
        val connectionId = listOf(currentUid, peerUid)
            .sorted().joinToString("_")
        val data = mapOf(
            "user1" to currentUid,
            "user2" to peerUid,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("connections")
            .document(connectionId)
            .set(data)
    }

    override fun getConnectionStatus(
        currentUid: String,
        peerUid: String
    ): Flow<String> {
        val connectionId = listOf(currentUid, peerUid)
            .sorted().joinToString("_")
        return firestore.collection("connections")
            .document(connectionId)
            .snapshots()
            .map { it.getString("status") ?: "none" }
    }
}
