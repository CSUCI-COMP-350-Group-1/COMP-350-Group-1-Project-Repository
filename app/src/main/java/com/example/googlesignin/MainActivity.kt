package com.example.googlesignin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.googlesignin.ui.theme.GoogleSignInTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException

private lateinit var googleSignInClient: GoogleSignInClient
private val RC_SIGN_IN = 100 // any number you choose

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID_HERE") // replace with Web OAuth client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        enableEdgeToEdge()
        setContent {
            GoogleSignInTheme {
                androidx.compose.material3.Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    val context = LocalContext.current

                    // Google Sign-In Button
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = {
                            com.google.android.gms.common.SignInButton(context).apply {
                                setSize(com.google.android.gms.common.SignInButton.SIZE_WIDE)
                                setOnClickListener {
                                    val signInIntent = googleSignInClient.signInIntent
                                    startActivityForResult(signInIntent, RC_SIGN_IN)
                                }
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )

                    // Optional greeting text below
                    Greeting(name = "Android", modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GoogleSignInTheme {
        Greeting("Android")
    }
}