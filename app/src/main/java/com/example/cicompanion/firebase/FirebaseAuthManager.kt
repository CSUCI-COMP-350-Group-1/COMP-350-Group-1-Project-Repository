package com.example.cicompanion.firebase

import android.content.Context
import android.util.Log
import com.example.cicompanion.social.FirestoreManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.cicompanion.firebase.FriendRequestNotificationSender

object FirebaseAuthManager {

    private const val TAG = "FirebaseAuthManager"

    /**
     * Builds and returns the Google Sign-In client used by the app.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("326604100597-q15krhlglv2mha9jv01mh3au2sadiie7.apps.googleusercontent.com")
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Signs the user into Firebase with the Google ID token.
     * If sign-in succeeds, the user's profile is also saved to Firestore.
     */
    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser

                    if (user != null) {
                        FirestoreManager.saveUserToFirestore(user) {
                            FriendRequestNotificationSender.syncCurrentUserFcmToken()
                        }

                        Log.d(TAG, "Signed in successfully as: ${user.email}")
                    } else {
                        Log.e(TAG, "Sign-in succeeded, but currentUser was null.")
                    }
                } else {
                    Log.e(TAG, "Firebase sign-in failed.", task.exception)
                }
            }
    }
}