package com.fs.sample.cameraxMLsample.UI

import android.Manifest
import android.content.Context
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.fs.sample.cameraxMLsample.R
import com.fs.sample.cameraxMLsample.viewmodel.TextRecognitionViewModel

@ExperimentalPermissionsApi
@Composable
fun BuildCameraUI(
    context: Context,
    textRecognitionViewModel: TextRecognitionViewModel,
    listener: CameraScanComposableInterface
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    PermissionRequired(
        permissionState = cameraPermissionState,
        permissionNotGrantedContent = {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(Modifier.padding(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.camera_permission_info_0))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        cameraPermissionState.launchPermissionRequest()
                    }) {
                        Text(stringResource(R.string.camera_permission_grantbutton_0))
                    }
                }
            }
        },
        permissionNotAvailableContent = {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(Modifier.padding(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.camera_permission_info_1))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        listener.onOpenSettingsClick()
                    }) {
                        Text(stringResource(R.string.camera_permission_grantbutton_1))
                    }
                }
            }
        }
    ) {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (preview, takePhotoButton, progress) = createRefs()

            AndroidView(
                modifier = Modifier.constrainAs(preview) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }, // Occupy the max size in the Compose UI tree
                factory = {
                    PreviewView(context)
                },
                update = { previewView ->
                    listener.onStartCamera(
                        Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            })
                }
            )

            Button(modifier = Modifier.constrainAs(takePhotoButton) {
                linkTo(start = parent.start, end = parent.end)
                bottom.linkTo(parent.bottom, 16.dp)
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
            }, onClick = { listener.onTakePhotoClick() })
            {
                Text(stringResource(R.string.camera_scan_button))
            }

            val isLoading = remember { textRecognitionViewModel.getLoadingProgress() }
            CircularProgressIndicator(
                modifier = Modifier
                    .constrainAs(progress) {
                        linkTo(top = parent.top, bottom = parent.bottom)
                        linkTo(start = parent.start, end = parent.end)
                        width = Dimension.value(80.dp)
                        height = Dimension.value(80.dp)
                    }
                    .then(Modifier.alpha(if (isLoading.value) 1f else 0f)),
            )
        }
    }
}

interface CameraScanComposableInterface {
    fun onStartCamera(preview: Preview)
    fun onOpenSettingsClick()
    fun onTakePhotoClick()
}