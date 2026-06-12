package misartech

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import firstcry.commonlibrary.app.camerautils.inappcamera.CameraMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel(private val repository: CameraRepository) : ViewModel() {

    private val _currentMode = MutableStateFlow(CameraMode.PHOTO)
    val currentMode: StateFlow<CameraMode> = _currentMode.asStateFlow()

    val isRecording = repository.isRecording

    fun setMode(mode: CameraMode) {
        if (isRecording.value) return
        _currentMode.value = mode
    }

    fun toggleLens() = repository.toggleCameraLens()

    fun executeAction(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (_currentMode.value == CameraMode.PHOTO) {
            repository.takePhoto(
                onSuccess = { result ->
                    // Prints and returns only the clean, simple file URL string
                    onSuccess(result.fileUri)
                },
                onError = { onError("${Constants.PHOTO_FAILED}: ${it.message}") }
            )
        } else {
            if (isRecording.value) {
                repository.stopVideoRecording()
            } else {
                repository.startVideoRecording(
                    onSuccess = { result ->
                        // Prints and returns only the clean, simple file URL string
                        onSuccess(result.fileUri)
                    },
                    onError = { onError("${Constants.VIDEO_FAILED}: ${it.message}") }
                )
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun onRecordingTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(
            Constants.VIDEO_TIME_FORMATE,
            hours,
            minutes,
            seconds
        )
    }
}
