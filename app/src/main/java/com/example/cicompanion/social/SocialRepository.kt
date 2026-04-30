package com.example.cicompanion.social

import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object SocialRepository {

    // MESSAGING: reusable helper for accepted friends only
    fun fetchAcceptedFriends(
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchAllFriendRequestStatuses(
            currentUserId = currentUserId,
            onSuccess = { statuses ->
                val friendIds = statuses.filter { it.value == "accepted" }.keys.toList()
                if (friendIds.isEmpty()) {
                    onSuccess(emptyList())
                } else {
                    fetchUsersByIdsParallel(friendIds, onSuccess, onError)
                }
            },
            onError = { onError("Friends Load Error: $it") }
        )
    }

    private fun fetchUsersByIdsParallel(
        userIds: List<String>,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        val allUsers = mutableListOf<UserProfile>()
        var completedCount = 0
        var hasError = false

        if (userIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        userIds.forEach { userId ->
            usersCollection().document(userId).get()
                .addOnSuccessListener { document ->
                    if (hasError) return@addOnSuccessListener
                    
                    val user = document.toObject(UserProfile::class.java)
                    if (user != null) {
                        allUsers.add(user)
                    }
                    
                    completedCount++
                    if (completedCount == userIds.size) {
                        onSuccess(allUsers.sortedBy { displayNameOrEmail(it).lowercase() })
                    }
                }
                .addOnFailureListener { exception ->
                    if (!hasError) {
                        hasError = true
                        onError("Profile Fetch Error: ${exception.message}")
                    }
                }
        }
    }

    fun fetchSearchableUsers(
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Warning: This lists the 'users' collection. 
        // If rules forbid listing, this will return PERMISSION_DENIED.
        usersCollection()
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents
                    .mapNotNull { it.toObject(UserProfile::class.java) }
                    .filter { it.uid.isNotBlank() && it.uid != currentUserId }
                    .sortedBy { displayNameOrEmail(it).lowercase() }
                onSuccess(users)
            }
            .addOnFailureListener { exception ->
                val msg = if (exception.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    "User Search Denied: Global listing is disabled. Try searching by exact name/email."
                } else {
                    exception.message ?: "Could not load users."
                }
                onError(msg)
            }
    }

    fun fetchUserProfile(
        userId: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserProfile::class.java)
                if (user != null) {
                    onSuccess(user)
                } else {
                    onError("User profile not found.")
                }
            }
            .addOnFailureListener { exception ->
                onError("Load Profile Error: ${exception.message}")
            }
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

        friendRequestsCollection().document(requestId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onError("You already sent a friend request to this user.")
                } else {
                    val request = FriendRequest(
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

                    friendRequestsCollection().document(requestId).set(request)
                        .addOnSuccessListener {
                            FriendRequestNotificationSender.sendFriendRequestNotification(
                                targetUserId = targetUser.uid,
                                senderDisplayName = currentUser.displayName ?: currentUser.email ?: "Someone"
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { onError("Send Request Error: ${it.message}") }
                }
            }
            .addOnFailureListener { onError("Check Request Error: ${it.message}") }
    }

    fun fetchIncomingFriendRequests(
        currentUserId: String,
        onSuccess: (List<FriendRequest>) -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.toObject(FriendRequest::class.java) })
            }
            .addOnFailureListener { exception ->
                onError("Incoming Requests Error: ${exception.message}")
            }
    }

    fun fetchOutgoingFriendRequests(
        currentUserId: String,
        onSuccess: (List<FriendRequest>) -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .whereEqualTo("fromUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.toObject(FriendRequest::class.java) })
            }
            .addOnFailureListener { exception ->
                onError("Outgoing Requests Error: ${exception.message}")
            }
    }

    fun acceptFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        acceptFriendRequestById(request.id, onSuccess, onError)
    }

    fun acceptFriendRequestById(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(requestId)
            .update("status", "accepted")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Accept Request Error: ${it.message}") }
    }

    fun declineFriendRequest(
        request: FriendRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(request.id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Decline Request Error: ${it.message}") }
    }

    fun removeFriend(
        currentUserId: String,
        targetUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val collection = friendRequestsCollection()

        collection.whereEqualTo("fromUserId", currentUserId).get()
            .addOnSuccessListener { outgoingSnapshot ->
                val outgoingDoc = outgoingSnapshot.documents.find { it.getString("toUserId") == targetUserId }

                if (outgoingDoc != null) {
                    outgoingDoc.reference.delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError("Remove Friend Error: ${it.message}") }
                } else {
                    collection.whereEqualTo("toUserId", currentUserId).get()
                        .addOnSuccessListener { incomingSnapshot ->
                            val incomingDoc = incomingSnapshot.documents.find { it.getString("fromUserId") == targetUserId }

                            if (incomingDoc != null) {
                                incomingDoc.reference.delete()
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { onError("Remove Friend Error: ${it.message}") }
                            } else {
                                onError("No friendship found to remove.")
                            }
                        }
                        .addOnFailureListener { onError("Find Incoming Error: ${it.message}") }
                }
            }
            .addOnFailureListener { onError("Find Outgoing Error: ${it.message}") }
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
                        allStatuses[request.toUserId] = if (request.status == "pending") "pending_sent" else request.status
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
                                if (existingStatus != "accepted") {
                                    allStatuses[request.fromUserId] = if (request.status == "pending") "pending_received" else request.status
                                }
                            }
                        }
                        onSuccess(allStatuses)
                    }
                    .addOnFailureListener { onError("Load Incoming Statuses Error: ${it.message}") }
            }
            .addOnFailureListener { onError("Load Outgoing Statuses Error: ${it.message}") }
    }

    fun fetchFriendCount(
        currentUserId: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchAllFriendRequestStatuses(
            currentUserId = currentUserId,
            onSuccess = { statuses ->
                val count = statuses.values.count { it == "accepted" }
                onSuccess(count)
            },
            onError = { onError("Friend Count Error: $it") }
        )
    }

    fun createFriendRequestId(fromUserId: String, toUserId: String): String {
        return "${fromUserId}_${toUserId}"
    }

    fun setNickname(
        currentUserId: String,
        friendUid: String,
        nickname: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "friendUid" to friendUid,
            "nickname" to nickname
        )
        nicknamesCollection(currentUserId)
            .document(friendUid)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Save Nickname Error: ${it.message}") }
    }

    fun fetchNicknames(
        currentUserId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        nicknamesCollection(currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val nicknamesMap = snapshot.documents.associate { doc ->
                    val friendId = doc.getString("friendUid") ?: doc.id
                    val nickname = doc.getString("nickname") ?: ""
                    friendId to nickname
                }
                onSuccess(nicknamesMap)
            }
            .addOnFailureListener { onError("Load Nicknames Error: ${it.message}") }
    }

    fun displayNameOrEmail(user: UserProfile): String {
        return user.displayName.ifBlank { user.email }
    }

    private fun usersCollection() = FirebaseFirestore.getInstance().collection("users")

    private fun friendRequestsCollection() =
        FirebaseFirestore.getInstance().collection("friend_requests")

    private fun nicknamesCollection(currentUserId: String) =
        usersCollection().document(currentUserId).collection("nicknames")
}
