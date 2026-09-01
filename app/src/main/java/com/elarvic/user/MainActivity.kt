package com.elarvic.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

private const val WHATSAPP_LINK = "https://whatsapp.com/channel/0029VbDUColKQuJI4D5IVA2L"
private const val PREFS = "elarvic_user"
private const val KEY_VALUE = "access_key"
private const val WHATSAPP_SEEN = "whatsapp_gate_seen"

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            MaterialTheme {
                ElarvicApp(
                    savedKey = prefs.getString(KEY_VALUE, null),
                    whatsappSeen = prefs.getBoolean(WHATSAPP_SEEN, false),
                    onLogin = ::validateKey,
                    onOpenWhatsApp = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_LINK)))
                        prefs.edit().putBoolean(WHATSAPP_SEEN, true).apply()
                    },
                    onLogout = {
                        auth.signOut()
                        prefs.edit().remove(KEY_VALUE).remove(WHATSAPP_SEEN).apply()
                    }
                )
            }
        }
    }

    private fun validateKey(keyInput: String, onResult: (Boolean, String) -> Unit) {
        val key = keyInput.trim()
        if (key.isBlank()) {
            onResult(false, "Enter your Elarvic key.")
            return
        }
        auth.signInAnonymously().addOnSuccessListener {
            db.collection("keys").document(key).get()
                .addOnSuccessListener { doc ->
                    val active = doc.getBoolean("active") ?: false
                    val expiresAt = doc.getTimestamp("expiresAt")?.toDate()
                    val valid = doc.exists() && active && expiresAt != null && expiresAt.after(Date())
                    if (!valid) {
                        onResult(false, "Invalid, inactive or expired key.")
                        return@addOnSuccessListener
                    }
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_VALUE, key).apply()
                    onResult(true, "Login successful")
                }
                .addOnFailureListener { onResult(false, it.message ?: "Unable to verify key.") }
        }.addOnFailureListener { onResult(false, it.message ?: "Unable to connect to authentication service.") }
    }
}

@Composable
private fun ElarvicApp(
    savedKey: String?,
    whatsappSeen: Boolean,
    onLogin: (String, (Boolean, String) -> Unit) -> Unit,
    onOpenWhatsApp: () -> Unit,
    onLogout: () -> Unit
) {
    var loggedIn by remember { mutableStateOf(false) }
    var showWhatsApp by remember { mutableStateOf(false) }
    var initializing by remember { mutableStateOf(savedKey != null) }
    var key by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedKey) {
        if (savedKey == null) {
            initializing = false
            return@LaunchedEffect
        }
        onLogin(savedKey) { success, message ->
            initializing = false
            loggedIn = success
            showWhatsApp = success && !whatsappSeen
            if (!success) {
                error = message
                onLogout()
            }
        }
    }

    if (initializing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when {
        !loggedIn -> LoginScreen(
            key = key,
            onKeyChange = { key = it; error = null },
            loading = loading,
            error = error,
            onLogin = {
                loading = true
                onLogin(key) { success, message ->
                    loading = false
                    if (success) {
                        loggedIn = true
                        showWhatsApp = true
                    } else error = message
                }
            }
        )
        showWhatsApp -> WhatsAppGate(onOpenWhatsApp = {
            onOpenWhatsApp()
            showWhatsApp = false
        })
        else -> Dashboard(onLogout = {
            loggedIn = false
            showWhatsApp = false
            key = ""
            onLogout()
        })
    }
}

@Composable
private fun LoginScreen(key: String, onKeyChange: (String) -> Unit, loading: Boolean, error: String?, onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.elarvic_mark), contentDescription = "Elarvic logo", modifier = Modifier.size(110.dp))
        Spacer(Modifier.height(8.dp))
        Text("ELARVIC", style = MaterialTheme.typography.headlineLarge)
        Text("V1", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(value = key, onValueChange = onKeyChange, label = { Text("Elarvic Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onLogin, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Login")
        }
    }
}

@Composable
private fun WhatsAppGate(onOpenWhatsApp: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Join our WhatsApp channel", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text("Tap below to open the channel. After it opens, return to Elarvic to continue.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenWhatsApp, modifier = Modifier.fillMaxWidth()) { Text("Open WhatsApp") }
    }
}

@Composable
private fun Dashboard(onLogout: () -> Unit) {
    val features = listOf("CORE V10", "Session Start", "Internet Status", "Game Boost", "Background", "Floating Display", "VPN Connection", "Notifications", "Settings · Control Surface 1", "Settings · Control Surface 2", "Settings · Control Surface 3", "Floating Button Preview", "Floating Button Appearance", "Color Theme")
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("ELARVIC", style = MaterialTheme.typography.headlineSmall); Text("V1") }
            TextButton(onClick = onLogout) { Text("Logout") }
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(features) { feature ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween) { Text(feature); Text("›") }
                }
            }
        }
    }
}
