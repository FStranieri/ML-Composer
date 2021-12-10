package com.fs.sample.cameraxMLsample.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.view.WindowInsets
import androidx.camera.core.ImageProxy
import androidx.core.view.WindowInsetsCompat

object CameraUtils {
    private fun getWindowHeight(context: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = context.windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            metrics.bounds.height() - insets.bottom - insets.top
        } else {
            val view = context.window.decorView
            val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            context.resources.displayMetrics.heightPixels - insets.bottom - insets.top
        }
    }

    private fun getWindowWidth(context: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = context.windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            metrics.bounds.width() - insets.left - insets.right
        } else {
            val view = context.window.decorView
            val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            context.resources.displayMetrics.widthPixels - insets.left - insets.right
        }
    }

    fun convertImageProxyToBitmap(imageProxy: ImageProxy, activity: Activity): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).let {
            //workaround: some devices are capturing rotated pictures, causing ML recog to fail
            val rotation =
                if (imageProxy.imageInfo.rotationDegrees != 0 && getWindowHeight(activity) > getWindowWidth(
                        activity
                    )
                ) 90f else 0f
            val matrix = Matrix()
            matrix.postRotate(rotation)

            return@let Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, false)
        }
    }
}