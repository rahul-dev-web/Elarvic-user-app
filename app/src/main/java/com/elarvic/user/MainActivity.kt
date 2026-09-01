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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.firebase_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, options)
        setContent { MaterialTheme { ElarvicApp(auth, googleClient, db) } }
    }

    @Deprecated("Use Activity Result APIs in a later cleanup")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_GOOGLE || resultCode != Activity.RESULT_OK) return
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
    }

    companion object { const val RC_GOOGLE = 9001 }
}

@Composable
private fun ElarvicApp(auth: FirebaseAuth, googleClient: GoogleSignInClient, db: FirebaseFirestore) {
    val user = auth.currentUser
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Elarvic V1", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(if (user == null) "Sign in to continue" else "Welcome, ${user.displayName ?: "User"}")
        Spacer(Modifier.height(24.dp))

        if (user == null) {
            Button(
                enabled = !loading,
                onClick = {
                    loading = true
                    // Activity Result integration will launch the Google intent from MainActivity.
                    message = "Google sign-in is ready. Configure Firebase before testing."
                },
                modifier = Modifier.fillMaxWidth()
            ) { if (loading) CircularProgressIndicator() else Text("Continue with Google") }
        } else {
            Button(onClick = { auth.signOut(); googleClient.signOut() }, Modifier.fillMaxWidth()) { Text("Logout") }
        }
        message?.let { Text(it, Modifier.padding(top = 16.dp)) }
    }
}
