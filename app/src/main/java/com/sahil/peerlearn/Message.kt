package com.sahil.peerlearn

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text", // "text" or "code"
    val language: String = "", // for "code" type
    val isRead: Boolean = false,
    val replyToId: String = "",
    val replyToText: String = "",
    val status: String = "sent" // "sending", "sent", "read"
)
