package com.sahil.peerlearn

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val messagesCollection = db.collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isPeerTyping = MutableStateFlow(false)
    val isPeerTyping: StateFlow<Boolean> = _isPeerTyping

    private var messageListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile

    fun loadMessages(chatId: String, currentUid: String) {
        messageListener?.remove()
        messageListener = db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Firestore Error: ${e.message}")
                    return@addSnapshotListener
                }
                
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.timestamp } ?: emptyList()
                
                _messages.value = msgs

                // Auto-mark as read if we are the receiver
                if (msgs.any { it.receiverId == currentUid && !it.isRead }) {
                    markMessagesAsRead(chatId, currentUid)
                }
            }
    }

    fun setTyping(chatId: String, uid: String, isTyping: Boolean) {
        db.collection("typing_status")
            .document(chatId)
            .set(
                mapOf(uid to isTyping),
                com.google.firebase.firestore.SetOptions.merge()
            )
    }

    fun observeTyping(chatId: String, peerUid: String) {
        typingListener?.remove()
        typingListener = db.collection("typing_status")
            .document(chatId)
            .addSnapshotListener { doc, _ ->
                _isPeerTyping.value = doc?.getBoolean(peerUid) ?: false
            }
    }

    private var typingJob: Job? = null

    fun onTextChanged(
        text: String,
        chatId: String,
        uid: String
    ) {
        typingJob?.cancel()
        if (text.isNotEmpty()) {
            setTyping(chatId, uid, true)
            typingJob = viewModelScope.launch {
                delay(3000)
                setTyping(chatId, uid, false)
            }
        } else {
            setTyping(chatId, uid, false)
        }
    }

    fun markMessagesAsRead(chatId: String, currentUid: String) {
        db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("receiverId", currentUid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
            }
    }

    fun loadPeerProfile(peerUid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(peerUid).get().await()
                _peerProfile.value = doc.toObject(UserProfile::class.java)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendTextMessage(
        senderId: String,
        receiverId: String,
        text: String,
        replyToId: String = "",
        replyToText: String = ""
    ) {
        val uids = listOf(senderId, receiverId).sorted()
        val chatId = "${uids[0]}_${uids[1]}"
        
        val tempMessage = Message(
            id = "temp_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            type = "text",
            timestamp = Timestamp.now(),
            isRead = false,
            replyToId = replyToId,
            replyToText = replyToText
        )

        // Optimistic UI update
        _messages.value = _messages.value + tempMessage

        viewModelScope.launch {
            try {
                val messageData = hashMapOf(
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "receiverId" to receiverId,
                    "message" to text,
                    "timestamp" to Timestamp.now(),
                    "type" to "text",
                    "isRead" to false,
                    "replyToId" to replyToId,
                    "replyToText" to replyToText
                )
                
                messagesCollection.add(messageData).await()
                updateLastMessage(chatId, text)
            } catch (e: Exception) {
                // Remove temp message if failed
                _messages.value = _messages.value.filter { it.id != tempMessage.id }
            }
        }
    }

    fun sendCodeMessage(
        senderId: String,
        receiverId: String,
        code: String,
        language: String,
        replyToId: String = "",
        replyToText: String = ""
    ) {
        val uids = listOf(senderId, receiverId).sorted()
        val chatId = "${uids[0]}_${uids[1]}"

        val tempMessage = Message(
            id = "temp_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            message = code,
            type = "code",
            language = language,
            timestamp = Timestamp.now(),
            isRead = false,
            replyToId = replyToId,
            replyToText = replyToText
        )

        // Optimistic UI update
        _messages.value = _messages.value + tempMessage

        viewModelScope.launch {
            try {
                val messageData = hashMapOf(
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "receiverId" to receiverId,
                    "message" to code,
                    "timestamp" to Timestamp.now(),
                    "type" to "code",
                    "language" to language,
                    "isRead" to false,
                    "replyToId" to replyToId,
                    "replyToText" to replyToText
                )
                
                messagesCollection.add(messageData).await()
                updateLastMessage(chatId, "Sent code in $language")
            } catch (e: Exception) {
                // Remove temp message if failed
                _messages.value = _messages.value.filter { it.id != tempMessage.id }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                messagesCollection.document(messageId).delete().await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun updateLastMessage(chatId: String, text: String) {
        try {
            db.collection("connections").document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to Timestamp.now()
                )
            ).await()
        } catch (e: Exception) {
            // Document might not exist with this ID
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.remove()
        typingListener?.remove()
    }
}
