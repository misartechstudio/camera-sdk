package misartech

class Constants {
    companion object {
        const val EXTRA_CAPTURED_MEDIA_URL: String = "extra_captured_media_url"
        const val EXTRA_REQUEST_CODE: String = "extra_request_code"
        const val EXTRA_IS_VIDEO_ALLOWED: String = "extra_is_video_allowed"
        const val EXTRA_IS_PHOTO_ALLOWED: String = "extra_is_photo_allowed"
        const val EX_IMAGE_CAPTURE: String = "ImageCapture use case target configuration is null."
        const val EX_IMAGE_SURFACE: String = "Camera video surface recorder failed to initialize."
        const val EX_VIDEO_CAPTURE: String =
            "VideoCapture use case target framework configuration is null."
        const val DATE_FORMATE: String = "yyyyMMdd_HHmmss"
        const val IMAGE_MIME_TYPE: String = "image/jpeg"

        const val VIDEO_MIME_TYPE: String = "video/mp4"

        const val EX_AUDIO_PERMISSION: String = "Audio permission missing or revoked."

        const val EX_DEVICE_STORAGE_FULL: String = "Recording stopped: Device storage is full."

        const val EX_VIDEO_RECORD: String = "Video record execution error code:"

        const val FILE_PROVIDER_SUFFIX: String = ".provider"

        const val PHOTO_FAILED: String = "Photo failed"

        const val VIDEO_FAILED: String = "Video failed"

        const val VIDEO_TIME_FORMATE: String = "%02d:%02d:%02d"
        const val EXT_JPG: String = ".jpg"
        const val EXT_MP4: String = ".mp4"


    }

}