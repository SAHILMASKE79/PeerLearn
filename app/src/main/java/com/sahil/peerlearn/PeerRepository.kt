package com.sahil.peerlearn

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PeerRepository(private val userRepository: UserRepository) {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    fun getAllPeersStream(): Flow<List<UserProfile>> {
        return userRepository.getAllUserProfiles()
    }
    
    fun getCurrentUser(uid: String): Flow<UserProfile?> {
        return userRepository.getUserProfile(uid)
    }

    suspend fun sendConnectionRequest(currentUid: String, currentUserName: String, peerUid: String): Result<Unit> {
        return userRepository.sendConnectionRequest(currentUid, currentUserName, peerUid)
    }

    fun getRecommendedPeersWithMatch(currentUid: String): Flow<List<Pair<UserProfile, Int>>> {
        return userRepository.getAllUserProfiles().combine(userRepository.getUserProfile(currentUid)) { allUsers, currentUser ->
            if (currentUser == null) return@combine emptyList()

            allUsers.filter { it.uid != currentUid }
                .map { peer ->
                    val matchPercentage = calculateMatchPercentage(currentUser, peer)
                    Pair(peer, matchPercentage)
                }
                .filter { it.second > 0 } // Only show peers with some match
                .sortedByDescending { it.second }
        }
    }

    private fun calculateMatchPercentage(current: UserProfile, peer: UserProfile): Int {
        val iCanTeachHeWants = current.teachSkills.intersect(peer.learnSkills.toSet()).size
        val heCanTeachIWant = peer.teachSkills.intersect(current.learnSkills.toSet()).size

        // Total possible matches (what I want + what he wants)
        val totalNeeded = (current.learnSkills.size + peer.learnSkills.size).coerceAtLeast(1)
        val matches = iCanTeachHeWants + heCanTeachIWant
        
        return ((matches.toFloat() / totalNeeded) * 100).toInt().coerceIn(0, 100)
    }
}
