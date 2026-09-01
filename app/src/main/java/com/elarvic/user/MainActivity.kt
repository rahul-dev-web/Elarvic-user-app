package com.elarvic.user

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.elarvic.user.R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, options)

        setContent {
            MaterialTheme {
                ElarvicApp(
                    signedIn = auth.currentUser != null,
                    onGoogleLogin = { signInWithGoogle() },
                    onLogout = { auth.signOut(); googleClient.signOut() }
                )
            }
        }
    }

    private fun signInWithGoogle() {
        startActivityForResult(googleClient.signInIntent, RC_GOOGLE)
    }

    @Deprecated("Use Activity Result APIs in the next cleanup pass")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE && resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
        }
    }

    companion object { private const val RC_GOOGLE = 9001 }
}

@Composable
private fun ElarvicApp(signedIn: Boolean, onGoogleLogin: () -> Unit, onLogout: () -> Unit) {
    var loading by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Elarvic V1", style = MaterialTheme.typography.headlineLarge)
        Text("Welcome", modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        if (signedIn) {
            Text("You are signed in.")
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Logout") }
        } else {
            Button(onClick = { loading = true; onGoogleLogin() }, modifier = Modifier.fillMaxWidth()) {
                if (loading) CircularProgressIndicator() else Text("Continue with Google")
            }
        }
    }
}
