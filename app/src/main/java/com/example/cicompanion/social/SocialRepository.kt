package com.example.cicompanion.social

import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.example.cicompanion.maps.CustomPin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

object SocialRepository {

    // MESSAGING: reusable helper for accepted friends only
    fun fetchAcceptedFriends(
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().get()
            .addOnSuccessListener { snapshot ->
                val allUsers = snapshot.documents
                    .mapNotNull { it.toObject(UserProfile::class.java) }
                    .filter { it.uid.isNotBlank() && it.uid != currentUserId }

                fetchAllFriendRequestStatuses(
                    currentUserId = currentUserId,
                    onSuccess = { statuses ->
                        onSuccess(
                            allUsers.filter { statuses[it.uid] == "accepted" }
                        )
                    },
                    onError = onError
                )
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load friends.")
            }
    }

    fun fetchSearchableUsers(
        currentUserId: String,
        searchQuery: String = "",
        lastDocument: DocumentSnapshot? = null,
        onSuccess: (List<UserProfile>, DocumentSnapshot?) -> Unit,
        onError: (String) -> Unit
    ) {
        var query: Query = usersCollection()
            .orderBy("displayName")
            .limit(20)

        if (searchQuery.isNotBlank()) {
            query = query.whereGreaterThanOrEqualTo("displayName", searchQuery)
                .whereLessThanOrEqualTo("displayName", searchQuery + "\uf8ff")
        }

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents
                    .mapNotNull { it.toObject(UserProfile::class.java) }
                    .filter { it.uid.isNotBlank() && it.uid != currentUserId }

                val nextDoc = snapshot.documents.lastOrNull()
                onSuccess(users, nextDoc)
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load users.")
            }
    }

    /**
     * Fetches a specific user's profile data by their UID.
     */
    fun fetchUserProfile(
        userId: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isBlank()) {
            onError("Invalid user ID.")
            return
        }
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
                onError(exception.message ?: "Could not load user profile.")
            }
    }

    fun updateDisplayName(
        userId: String,
        newDisplayName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().document(userId)
            .update("displayName", newDisplayName)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to update display name") }
    }

    fun updateBio(
        userId: String,
        newBio: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().document(userId)
            .update("bio", newBio)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to update bio") }
    }

    fun updateMajor(
        userId: String,
        newMajor: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().document(userId)
            .update("major", newMajor)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to update major") }
    }

    fun updateUserNote(
        userId: String,
        note: UserNote?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        usersCollection().document(userId)
            .update("note", note)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to update status") }
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
                if (exists) {
                    onError("You already sent a friend request to this user.")
                } else {
                    val request = buildFriendRequest(currentUser, targetUser, requestId)
                    saveFriendRequest(request, currentUser, targetUser, onSuccess, onError)
                }
            },
            onError = onError
        )
    }

    fun createFriendRequestId(fromUserId: String, toUserId: String): String {
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
                onError(exception.message ?: "Could not verify the existing friend request.")
            }
    }

    private fun saveFriendRequest(
        request: FriendRequest,
        currentUser: FirebaseUser,
        targetUser: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        friendRequestsCollection()
            .document(request.id)
            .set(request)
            .addOnSuccessListener {
                FriendRequestNotificationSender.sendFriendRequestNotification(
                    targetUserId = targetUser.uid,
                    senderDisplayName = currentUser.displayName ?: currentUser.email ?: "Someone"
                )
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not send the friend request.")
            }
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
                onError(exception.message ?: "Could not load incoming friend requests.")
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
                onError(exception.message ?: "Could not load outgoing friend requests.")
            }
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
    
    fun acceptFriendRequestById(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateFriendRequestStatus(
            requestId = requestId,
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
                onError(exception.message ?: "Could not update the friend request.")
            }
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
                                // Accepted status takes priority
                                if (existingStatus != "accepted") {
                                    allStatuses[request.fromUserId] = if (request.status == "pending") "pending_received" else request.status
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

    fun fetchMutualFriends(
        currentUserId: String,
        targetUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchAcceptedFriendIds(currentUserId, onSuccess = { currentUserFriends ->
            fetchAcceptedFriendIds(targetUserId, onSuccess = { targetUserFriends ->
                val mutualIds = currentUserFriends.intersect(targetUserFriends)
                if (mutualIds.isEmpty()) {
                    onSuccess(emptyList())
                } else {
                    fetchUserProfiles(mutualIds.toList(), onSuccess, onError)
                }
            }, onError)
        }, onError)
    }

    private fun fetchAcceptedFriendIds(
        userId: String,
        onSuccess: (Set<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val friendIds = mutableSetOf<String>()

        friendRequestsCollection()
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val from = doc.getString("fromUserId") ?: ""
                    val to = doc.getString("toUserId") ?: ""
                    if (from == userId) friendIds.add(to)
                    else if (to == userId) friendIds.add(from)
                }
                onSuccess(friendIds)
            }
            .addOnFailureListener { onError(it.message ?: "Error fetching friends") }
    }

    private fun fetchUserProfiles(
        userIds: List<String>,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        // Firestore 'in' query supports up to 10 elements. If more, we'd need to chunk.
        // For mutual friends preview, 10 is usually enough or we can chunk.
        val chunks = userIds.chunked(10)
        val allProfiles = mutableListOf<UserProfile>()
        var chunksLoaded = 0

        chunks.forEach { chunk ->
            usersCollection()
                .whereIn("uid", chunk)
                .get()
                .addOnSuccessListener { snapshot ->
                    allProfiles.addAll(snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) })
                    chunksLoaded++
                    if (chunksLoaded == chunks.size) {
                        onSuccess(allProfiles)
                    }
                }
                .addOnFailureListener { onError(it.message ?: "Error fetching profiles") }
        }
    }

    fun displayNameOrEmail(user: UserProfile): String {
        return user.displayName.ifBlank { user.email }}
    fun fetchNicknames(
        currentUserId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        nicknamesCollection(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.documents.associate {
                    it.id to (it.getString("nickname") ?: "")
                }
                onSuccess(map)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Could not load nicknames.")
            }
    }

    fun setNickname(
        currentUserId: String,
        friendUid: String,
        nickname: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nickname.isBlank()) {
            nicknamesCollection(currentUserId).document(friendUid).delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.message ?: "Could not remove nickname.") }
        } else {
            nicknamesCollection(currentUserId).document(friendUid)
                .set(mapOf("nickname" to nickname))
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.message ?: "Could not set nickname.") }
        }
    }

    fun fetchIncomingEventInvites(
        currentUserId: String,
        onSuccess: (List<EventInvite>) -> Unit,
        onError: (String) -> Unit
    ) {
        eventInvitesCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.toObject(EventInvite::class.java) })
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Could not load event invites.")
            }
    }

    fun fetchEventInvite(
        inviteId: String,
        onSuccess: (EventInvite?) -> Unit,
        onError: (String) -> Unit
    ) {
        eventInvitesCollection().document(inviteId).get()
            .addOnSuccessListener { doc -> onSuccess(doc.toObject(EventInvite::class.java)) }
            .addOnFailureListener { onError(it.message ?: "Could not fetch event invite.") }
    }

    fun sendEventInvite(
        currentUser: FirebaseUser,
        targetUserId: String,
        event: CalendarEvent,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val inviteId = "${currentUser.uid}_${targetUserId}_${event.id}"
        val invite = EventInvite(
            id = inviteId,
            fromUserId = currentUser.uid,
            toUserId = targetUserId,
            fromDisplayName = currentUser.displayName ?: currentUser.email ?: "Someone",
            eventId = event.id,
            eventTitle = event.title,
            eventDescription = event.description,
            eventLocation = event.location,
            eventStart = event.start.toString(),
            eventEnd = event.endExclusive.toString(),
            isAllDay = event.isAllDay,
            isPinnedByLeader = event.isPinnedByLeader,
            status = "pending",
            sentAt = System.currentTimeMillis()
        )

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        
        val inviteRef = eventInvitesCollection().document(inviteId)
        batch.set(inviteRef, invite)

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not send event invite.") }
    }

    fun listenToIncomingEventInvites(
        currentUserId: String,
        onInvitesChanged: (List<EventInvite>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return eventInvitesCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to event invites.")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onInvitesChanged(emptyList())
                    return@addSnapshotListener
                }
                onInvitesChanged(snapshot.documents.mapNotNull { it.toObject(EventInvite::class.java) })
            }
    }

    /**
     * Real-time listener for accepted event invites of the current user.
     */
    fun listenToAcceptedEventInvites(
        currentUserId: String,
        onInvitesChanged: (List<EventInvite>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return eventInvitesCollection()
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to accepted invites.")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onInvitesChanged(emptyList())
                    return@addSnapshotListener
                }
                onInvitesChanged(snapshot.documents.mapNotNull { it.toObject(EventInvite::class.java) })
            }
    }

    fun acceptEventInvite(
        invite: EventInvite,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val inviteRef = eventInvitesCollection().document(invite.id)
        
        db.runTransaction { transaction ->
            transaction.update(inviteRef, "status", "accepted")
            
            // Also save to receiver's custom events
            val eventData = hashMapOf(
                "id" to invite.eventId,
                "title" to invite.eventTitle,
                "description" to (invite.eventDescription ?: ""),
                "location" to (invite.eventLocation ?: ""),
                "start" to invite.eventStart,
                "end" to invite.eventEnd,
                "isAllDay" to invite.isAllDay,
                "calendarId" to "custom",
                "isPinned" to false,
                "isBookmarked" to false,
                "ownerId" to invite.fromUserId,
                "isPinnedByLeader" to invite.isPinnedByLeader
            )
            
            val eventRef = db.collection("users").document(invite.toUserId)
                .collection("customEvents").document(invite.eventId)
            transaction.set(eventRef, eventData)
        }.addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it.message ?: "Could not accept event invite.") }
    }

    fun declineEventInvite(
        invite: EventInvite,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        eventInvitesCollection().document(invite.id).update("status", "declined")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Could not decline invite.") }
    }

    /**
     * Real-time listener for event member IDs.
     */
    fun listenToEventMemberIds(
        eventId: String,
        onIdsChanged: (List<String>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return eventInvitesCollection()
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to members.")
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                onIdsChanged(ids)
            }
    }

    /**
     * Fetches all user IDs who have an invite (accepted or pending) for a specific event sent by the current user.
     */
    fun fetchInvitedUserIds(
        eventId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            onError("User not authenticated")
            return
        }
        eventInvitesCollection()
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot?.documents?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                onSuccess(ids)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching invited users.")
            }
    }

    /**
     * Real-time listener for event members (Full Profiles)
     */
    fun listenToEventMembers(
        eventId: String,
        onMembersChanged: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return eventInvitesCollection()
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to members.")
                    return@addSnapshotListener
                }
                
                val memberIds = snapshot?.documents?.mapNotNull { it.getString("toUserId") } ?: emptyList()
                
                if (memberIds.isEmpty()) {
                    onMembersChanged(emptyList())
                    return@addSnapshotListener
                }

                val profiles = mutableListOf<UserProfile>()
                var loadedCount = 0
                memberIds.forEach { uid ->
                    fetchUserProfile(uid, 
                        onSuccess = { 
                            profiles.add(it)
                            if (++loadedCount == memberIds.size) onMembersChanged(profiles) 
                        },
                        onError = { 
                            if (++loadedCount == memberIds.size) onMembersChanged(profiles) 
                        }
                    )
                }
            }
    }

    /**
     * Real-time listener for custom events
     */
    fun listenToCustomEvents(
        userId: String,
        onEventsChanged: (List<CalendarEvent>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("customEvents")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to custom events.")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onEventsChanged(emptyList())
                    return@addSnapshotListener
                }
                
                val events = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: ""
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description")
                    val location = doc.getString("location")
                    val startStr = doc.getString("start") ?: return@mapNotNull null
                    val endStr = doc.getString("end") ?: return@mapNotNull null
                    val isAllDay = doc.getBoolean("isAllDay") ?: false
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    val ownerId = doc.getString("ownerId")
                    val maxMembers = doc.getLong("maxMembers")?.toInt()
                    val isPinnedByLeader = doc.getBoolean("isPinnedByLeader") ?: false
                    val isBookmarked = doc.getBoolean("isBookmarked") ?: false
                    val calendarId = doc.getString("calendarId") ?: "custom"

                    CalendarEvent(
                        id = id,
                        calendarId = calendarId,
                        title = title,
                        description = description,
                        location = location,
                        htmlLink = null,
                        start = ZonedDateTime.parse(startStr),
                        endExclusive = ZonedDateTime.parse(endStr),
                        isAllDay = isAllDay,
                        isPinned = isPinned,
                        ownerId = ownerId,
                        maxMembers = maxMembers,
                        isPinnedByLeader = isPinnedByLeader,
                        isBookmarked = isBookmarked,
                        isShared = false // Logic in ViewModel determines visibility based on ownerId
                    )
                }
                onEventsChanged(events)
            }
    }

    /**
     * Real-time listener for custom pins
     */
    fun listenToCustomPins(
        userId: String,
        onPinsChanged: (List<CustomPin>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("customPins")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Error listening to custom pins.")
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onPinsChanged(emptyList())
                    return@addSnapshotListener
                }
                
                val pins = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CustomPin::class.java)
                }
                onPinsChanged(pins)
            }
    }
    
    fun leaveEvent(
        userId: String,
        eventId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val eventRef = db.collection("users").document(userId).collection("customEvents").document(eventId)
        
        eventInvitesCollection()
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                batch.delete(eventRef)
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Failed to leave event.") }
            }
    }

    fun kickFromEvent(
        ownerId: String,
        targetUserId: String,
        eventId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val inviteId = "${ownerId}_${targetUserId}_${eventId}"
        val inviteRef = eventInvitesCollection().document(inviteId)
        
        // Only delete the invite. We cannot delete documents in other users' collections.
        inviteRef.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to kick user.") }
    }

    fun kickMultipleFromEvent(
        ownerId: String,
        targetUserIds: List<String>,
        eventId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        targetUserIds.forEach { targetUserId ->
            val inviteId = "${ownerId}_${targetUserId}_${eventId}"
            val inviteRef = eventInvitesCollection().document(inviteId)
            batch.delete(inviteRef)
        }
        batch.commit().addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to kick users.") }
    }

    fun deleteEventForAll(
        ownerId: String,
        eventId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        
        eventInvitesCollection()
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("fromUserId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                
                // Delete owner's copy
                val ownerEventRef = db.collection("users").document(ownerId)
                    .collection("customEvents").document(eventId)
                batch.delete(ownerEventRef)
                
                // Delete all invites (makes the event disappear for members via VM filtering)
                snapshot.documents.forEach { batch.delete(it.reference) }
                
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Failed to delete event globally.") }
            }
            .addOnFailureListener { onError(it.message ?: "Failed to find invites for deletion.") }
    }


    private fun usersCollection() = FirebaseFirestore.getInstance().collection("users")

    private fun friendRequestsCollection() =
        FirebaseFirestore.getInstance().collection("friend_requests")

    private fun nicknamesCollection(currentUserId: String) =
        usersCollection().document(currentUserId).collection("nicknames")

    private fun eventInvitesCollection() =
        FirebaseFirestore.getInstance().collection("event_invites")
}
