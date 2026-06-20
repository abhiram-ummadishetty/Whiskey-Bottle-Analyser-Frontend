package com.edgeai.ui.glasses

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.edgeai.data.AppContainer
import com.edgeai.glasses.MetaGlassesSource
import com.edgeai.net.Detection
import com.edgeai.vision.DetectionOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GlassesScreen(container: AppContainer, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val perms = rememberMultiplePermissionsState(buildList {
        add(Manifest.permission.CAMERA) // fallback source
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    })

    var status by remember { mutableStateOf("Not connected") }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    val source = remember { MetaGlassesSource(ctx) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { source.stop() } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Meta Glasses") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(status, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (!perms.allPermissionsGranted) {
                        perms.launchMultiplePermissionRequest(); return@Button
                    }
                    scope.launch {
                        status = "Connecting to glasses…"
                        val r = source.start()
                        status = r.fold(
                            onSuccess = { "Streaming from glasses" },
                            onFailure = { it.message ?: "Failed to connect" },
                        )
                        if (r.isSuccess) collectFrames(source, container, scope) { d -> detections = d }
                    }
                }) { Text("Pair glasses") }

                OutlinedButton(onClick = { source.stop(); status = "Disconnected" }) {
                    Text("Disconnect")
                }
            }

            Text(
                "Streaming live from your Meta glasses. Ensure they are paired in the Meta View app first. " +
                "Frames are processed through the YOLO + OCR backend for real-time analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center,
            ) {
                DetectionOverlay(detections)
                if (detections.isEmpty()) Text("Awaiting frames…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun collectFrames(
    source: MetaGlassesSource,
    container: AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
    onDetections: (List<Detection>) -> Unit,
) {
    scope.launch {
        source.frames.collect { jpeg ->
            val url = container.prefs.activeNow() ?: return@collect
            runCatching {
                withContext(Dispatchers.IO) { container.inference.infer(url, jpeg, 0, 0) }
            }.onSuccess { onDetections(it.detections) }
        }
    }
}