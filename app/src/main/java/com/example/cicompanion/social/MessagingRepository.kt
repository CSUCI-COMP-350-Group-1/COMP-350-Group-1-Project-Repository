package com.example.cicompanion.social

import com.example.cicompanion.firebase.MessageNotificationSender
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object MessagingRepository {

    private const val CONVERSATIONS_COLLECTION = "conversations"
    private const val MESSAGES_SUBCOLLECTION = "messages"

    private fun buildParticipantIds(firstUserId: String, secondUserId: String): List<String> {
        return listOf(firstUserId, secondUserId).sorted()
    }

    fun createConversationId(firstUserId: String, secondUserId: String): String {
        return listOf(firstUserId, secondUserId)
            .sorted()
            .joinToString("_")
    }

    fun findOtherParticipantId(
        conversation: ConversationSummary,
        currentUserId: String
    ): String? {
        return conversation.participantIds.firstOrNull { it != currentUserId }
    }

    fun fetchUserProfile(
        userId: String,
        onSuccess: (UserProfile?) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(UserProfile::class.java))
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load user profile.")
            }
    }

    fun checkConversationExists(
        conversationId: String,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.exists())
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not check conversation.")
            }
    }

    fun listenToConversations(
        currentUserId: String,
        onUpdate: (List<ConversationSummary>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception.message ?: "Could not load conversations.")
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents
                    ?.mapNotNull { it.toObject(ConversationSummary::class.java) }
                    ?.sortedByDescending { it.lastMessageAt }
                    .orEmpty()

                onUpdate(conversations)
            }
    }

    fun listenToMessages(
        conversationId: String,
        onUpdate: (List<DirectMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception.message ?: "Could not load messages.")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents
                    ?.mapNotNull { it.toObject(DirectMessage::class.java) }
                    .orEmpty()

                onUpdate(messages)
            }
    }

    fun deleteMessage(
        conversationId: String,
        messageId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Could not delete message.") }
    }

    fun sendMessage(
        currentUser: FirebaseUser,
        friend: UserProfile,
        messageText: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedText = messageText.trim()

        if (trimmedText.isBlank() && type == "text") {
            onError("Message cannot be empty.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val conversationId = createConversationId(currentUser.uid, friend.uid)
        val conversationRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
        val messageRef = conversationRef.collection(MESSAGES_SUBCOLLECTION).document()
        val sentAt = System.currentTimeMillis()

        val participantIds = buildParticipantIds(currentUser.uid, friend.uid)

        // Modified: always use messageText for preview if available, otherwise fallback to bracketed type.
        val lastMessageText = when {
            trimmedText.isNotBlank() -> trimmedText
            else -> "[$type]"
        }

        val summary = hashMapOf(
            "id" to conversationId,
            "participantIds" to participantIds,
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to currentUser.uid,
            "lastMessageAt" to sentAt
        )

        val message = DirectMessage(
            id = messageRef.id,
            conversationId = conversationId,
            senderId = currentUser.uid,
            receiverId = friend.uid,
            text = trimmedText,
            sentAt = sentAt,
            type = type,
            metadata = metadata
        )

        db.runBatch { batch ->
            batch.set(conversationRef, summary, SetOptions.merge())
            batch.set(messageRef, message)
        }.addOnSuccessListener {
            MessageNotificationSender.sendDirectMessageNotification(
                targetUserId = friend.uid,
                senderUserId = currentUser.uid,
                senderDisplayName = currentUser.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser.email
                    ?: "Someone",
                conversationId = conversationId,
                messagePreview = lastMessageText.take(120)
            )
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(exception.message ?: "Could not send message.")
        }
    }
}