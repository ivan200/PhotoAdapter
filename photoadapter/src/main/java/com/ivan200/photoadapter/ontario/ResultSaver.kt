package com.ivan200.photoadapter.ontario

import android.os.Handler
import com.otaliastudios.cameraview.PictureResult
import java.io.File
import java.io.FileOutputStream

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