package misartech

import firstcry.commonlibrary.app.camerautils.inappcamera.CaptureResult
import kotlinx.coroutines.flow.StateFlow

interface CameraRepository {
    val isRecording: StateFlow<Boolean>
    fun toggleCameraLens()
    fun takePhoto(onSuccess: (CaptureResult) -> Unit, onError: (Exception) -> Unit)
    fun startVideoRecording(onSuccess: (CaptureResult) -> Unit, onError: (Exception) -> Unit)
    fun stopVideoRecording()
    fun toggleFlash(enable: Boolean)
}