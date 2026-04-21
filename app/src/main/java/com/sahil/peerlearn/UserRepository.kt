package com.sahil.peerlearn

import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.storage
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    private val storage = Firebase.storage
    private val usersCollection = db.collection("users")

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
            val college = document.getString("college") ?: ""
            college.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun getUserProfile(uid: String): Flow<UserProfile?> {
        return usersCollection.document(uid).snapshots().map { snapshot ->
            if (!snapshot.exists()) null
            else UserProfile(
                uid = snapshot.getString("uid") ?: uid,
                email = snapshot.getString("email") ?: "",
                name = snapshot.getString("name") ?: "",
                bio = snapshot.getString("bio") ?: "",
                college = snapshot.getString("college") ?: "",
                year = snapshot.getString("year") ?: "",
                githubLink = snapshot.getString("githubLink") ?: "",
                linkedinLink = snapshot.getString("linkedinLink") ?: "",
                role = snapshot.getString("role") ?: "student",
                createdAt = snapshot.getTimestamp("createdAt") as Timestamp?,
                teachSkills = (snapshot.get("teachSkills") as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList(),
                learnSkills = (snapshot.get("learnSkills") as? List<*>)
                    ?.mapNotNull { it as? String } ?: emptyList(),
                profileImageUrl = snapshot.getString("profileImageUrl") ?: ""
            )
        }
    }

    // ── Connections ──

    private fun getConnectionDocId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    suspend fun sendConnectionRequest(
        currentUid: String,
        currentUserName: String,
        peerUid: String
    ): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, peerUid)

            val senderName = if (currentUserName == "Peer" || currentUserName.isEmpty()) {
                usersCollection.document(currentUid).get().await().getString("name") ?: "A Peer"
            } else currentUserName

            // Check if connection already exists
            val existing = db.collection("connections").document(docId).get().await()
            if (existing.exists()) {
                val status = existing.getString("status") ?: ""

                when (status) {
                    "connected" -> return Result.failure(Exception("Connection already exists"))
                    "pending" -> return Result.failure(Exception("Connection already exists"))
                    "disconnected" -> {
                        // Allow reconnect — update existing document
                        db.collection("connections").document(docId).update(
                            mapOf(
                                "status" to "pending",
                                "requestedBy" to currentUid,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "connectedAt" to null
                            )
                        ).await()

                        val notifData = mapOf(
                            "toUid" to peerUid,
                            "fromUid" to currentUid,
                            "fromName" to senderName,
                            "type" to "connection_request",
                            "message" to "$senderName wants to connect with you! 🤝",
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("notifications").add(notifData).await()

                        sendPushNotification(
                            toUid = peerUid,
                            title = "New Connection Request",
                            body = "$senderName wants to connect with you! 🤝"
                        )

                        return Result.success(Unit)
                    }
                }
            }

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

            val notificationData = mapOf(
                "toUid" to peerUid,
                "fromUid" to currentUid,
                "fromName" to senderName,
                "type" to "connection_request",
                "message" to "$senderName wants to connect with you! 🤝",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").add(notificationData).await()

            sendPushNotification(
                toUid = peerUid,
                title = "New Connection Request",
                body = "$senderName wants to connect with you! 🤝"
            )

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
                    "disconnected" -> ConnectionStatus.DISCONNECTED
                    else -> ConnectionStatus.NOT_CONNECTED
                }
                ConnectionDetails(
                    status = status,
                    requestedBy = snapshot.getString("requestedBy") ?: ""
                )
            }
        }
    }

    suspend fun disconnectPeer(currentUid: String, peerUid: String): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, peerUid)
            db.collection("connections").document(docId)
                .update("status", "disconnected").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Notifications ──

    fun getNotifications(uid: String): Flow<List<NotificationItem>> {
        return db.collection("notifications")
            .whereEqualTo("toUid", uid)
            .snapshots()
            .map { snapshot ->
                try {
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.createdAt }
                } catch (e: Exception) {
                    emptyList()
                }
            }
    }

    fun getUnreadNotificationCount(uid: String): Flow<Int> {
        return db.collection("notifications")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("isRead", false)
            .snapshots()
            .map { it.size() }
    }

    suspend fun markAllNotificationsAsRead(uid: String): Result<Unit> {
        return try {
            val unreadNotifications = db.collection("notifications")
                .whereEqualTo("toUid", uid)
                .whereEqualTo("isRead", false)
                .get().await()

            if (unreadNotifications.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            for (doc in unreadNotifications.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

            val senderName = if (currentUserName == "Peer" || currentUserName.isEmpty()) {
                usersCollection.document(currentUid).get().await().getString("name") ?: "A Peer"
            } else currentUserName

            db.collection("connections").document(docId)
                .update(mapOf(
                    "status" to "connected",
                    "connectedAt" to FieldValue.serverTimestamp()
                )).await()

            if (notificationId.isNotEmpty()) {
                db.collection("notifications").document(notificationId).delete().await()
            }

            val notificationData = mapOf(
                "toUid" to fromUid,
                "fromUid" to currentUid,
                "fromName" to senderName,
                "type" to "connection_accepted",
                "message" to "$senderName accepted your request! 🎉",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").add(notificationData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineConnection(
        notificationId: String,
        currentUid: String,
        fromUid: String
    ): Result<Unit> {
        return try {
            val docId = getConnectionDocId(currentUid, fromUid)
            db.collection("connections").document(docId).delete().await()
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
                if (doc.getString("user1") == uid) doc.getString("user2")!!
                else doc.getString("user1")!!
            }.distinct()

            val profiles = peerUids.mapNotNull { peerUid ->
                usersCollection.document(peerUid).get().await()
                    .toObject(UserProfile::class.java)
            }

            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ FIXED: Only "connected" status + proper error handling
    fun getChatSummaries(currentUid: String): Flow<List<ChatSummary>> = callbackFlow {
        val listener = db.collection("connections")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("ChatSummaries", "Error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // ✅ Only show "connected" status connections
                val myConnections = snap.documents.filter { doc ->
                    val u1 = doc.getString("user1") ?: ""
                    val u2 = doc.getString("user2") ?: ""
                    val status = doc.getString("status") ?: ""
                    (u1 == currentUid || u2 == currentUid) && status == "connected"
                }

                if (myConnections.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val summaries = mutableListOf<ChatSummary>()
                var pending = myConnections.size

                myConnections.forEach { doc ->
                    val u1 = doc.getString("user1") ?: ""
                    val u2 = doc.getString("user2") ?: ""
                    val peerUid = if (u1 == currentUid) u2 else u1
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastTimestamp = doc.getTimestamp("lastMessageTimestamp")

                    db.collection("users").document(peerUid).get()
                        .addOnSuccessListener { userDoc ->
                            val peer = userDoc.toObject(UserProfile::class.java)?.copy(uid = peerUid)
                            if (peer != null) {
                                summaries.add(
                                    ChatSummary(
                                        peer = peer,
                                        lastMessage = lastMessage,
                                        lastMessageTimestamp = lastTimestamp,
                                        unreadCount = 0
                                    )
                                )
                            }
                            pending--
                            if (pending == 0) {
                                trySend(summaries.sortedByDescending {
                                    it.lastMessageTimestamp?.seconds ?: 0
                                })
                            }
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) trySend(summaries.toList())
                        }
                }
            }

        awaitClose { listener.remove() }
    }

    // ✅ FIXED: Messages now stored in chats/{chatId}/messages subcollection
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = storage.app.applicationContext
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image bytes")
            val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            
            val timestamp = System.currentTimeMillis() / 1000
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
            val apiKey = BuildConfig.CLOUDINARY_API_KEY
            val apiSecret = BuildConfig.CLOUDINARY_API_SECRET

            android.util.Log.d("Cloudinary", "CloudName: $cloudName")
            android.util.Log.d("Cloudinary", "ApiKey: $apiKey")
            android.util.Log.d("Cloudinary", "Secret empty: ${apiSecret.isEmpty()}")

            val toSign = "public_id=profile_$uid&timestamp=$timestamp$apiSecret"
            val signature = sha1(toSign)

            val client = OkHttpClient()
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", "data:image/jpeg;base64,$base64Image")
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp.toString())
                .addFormDataPart("public_id", "profile_$uid")
                .addFormDataPart("signature", signature)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from Cloudinary")
            
            if (!response.isSuccessful) {
                throw Exception("Cloudinary upload failed: $responseBody")
            }

            val jsonResponse = JSONObject(responseBody)
            val imageUrl = jsonResponse.getString("secure_url")

            // ✅ Save URL to Firestore using set with merge
            usersCollection.document(uid)
                .set(mapOf("profileImageUrl" to imageUrl), com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(imageUrl)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Upload failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun sha1(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-1")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    // ✅ FIXED: getMessages bhi same subcollection se fetch karo
    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("getMessages", "Error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        messageText: String,
        type: String = "text"
    ): Result<Unit> {
        return try {
            val messageData = mapOf(
                "senderId" to senderId,
                "receiverId" to receiverId,
                "text" to messageText,
                "timestamp" to FieldValue.serverTimestamp(),
                "type" to type,
                "isRead" to false
            )

            db.collection("chats").document(chatId)
                .collection("messages").add(messageData).await()

            db.collection("connections").document(chatId).update(
                mapOf(
                    "lastMessage" to if (type == "text") messageText else "[$type]",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            sendPushNotification(
                toUid = receiverId,
                title = "New message from $senderName",
                body = if (type == "text") messageText else "Sent a $type"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPushNotification(
        toUid: String,
        title: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("app_id", "74ed12e3-94a3-48bc-b54e-41651cc735cc")
                put("include_external_user_ids", JSONArray().apply { put(toUid) })
                put("headings", JSONObject().apply { put("en", title) })
                put("contents", JSONObject().apply { put("en", body) })
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .post(requestBody)
                .addHeader("Authorization", "Basic YOUR_REST_API_KEY")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("OneSignal", "Push failed: ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OneSignal", "Push Error: ${e.message}")
        }
    }

    fun getAllUserProfiles(): Flow<List<UserProfile>> {
        return usersCollection.snapshots().map { snapshot ->
            snapshot.toObjects(UserProfile::class.java)
        }
    }
}

enum class ConnectionStatus {
    NOT_CONNECTED, PENDING, CONNECTED, DISCONNECTED
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

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val type: String = "text",
    val isRead: Boolean = false,
    val codeLanguage: String? = null
)