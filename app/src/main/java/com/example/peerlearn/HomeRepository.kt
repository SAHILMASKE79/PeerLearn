package com.sahil.peerlearn

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HomeRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    fun getCurrentUser(uid: String): Flow<UserProfile?> {
        return usersCollection.document(uid).snapshots().map { snapshot ->
            snapshot.toObject(UserProfile::class.java)
        }
    }

    suspend fun getRecommendedPeers(learnSkills: List<String>, currentUid: String): List<UserProfile> {
        if (learnSkills.isEmpty()) return emptyList()
        
        return try {
            // Firestore whereArrayContainsAny limit is 10 items in the list, but our learnSkills might be more.
            // For now, we take first 10 or handle chunks if needed.
            val limitedSkills = learnSkills.take(10)
            
            val querySnapshot = usersCollection
                .whereArrayContainsAny("teachSkills", limitedSkills)
                .limit(10)
                .get()
                .await()
            
            querySnapshot.toObjects(UserProfile::class.java).filter { it.uid != currentUid }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllPeers(currentUid: String): List<UserProfile> {
        return try {
            val querySnapshot = usersCollection
                .limit(20)
                .get()
                .await()
            
            querySnapshot.toObjects(UserProfile::class.java).filter { it.uid != currentUid }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
