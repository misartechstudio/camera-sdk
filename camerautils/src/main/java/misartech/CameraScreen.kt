package misartech

import android.Manifest
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import app.misartech.camerautils.R
import firstcry.commonlibrary.app.camerautils.inappcamera.CameraMode
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    previewView: PreviewView,
    viewModel: CameraViewModel,
    repository: CameraRepositoryImpl,
    isVideoAllowed: Boolean = false,
    isPhotoAllowed: Boolean = true,
    onMediaCaptured: (String) -> Unit,
    onMediaCapturedCancel: () -> Unit,

    ) {
    val context = LocalContext.current
    //var repository by remember { mutableStateOf<CameraRepositoryImpl?>(null) }
   // var viewModel by remember { mutableStateOf<CameraViewModel?>(null) }
    //val previewView = remember { PreviewView(context) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var isFlashOn by remember { mutableStateOf(false) }
    val audioPermissionHandler = rememberComposePermissionHandler(
        context = context,
        permission = Manifest.permission.RECORD_AUDIO,
        permissionLabel = stringResource(R.string.permissionLabel),
        rationaleDescription = stringResource(R.string.rationaleDescription),
        onPermissionGranted = {
            viewModel?.setMode(CameraMode.VIDEO)
        }
    )
   /* DisposableEffect(lifecycleOwner) {
        val repo = CameraRepositoryImpl(context, lifecycleOwner, previewView.surfaceProvider)
        repository = repo
        viewModel = CameraViewModel(repo)

        onDispose {
            repository?.shutDownExecutor()
            repository?.stopVideoRecording()
            repository = null
            viewModel = null
        }
    }*/

    val vm = viewModel
    if (vm != null) {
        val isRecording by vm.isRecording.collectAsState()
        val currentMode by vm.currentMode.collectAsState()
        LaunchedEffect(isPhotoAllowed, isVideoAllowed) {
            if (!isPhotoAllowed && currentMode == CameraMode.PHOTO) {
                if (isVideoAllowed) {
                    vm.setMode(CameraMode.VIDEO)
                }
            } else if (!isVideoAllowed && currentMode == CameraMode.VIDEO) {
                if (isPhotoAllowed) {
                    vm.setMode(CameraMode.PHOTO)
                }
            }
        }
        LaunchedEffect(isRecording) {
            if (isRecording) {
                recordingSeconds = 0L

                while (true) {
                    delay(1000)
                    recordingSeconds++
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            AndroidView(
                factory = {
                    previewView.apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // TOP PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        repository?.toggleFlash(isFlashOn)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = stringResource(R.string.cameraDescription),
                        tint = if (isFlashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (isRecording && isVideoAllowed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = vm.onRecordingTime(recordingSeconds),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTTOM PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isVideoAllowed) {
                        Text(
                            stringResource(R.string.cameraVideo),
                            color = if (currentMode == CameraMode.VIDEO) Color.Yellow else Color.White.copy(
                                alpha = 0.5f
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    audioPermissionHandler.checkAndRequestPermission()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (isPhotoAllowed) {
                        Text(
                            stringResource(R.string.cameraPhoto),
                            color = if (currentMode == CameraMode.PHOTO) Color.Yellow else Color.White.copy(
                                alpha = 0.5f
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    vm.setMode(CameraMode.PHOTO)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                onMediaCapturedCancel()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cameraCloseDescription),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    val shutterColor by animateColorAsState(
                        targetValue = if (currentMode == CameraMode.VIDEO) Color.Red else Color.White,
                        label = ""
                    )
                    val shutterShapeSize by animateDpAsState(
                        targetValue = if (isRecording) 24.dp else 64.dp,
                        label = ""
                    )
                    val shutterCornerShape =
                        if (isRecording) RoundedCornerShape(8.dp) else CircleShape

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isVideoAllowed && !isPhotoAllowed){
                                    audioPermissionHandler.checkAndRequestPermission()
                                }
                                vm.executeAction(
                                    onSuccess = { pureUrl ->
                                        onMediaCaptured(pureUrl)
                                    },
                                    onError = { Log.e("CameraScreen", "Execution error: $it") }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(shutterShapeSize)
                                .clip(shutterCornerShape)
                                .background(shutterColor)
                        )
                    }

                    IconButton(
                        onClick = { vm.toggleLens() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = stringResource(R.string.cameraSwitchDescription),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}