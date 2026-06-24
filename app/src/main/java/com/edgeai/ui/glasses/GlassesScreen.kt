package com.edgeai.ui.glasses

import android.Manifest
import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.edgeai.data.AppContainer
import com.edgeai.glasses.MetaGlassesSource
import com.edgeai.net.Detection
import com.edgeai.vision.DetectionOverlay
import com.edgeai.vision.nv21ToJpeg
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GlassesScreen"

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GlassesScreen(container: AppContainer, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val perms = rememberMultiplePermissionsState(buildList {
        add(Manifest.permission.CAMERA) // fallback source
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    })

    var status by remember { mutableStateOf("Checking status...") }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var currentFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val source = remember { MetaGlassesSource(ctx) }
    val scope = rememberCoroutineScope()

    // Activity Result Launcher for Wearable Hardware Permissions
    val permLauncher = rememberLauncherForActivityResult(
        Wearables.RequestPermissionContract()
    ) { result ->
        if (result.isSuccess) {
            status = "Permission granted. Tap connect again."
        } else {
            status = "Hardware permission denied: ${result.errorOrNull()}"
        }
    }

    DisposableEffect(Unit) { onDispose { source.stop() } }

    LaunchedEffect(Unit) {
        val s = source.getStatus()
        status = when (s) {
            MetaGlassesSource.ConnectionStatus.UNREGISTERED -> "Registration required"
            MetaGlassesSource.ConnectionStatus.NEED_PERMISSION -> "Hardware permission required"
            MetaGlassesSource.ConnectionStatus.READY -> "Ready to connect"
        }
    }

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
                        val s = source.getStatus()
                        when (s) {
                            MetaGlassesSource.ConnectionStatus.UNREGISTERED -> {
                                status = "Opening Meta View app..."
                                if (activity != null) Wearables.startRegistration(activity)
                            }
                            MetaGlassesSource.ConnectionStatus.NEED_PERMISSION -> {
                                status = "Requesting hardware permission..."
                                permLauncher.launch(Permission.CAMERA)
                            }
                            MetaGlassesSource.ConnectionStatus.READY -> {
                                status = "Connecting to glasses…"
                                val r = source.start()
                                status = r.fold(
                                    onSuccess = { "Streaming from glasses" },
                                    onFailure = { 
                                        Log.e(TAG, "Connection failed", it)
                                        it.message ?: "Failed to connect" 
                                    },
                                )
                                if (r.isSuccess) {
                                    collectFrames(source, container, scope, 
                                        onDetections = { detections = it },
                                        onFrame = { currentFrame = it }
                                    )
                                }
                            }
                        }
                    }
                }) { 
                    Text(if (status == "Registration required") "Pair with Meta View" else "Connect")
                }

                OutlinedButton(onClick = { 
                    source.stop()
                    status = "Disconnected"
                    currentFrame = null
                    detections = emptyList()
                }) {
                    Text("Disconnect")
                }
            }

            Text(
                "Ensure your glasses are worn and arms are open. If pairing for the first time, " +
                "this app will redirect you to Meta View for approval.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                currentFrame?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    DetectionOverlay(
                        detections = detections,
                        imageAspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
                    )
                } ?: Text("Awaiting frames…", color = Color.Gray)
            }
        }
    }
}

private fun collectFrames(
    source: MetaGlassesSource,
    container: AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
    onDetections: (List<Detection>) -> Unit,
    onFrame: (android.graphics.Bitmap) -> Unit,
) {
    scope.launch {
        source.frames.collect { frame ->
            val jpeg = nv21ToJpeg(frame.data, frame.width, frame.height)
            val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            if (bmp != null) onFrame(bmp)
            
            val url = container.prefs.activeNow() ?: return@collect
            runCatching {
                withContext(Dispatchers.IO) { container.inference.infer(url, jpeg, frame.width, frame.height) }
            }.onSuccess { onDetections(it.detections) }
        }
    }
}
