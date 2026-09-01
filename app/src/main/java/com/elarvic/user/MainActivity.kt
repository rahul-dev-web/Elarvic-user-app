package com.elarvic.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.Date

private const val WHATSAPP_LINK = "https://whatsapp.com/channel/0029VbDUColKQuJI4D5IVA2L"
private const val PREFS = "elarvic_user"
private const val KEY_VALUE = "access_key"
private const val WHATSAPP_SEEN = "whatsapp_gate_seen"

private val ElarvicBlack = Color(0xFF050505)
private val ElarvicSurface = Color(0xFF111111)
private val ElarvicSilver = Color(0xFFE7E7E7)
private val ElarvicMuted = Color(0xFF9A9A9A)

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElarvicTheme {
                ElarvicApp(
                    auth = auth,
                    db = db,
                    onOpenWhatsApp = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_LINK)))
                        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putBoolean(WHATSAPP_SEEN, true).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun ElarvicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ElarvicSilver,
            onPrimary = Color.Black,
            secondary = Color(0xFFBDBDBD),
            background = ElarvicBlack,
            surface = ElarvicSurface,
            onBackground = ElarvicSilver,
            onSurface = ElarvicSilver,
            outline = Color(0xFF404040)
        ),
        content = content
    )
}

@Composable
private fun ElarvicApp(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onOpenWhatsApp: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var key by remember { mutableStateOf(prefs.getString(KEY_VALUE, "") ?: "") }
    var loggedIn by remember { mutableStateOf(key.isNotBlank()) }
    var checkingSavedKey by remember { mutableStateOf(loggedIn) }
    var showWhatsApp by remember { mutableStateOf(loggedIn && !prefs.getBoolean(WHATSAPP_SEEN, false)) }
    var expiry by remember { mutableStateOf<Date?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun logout() {
        key = ""
        expiry = null
        loggedIn = false
        showWhatsApp = false
        checkingSavedKey = false
        prefs.edit().clear().apply()
        auth.signOut()
    }

    fun verifyKey(input: String, afterSuccess: () -> Unit) {
        val normalized = input.trim().uppercase()
        if (normalized.isBlank()) {
            error = "Enter your Elarvic key."
            return
        }
        error = null
        checkingSavedKey = true
        auth.signInAnonymously()
            .addOnSuccessListener {
                db.collection("keys").document(normalized).get()
                    .addOnSuccessListener { doc ->
                        val active = doc.getBoolean("active") == true
                        val expires = doc.getTimestamp("expiresAt")?.toDate()
                        val valid = doc.exists() && active && expires != null && expires.after(Date())
                        if (!valid) {
                            checkingSavedKey = false
                            error = "Invalid, revoked or expired Elarvic key."
                            if (loggedIn) logout()
                            return@addOnSuccessListener
                        }
                        key = normalized
                        expiry = expires
                        loggedIn = true
                        checkingSavedKey = false
                        prefs.edit().putString(KEY_VALUE, normalized).apply()
                        afterSuccess()
                    }
                    .addOnFailureListener {
                        checkingSavedKey = false
                        error = it.message ?: "Unable to verify key."
                    }
            }
            .addOnFailureListener {
                checkingSavedKey = false
                error = it.message ?: "Unable to connect to Firebase."
            }
    }

    LaunchedEffect(loggedIn) {
        if (!loggedIn || key.isBlank()) return@LaunchedEffect
        verifyKey(key) { }
    }

    when {
        checkingSavedKey && !loggedIn -> LoadingScreen()
        !loggedIn -> LoginScreen(
            key = key,
            onKeyChange = { key = it; error = null },
            loading = checkingSavedKey,
            error = error,
            onLogin = { verifyKey(key) { showWhatsApp = true } }
        )
        showWhatsApp -> WhatsAppGate(onOpenWhatsApp = {
            onOpenWhatsApp()
            showWhatsApp = false
        })
        else -> Dashboard(expiry = expiry, onLogout = ::logout)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(ElarvicBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.elarvic_mark), null, Modifier.size(96.dp))
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = ElarvicSilver, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("Checking access…", color = ElarvicMuted)
        }
    }
}

@Composable
private fun LoginScreen(
    key: String,
    onKeyChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    onLogin: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(ElarvicBlack).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.elarvic_mark), "Elarvic logo", Modifier.size(130.dp))
        Spacer(Modifier.height(14.dp))
        Text("ELARVIC", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("V1", color = ElarvicMuted)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            label = { Text("Elarvic Key") },
            placeholder = { Text("ELARVIC_XXXXXXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onLogin, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
            else Text("Login")
        }
    }
}

@Composable
private fun WhatsAppGate(onOpenWhatsApp: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ElarvicBlack).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Notifications, null, tint = ElarvicSilver, modifier = Modifier.size(58.dp))
        Spacer(Modifier.height(18.dp))
        Text("Join our WhatsApp channel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("Open the channel once, then return to Elarvic to continue.", color = ElarvicMuted)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenWhatsApp, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.OpenInNew, null)
            Spacer(Modifier.width(8.dp))
            Text("Open WhatsApp")
        }
    }
}

@Composable
private fun Dashboard(expiry: Date?, onLogout: () -> Unit) {
    var session by remember { mutableStateOf(false) }
    var internet by remember { mutableStateOf(true) }
    var gameBoost by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(true) }
    var floating by remember { mutableStateOf(true) }
    var vpn by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var controlSurface by remember { mutableFloatStateOf(1f) }
    var buttonOpacity by remember { mutableFloatStateOf(0.9f) }

    Scaffold(containerColor = ElarvicBlack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.elarvic_mark), null, Modifier.size(46.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ELARVIC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("V1 · CORE V10", color = ElarvicMuted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ElarvicSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Access status", fontWeight = FontWeight.SemiBold)
                        Text("Active", color = ElarvicSilver)
                        Text("Expires: ${expiry?.let { DateFormat.getDateTimeInstance().format(it) } ?: "—"}", color = ElarvicMuted)
                    }
                }
            }
            item { Text("Session", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
            item { SettingRow(Icons.Default.PlayArrow, "Session Start", "Start or stop the current session", session) { session = it } }
            item { SettingRow(Icons.Default.Wifi, "Internet Status", "Connection state", internet) { internet = it } }
            item { SettingRow(Icons.Default.Bolt, "Game Boost", "Performance mode", gameBoost) { gameBoost = it } }
            item { SettingRow(Icons.Default.Layers, "Background", "Keep service state available", background) { background = it } }
            item { SettingRow(Icons.Default.Widgets, "Floating Display", "Show floating control", floating) { floating = it } }
            item { SettingRow(Icons.Default.VpnKey, "VPN Connection", "Connection control", vpn) { vpn = it } }
            item { SettingRow(Icons.Default.Notifications, "Notifications", "Session notifications", notifications) { notifications = it } }
            item { Text("Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
            item { SliderSetting("Control Surface", "${controlSurface.toInt()}", controlSurface, 1f..3f) { controlSurface = it } }
            item { SliderSetting("Floating Button Preview", "Preview opacity ${(buttonOpacity * 100).toInt()}%", buttonOpacity, 0.2f..1f) { buttonOpacity = it } }
            item { SettingRow(Icons.Default.Palette, "Floating Button Appearance", "Customize the floating control", null) { } }
            item { SettingRow(Icons.Default.ColorLens, "Color Theme", "Elarvic black / silver theme", null) { } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean?, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ElarvicSurface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ElarvicSilver, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ElarvicMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (checked != null) Switch(checked = checked, onCheckedChange = onCheckedChange)
            else Icon(Icons.Default.ChevronRight, null, tint = ElarvicMuted)
        }
    }
}

@Composable
private fun SliderSetting(title: String, subtitle: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ElarvicSurface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = ElarvicMuted, style = MaterialTheme.typography.bodySmall)
            Slider(value = value, onValueChange = onChange, valueRange = range)
        }
    }
}
