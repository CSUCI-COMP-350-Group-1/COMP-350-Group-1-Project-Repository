package com.example.cicompanion.ui

object Routes {
    const val HOME = "home"
    const val SOCIAL = "social" 
    const val MAP = "map"
    const val CALENDAR = "calendar"
    const val STUDY_ROOM = "studyRoom"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val USER_SEARCH = "user_search"
    const val FRIEND_REQUESTS = "friendRequests"
    const val SEARCH = "search"
    const val FRIENDS_AND_REQUESTS = "friends_and_requests"

    // MESSAGING: new thread route
    const val MESSAGE_THREAD_BASE = "message_thread"
    const val MESSAGE_THREAD = "$MESSAGE_THREAD_BASE/{conversationId}/{friendUserId}?initialMessage={initialMessage}"

    // MESSAGING: helper to build a concrete route
    fun messageThread(conversationId: String, friendUserId: String, initialMessage: String? = null): String {
        val base = "$MESSAGE_THREAD_BASE/$conversationId/$friendUserId"
        return if (initialMessage != null) "$base?initialMessage=$initialMessage" else base
    }

    // MAP: helper to build map route with location
    fun mapWithLocation(
        lat: Double, 
        lng: Double, 
        tempName: String? = null, 
        tempDesc: String? = null, 
        tempColor: Int? = null,
        tempEventId: String? = null
    ): String {
        var route = "$MAP?lat=$lat&lng=$lng"
        if (tempName != null) route += "&tempName=$tempName"
        if (tempDesc != null) route += "&tempDesc=$tempDesc"
        if (tempColor != null) route += "&tempColor=$tempColor"
        if (tempEventId != null) route += "&tempEventId=$tempEventId"
        return route
    }
}
