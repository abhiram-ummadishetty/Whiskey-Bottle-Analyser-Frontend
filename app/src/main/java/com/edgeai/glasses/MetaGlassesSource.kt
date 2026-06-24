package com.edgeai.glasses

import android.content.Context
import android.util.Log
import com.meta.wearable.dat.camera.addStream
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "MetaGlassesSource"

data class MetaFrame(val data: ByteArray, val width: Int, val height: Int)

class MetaGlassesSource(private val ctx: Context) {

    private val _frames = MutableSharedFlow<MetaFrame>(extraBufferCapacity = 4)
    val frames: Flow<MetaFrame> = _frames.asSharedFlow()

    private var currentSession: DeviceSession? = null
    private var streamJob: Job? = null
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun isConnected(): Boolean = connected

    /**
     * Checks if the app is registered with Meta and has hardware permissions.
     */
    suspend fun getStatus(): ConnectionStatus {
        val reg = Wearables.registrationState.value
        if (reg != RegistrationState.REGISTERED) return ConnectionStatus.UNREGISTERED
        
        val permResult = Wearables.checkPermissionStatus(Permission.CAMERA)
        val status = permResult.getOrNull()
        if (status !is PermissionStatus.Granted) return ConnectionStatus.NEED_PERMISSION
        
        return ConnectionStatus.READY
    }

    suspend fun start(): Result<Unit> = runCatching {
        Log.d(TAG, "Starting MetaGlassesSource")
        
        // 1. Ensure we are registered
        if (Wearables.registrationState.value != RegistrationState.REGISTERED) {
            error("App is not registered with Meta. Please pair first.")
        }

        // 2. Create the session
        val sessionResult = Wearables.createSession(AutoDeviceSelector())
        val session = sessionResult.getOrThrow()
        currentSession = session
        
        session.start()
        
        // 3. Wait for the session to reach STARTED state
        withTimeout(10000) {
            session.state.first { it == DeviceSessionState.STARTED }
        }
        
        // 4. Add the camera stream
        val streamResult = session.addStream(StreamConfiguration())
        val stream = streamResult.getOrThrow()
        
        // 5. Collect frames
        streamJob = scope.launch {
            stream.videoStream.collect { frame ->
                val buffer = frame.buffer
                val byteArray = ByteArray(buffer.remaining())
                buffer.get(byteArray)
                _frames.tryEmit(MetaFrame(byteArray, frame.width, frame.height))
            }
        }
        
        // 6. Start the stream capability
        stream.start().getOrThrow()
        
        connected = true
        Log.d(TAG, "Real Meta glasses connected successfully")
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        currentSession?.stop()
        currentSession = null
        connected = false
    }

    enum class ConnectionStatus {
        UNREGISTERED,
        NEED_PERMISSION,
        READY
    }
}
