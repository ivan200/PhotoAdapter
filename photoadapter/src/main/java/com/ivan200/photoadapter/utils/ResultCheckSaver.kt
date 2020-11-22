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


class ResultCheckSaver(
    private var activity: FragmentActivity,
    private var result: PictureResult,
    private var cameraBuilder: CameraBuilder,
    private var onCheckSaved: (ResultCheckSaver) -> Unit
) {
    private var isFileSaved = false
    private var isThumbSaved = !cameraBuilder.hasThumbnails
    private var isPhotoChecked = cameraBuilder.photoChecker == null

    var checkResult = true

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

    fun checkSave() {
        saveInBackground(Runnable {
            photoFile = SavePictureResultToFile(photoFile, cameraBuilder.fixJpegBytes).invoke(result)
            isFileSaved = true
            if (isPhotoChecked && !checkResult) {
                isThumbSaved = true
                onSomeThing()
                return@Runnable
            }
            val imageMaxSize = cameraBuilder.maxImageSize ?: max(result.size.width, result.size.height)
            thumbsFile = SaveThumbnailToFile(thumbsFile, imageMaxSize / 3).invoke(photoFile)
            isThumbSaved = true
            onSomeThing()
        })
        if (cameraBuilder.photoChecker != null) {
            result.toBitmap {
                checkInBackground {
                    if (it != null) {
                        checkResult = cameraBuilder.photoChecker!!.checkPhoto(activity, it)
                        it.recycle()
                        isPhotoChecked = true
                        onSomeThing()
                    }
                }
            }
        } else {
            isPhotoChecked = true
        }
    }

    private fun onSomeThing() {
        if (isPhotoChecked && !checkResult) {
            photoFile.applyIf(isFileSaved) {
                if (exists()) delete()
            }
            thumbsFile?.applyIf(isThumbSaved) {
                if (exists()) delete()
            }
        }

        if (isFileSaved && isThumbSaved && isPhotoChecked) {
            onCheckSaved.invoke(this)
        }
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