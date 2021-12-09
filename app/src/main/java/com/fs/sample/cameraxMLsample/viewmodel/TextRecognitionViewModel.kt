package com.fs.sample.cameraxMLsample.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting
import com.huawei.hms.mlsdk.text.MLTextAnalyzer
import java.lang.Exception

class TextRecognitionViewModel: ViewModel() {

    private lateinit var mlAnalyzer: MLTextAnalyzer

    private var output: MutableLiveData<String> = MutableLiveData()
    var showOutput: MutableState<Boolean> = mutableStateOf(false)
    private var failureOutput: MutableLiveData<Exception> = MutableLiveData()

    private var loadingProgress: MutableState<Boolean> = mutableStateOf(false)

    //with this config we are using the sdk only, scanning latin characters
    fun initializeMLLocalTextAnalyzer() {
        val setting = MLLocalTextSetting.Factory()
            .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
            .setLanguage("en")
            .create()
        this.mlAnalyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
    }

    //with this config we are able to recognize both latin and chinese characters using the cloud
    fun initializeMLRemoteTextAnalyzer() {
        val setting = MLRemoteTextSetting.Factory()
            .setTextDensityScene(MLRemoteTextSetting.OCR_LOOSE_SCENE)
            .setLanguageList(mutableListOf("en", "zh"))
            .setBorderType(MLRemoteTextSetting.ARC)
            .create()
        this.mlAnalyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(setting)
    }

    fun getOutput(): LiveData<String> {
        return output
    }

    fun getFailureOutput(): LiveData<Exception> {
        return failureOutput
    }

    fun getLoadingProgress(): MutableState<Boolean> {
        return loadingProgress
    }

    private fun setLoading (loading: Boolean) {
        this.loadingProgress.value = loading
    }

    fun scan(bitmap: Bitmap) {
        setLoading(true)

        mlAnalyzer.asyncAnalyseFrame(MLFrame.fromBitmap(bitmap)).apply {
            addOnSuccessListener {
                setLoading(false)
                output.value = it.stringValue
                showOutput.value = true
            }
            addOnFailureListener {
                setLoading(false)
                failureOutput.value = it
            }
        }
    }
}