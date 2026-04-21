package com.sahil.peerlearn

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val type: String = "text",
    val imageUrl: String = "",
    val codeSnippet: String = "",
    val codeLanguage: String = "kotlin",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val replyToId: String = "",
    val replyToText: String = "",
    val status: String = "sent" // "sending", "sent", "read"
)
