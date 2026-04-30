package com.example.cicompanion.social

import com.example.cicompanion.calendar.model.CalendarEvent
import com.example.cicompanion.firebase.FriendRequestNotificationSender
import com.example.cicompanion.maps.CustomPin
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

object SocialRepository {

    // MESSAGING: reusable helper for accepted friends only
    fun fetchAcceptedFriends(
        currentUserId: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchSearchableUsers(
            currentUserId = currentUserId,
            onSuccess = { allUsers ->
                fetchAllFriendRequestStatuses(
                    currentUserId = currentUserId,
                    onSuccess = { statuses ->
                        onSuccess(
                            allUsers.filter { statuses[it.uid] == "accepted" }
                        )
                    },
                    onError = onError
                )
            },
            onError = onError
        )
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
        saveFriendRequest(
            request = request,
            currentUser = currentUser,
            targetUser = targetUser,
            onSuccess = onSuccess,
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
                onError(existingRequestErrorMessage(exception))
            }
    }

    private fun existingRequestErrorMessage(exception: Exception): String {
        return exception.message ?: "Could not verify the existing friend request."
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

    fun displayNameOrEmail(user: UserProfile): String {
        return user.displayName.ifBlank { user.email }
    }

    // --- EVENT INVITES ---

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
            fromDisplayName = currentUser.displayName ?: currentUser.email ?: "Friend",
            eventId = event.id,
            eventTitle = event.title,
            eventDescription = event.description,
            eventLocation = event.location,
            eventStart = event.start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            eventEnd = event.endExclusive.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            isAllDay = event.isAllDay,
            isPinnedByLeader = event.isPinned,
            status = "pending",
            sentAt = System.currentTimeMillis()
        )

        eventInvitesCollection().document(inviteId).set(invite)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not send event invite.") }
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
                onError(exception.message ?: "Could not fetch event invites.") }
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
        eventInvitesCollection().document(invite.id).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Could not decline event invite.") }
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
     * Fetches all user IDs who have an invite (accepted or pending) for a specific event.
     */
    fun fetchInvitedUserIds(
        eventId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        eventInvitesCollection()
            .whereEqualTo("eventId", eventId)
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

                    CalendarEvent(
                        id = id,
                        calendarId = "custom",
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
                        isPinnedByLeader = isPinnedByLeader
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
                db.runBatch { batch ->
                    batch.delete(eventRef)
                    snapshot.documents.forEach { batch.delete(it.reference) }
                }.addOnSuccessListener { onSuccess() }
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
        val db = FirebaseFirestore.getInstance()
        val inviteId = "${ownerId}_${targetUserId}_${eventId}"
        val inviteRef = eventInvitesCollection().document(inviteId)
        val eventRef = db.collection("users").document(targetUserId).collection("customEvents").document(eventId)
        
        db.runBatch { batch ->
            batch.delete(inviteRef)
            batch.delete(eventRef)
        }.addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it.message ?: "Failed to kick user.") }
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
            .get()
            .addOnSuccessListener { snapshot ->
                db.runTransaction { transaction ->
                    transaction.delete(db.collection("users").document(ownerId).collection("customEvents").document(eventId))
                    
                    snapshot.documents.forEach { doc ->
                        val memberId = doc.getString("toUserId")
                        if (memberId != null) {
                            transaction.delete(db.collection("users").document(memberId).collection("customEvents").document(eventId))
                        }
                        transaction.delete(doc.reference)
                    }
                }.addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Failed to delete event globally.") }
            }
    }

    private fun usersCollection() = FirebaseFirestore.getInstance().collection("users")

    private fun friendRequestsCollection() =
        FirebaseFirestore.getInstance().collection("friend_requests")

    private fun eventInvitesCollection() =
        FirebaseFirestore.getInstance().collection("event_invites")
}
