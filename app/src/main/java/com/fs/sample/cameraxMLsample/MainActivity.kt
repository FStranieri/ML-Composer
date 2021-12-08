package com.fs.sample.cameraxMLsample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fs.sample.cameraxMLsample.UI.*
import com.fs.sample.cameraxMLsample.viewmodel.TextRecognitionViewModel
import com.fs.sample.cameraxMLsample.viewmodel.TextTranslationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.huawei.hms.mlsdk.common.MLApplication
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), CameraScanComposableInterface {
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    private val textRecognitionViewModel: TextRecognitionViewModel by viewModels()
    private val textTranslationViewModel: TextTranslationViewModel by viewModels()

    @ExperimentalAnimationApi
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            val navController = rememberNavController()
            val showTextRecognitionOutput = remember { textRecognitionViewModel.showOutput }

            NavHost(navController, startDestination = NAV_MAIN) {
                composable(NAV_MAIN) {
                    BuildCameraUI(
                        context = this@MainActivity,
                        textRecognitionViewModel = textRecognitionViewModel,
                        listener = this@MainActivity
                    )
                }
                composable(NAV_TEXTRECOG) {
                    BindTextRecognitionOutput(
                        textRecognitionViewModel = textRecognitionViewModel,
                        textTranslationViewModel = textTranslationViewModel,
                        listener = object : TextRecognitionComposableInterface {
                            override fun onCameraBackButtonClick() {
                                textRecognitionViewModel.showOutput.value = false
                            }

                            override fun onLanguageToTranslateSelected(lang: String) {
                                textTranslationViewModel.initializeMLRemoteTranslator(lang)
                                textTranslationViewModel.translate(textRecognitionViewModel.getOutput().value!!)
                            }
                        }
                    )
                }
            }

            if (showTextRecognitionOutput.value) {
                navController.navigate(NAV_TEXTRECOG) {
                    launchSingleTop = true
                }
            }

            BackHandler(true) {
                if (navController.currentBackStackEntry?.destination?.route == NAV_TEXTRECOG) {
                    textRecognitionViewModel.showOutput.value = false
                    navController.popBackStack()
                } else {
                    finish()
                }
            }
        }

        MLApplication.getInstance().apiKey = resources.getString(R.string.app_client_api_key)

        cameraExecutor = Executors.newSingleThreadExecutor()

        manageTextRecognition()
        manageTextTranslation()
    }

    private fun manageTextRecognition() {
        textRecognitionViewModel.initializeMLLocalTextAnalyzer()
        textRecognitionViewModel.getFailureOutput().observe(this, {
            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
        })
    }

    private fun manageTextTranslation() {
        textTranslationViewModel.initializeMLRemoteTranslator("en")
        textTranslationViewModel.getFailureOutput().observe(this, {
            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_act_menu, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_scan_local -> {
                textRecognitionViewModel.initializeMLLocalTextAnalyzer()
            }
            R.id.action_scan_remote -> {
                textRecognitionViewModel.initializeMLRemoteTextAnalyzer()
            }
        }

        item.isChecked = true

        return true
    }

    private fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onStartCamera(preview: Preview) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

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

    override fun onOpenSettingsClick() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onTakePhotoClick() {
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraMLKit"
        private const val NAV_MAIN = "main"
        private const val NAV_TEXTRECOG = "textrec"
    }
}