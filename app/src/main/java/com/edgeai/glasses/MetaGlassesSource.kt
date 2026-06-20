package com.edgeai.glasses

import android.content.Context
import com.meta.wearable.dat.camera.addStream
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Wrapper around Meta's Wearables DAT (Device Access Toolkit) SDK.
 * 
 * This implementation uses the 0.7.0 SDK APIs to connect directly to 
 * paired Meta glasses.
 */
class MetaGlassesSource(private val ctx: Context) {

    private val _frames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 4)
    val frames: Flow<ByteArray> = _frames.asSharedFlow()

    private var currentSession: DeviceSession? = null
    private var streamJob: Job? = null
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    /** True once the SDK reports a paired + streaming session. */
    fun isConnected(): Boolean = connected

    fun start(): Result<Unit> = runCatching {
        // 1. Initialize the SDK
        Wearables.initialize(ctx).getOrThrow()
        
        // 2. Create a session. AutoDeviceSelector() will find the paired glasses.
        val session = Wearables.createSession(AutoDeviceSelector()).getOrThrow()
        currentSession = session
        
        // 3. Add a camera stream to the session
        val stream = session.addStream(StreamConfiguration()).getOrThrow()
        
        // 4. Collect and emit frames
        streamJob = scope.launch {
            stream.videoStream.collect { frame ->
                val byteArray = ByteArray(frame.buffer.remaining())
                frame.buffer.get(byteArray)
                _frames.tryEmit(byteArray)
            }
        }
        
        // 5. Start the stream and session
        stream.start().getOrThrow()
        session.start()
        connected = true
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        currentSession?.stop()
        currentSession = null
        connected = false
    }
}
