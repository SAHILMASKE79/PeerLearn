package com.sahil.peerlearn

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val userRepository = UserRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isPeerTyping = MutableStateFlow(false)
    val isPeerTyping: StateFlow<Boolean> = _isPeerTyping

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile

    private val _activeSession = MutableStateFlow<StudySession?>(null)
    val activeSession: StateFlow<StudySession?> = _activeSession

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds: StateFlow<Long> = _timerSeconds

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress

    private var timerJob: Job? = null
    private var typingJob: Job? = null

    // ── Messages ──

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .snapshots()
                    .collect { snapshot ->
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            try {
                                Message(
                                    id = doc.id,
                                    senderId = doc.getString("senderId") ?: "",
                                    receiverId = doc.getString("receiverId") ?: "",
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                    type = doc.getString("type") ?: "text",
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    codeSnippet = doc.getString("codeSnippet") ?: "",
                                    codeLanguage = doc.getString("codeLanguage") ?: "",
                                    imageUrl = doc.getString("imageUrl") ?: ""
                                )
                            } catch (e: Exception) {
                                Log.e("ChatVM", "Message parse error: ${e.message}")
                                null
                            }
                        }
                        _messages.value = msgs
                    }
            } catch (e: Exception) {
                Log.e("ChatVM", "loadMessages failed: ${e.message}")
            }
        }
    }

    fun sendTextMessage(senderId: String, receiverId: String, text: String) {
        viewModelScope.launch {
            try {
                val chatId = listOf(senderId, receiverId).sorted().joinToString("_")
                val senderName = Firebase.auth.currentUser?.displayName ?: ""
                userRepository.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    receiverId = receiverId,
                    messageText = text,
                    type = "text"
                )
            } catch (e: Exception) {
                Log.e("ChatVM", "sendTextMessage failed: ${e.message}")
            }
        }
    }

    fun sendCodeSnippet(senderId: String, receiverId: String, code: String, language: String) {
        viewModelScope.launch {
            try {
                val chatId = listOf(senderId, receiverId).sorted().joinToString("_")
                val senderName = Firebase.auth.currentUser?.displayName ?: ""

                val messageData = mutableMapOf<String, Any>(
                    "senderId" to senderId,
                    "receiverId" to receiverId,
                    "text" to "[Code snippet]",
                    "codeSnippet" to code,
                    "codeLanguage" to language,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "type" to "code",
                    "isRead" to false
                )

                db.collection("chats").document(chatId)
                    .collection("messages").add(messageData).await()

                db.collection("connections").document(chatId).update(
                    mapOf(
                        "lastMessage" to "📎 Code snippet",
                        "lastMessageTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

            } catch (e: Exception) {
                Log.e("ChatVM", "sendCodeSnippet failed: ${e.message}")
            }
        }
    }

    fun uploadImage(chatId: String, senderId: String, receiverId: String, uri: Uri) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = 0f
                val storageRef = storage.reference
                    .child("chat_images/$chatId/${UUID.randomUUID()}.jpg")

                val uploadTask = storageRef.putFile(uri)
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
                    _uploadProgress.value = progress
                }
                uploadTask.await()

                val downloadUrl = storageRef.downloadUrl.await().toString()

                val messageData = mapOf<String, Any>(
                    "senderId" to senderId,
                    "receiverId" to receiverId,
                    "text" to "",
                    "imageUrl" to downloadUrl,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "type" to "image",
                    "isRead" to false
                )

                db.collection("chats").document(chatId)
                    .collection("messages").add(messageData).await()

                db.collection("connections").document(chatId).update(
                    mapOf(
                        "lastMessage" to "📷 Image",
                        "lastMessageTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                _uploadProgress.value = null
            } catch (e: Exception) {
                _uploadProgress.value = null
                Log.e("ChatVM", "Image upload failed: ${e.message}")
            }
        }
    }

    fun markMessagesAsRead(chatId: String, currentUid: String) {
        viewModelScope.launch {
            try {
                val unread = db.collection("chats").document(chatId)
                    .collection("messages")
                    .whereEqualTo("receiverId", currentUid)
                    .whereEqualTo("isRead", false)
                    .get().await()

                val batch = db.batch()
                unread.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                if (!unread.isEmpty) batch.commit().await()
            } catch (e: Exception) {
                Log.e("ChatVM", "markMessagesAsRead failed: ${e.message}")
            }
        }
    }

    // ── Typing ──

    fun onTextChanged(text: String, chatId: String, currentUid: String) {
        typingJob?.cancel()
        viewModelScope.launch {
            setTyping(chatId, currentUid, text.isNotEmpty())
            if (text.isNotEmpty()) {
                typingJob = viewModelScope.launch {
                    delay(2000)
                    setTyping(chatId, currentUid, false)
                }
            }
        }
    }

    private suspend fun setTyping(chatId: String, uid: String, isTyping: Boolean) {
        try {
            db.collection("chats").document(chatId)
                .collection("typing").document(uid)
                .set(mapOf(
                    "isTyping" to isTyping,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )).await()
        } catch (e: Exception) {
            Log.e("ChatVM", "setTyping failed: ${e.message}")
        }
    }

    fun observeTyping(chatId: String, peerUid: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("typing").document(peerUid)
                    .snapshots()
                    .collect { snapshot ->
                        _isPeerTyping.value = snapshot.getBoolean("isTyping") ?: false
                    }
            } catch (e: Exception) {
                Log.e("ChatVM", "observeTyping failed: ${e.message}")
            }
        }
    }

    // ── Peer Profile ──

    fun loadPeerProfile(peerUid: String) {
        viewModelScope.launch {
            try {
                userRepository.getUserProfile(peerUid).collect {
                    _peerProfile.value = it
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "loadPeerProfile failed: ${e.message}")
            }
        }
    }

    // ── Study Session ──

    fun observeSession(chatId: String) {
        viewModelScope.launch {
            try {
                db.collection("studySessions")
                    .whereEqualTo("chatId", chatId)
                    .whereIn("status", listOf("active", "paused"))
                    .snapshots()
                    .collect { snapshot ->
                        val session = snapshot.documents.firstOrNull()?.let { doc ->
                            StudySession(
                                sessionId = doc.id,
                                chatId = doc.getString("chatId") ?: "",
                                topic = doc.getString("topic") ?: "",
                                status = doc.getString("status") ?: "active",
                                startedAt = doc.getTimestamp("startedAt") ?: Timestamp.now(),
                                elapsedSeconds = doc.getLong("elapsedSeconds") ?: 0L
                            )
                        }
                        _activeSession.value = session
                        if (session != null && session.status == "active") {
                            startTimer(session.elapsedSeconds)
                        } else {
                            timerJob?.cancel()
                        }
                    }
            } catch (e: Exception) {
                Log.e("ChatVM", "observeSession failed: ${e.message}")
            }
        }
    }

    private fun startTimer(initialSeconds: Long) {
        timerJob?.cancel()
        _timerSeconds.value = initialSeconds
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerSeconds.value++
            }
        }
    }

    fun startSession(chatId: String, topic: String) {
        viewModelScope.launch {
            try {
                db.collection("studySessions").add(mapOf(
                    "chatId" to chatId,
                    "topic" to topic,
                    "status" to "active",
                    "startedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "elapsedSeconds" to 0L
                )).await()
            } catch (e: Exception) {
                Log.e("ChatVM", "startSession failed: ${e.message}")
            }
        }
    }

    fun endSession(sessionId: String) {
        viewModelScope.launch {
            try {
                timerJob?.cancel()
                db.collection("studySessions").document(sessionId)
                    .update(mapOf(
                        "status" to "ended",
                        "elapsedSeconds" to _timerSeconds.value
                    )).await()
                _activeSession.value = null
                _timerSeconds.value = 0L
            } catch (e: Exception) {
                Log.e("ChatVM", "endSession failed: ${e.message}")
            }
        }
    }

    fun toggleSessionStatus(sessionId: String) {
        viewModelScope.launch {
            try {
                val current = _activeSession.value ?: return@launch
                val newStatus = if (current.status == "active") "paused" else "active"
                db.collection("studySessions").document(sessionId)
                    .update(mapOf(
                        "status" to newStatus,
                        "elapsedSeconds" to _timerSeconds.value
                    )).await()
            } catch (e: Exception) {
                Log.e("ChatVM", "toggleSession failed: ${e.message}")
            }
        }
    }

    fun updateSessionTopic(sessionId: String, newTopic: String) {
        viewModelScope.launch {
            try {
                db.collection("studySessions").document(sessionId)
                    .update("topic", newTopic).await()
            } catch (e: Exception) {
                Log.e("ChatVM", "updateSessionTopic failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        typingJob?.cancel()
    }
}