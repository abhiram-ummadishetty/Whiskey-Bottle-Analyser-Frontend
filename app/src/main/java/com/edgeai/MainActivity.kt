package com.edgeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.edgeai.ui.AppNav
import com.meta.wearable.dat.core.Wearables

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize the Meta Wearables SDK with Activity context
        // This is crucial for attestation and deep-link handling.
        Wearables.initialize(this)
        
        setContent { EdgeAITheme { AppNav() } }
    }
}

@Composable
fun EdgeAITheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = Color(0xFF09090B),
        surface = Color(0xFF18181B),
        primary = Color(0xFF22D3EE),
        onPrimary = Color(0xFF09090B),
    )
    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) { content() }
    }
}
