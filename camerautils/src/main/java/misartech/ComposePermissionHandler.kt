package misartech

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import app.misartech.camerautils.R

class ComposePermissionHandler(
    private val context: Context,
    private val permission: String,
    private val permissionLabel: String,
    private val rationaleDescription: String,
    private val onPermissionGranted: () -> Unit
) {
    private var denyCount by mutableIntStateOf(0)

    private var showSoftRationale by mutableStateOf(false)
    private var showSettingsRationale by mutableStateOf(false)

    private var launcher: ManagedActivityResultLauncher<String, Boolean>? = null

    @Composable
    fun Register() {
        // 1. Initialize the system Activity Result contract launcher
        launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                denyCount++
                val activity = context.findActivity()
                if (activity != null) {
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    if (denyCount >= 2 || !showRationale) {
                        showSettingsRationale = true
                    } else {
                        showSoftRationale = true
                    }
                }
            }
        }

        // 2. Self-contained Soft Rationale Dialog (Triggered after 1st denial)
        if (showSoftRationale) {
            AlertDialog(
                onDismissRequest = { showSoftRationale = false },
                title = { Text(text = permissionLabel) },
                text = { Text(text = rationaleDescription) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSoftRationale = false
                            launcher?.launch(permission)
                        }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSoftRationale = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }

        // 3. Self-contained Settings Redirection Dialog (Triggered after 2nd denial)
        if (showSettingsRationale) {
            AlertDialog(
                onDismissRequest = { showSettingsRationale = false },
                title = { Text(text = stringResource(R.string.permission_required)) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.permission_is_permanently_denied_please_allow_recording_permissions_in_app_settings_to_proceed,
                            permissionLabel,
                            permissionLabel
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSettingsRationale = false
                            launchAppSettings()
                        }
                    ) {
                        Text(stringResource(R.string.go_to_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsRationale = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

    fun checkAndRequestPermission() {
        when {
            checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }

            else -> {
                val activity = context.findActivity()
                if (activity != null) {
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    if (denyCount >= 2 || (!showRationale && denyCount > 0)) {
                        showSettingsRationale = true
                    } else {
                        launcher?.launch(permission)
                    }
                } else {
                    launcher?.launch(permission)
                }
            }
        }
    }

    private fun launchAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val generalSettingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalSettingsIntent)
        }
    }

    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }
}

@Composable
fun rememberComposePermissionHandler(
    context: Context,
    permission: String,
    permissionLabel: String,
    rationaleDescription: String,
    onPermissionGranted: () -> Unit
): ComposePermissionHandler {
    val handler = remember(permission) {
        ComposePermissionHandler(
            context,
            permission,
            permissionLabel,
            rationaleDescription,
            onPermissionGranted
        )
    }
    handler.Register() // Starts the observation engine seamlessly
    return handler
}