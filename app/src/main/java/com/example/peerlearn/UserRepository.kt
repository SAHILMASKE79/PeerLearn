package com.sahil.peerlearn

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    /**
     * Called ONLY on signup. Sets both immutable and mutable fields.
     */
    suspend fun createUserProfile(uid: String, email: String, name: String): Result<Unit> {
        return try {
            val userProfile = mutableMapOf(
                "uid" to uid,
                "email" to email,
                "name" to name,
                "bio" to "",
                "college" to "",
                "year" to "",
                "githubLink" to "",
                "linkedinLink" to "",
                "role" to "student",
                "createdAt" to FieldValue.serverTimestamp(),
                "teachSkills" to emptyList<String>(),
                "learnSkills" to emptyList<String>()
            )
            usersCollection.document(uid).set(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAndCreateProfile(uid: String, name: String, email: String): Result<Unit> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (!document.exists()) {
                createUserProfile(uid, email, name)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates ONLY mutable fields. Never touches uid, email, createdAt, role.
     */
    suspend fun updateUserProfile(
        uid: String,
        name: String,
        bio: String,
        college: String,
        year: String,
        githubLink: String,
        linkedinLink: String,
        teachSkills: List<String>,
        learnSkills: List<String>
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "bio" to bio,
                "college" to college,
                "year" to year,
                "githubLink" to githubLink,
                "linkedinLink" to linkedinLink,
                "teachSkills" to teachSkills,
                "learnSkills" to learnSkills
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(
        uid: String,
        name: String,
        college: String,
        year: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "college" to college,
                "year" to year,
                "bio" to bio,
                "teachSkills" to teachSkills,
                "learnSkills" to learnSkills
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isProfileComplete(uid: String): Boolean {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (!document.exists()) return false
            
            val name = document.getString("name") ?: ""
            name.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Real-time Firestore listener for user profile.
     */
    fun getUserProfile(uid: String): Flow<UserProfile?> {
        return usersCollection.document(uid).snapshots().map { snapshot ->
            snapshot.toObject(UserProfile::class.java)
        }
    }

    // ── Connections ──

    private fun getConnectionDocId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    suspend fun sendConnectionRequest(currentUid: String, currentUserName: String, peerUid: String): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, peerUid)
            val sortedUids = listOf(currentUid, peerUid).sorted()
            
            val connectionData = mapOf(
                "user1" to sortedUids[0],
                "user2" to sortedUids[1],
                "status" to "pending",
                "requestedBy" to currentUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "connectedAt" to null
            )
            db.collection("connections").document(docId).set(connectionData).await()

            // Create notification
            val notificationData = mapOf(
                "toUid" to peerUid,
                "fromUid" to currentUid,
                "fromName" to currentUserName,
                "type" to "connection_request",
                "message" to "$currentUserName wants to connect with you! 🤝",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").add(notificationData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getConnectionStatus(currentUid: String, peerUid: String): Flow<ConnectionDetails> {
        val docId = getConnectionDocId(currentUid, peerUid)
        return db.collection("connections").document(docId).snapshots().map { snapshot ->
            if (!snapshot.exists()) {
                ConnectionDetails(ConnectionStatus.NOT_CONNECTED)
            } else {
                val status = when (snapshot.getString("status")) {
                    "pending" -> ConnectionStatus.PENDING
                    "connected" -> ConnectionStatus.CONNECTED
                    else -> ConnectionStatus.NOT_CONNECTED
                }
                ConnectionDetails(
                    status = status,
                    requestedBy = snapshot.getString("requestedBy") ?: ""
                )
            }
        }
    }

    // ── Notifications ──

    fun getNotifications(uid: String): Flow<List<NotificationItem>> {
        return db.collection("notifications")
            .whereEqualTo("toUid", uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.toObject(NotificationItem::class.java)!!.copy(id = doc.id)
                }
            }
    }

    suspend fun acceptConnection(
        notificationId: String,
        currentUid: String,
        currentUserName: String,
        fromUid: String
    ): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, fromUid)
            
            // 1. Update connection
            db.collection("connections").document(docId)
                .update(mapOf(
                    "status" to "connected",
                    "connectedAt" to FieldValue.serverTimestamp()
                )).await()
            
            // 2. Delete notification (if ID provided)
            if (notificationId.isNotEmpty()) {
                db.collection("notifications").document(notificationId).delete().await()
            }
            
            // 3. Send notification back
            val notificationData = mapOf(
                "toUid" to fromUid,
                "fromUid" to currentUid,
                "fromName" to currentUserName,
                "type" to "connection_accepted",
                "message" to "$currentUserName accepted your request! 🎉",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").add(notificationData).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineConnection(notificationId: String, currentUid: String, fromUid: String): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, fromUid)
            
            // 1. Delete connection
            db.collection("connections").document(docId).delete().await()
            
            // 2. Delete notification (if ID provided)
            if (notificationId.isNotEmpty()) {
                db.collection("notifications").document(notificationId).delete().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectedPeers(uid: String): Result<List<UserProfile>> {
        return try {
            val connections1 = db.collection("connections")
                .whereEqualTo("user1", uid)
                .whereEqualTo("status", "connected")
                .get().await()

            val connections2 = db.collection("connections")
                .whereEqualTo("user2", uid)
                .whereEqualTo("status", "connected")
                .get().await()

            val peerUids = (connections1.documents + connections2.documents).map { doc ->
                if (doc.getString("user1") == uid) doc.getString("user2")!! else doc.getString("user1")!!
            }.distinct()

            val profiles = peerUids.mapNotNull { peerUid ->
                usersCollection.document(peerUid).get().await().toObject(UserProfile::class.java)
            }

            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatSummaries(uid: String): Flow<List<ChatSummary>> {
        val connections1 = db.collection("connections")
            .whereEqualTo("user1", uid)
            .whereEqualTo("status", "connected")
            .snapshots()

        val connections2 = db.collection("connections")
            .whereEqualTo("user2", uid)
            .whereEqualTo("status", "connected")
            .snapshots()

        // Combine both queries
        return kotlinx.coroutines.flow.combine(connections1, connections2) { s1, s2 ->
            (s1.documents + s2.documents).distinctBy { it.id }
        }.map { docs ->
            docs.map { doc ->
                val peerUid = if (doc.getString("user1") == uid) doc.getString("user2")!! else doc.getString("user1")!!
                val peerProfile = usersCollection.document(peerUid).get().await().toObject(UserProfile::class.java)
                    ?: UserProfile(uid = peerUid, name = "Unknown")
                
                ChatSummary(
                    peer = peerProfile,
                    lastMessage = doc.getString("lastMessage") ?: "",
                    lastMessageTimestamp = doc.getTimestamp("lastMessageTimestamp"),
                    unreadCount = 0 // For now, we don't have unread count logic implemented
                )
            }.sortedByDescending { it.lastMessageTimestamp }
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        messageText: String,
        type: String = "text",
        language: String? = null
    ): Result<Unit> {
        return try {
            val messageData = mutableMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "message" to messageText,
                "timestamp" to FieldValue.serverTimestamp(),
                "type" to type,
                "isRead" to false
            )
            language?.let { messageData["language"] = it }

            db.collection("messages").add(messageData).await()

            // Update last message in connections
            db.collection("connections").document(chatId).update(
                mapOf(
                    "lastMessage" to messageText,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            // Get peer FCM token
            val peerDoc = db.collection("users").document(receiverId).get().await()
            val fcmToken = peerDoc.getString("fcmToken")
            
            if (fcmToken != null) {
                // Save notification to fcm_queue for Cloud Functions
                db.collection("fcm_queue").add(mapOf(
                    "token" to fcmToken,
                    "title" to senderName,
                    "body" to messageText,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }

            // OneSignal Push Notification
            val receiverDoc = db.collection("users").document(receiverId).get().await()
            val oneSignalId = receiverDoc.getString("oneSignalId")
            if (oneSignalId != null) {
                sendPushNotification(
                    toOneSignalId = oneSignalId,
                    title = senderName,
                    body = messageText
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPushNotification(
        toOneSignalId: String,
        title: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("app_id", "74ed12e3-94a3-48bc-b54e-41651cc735cc")
                put("include_subscription_ids", JSONArray().apply { put(toOneSignalId) })
                put("headings", JSONObject().apply { put("en", title) })
                put("contents", JSONObject().apply { put("en", body) })
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .post(requestBody)
                .addHeader("Authorization", "Basic YOUR_REST_API_KEY") // Replace with your actual REST API Key
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("OneSignal", "Failed: ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OneSignal", "Error: ${e.message}")
        }
    }
}

enum class ConnectionStatus {
    NOT_CONNECTED, PENDING, CONNECTED
}

data class ConnectionDetails(
    val status: ConnectionStatus,
    val requestedBy: String = ""
)

data class ChatSummary(
    val peer: UserProfile,
    val lastMessage: String = "",
    val lastMessageTimestamp: com.google.firebase.Timestamp? = null,
    val unreadCount: Int = 0
)

