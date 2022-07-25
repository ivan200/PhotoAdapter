package com.ivan200.photoadapter.utils

import android.os.Handler
import androidx.fragment.app.FragmentActivity
import com.ivan200.photoadapter.CameraBuilder
import com.otaliastudios.cameraview.PictureResult
import java.io.File
import kotlin.math.max

//
// Created by Ivan200 on 08.11.2019.
//


class ResultSaver(
    private var activity: FragmentActivity,
    private var result: PictureResult,
    private var cameraBuilder: CameraBuilder,
    private var onSaved: (ResultSaver) -> Unit
) {
    private var isFileSaved = false
    private var isThumbSaved = !cameraBuilder.hasThumbnails

    private val saveHandler = Handler()
    private val checkHandler = Handler()

    var photoFile: File
    var thumbsFile: File? = null

    init {
        val photoDir = ImageUtils.getPhotosDir(activity, cameraBuilder.photosPath)
        photoFile = ImageUtils.createImageFile(activity, photoDir)
        if (cameraBuilder.hasThumbnails) {
            val thumbsDir = ImageUtils.getThumbsDir(activity, cameraBuilder.thumbnailsPath)
            thumbsFile = File(thumbsDir, photoFile.name)
        }
    }

    fun save() {
        saveInBackground(Runnable {
            photoFile = SavePictureResultToFile(photoFile).invoke(result)
            isFileSaved = true

            if(!isThumbSaved) {
                val imageMaxSize = cameraBuilder.maxImageSize ?: max(result.size.width, result.size.height)
                thumbsFile = SaveThumbnailToFile(thumbsFile, imageMaxSize / 3).invoke(photoFile)
                isThumbSaved = true
            }
            onSaved.invoke(this)
        })
    }

    @Synchronized
    private fun saveInBackground(runnable: Runnable) {
        saveHandler.post(runnable)
    }

    @Synchronized
    private fun checkInBackground(runnable: Runnable) {
        checkHandler.post(runnable)
    }
}