package com.example.cicompanion.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient

object FirebaseAuthManager {

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("326604100597-q15krhlglv2mha9jv01mh3au2sadiie7.apps.googleusercontent.com")
            .requestEmail()
            .build()

        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener {

                if (it.isSuccessful) {

                    val user = FirebaseAuth.getInstance().currentUser

                    println("Signed in as: ${user?.email}")

                } else {

                    println("Sign in failed")

                }
            }
    }
}