package com.edgeai.ui.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgeai.data.AppContainer
import com.edgeai.net.Detection
import com.edgeai.vision.DetectionOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var latency by remember { mutableStateOf(0L) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
            detections = emptyList()
            error = null
        }
    )

    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it) 
                    }
                }.getOrNull()
            }?.let { 
                bitmap = it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Image") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let { b ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = b.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        DetectionOverlay(
                            detections = detections,
                            imageAspectRatio = b.width.toFloat() / b.height.toFloat()
                        )
                    }
                } ?: Text("No image selected", color = Color.Gray)
            }

            Spacer(Modifier.height(16.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            if (detections.isNotEmpty()) {
                Text("${detections.size} objects detected in ${latency}ms", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select Image")
                }

                Button(
                    onClick = {
                        val b = bitmap
                        val url = container.prefs.activeNow()
                        if (b != null && url != null) {
                            scope.launch {
                                isUploading = true
                                error = null
                                runCatching {
                                    val stream = ByteArrayOutputStream()
                                    b.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                    val jpeg = stream.toByteArray()
                                    withContext(Dispatchers.IO) {
                                        container.inference.infer(url, jpeg, b.width, b.height)
                                    }
                                }.onSuccess { 
                                    detections = it.detections
                                    latency = it.latencyMs
                                }.onFailure { 
                                    error = it.message ?: "Upload failed"
                                }
                                isUploading = false
                            }
                        }
                    },
                    enabled = bitmap != null && !isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze")
                    }
                }
            }
        }
    }
}
