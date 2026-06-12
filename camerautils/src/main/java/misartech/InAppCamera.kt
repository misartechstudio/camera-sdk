package misartech


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.PreviewView

class InAppCamera : ComponentActivity() {
    private var cameraRepository: CameraRepositoryImpl? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val isVideoAllowed = intent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_ALLOWED, false)
        val isPhotoAllowed = intent.getBooleanExtra(Constants.EXTRA_IS_PHOTO_ALLOWED, true)
        super.onCreate(savedInstanceState)
        // Step 1: Instantiate PreviewView directly pinned onto the host context layer
        val previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
        // Step 2: Instantiated safely ONCE inside Activity onCreate framework scope
        val repo = CameraRepositoryImpl(this, this, previewView.surfaceProvider)
        cameraRepository = repo
        val viewModel = CameraViewModel(repo)
        setContent {
            CameraScreen(
                previewView = previewView,
                viewModel = viewModel,
                repository = repo,
                isVideoAllowed, isPhotoAllowed,
                onMediaCaptured = { pureUrl ->
                    val incomingRequestCode = intent.getIntExtra(Constants.EXTRA_REQUEST_CODE, -1)
                    val resultIntent = Intent().apply {
                        putExtra(Constants.EXTRA_CAPTURED_MEDIA_URL, pureUrl)
                        putExtra(Constants.EXTRA_REQUEST_CODE, incomingRequestCode)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onMediaCapturedCancel = {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Step 3: Explicit shutdown prevents dangling single-thread executor leaks
        cameraRepository?.shutDownExecutor()
        cameraRepository?.stopVideoRecording()
        cameraRepository = null
    }
}

