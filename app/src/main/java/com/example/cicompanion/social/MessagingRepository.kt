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
    private const val MAX_GROUP_NAME_LENGTH = 80

    // GROUP MESSAGING CHANGE: one participant normalizer for both direct and group chats.
    private fun buildParticipantIds(userIds: Collection<String>): List<String> {
        return userIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Existing direct-message helper kept for current callers.
    private fun buildParticipantIds(firstUserId: String, secondUserId: String): List<String> {
        return buildParticipantIds(listOf(firstUserId, secondUserId))
    }

    fun createConversationId(firstUserId: String, secondUserId: String): String {
        return buildParticipantIds(firstUserId, secondUserId)
            .joinToString("_")
    }

    // GROUP MESSAGING CHANGE: group chats are intentionally unique, so the same people can
    // create more than one group if they want different names/topics.
    fun createGroupConversationId(participantIds: List<String>): String {
        return "group_${participantIds.joinToString("_")}_${System.currentTimeMillis()}"
    }

    fun findOtherParticipantId(
        conversation: ConversationSummary,
        currentUserId: String
    ): String? {
        return conversation.participantIds.firstOrNull { it != currentUserId }
    }

    // GROUP MESSAGING CHANGE:
    // supports conversation list/header display for group chats.
    fun findOtherParticipantIds(
        conversation: ConversationSummary,
        currentUserId: String
    ): List<String> {
        return conversation.participantIds.filter { it != currentUserId }
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

    // GROUP MESSAGING CHANGE: loads all group members with chunking for Firestore's whereIn limit.
    fun fetchUserProfiles(
        userIds: List<String>,
        onSuccess: (Map<String, UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanIds = userIds.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) {
            onSuccess(emptyMap())
            return
        }

        val chunks = cleanIds.chunked(10)
        val profiles = mutableMapOf<String, UserProfile>()
        var completedChunks = 0

        chunks.forEach { chunk ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .whereIn("uid", chunk)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents
                        .mapNotNull { it.toObject(UserProfile::class.java) }
                        .forEach { profile -> profiles[profile.uid] = profile }

                    completedChunks++
                    if (completedChunks == chunks.size) {
                        onSuccess(profiles)
                    }
                }
                .addOnFailureListener { exception ->
                    onError(exception.message ?: "Could not load group members.")
                }
        }
    }

    // Check whether the conversation document already exists before attaching a message listener.
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

    // GROUP MESSAGING CHANGE: listen to the shared conversation document so group name
    // changes sync live on every device in the group.
    fun listenToConversation(
        conversationId: String,
        onUpdate: (ConversationSummary?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception.message ?: "Could not load conversation.")
                    return@addSnapshotListener
                }

                onUpdate(snapshot?.toObject(ConversationSummary::class.java))
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

    // GROUP MESSAGING CHANGE:
    // creates the group conversation immediately, even before the
    // first message, so users can rename the group right away.
    fun createGroupConversation(
        currentUser: FirebaseUser,
        selectedFriendIds: List<String>,
        groupName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val participantIds = buildParticipantIds(selectedFriendIds + currentUser.uid)
        if (participantIds.size < 3) {
            onError("Choose at least two friends for a group chat.")
            return
        }

        val conversationId = createGroupConversationId(participantIds)
        val now = System.currentTimeMillis()
        val safeGroupName = groupName.trim().take(MAX_GROUP_NAME_LENGTH)

        val conversation = hashMapOf(
            "id" to conversationId,
            "participantIds" to participantIds,
            "groupName" to safeGroupName,
            "groupNameUpdatedAt" to now,
            "groupNameUpdatedBy" to currentUser.uid,
            "lastMessageText" to "",
            "lastMessageType" to "text",
            "lastMessageSenderId" to "",
            "lastMessageAt" to 0L
        )

        FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .set(conversation)
            .addOnSuccessListener { onSuccess(conversationId) }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not create group chat.")
            }
    }

    // GROUP MESSAGING CHANGE group name is stored on the conversation, so all participants
    // see updates through their conversation listener.
    fun updateGroupName(
        conversationId: String,
        currentUserId: String,
        newGroupName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val safeGroupName = newGroupName.trim().take(MAX_GROUP_NAME_LENGTH)
        val updates = mapOf(
            "groupName" to safeGroupName,
            "groupNameUpdatedAt" to System.currentTimeMillis(),
            "groupNameUpdatedBy" to currentUserId
        )

        FirebaseFirestore.getInstance()
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not update group name.")
            }
    }

    // Existing one-to-one API kept so current direct-chat call sites do not need a rewrite.
    fun sendMessage(
        currentUser: FirebaseUser,
        friend: UserProfile,
        messageText: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val conversationId = createConversationId(currentUser.uid, friend.uid)
        val conversation = ConversationSummary(
            id = conversationId,
            participantIds = buildParticipantIds(currentUser.uid, friend.uid)
        )

        sendMessageToConversation(
            currentUser = currentUser,
            conversation = conversation,
            messageText = messageText,
            type = type,
            metadata = metadata,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    // GROUP MESSAGING CHANGE: shared send path for direct and group messages.
    // This keeps message creation cohesive and avoids duplicating Firestore batch logic.
    fun sendMessageToConversation(
        currentUser: FirebaseUser,
        conversation: ConversationSummary,
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

        if (currentUser.uid !in conversation.participantIds) {
            onError("You are not a member of this conversation.")
            return
        }

        val recipientIds = conversation.participantIds.filter { it != currentUser.uid }
        if (recipientIds.isEmpty()) {
            onError("No recipients found for this conversation.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val conversationRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        val messageRef = conversationRef.collection(MESSAGES_SUBCOLLECTION).document()
        val sentAt = System.currentTimeMillis()
        val previewText = if (type == "text") trimmedText else "[$type]"

        val summary = hashMapOf(
            "id" to conversation.id,
            "participantIds" to conversation.participantIds.sorted(),
            "lastMessageText" to previewText,
            "lastMessageType" to type,
            "lastMessageSenderId" to currentUser.uid,
            "lastMessageAt" to sentAt
        )

        val message = DirectMessage(
            id = messageRef.id,
            conversationId = conversation.id,
            senderId = currentUser.uid,
            receiverId = recipientIds.singleOrNull().orEmpty(),
            recipientIds = recipientIds,
            text = trimmedText,
            sentAt = sentAt,
            type = type,
            metadata = metadata
        )

        // MESSAGING batch keeps summary + first/new message together.
        db.runBatch { batch ->
            batch.set(conversationRef, summary, SetOptions.merge())
            batch.set(messageRef, message)
        }.addOnSuccessListener {
            val senderName = currentUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email
                ?: "Someone"

            // GROUP MESSAGING CHANGE:direct messages notify one recipient; groups notify everyone else.
            recipientIds.forEach { targetUserId ->
                MessageNotificationSender.sendDirectMessageNotification(
                    targetUserId = targetUserId,
                    senderUserId = currentUser.uid,
                    senderDisplayName = senderName,
                    conversationId = conversation.id,
                    messagePreview = previewText.take(120),
                    isGroup = conversation.isGroup,
                    conversationTitle = conversation.groupName
                )
            }

            onSuccess()
        }.addOnFailureListener { exception ->
            onError(exception.message ?: "Could not send message.")
        }
    }
}