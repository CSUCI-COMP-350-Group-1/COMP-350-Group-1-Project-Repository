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
}