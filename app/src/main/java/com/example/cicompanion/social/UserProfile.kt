package com.example.cicompanion.social

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val originalDisplayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val lastSignInAt: Long = System.currentTimeMillis()
)