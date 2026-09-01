package com.elarvic.user

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.firebase_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, options)
        setContent {
            MaterialTheme {
                ElarvicApp(
                    signedIn = auth.currentUser != null,
                    onGoogleLogin = { startActivityForResult(googleClient.signInIntent, RC_GOOGLE) },
                    onLogout = { auth.signOut(); googleClient.signOut() }
                )
            }
        }
    }

    @Deprecated("Use Activity Result APIs in a later cleanup")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_GOOGLE || resultCode != Activity.RESULT_OK) return
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
    }

    companion object { const val RC_GOOGLE = 9001 }
}

@Composable
private fun ElarvicApp(signedIn: Boolean, onGoogleLogin: () -> Unit, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Elarvic V1", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(if (signedIn) "Signed in successfully" else "Sign in to continue")
        Spacer(Modifier.height(24.dp))
        if (signedIn) {
            Button(onClick = onLogout, Modifier.fillMaxWidth()) { Text("Logout") }
        } else {
            Button(onClick = onGoogleLogin, Modifier.fillMaxWidth()) { Text("Continue with Google") }
        }
    }
}
