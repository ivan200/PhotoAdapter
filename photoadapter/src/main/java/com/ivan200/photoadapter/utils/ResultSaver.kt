package com.ivan200.photoadapter.utils

import android.os.Handler
import androidx.fragment.app.FragmentActivity
import com.ivan200.photoadapter.CameraBuilder
import com.otaliastudios.cameraview.PictureResult
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

//
// Created by Ivan200 on 08.11.2019.
//


class ResultSaver(
    val photoFile: File,
    private var result: PictureResult,
    private var onSaved: (File) -> Unit,
    private var onSavedError: (Throwable) -> Unit
) {
    private val saveHandler = Handler()

    fun save() {
        saveInBackground(Runnable {
            runCatching {
                FileOutputStream(photoFile).buffered().use {
                    it.write(result.data)
                }
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
}