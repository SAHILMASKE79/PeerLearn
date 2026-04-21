package com.sahil.peerlearn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NotificationNavEvent>()
    val navigationEvent: SharedFlow<NotificationNavEvent> = _navigationEvent.asSharedFlow()

    fun fetchNotifications(uid: String) {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            try {
                userRepository.getNotifications(uid).collect { items ->
                    _notifications.value = items
                    _uiState.value = NotificationUiState.Success
                    // Mark all as read when screen is opened
                    userRepository.markAllNotificationsAsRead(uid)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error fetching: ${e.message}")
                _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun acceptConnection(notification: NotificationItem, currentUid: String, currentUserName: String) {
        viewModelScope.launch {
            userRepository.acceptConnection(notification.id, currentUid, currentUserName, notification.fromUid).fold(
                onSuccess = {
                    _navigationEvent.emit(NotificationNavEvent.NavigateToChat(notification.fromUid))
                },
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

sealed class NotificationNavEvent {
    data class NavigateToChat(val peerUid: String) : NotificationNavEvent()
}
