package com.example.cicompanion.social

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {
    var allUsers by mutableStateOf<List<UserProfile>>(emptyList())
        private set
    
    val requestStatuses = mutableStateMapOf<String, String>()
    
    var incomingRequests by mutableStateOf<List<FriendRequest>>(emptyList())
        private set

    var outgoingRequests by mutableStateOf<List<FriendRequest>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var currentLoadedUid: String? = null

    fun loadSocialData(forceRefresh: Boolean = false) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!forceRefresh && currentLoadedUid == user.uid) return

        currentLoadedUid = user.uid
        refreshAll()
    }

    fun refreshAll() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            isLoading = true
            SocialRepository.fetchSearchableUsers(
                currentUserId = user.uid,
                onSuccess = { users, _ -> allUsers = users },
                onError = { errorMessage = it }
            )

            SocialRepository.fetchAllFriendRequestStatuses(
                currentUserId = user.uid,
                onSuccess = { statuses ->
                    requestStatuses.clear()
                    requestStatuses.putAll(statuses)
                },
                onError = { errorMessage = it }
            )

            refreshRequests()
            isLoading = false
        }
    }

    fun refreshRequests() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        SocialRepository.fetchIncomingFriendRequests(
            currentUserId = user.uid,
            onSuccess = { incomingRequests = it },
            onError = { errorMessage = it }
        )
        SocialRepository.fetchOutgoingFriendRequests(
            currentUserId = user.uid,
            onSuccess = { outgoingRequests = it },
            onError = { errorMessage = it }
        )
    }

    fun sendRequest(targetUser: UserProfile, onSuccess: () -> Unit = {}) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        SocialRepository.sendFriendRequest(
            currentUser = currentUser,
            targetUser = targetUser,
            onSuccess = {
                requestStatuses[targetUser.uid] = "pending"
                refreshRequests()
                onSuccess()
            },
            onError = { errorMessage = it }
        )
    }

    fun acceptRequest(request: FriendRequest, onSuccess: () -> Unit = {}) {
        SocialRepository.acceptFriendRequest(
            request = request,
            onSuccess = {
                requestStatuses[request.fromUserId] = "accepted"
                refreshAll()
                onSuccess()
            },
            onError = { errorMessage = it }
        )
    }

    fun declineOrCancelRequest(request: FriendRequest, onSuccess: () -> Unit = {}) {
        SocialRepository.declineFriendRequest(
            request = request,
            onSuccess = {
                requestStatuses.remove(request.fromUserId)
                requestStatuses.remove(request.toUserId)
                refreshAll()
                onSuccess()
            },
            onError = { errorMessage = it }
        )
    }

    fun unfriend(friend: UserProfile, onSuccess: () -> Unit = {}) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        SocialRepository.removeFriend(
            currentUserId = user.uid,
            targetUserId = friend.uid,
            onSuccess = {
                requestStatuses.remove(friend.uid)
                refreshAll()
                onSuccess()
            },
            onError = { errorMessage = it }
        )
    }
    
    fun clearError() { errorMessage = null }
}
