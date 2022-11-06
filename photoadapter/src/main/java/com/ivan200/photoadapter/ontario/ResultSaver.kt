package com.ivan200.photoadapter.ontario

import android.os.Handler
import androidx.exifinterface.media.ExifInterface
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import java.io.File
import java.io.FileOutputStream

//
// Created by Ivan200 on 08.11.2019.
//

class ResultSaver(
    val photoFile: File,
    val flipFrontRequested: Boolean,
    private var result: PictureResult,
    private var onSaved: (File) -> Unit,
    private var onSavedError: (Throwable) -> Unit
) {
    private val saveHandler = Handler()

    fun save() {
        saveInBackground {
            runCatching {
                FileOutputStream(photoFile).buffered().use {
                    it.write(result.data)
                }
                if (flipFrontRequested &&
                    !result.isSnapshot &&
                    result.facing == Facing.FRONT
                ) {
                    val exif = when (result.rotation) {
                        90 -> ExifInterface.ORIENTATION_TRANSPOSE
                        180 -> ExifInterface.ORIENTATION_FLIP_VERTICAL
                        270 -> ExifInterface.ORIENTATION_TRANSVERSE
                        else -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
                    }

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
