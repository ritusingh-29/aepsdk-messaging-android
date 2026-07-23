package com.adobe.marketing.mobile.pushtestapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.messaging.MessagingService
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleAssuranceDeepLink(intent)
        setContent {
            MaterialTheme {
                PushTestScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAssuranceDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        MobileCore.lifecycleStart(null)
    }

    override fun onPause() {
        super.onPause()
        MobileCore.lifecyclePause()
    }

    private fun handleAssuranceDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.getQueryParameter("adb_validation_sessionid") != null) {
            Assurance.startSession(data.toString())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ecid by remember { mutableStateOf("Loading...") }
    var pushToken by remember { mutableStateOf("Loading...") }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showAssuranceDialog by remember { mutableStateOf(false) }
    var assuranceUrl by remember { mutableStateOf("") }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var templatePickerType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTrigger) {
        ecid = "Loading..."
        Identity.getExperienceCloudId { id ->
            ecid = id ?: "Not available"
        }
        try {
            pushToken = Firebase.messaging.token.await()
        } catch (e: Exception) {
            pushToken = "Error fetching token"
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Push Test App") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoField(label = "ECID", value = ecid, context = context)
                    HorizontalDivider()
                    InfoField(label = "Push Token", value = pushToken, context = context)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val token = Firebase.messaging.token.await()
                            MobileCore.setPushIdentifier(token)
                            pushToken = token
                            Toast.makeText(context, "Push identifier set", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to get token", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Push Identifier")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        MobileCore.resetIdentities()
                        delay(1000)
                        try {
                            val token = Firebase.messaging.token.await()
                            MobileCore.setPushIdentifier(token)
                            pushToken = token
                        } catch (e: Exception) {
                            pushToken = "Error fetching token"
                        }
                        refreshTrigger++
                    }
                    Toast.makeText(context, "Identities reset", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Identities")
            }

            OutlinedButton(
                onClick = { showAssuranceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect Assurance")
            }

            OutlinedButton(
                onClick = { showTemplatePicker = true; templatePickerType = null },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test AJO Template")
            }
        }
    }

    if (showAssuranceDialog) {
        AlertDialog(
            onDismissRequest = { showAssuranceDialog = false },
            title = { Text("Connect to Assurance") },
            text = {
                OutlinedTextField(
                    value = assuranceUrl,
                    onValueChange = { assuranceUrl = it },
                    label = { Text("Assurance URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (assuranceUrl.isNotBlank()) {
                            Assurance.startSession(assuranceUrl)
                            showAssuranceDialog = false
                            assuranceUrl = ""
                            Toast.makeText(context, "Connecting to Assurance...", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { showAssuranceDialog = false; assuranceUrl = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    val templateTypes = listOf("ajo_basic", "ajo_bigtext")
    if (showTemplatePicker && templatePickerType == null) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Select template type") },
            text = {
                Column {
                    templateTypes.forEach { type ->
                        TextButton(
                            onClick = { templatePickerType = type },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(type) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") }
            }
        )
    }

    val currentType = templatePickerType
    if (showTemplatePicker && currentType != null) {
        val files = context.assets.list(currentType).orEmpty().toList()
        AlertDialog(
            onDismissRequest = { templatePickerType = null },
            title = { Text("Select $currentType payload") },
            text = {
                Column {
                    files.forEach { file ->
                        TextButton(
                            onClick = {
                                showTemplatePicker = false
                                templatePickerType = null
                                fireAjoTemplatePush(context, "$currentType/$file")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(file) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { templatePickerType = null }) { Text("Back") }
            }
        )
    }
}

private fun fireAjoTemplatePush(context: Context, assetPath: String) {
    val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    val obj = JSONObject(json)
    val data = HashMap<String, String>()
    obj.keys().forEach { key -> data[key] = obj.getString(key) }
    val remoteMessage = RemoteMessage.Builder("pushtestapp@gcm.googleapis.com")
        .setMessageId(UUID.randomUUID().toString())
        .setData(data)
        .build()
    val handled = MessagingService.handleRemoteMessage(context, remoteMessage)
    Toast.makeText(context, if (handled) "AJO push handled ✓" else "Not handled — check logs", Toast.LENGTH_SHORT).show()
}

@Composable
private fun InfoField(label: String, value: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
