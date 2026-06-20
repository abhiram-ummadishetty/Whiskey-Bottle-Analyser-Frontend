package com.edgeai.ui.gate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeai.data.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendGateScreen(container: AppContainer, onReady: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        url = container.prefs.lastBackendUrl.first().orEmpty()
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Connect to your backend", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Frames stream to a Python inference service on your network. " +
                "Enter its URL — you'll be asked again next time you open the app.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            url, { url = it },
            label = { Text("Backend URL") },
            placeholder = { Text("http://192.168.1.42:8000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy && url.isNotBlank(),
            onClick = {
                busy = true; error = null
                scope.launch {
                    val ok = container.inference.health(url)
                    if (ok) { container.prefs.setActive(url); onReady() }
                    else { error = "Couldn't reach $url/health" }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Checking…" else "Connect") }
    }
}