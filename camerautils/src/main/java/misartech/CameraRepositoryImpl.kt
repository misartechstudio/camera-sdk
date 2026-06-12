package misartech

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import firstcry.commonlibrary.app.camerautils.inappcamera.CaptureResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraRepositoryImpl(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewViewSurfaceProvider: Preview.SurfaceProvider,
) : CameraRepository {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val _isRecording = MutableStateFlow(false)

    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private var camera: Camera? = null

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder().build().also {
            it.surfaceProvider = previewViewSurfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (_: Exception) {
            try {
                // Device fallback strategy if concurrent video + capture fails on older chipsets
                camera =
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (fatal: Exception) {
                fatal.printStackTrace()
            }
        }
    }

    override fun toggleCameraLens() {
        if (_isRecording.value) return

        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    override fun takePhoto(onSuccess: (CaptureResult) -> Unit, onError: (Exception) -> Unit) {
        val capture = imageCapture ?: run {
            onError(Exception(Constants.EX_IMAGE_CAPTURE))
            return
        }

        val timeStamp = SimpleDateFormat(Constants.DATE_FORMATE, Locale.getDefault()).format(Date())
        val ext =
            if (Constants.EXT_JPG.startsWith(".")) Constants.EXT_JPG else ".${Constants.EXT_JPG}"
        val imageName = "$timeStamp$ext"

        // Handle MediaStore for Android 10 (API 29) and above; fall back to local file provider for legacy
        val outputOptions: ImageCapture.OutputFileOptions
        var legacyFile: File? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
                put(MediaStore.MediaColumns.MIME_TYPE, Constants.IMAGE_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
        } else {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, imageName)
            legacyFile = file
            outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        }

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val finalUriString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        output.savedUri?.toString() ?: ""
                    } else {
                        legacyFile?.let {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}${Constants.FILE_PROVIDER_SUFFIX}",
                                it
                            ).toString()
                        } ?: ""
                    }
                    onSuccess(CaptureResult(finalUriString, isVideo = false))
                }

                override fun onError(exc: ImageCaptureException) {
                    onError(exc)
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startVideoRecording(
        onSuccess: (CaptureResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onError(Exception(Constants.EX_VIDEO_CAPTURE))
            return
        }

        if (activeRecording != null) return

        val timeStamp =
            SimpleDateFormat(Constants.DATE_FORMATE, Locale.US).format(System.currentTimeMillis())
        val videoName = "$timeStamp${Constants.EXT_MP4}"

        var legacyFile: File? = null
        val pendingRecording = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, videoName)
                put(MediaStore.MediaColumns.MIME_TYPE, Constants.VIDEO_MIME_TYPE)
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
            val mediaStoreOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            capture.output.prepareRecording(context, mediaStoreOptions)
        } else {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            val file = File(storageDir, videoName)
            legacyFile = file
            val fileOptions = FileOutputOptions.Builder(file).build()

            capture.output.prepareRecording(context, fileOptions)
        }

        _isRecording.value = true

        try {
            activeRecording = pendingRecording
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.value = false
                            activeRecording = null

                            if (!recordEvent.hasError()) {
                                val finalVideoUriString =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        recordEvent.outputResults.outputUri.toString()
                                    } else {
                                        legacyFile?.let {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}${Constants.FILE_PROVIDER_SUFFIX}",
                                                it
                                            ).toString()
                                        } ?: ""
                                    }
                                onSuccess(CaptureResult(finalVideoUriString, isVideo = true))
                            } else {
                                val errorCode = recordEvent.error
                                if (errorCode == VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE) {
                                    onError(Exception(Constants.EX_DEVICE_STORAGE_FULL))
                                } else {
                                    onError(Exception("${Constants.EX_VIDEO_RECORD}: $errorCode"))
                                }
                            }
                        }

                        else -> { /* Handle Start/Pause/Resume transitions if needed */
                        }
                    }
                }
        } catch (securityException: SecurityException) {
            _isRecording.value = false
            activeRecording = null
            onError(Exception(Constants.EX_AUDIO_PERMISSION, securityException))
        } catch (illegalStateException: IllegalStateException) {
            _isRecording.value = false
            activeRecording = null
            onError(
                Exception(
                    Constants.EX_IMAGE_SURFACE,
                    illegalStateException
                )
            )
        }
    }

    override fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        _isRecording.value = false
    }

    fun shutDownExecutor() {
        cameraExecutor.shutdown()
    }

    override fun toggleFlash(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }
}