package firstcry.commonlibrary.app.camerautils.inappcamera

enum class CameraMode { PHOTO, VIDEO }

data class CaptureResult(
    val fileUri: String,
    val isVideo: Boolean
)