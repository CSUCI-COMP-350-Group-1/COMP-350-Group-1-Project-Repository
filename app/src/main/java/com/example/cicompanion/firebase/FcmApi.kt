package com.example.cicompanion.firebase

import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {

    @POST("/send")
    suspend fun sendMessage(
        @Body body: SendMessageDto
    )

    @POST("/broadcast")
    suspend fun broadcast(
        @Body body: SendMessageDto
    )

    // PUSH NOTIFICATIONS CHANGE:
    @POST("/send-friend-request")
    suspend fun sendFriendRequestPush(
        @Body body: Map<String, String>
    )
    // MESSAGING: minimal backend endpoint for newmessage notifications
    @POST("/send-direct-message")
    suspend fun sendDirectMessagePush(
        @Body body: Map<String, String>
    )
}