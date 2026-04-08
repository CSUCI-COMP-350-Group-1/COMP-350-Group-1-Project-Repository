package com.example.cicompanion.social

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

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
     * Sends a friend request only when one does not already exist for the same pair of users.
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

        checkIfRequestExists(
            requestId = requestId,
            onComplete = { exists ->
                if (exists) {
                    onError("You already sent a friend request to this user.")
                } else {
                    val request = buildFriendRequest(currentUser, targetUser, requestId)
                    saveFriendRequest(request, onSuccess, onError)
                }
            },
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
     * Checks whether a friend request document already exists.
     */
    private fun checkIfRequestExists(
        requestId: String,
        onComplete: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->
                onComplete(document.exists())
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not verify the existing friend request.")
            }
    }

    /**
     * Saves a new friend request to Firestore.
     */
    private fun saveFriendRequest(
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
                onError(exception.message ?: "Could not send the friend request.")
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
    private fun friendRequestsCollection() = FirebaseFirestore.getInstance().collection("friend_requests")
}
