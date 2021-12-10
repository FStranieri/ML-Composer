package com.fs.sample.cameraxMLsample.UI

import android.Manifest
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.fs.sample.cameraxMLsample.R
import com.fs.sample.cameraxMLsample.utils.CameraUtils
import com.fs.sample.cameraxMLsample.viewmodel.TextRecognitionViewModel
import java.util.concurrent.Executor

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
            val executor = remember(context) { ContextCompat.getMainExecutor(context) }
            var imageCapture: MutableState<ImageCapture?> = remember { mutableStateOf(null) }

            MLCameraView(
                modifier = Modifier.constrainAs(preview) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }, // Occupy the max size in the Compose UI tree
                imageCapture = imageCapture,
                context = context,
                executor = executor
            )

            Button(modifier = Modifier.constrainAs(takePhotoButton) {
                linkTo(start = parent.start, end = parent.end)
                bottom.linkTo(parent.bottom, 16.dp)
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
            }, onClick = {
                imageCapture.value?.takePicture(executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            textRecognitionViewModel.scan(
                                CameraUtils.convertImageProxyToBitmap(
                                    image,
                                    context as Activity
                                )
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, exception.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    })
            })
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

@Composable
fun MLCameraView(
    modifier: Modifier,
    imageCapture: MutableState<ImageCapture?>,
    executor: Executor,
    context: Context
) {
    val previewCameraView = remember { PreviewView(context) }
    val cameraProviderFuture =
        remember(context) { ProcessCameraProvider.getInstance(context) }
    val cameraProvider = remember(cameraProviderFuture) { cameraProviderFuture.get() }
    var cameraSelector: CameraSelector? by remember { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = {
            cameraProviderFuture.addListener(
                {
                    cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    imageCapture.value = ImageCapture.Builder().build()

                    cameraProvider.unbindAll()

                    val prev = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewCameraView.surfaceProvider)
                    }

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector as CameraSelector,
                        imageCapture.value,
                        prev
                    )
                }, executor
            )
            previewCameraView
        }
    )
}

interface CameraScanComposableInterface {
    fun onOpenSettingsClick()
}