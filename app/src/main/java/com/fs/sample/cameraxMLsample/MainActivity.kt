package com.fs.sample.cameraxMLsample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fs.sample.cameraxMLsample.UI.*
import com.fs.sample.cameraxMLsample.viewmodel.TextRecognitionViewModel
import com.fs.sample.cameraxMLsample.viewmodel.TextTranslationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.huawei.hms.mlsdk.common.MLApplication
import java.util.*


class MainActivity : AppCompatActivity(), CameraScanComposableInterface {

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
                                navController.popBackStack()
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
                    popUpTo(route = NAV_MAIN) {
                        textRecognitionViewModel.showOutput.value = false
                        this.inclusive = false
                    }

                    this.launchSingleTop = true
                }
            }
        }

        MLApplication.getInstance().apiKey = resources.getString(R.string.app_client_api_key)

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

    override fun onOpenSettingsClick() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraMLKit"
        private const val NAV_MAIN = "main"
        private const val NAV_TEXTRECOG = "textrec"
    }
}