package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun fetchNotifications(uid: String) {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            userRepository.getNotifications(uid).collect { items ->
                _notifications.value = items
                _uiState.value = NotificationUiState.Success
            }
        }
    }

    fun acceptConnection(notification: NotificationItem, currentUid: String, currentUserName: String) {
        viewModelScope.launch {
            userRepository.acceptConnection(notification.id, currentUid, currentUserName, notification.fromUid).fold(
                onSuccess = { },
                onFailure = { e ->
                    _uiState.value = NotificationUiState.Error(e.message ?: "Failed to accept")
                }
            )
        }
    }

    fun declineConnection(notification: NotificationItem, currentUid: String) {
        viewModelScope.launch {
            userRepository.declineConnection(notification.id, currentUid, notification.fromUid).fold(
                onSuccess = { },
                onFailure = { e ->
                    _uiState.value = NotificationUiState.Error(e.message ?: "Failed to decline")
                }
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            userRepository.markNotificationAsRead(notificationId)
        }
    }
}

sealed class NotificationUiState {
    object Idle : NotificationUiState()
    object Loading : NotificationUiState()
    object Success : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}
