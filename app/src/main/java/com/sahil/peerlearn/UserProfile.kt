package com.sahil.peerlearn

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val bio: String = "",
    val college: String = "",
    val year: String = "",
    val githubLink: String = "",
    val linkedinLink: String = "",
    val role: String = "student",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val teachSkills: List<String> = emptyList(),
    val learnSkills: List<String> = emptyList(),
    val profileImageUrl: String = ""
)
