package com.sahil.peerlearn

import com.google.firebase.Timestamp

data class StudySession(
    val sessionId: String = "",
    val chatId: String = "",
    val topic: String = "",
    val status: String = "active",
    val startedAt: Timestamp = Timestamp.now(),
    val elapsedSeconds: Long = 0L
)
