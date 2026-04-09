package com.example.cicompanion.social

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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
                handleSearchableUsersSuccess(
                    snapshot = snapshot,
                    currentUserId = currentUserId,
                    onSuccess = onSuccess
                )
            }
            .addOnFailureListener { exception ->
                onError(searchableUsersErrorMessage(exception))
            }
    }

    /**
     * Converts the users snapshot into sorted search results.
     */
    private fun handleSearchableUsersSuccess(
        snapshot: QuerySnapshot,
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit
    ) {
        val users = mapSearchableUsers(snapshot, currentUserId)
        onSuccess(users)
    }

    /**
     * Maps Firestore user documents into searchable user profiles.
     */
    private fun mapSearchableUsers(
        snapshot: QuerySnapshot,
        currentUserId: String
    ): List<UserProfile> {
        return snapshot.documents
            .mapNotNull { it.toObject(UserProfile::class.java) }
            .filter { it.uid.isNotBlank() && it.uid != currentUserId }
            .sortedBy { displayNameOrEmail(it).lowercase() }
    }

    /**
     * Returns the error message shown when searchable users fail to load.
     */
    private fun searchableUsersErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not load users."
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
                handleExistingRequestCheck(
                    exists = exists,
                    currentUser = currentUser,
                    targetUser = targetUser,
                    requestId = requestId,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onError = onError
        )
    }

    /**
     * Handles the result of the duplicate request check.
     */
    private fun handleExistingRequestCheck(
        exists: Boolean,
        currentUser: FirebaseUser,
        targetUser: UserProfile,
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (exists) {
            onError("You already sent a friend request to this user.")
            return
        }

        val request = buildFriendRequest(currentUser, targetUser, requestId)
        saveFriendRequest(request, onSuccess, onError)
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
                onError(existingRequestErrorMessage(exception))
            }
    }

    /**
     * Returns the error message shown when the duplicate check fails.
     */
    private fun existingRequestErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not verify the existing friend request."
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
                onError(sendFriendRequestErrorMessage(exception))
            }
    }

    /**
     * Returns the error message shown when sending a friend request fails.
     */
    private fun sendFriendRequestErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not send the friend request."
    }

    /**
     * Loads the current user's incoming pending friend requests.
     */
    fun fetchIncomingFriendRequests(
        currentUserId: String,
        onSuccess: (List<FriendRequest>) -> Unit,
        onError: (String) -> Unit
    ) {
        buildIncomingFriendRequestsQuery(currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                handleIncomingRequestsSuccess(snapshot, onSuccess)
            }
            .addOnFailureListener { exception ->
                onError(incomingRequestsErrorMessage(exception))
            }
    }

    /**
     * Builds the Firestore query used to load incoming pending friend requests.
     */
    private fun buildIncomingFriendRequestsQuery(currentUserId: String) =
        friendRequestsCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")

    /**
     * Converts the incoming requests snapshot into a sorted list.
     */
    private fun handleIncomingRequestsSuccess(
        snapshot: QuerySnapshot,
        onSuccess: (List<FriendRequest>) -> Unit
    ) {
        val requests = mapIncomingFriendRequests(snapshot)
        onSuccess(requests)
    }

    /**
     * Maps Firestore documents into incoming friend requests.
     */
    private fun mapIncomingFriendRequests(snapshot: QuerySnapshot): List<FriendRequest> {
        return snapshot.documents
            .mapNotNull { it.toObject(FriendRequest::class.java) }
            .sortedBy { incomingRequestDisplayName(it).lowercase() }
    }

    /**
     * Returns the best display name for an incoming friend request.
     */
    private fun incomingRequestDisplayName(request: FriendRequest): String {
        return request.fromDisplayName.ifBlank { request.fromEmail }
    }

    /**
     * Returns the error message shown when incoming requests fail to load.
     */
    private fun incomingRequestsErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not load incoming friend requests."
    }

    /**
     * Accepts a pending friend request by updating its status.
     */
    fun acceptFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateFriendRequestStatus(
            requestId = request.id,
            newStatus = "accepted",
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Declines a pending friend request by updating its status.
     */
    fun declineFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateFriendRequestStatus(
            requestId = request.id,
            newStatus = "declined",
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Updates the status field for one friend request document.
     */
    private fun updateFriendRequestStatus(
        requestId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(updateFriendRequestStatusErrorMessage(exception))
            }
    }

    /**
     * Returns the error message shown when a friend request status update fails.
     */
    private fun updateFriendRequestStatusErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not update the friend request."
    }

    /**
     * Loads all outgoing friend-request statuses for the current user.
     */
    fun fetchOutgoingFriendRequestStatuses(
        currentUserId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        buildOutgoingFriendRequestsQuery(currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                handleOutgoingStatusesSuccess(snapshot, onSuccess)
            }
            .addOnFailureListener { exception ->
                onError(outgoingStatusesErrorMessage(exception))
            }
    }

    /**
     * Builds the Firestore query used to load outgoing friend-request statuses.
     */
    private fun buildOutgoingFriendRequestsQuery(currentUserId: String) =
        friendRequestsCollection()
            .whereEqualTo("fromUserId", currentUserId)

    /**
     * Converts the outgoing requests snapshot into a target-user status map.
     */
    private fun handleOutgoingStatusesSuccess(
        snapshot: QuerySnapshot,
        onSuccess: (Map<String, String>) -> Unit
    ) {
        val statuses = mapOutgoingStatuses(snapshot)
        onSuccess(statuses)
    }

    /**
     * Maps outgoing request documents into a target-user to status map.
     */
    private fun mapOutgoingStatuses(snapshot: QuerySnapshot): Map<String, String> {
        return snapshot.documents
            .mapNotNull { it.toObject(FriendRequest::class.java) }
            .filter { it.toUserId.isNotBlank() }
            .associate { it.toUserId to it.status }
    }

    /**
     * Returns the error message shown when outgoing statuses fail to load.
     */
    private fun outgoingStatusesErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not load friend request statuses."
    }

    /**
     * Counts accepted friendships for the current user.
     */
    fun fetchFriendCount(
        currentUserId: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(countAcceptedFriends(snapshot, currentUserId))
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load friend count.")
            }
    }

    /**
     * Counts unique accepted friends for the current user.
     */
    private fun countAcceptedFriends(
        snapshot: QuerySnapshot,
        currentUserId: String
    ): Int {
        val friendIds = mutableSetOf<String>()

        snapshot.documents
            .mapNotNull { it.toObject(FriendRequest::class.java) }
            .forEach { request ->
                when (currentUserId) {
                    request.fromUserId -> friendIds.add(request.toUserId)
                    request.toUserId -> friendIds.add(request.fromUserId)
                }
            }

        return friendIds.size
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