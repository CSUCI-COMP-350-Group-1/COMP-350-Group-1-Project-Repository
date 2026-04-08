package com.example.cicompanion.firebase

import android.content.Context
import com.example.cicompanion.social.FirestoreManager
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object FirebaseAuthManager {

    /**
     * Builds and returns the Google sign-in client used by the profile screen.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("326604100597-q15krhlglv2mha9jv01mh3au2sadiie7.apps.googleusercontent.com")
            .requestEmail()
            .build()

        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    /**
     * Signs the Google user into Firebase and saves the user profile to Firestore.
     */
    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser

                    user?.let { signedInUser ->
                        FirestoreManager.saveUserToFirestore(signedInUser)
                    }
                    println("Signed in as: ${user?.email}")
                } else {
                    println("Sign in failed")
                }
            }
    }
}
