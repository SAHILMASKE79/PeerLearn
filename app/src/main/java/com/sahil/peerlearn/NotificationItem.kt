package com.sahil.peerlearn

import com.google.firebase.Timestamp

data class NotificationItem(
    val id: String = "",
    val toUid: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val type: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
