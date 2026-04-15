package com.example.cicompanion.social

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ListenerRegistration

object SocialRepository {

    // Real-time listener used by the notification observer.
    fun observePendingIncomingFriendRequests(
        currentUserId: String,
        onSnapshot: (QuerySnapshot) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return buildIncomingFriendRequestsQuery(currentUserId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception.message ?: "Could not observe incoming friend requests.")
                    return@addSnapshotListener
                }

                snapshot?.let(onSnapshot)
            }
    }

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

    private fun handleSearchableUsersSuccess(
        snapshot: QuerySnapshot,
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit
    ) {
        val users = mapSearchableUsers(snapshot, currentUserId)
        onSuccess(users)
    }

    private fun mapSearchableUsers(
        snapshot: QuerySnapshot,
        currentUserId: String
    ): List<UserProfile> {
        return snapshot.documents
            .mapNotNull { it.toObject(UserProfile::class.java) }
            .filter { it.uid.isNotBlank() && it.uid != currentUserId }
            .sortedBy { displayNameOrEmail(it).lowercase() }
    }

    private fun searchableUsersErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not load users."
    }

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

    private fun createFriendRequestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_$toUserId"
    }

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

    private fun existingRequestErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not verify the existing friend request."
    }

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

    private fun sendFriendRequestErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not send the friend request."
    }

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

    private fun buildIncomingFriendRequestsQuery(currentUserId: String) =
        friendRequestsCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")

    private fun handleIncomingRequestsSuccess(
        snapshot: QuerySnapshot,
        onSuccess: (List<FriendRequest>) -> Unit
    ) {
        val requests = mapIncomingFriendRequests(snapshot)
        onSuccess(requests)
    }

    private fun mapIncomingFriendRequests(snapshot: QuerySnapshot): List<FriendRequest> {
        return snapshot.documents
            .mapNotNull { it.toObject(FriendRequest::class.java) }
            .sortedBy { incomingRequestDisplayName(it).lowercase() }
    }

    private fun incomingRequestDisplayName(request: FriendRequest): String {
        return request.fromDisplayName.ifBlank { request.fromEmail }
    }

    private fun incomingRequestsErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not load incoming friend requests."
    }

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

    fun declineFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(request.id)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not decline (delete) the friend request.")
            }
    }

    fun removeFriend(
        currentUserId: String,
        targetUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = friendRequestsCollection()

        // 1. Check requests sent BY me
        collection.whereEqualTo("fromUserId", currentUserId).get()
            .addOnSuccessListener { outgoingSnapshot ->
                val outgoingDoc = outgoingSnapshot.documents.find { it.getString("toUserId") == targetUserId }
                
                if (outgoingDoc != null) {
                    outgoingDoc.reference.delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError("Permission Denied: Only the receiver can remove this friend. Please check your Firestore Security Rules.") }
                } else {
                    // 2. Check requests sent TO me
                    collection.whereEqualTo("toUserId", currentUserId).get()
                        .addOnSuccessListener { incomingSnapshot ->
                            val incomingDoc = incomingSnapshot.documents.find { it.getString("fromUserId") == targetUserId }
                            
                            if (incomingDoc != null) {
                                incomingDoc.reference.delete()
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { onError("Permission Denied: Could not delete incoming friendship. Check Firestore Rules.") }
                            } else {
                                onError("No friendship found to remove.")
                            }
                        }
                        .addOnFailureListener { onError("Error searching incoming requests: ${it.message}") }
                }
            }
            .addOnFailureListener { onError("Error searching outgoing requests: ${it.message}") }
    }

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

    private fun updateFriendRequestStatusErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not update the friend request."
    }

    fun fetchAllFriendRequestStatuses(
        currentUserId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val allStatuses = mutableMapOf<String, String>()

        friendRequestsCollection()
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .addOnSuccessListener { outgoingSnapshot ->
                outgoingSnapshot.documents.forEach { doc ->
                    val request = doc.toObject(FriendRequest::class.java)
                    if (request != null && request.toUserId.isNotBlank()) {
                        allStatuses[request.toUserId] = request.status
                    }
                }

                friendRequestsCollection()
                    .whereEqualTo("toUserId", currentUserId)
                    .get()
                    .addOnSuccessListener { incomingSnapshot ->
                        incomingSnapshot.documents.forEach { doc ->
                            val request = doc.toObject(FriendRequest::class.java)
                            if (request != null && request.fromUserId.isNotBlank()) {
                                val existingStatus = allStatuses[request.fromUserId]
                                // Accepted status takes priority
                                if (existingStatus != "accepted") {
                                    allStatuses[request.fromUserId] = request.status
                                }
                            }
                        }
                        onSuccess(allStatuses)
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.message ?: "Could not load incoming friend request statuses.")
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load outgoing friend request statuses.")
            }
    }

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

    fun displayNameOrEmail(user: UserProfile): String {
        return user.displayName.ifBlank { user.email }
    }

    private fun usersCollection() = FirebaseFirestore.getInstance().collection("users")

    private fun friendRequestsCollection() =
        FirebaseFirestore.getInstance().collection("friend_requests")
}
