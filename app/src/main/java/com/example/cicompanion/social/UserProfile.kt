package com.example.cicompanion.social

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val lastSignIn: Long = System.currentTimeMillis()
)