package com.edgeai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.edgeai.data.AppContainer
import com.edgeai.ui.auth.AuthScreen
import com.edgeai.ui.camera.CameraScreen
import com.edgeai.ui.upload.UploadScreen
import com.edgeai.ui.gate.BackendGateScreen
import com.edgeai.ui.glasses.GlassesScreen
import com.edgeai.ui.home.HomeScreen

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val container = remember(ctx) { AppContainer.get(ctx) }
    val nav = rememberNavController()
    val session by container.session.user.collectAsState(initial = null)
    val backendUrl by container.prefs.backendUrl.collectAsState(initial = null)

    val start = when {
        session == null -> "auth"
        backendUrl.isNullOrBlank() -> "gate"
        else -> "home"
    }

    NavHost(navController = nav, startDestination = start) {
        composable("auth") { AuthScreen(container, onAuthed = { nav.navigate("gate") { popUpTo(0) } }) }
        composable("gate") { BackendGateScreen(container, onReady = { nav.navigate("home") { popUpTo(0) } }) }
        composable("home") { HomeScreen(container,
            onCamera = { nav.navigate("camera") },
            onUpload = { nav.navigate("upload") },
            onGlasses = { nav.navigate("glasses") },
            onLogout = { nav.navigate("auth") { popUpTo(0) } },
            onReconfigure = { nav.navigate("gate") }) }
        composable("camera") { CameraScreen(container, onBack = { nav.popBackStack() }) }
        composable("upload") { UploadScreen(container, onBack = { nav.popBackStack() }) }
        composable("glasses") { GlassesScreen(container, onBack = { nav.popBackStack() }) }
    }
}