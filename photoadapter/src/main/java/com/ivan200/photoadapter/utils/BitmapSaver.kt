package com.ivan200.photoadapter.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.utils.ImageUtils.scaleDown
import com.otaliastudios.cameraview.PictureResult
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

//
// Created by Ivan200 on 08.11.2019.
//

class BitmapSaver(
    val photoFile: File,
    private var result: Bitmap,
    private var exif: Int,
    private var maxSide: Int?,
    private var onSaved: (File) -> Unit,
    private var onSavedError: (Throwable) -> Unit
) {
    private val saveHandler = Handler()

    fun save() {
        saveInBackground(Runnable {
            runCatching {
                val bitmap = transformBitmap(exif, maxSide, result)
                FileOutputStream(photoFile).buffered().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                bitmap.recycle()
            }.onSuccess {
                onSaved.invoke(photoFile)
            }.onFailure {
                onSavedError.invoke(it)
            }
        })
    }

    @Synchronized
    private fun saveInBackground(runnable: Runnable) {
        saveHandler.post(runnable)
    }

    fun transformBitmap(exif: Int, maxSide: Int?, source: Bitmap): Bitmap {
        val bitmapSize = Size(source.width, source.height)
        val scaledSize = bitmapSize.scaleDown(maxSide)
        val needScale = scaledSize != bitmapSize
        val needRotate = exif != ExifInterface.ORIENTATION_UNDEFINED && exif != ExifInterface.ORIENTATION_NORMAL

        if (!needRotate && !needScale) {
            return source
        }
        val matrix = Matrix()
        if (needScale) {
            val sx = scaledSize.width / source.width.toFloat()
            val sy = scaledSize.height / source.height.toFloat()
            matrix.setScale(sx, sy)
        }
        if (needRotate) {
            when (exif) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> matrix.setRotate(270f)
            }
            when (exif) {
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale( -1f, 1f)
            }
        }

        val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        source.recycle()
        return bitmap
    }
}