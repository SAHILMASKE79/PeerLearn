package com.sahil.peerlearn

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HomeRepository(private val userRepository: UserRepository) {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    fun getCurrentUser(uid: String): Flow<UserProfile?> {
        return usersCollection.document(uid).snapshots().map { snapshot ->
            snapshot.toObject(UserProfile::class.java)
        }
    }

    fun getAllUsersStream(): Flow<List<UserProfile>> {
        return userRepository.getAllUserProfiles()
    }
}
