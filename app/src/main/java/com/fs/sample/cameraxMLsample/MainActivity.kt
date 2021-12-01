package com.fs.sample.cameraxMLsample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.huawei.hms.mlsdk.common.MLApplication
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.res.stringResource


class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var previewView: PreviewView

    private lateinit var navController: NavHostController

    private val textRecognitionViewModel: TextRecognitionViewModel by viewModels()

    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = NAV_MAIN) {
                composable(NAV_MAIN) { BuildCameraUI() }
                composable(NAV_TEXTRECOG) { BindTextRecognitionOutput() }
            }
        }

        MLApplication.getInstance().apiKey = resources.getString(R.string.app_client_api_key)

        cameraExecutor = Executors.newSingleThreadExecutor()

        manageTextRecognition()
    }

    private fun manageTextRecognition() {
        textRecognitionViewModel.initializeMLLocalTextAnalyzer()
        textRecognitionViewModel.getOutput().observe(this, {
            navController.navigate(NAV_TEXTRECOG) {
                popUpTo(NAV_MAIN) { inclusive = false }
                launchSingleTop = true
            }
        })
        textRecognitionViewModel.getFailureOutput().observe(this, {
            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
        })
    }

    @ExperimentalPermissionsApi
    @androidx.compose.ui.tooling.preview.Preview
    @Composable
    private fun BuildCameraUI() {
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
                            openSettingsScreen()
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
                        PreviewView(this@MainActivity).also {
                            previewView = it
                        }
                    },
                    update = {
                        startCamera()
                    }
                )

                Button(modifier = Modifier.constrainAs(takePhotoButton) {
                    linkTo(start = parent.start, end = parent.end)
                    bottom.linkTo(parent.bottom, 16.dp)
                    width = Dimension.preferredWrapContent
                    height = Dimension.preferredWrapContent
                }, onClick = { takePhoto() })
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

    @androidx.compose.ui.tooling.preview.Preview
    @Composable
    private fun BindTextRecognitionOutput() {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (title, text, backToCameraButton) = createRefs()
            val textValue by textRecognitionViewModel.getOutput().observeAsState()
            val scrollState = rememberScrollState(0)

            Text(
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(parent.top)
                        linkTo(start = parent.start, end = parent.end)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    }
                    .then(Modifier.padding(16.dp)),
                text = stringResource(R.string.camera_output_result),
                style = TextStyle(fontStyle = FontStyle.Italic),
                fontWeight = FontWeight.Bold
            )

            SelectionContainer(modifier = Modifier
                .constrainAs(text) {
                    linkTo(top = title.bottom, bottom = backToCameraButton.top, 16.dp, 16.dp)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .then(
                    Modifier.padding(16.dp)
                )
                .then(
                    Modifier.border(
                        width = 4.dp,
                        color = Color.Black,
                        shape = RectangleShape
                    )
                )) {
                Text(
                    text = textValue ?: "",
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .then(Modifier.padding(8.dp))
                )
            }

            Button(modifier = Modifier.constrainAs(backToCameraButton) {
                linkTo(start = parent.start, end = parent.end)
                bottom.linkTo(parent.bottom, 16.dp)
                width = Dimension.preferredWrapContent
                height = Dimension.preferredWrapContent
            }, onClick = {
                navController.popBackStack()
            })
            {
                Text(stringResource(R.string.go_to_camerascreen_button))
            }
        }
    }

    private fun openSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_act_menu, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_scan_local ->
                textRecognitionViewModel.initializeMLLocalTextAnalyzer()
            R.id.action_scan_remote ->
                textRecognitionViewModel.initializeMLRemoteTextAnalyzer()
        }

        item.isChecked = true

        return true
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.convertImageProxyToBitmap()
                    textRecognitionViewModel.scan(bitmap)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            })
    }

    private fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraMLKit"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val NAV_MAIN = "main"
        private const val NAV_TEXTRECOG = "textrec"
    }
}