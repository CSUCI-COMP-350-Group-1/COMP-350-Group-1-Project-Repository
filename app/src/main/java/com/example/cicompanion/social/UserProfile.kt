package com.example.cicompanion.social

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val major: String = "",
    val note: UserNote? = null,
    val lastSignInAt: Long = System.currentTimeMillis()
)