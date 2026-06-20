package com.edgeai.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeai.data.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onCamera: () -> Unit,
    onUpload: () -> Unit,
    onGlasses: () -> Unit,
    onLogout: () -> Unit,
    onReconfigure: () -> Unit,
) {
    val user by container.session.user.collectAsState(initial = null)
    val backend by container.prefs.backendUrl.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Edge AI") },
            actions = {
                TextButton(onClick = onReconfigure) { Text("Backend") }
                TextButton(onClick = {
                    scope.launch { container.session.clear(); container.prefs.clearActive() }
                    onLogout()
                }) { Text("Sign out") }
            },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Hi ${user?.firstName ?: "there"} — what are we analysing today?",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Connected to ${backend ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ElevatedCard(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Phone camera", style = MaterialTheme.typography.titleMedium)
                        Text("Live stream from this device", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ElevatedCard(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Upload image", style = MaterialTheme.typography.titleMedium)
                        Text("Pick a photo from your gallery", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ElevatedCard(onClick = onGlasses, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Visibility, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Meta Glasses", style = MaterialTheme.typography.titleMedium)
                        Text("Pair smart glasses as the video source", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}