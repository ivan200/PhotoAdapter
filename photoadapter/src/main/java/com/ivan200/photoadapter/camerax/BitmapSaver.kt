package com.ivan200.photoadapter.camerax

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.exifinterface.media.ExifInterface
import com.ivan200.photoadapter.utils.ImageUtils.scaleBitmap
import java.io.File
import java.io.FileOutputStream

//
// Created by Ivan200 on 08.11.2019.
//

class BitmapSaver(
    val photoFile: File,
    private val result: Bitmap,
    private val exif: Int,
    private val maxWidth: Int?,
    private val maxHeight: Int?,
    private val jpegQuality: Int,
    private val onSaved: (File) -> Unit,
    private val onSavedError: (Throwable) -> Unit
) {
    private val saveHandler = Handler(Looper.myLooper()!!)

    fun save() {
        saveInBackground {
            runCatching {
                val bitmap = scaleBitmap(maxWidth, maxHeight, result)
                FileOutputStream(photoFile).buffered().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it)
                }
                bitmap.recycle()
                if (exif != ExifInterface.ORIENTATION_UNDEFINED && exif != ExifInterface.ORIENTATION_NORMAL) {
                    val exifInterface = ExifInterface(photoFile)
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, exif.toString())
                    exifInterface.saveAttributes()
                }
            }.onSuccess {
                onSaved.invoke(photoFile)
            }.onFailure {
                onSavedError.invoke(it)
            }
        }
    }

    @Synchronized
    private fun saveInBackground(runnable: Runnable) {
        saveHandler.post(runnable)
    }
}
