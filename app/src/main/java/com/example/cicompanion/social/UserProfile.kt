package com.example.cicompanion.social

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val lastSignInAt: Long = System.currentTimeMillis()
)