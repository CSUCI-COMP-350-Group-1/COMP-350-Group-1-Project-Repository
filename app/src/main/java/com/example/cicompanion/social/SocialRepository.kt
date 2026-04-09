package com.example.cicompanion.social

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object SocialRepository {

    /**
     * Loads all searchable users except the currently signed-in user.
     */
    fun fetchSearchableUsers(
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection()
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents
                    .mapNotNull { it.toObject(UserProfile::class.java) }
                    .filter { it.uid.isNotBlank() && it.uid != currentUserId }
                    .sortedBy { displayNameOrEmail(it).lowercase() }

                onSuccess(users)
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load users.")
            }
    }

    /**
     * Loads all outgoing friend request statuses for the current user.
     * The map key is the target user's uid.
     */
    fun fetchOutgoingFriendRequestStatuses(
        currentUserId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val statuses = snapshot.documents.associate { document ->
                    val request = document.toObject(FriendRequest::class.java)
                    val targetUid = request?.toUserId ?: ""
                    val status = request?.status ?: "pending"
                    targetUid to status
                }.filterKeys { it.isNotBlank() }

                onSuccess(statuses)
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load friend request statuses.")
            }
    }

    /**
     * Sends a friend request from the current user to the selected target user.
     */
    fun sendFriendRequest(
        currentUser: FirebaseUser,
        targetUser: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (currentUser.uid == targetUser.uid) {
            onError("You cannot send a friend request to yourself.")
            return
        }

        val requestId = createFriendRequestId(currentUser.uid, targetUser.uid)
        val request = buildFriendRequest(currentUser, targetUser, requestId)

        createFriendRequest(
            request = request,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Builds a stable friend-request document id for a sender and receiver.
     */
    private fun createFriendRequestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_$toUserId"
    }

    /**
     * Creates the Firestore friend-request payload from the current and target users.
     */
    private fun buildFriendRequest(
        currentUser: FirebaseUser,
        targetUser: UserProfile,
        requestId: String
    ): FriendRequest {
        return FriendRequest(
            id = requestId,
            fromUserId = currentUser.uid,
            toUserId = targetUser.uid,
            fromDisplayName = currentUser.displayName ?: "",
            fromEmail = currentUser.email ?: "",
            fromPhotoUrl = currentUser.photoUrl?.toString() ?: "",
            toDisplayName = targetUser.displayName,
            toEmail = targetUser.email,
            status = "pending",
            sentAt = System.currentTimeMillis()
        )
    }

    /**
     * Creates the friend request document in Firestore.
     */
    private fun createFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(request.id)
            .set(request)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(friendRequestErrorMessage(exception))
            }
    }

    /**
     * Converts Firestore failures into a friendlier message for the UI.
     */
    private fun friendRequestErrorMessage(exception: Exception): String {
        return if (
            exception is FirebaseFirestoreException &&
            exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            "This friend request already exists, or Firestore rules blocked the write."
        } else {
            exception.message ?: "Could not send the friend request."
        }
    }

    /**
     * Returns the best text label to show for a user in search results.
     */
    fun displayNameOrEmail(user: UserProfile): String {
        return user.displayName.ifBlank { user.email }
    }

    /**
     * Returns the users collection reference.
     */
    private fun usersCollection() = FirebaseFirestore.getInstance().collection("users")

    /**
     * Returns the friend-requests collection reference.
     */
    private fun friendRequestsCollection() =
        FirebaseFirestore.getInstance().collection("friend_requests")
}