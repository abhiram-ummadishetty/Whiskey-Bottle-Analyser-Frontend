package com.edgeai.ui.camera

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.edgeai.data.AppContainer
import com.edgeai.net.Detection
import com.edgeai.vision.DetectionOverlay
import com.edgeai.vision.toJpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(container: AppContainer, onBack: () -> Unit) {
    val perms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )
    LaunchedEffect(Unit) { if (!perms.allPermissionsGranted) perms.launchMultiplePermissionRequest() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Live camera") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (perms.allPermissionsGranted) LiveInference(container)
            else Text("Camera & microphone permission required", Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun LiveInference(container: AppContainer) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var latency by remember { mutableStateOf(0L) }
    var lastError by remember { mutableStateOf<String?>(null) }
    val inFlight = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val lastTs = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { c ->
            val view = PreviewView(c).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
            val providerFuture = ProcessCameraProvider.getInstance(c)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analyzer.setAnalyzer(executor) { img -> handleFrame(img, scope, container, inFlight, lastTs,
                    onResult = { d, ms -> detections = d; latency = ms; lastError = null },
                    onError = { e -> lastError = e }) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
            }, ContextCompat.getMainExecutor(c))
            view
        }, modifier = Modifier.fillMaxSize())

        DetectionOverlay(detections)

        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("  ${detections.size} objects · ${latency} ms${lastError?.let { " · $it" } ?: ""}  ",
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun handleFrame(
    img: ImageProxy,
    scope: kotlinx.coroutines.CoroutineScope,
    container: AppContainer,
    inFlight: java.util.concurrent.atomic.AtomicBoolean,
    lastTs: java.util.concurrent.atomic.AtomicLong,
    onResult: (List<Detection>, Long) -> Unit,
    onError: (String) -> Unit,
) {
    val now = System.currentTimeMillis()
    val url = container.prefs.activeNow()
    if (url == null || inFlight.get() || now - lastTs.get() < 180) {
        img.close(); return
    }
    val w = img.width; val h = img.height
    val jpeg = try { img.toJpeg() } catch (_: Throwable) { img.close(); return }
    img.close()
    inFlight.set(true); lastTs.set(now)
    scope.launch {
        runCatching { withContext(Dispatchers.IO) { container.inference.infer(url, jpeg, w, h) } }
            .onSuccess { onResult(it.detections, it.latencyMs) }
            .onFailure { onError(it.message?.take(80) ?: "infer failed") }
        inFlight.set(false)
    }
}