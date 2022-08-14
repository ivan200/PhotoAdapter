package com.ivan200.photoadapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.annotation.IntRange
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.utils.SaveTo
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Camera builder
 *
 * @property facingBack            camera facing: back (normal) or front (selfie)
 * @property changeCameraAllowed   allow to flip camera facing
 * @property previewImage          show result image after each photo taken
 * @property allowMultipleImages   allow take more than one image per session
 * @property lockRotate            lock auto rotating activity to disable view recreating
 * @property galleryName           allow to save images to phone gallery with specified name
 * @property fullScreenMode        full screen (16/9) or normal (4/3) mode
 * @property fitMode               fit camera surfaceView into screen
 * @property photosPath            specify photos path, if need
 * @property maxImageSize          preferred result image size
 * @property useSnapshot           take picture from snapshot (faster, no sound)
 * @property requestCode           specify code for starting activity
 * @property dialogTheme           customize alert dialog theme
 * @property outputJpegQuality     quality for saving jpeg image
 * @property forceUseCamera1Impl   force Camera1 implementation
 * Created by Ivan200 on 11.10.2019.
 */
@Suppress("unused")
@Parcelize
data class CameraBuilder private constructor(
    var facingBack: Boolean = true,
    var changeCameraAllowed: Boolean = true,
    var previewImage: Boolean = true,
    var allowMultipleImages: Boolean = true,
    var lockRotate: Boolean = true,
    var fullScreenMode: Boolean = false,
    var fitMode: Boolean = false,
    var saveTo: SaveTo = SaveTo.OnlyInternal,
    var maxImageSize: Int? = null,
    var useSnapshot: Boolean = true,
    var requestCode: Int = 0,
    var dialogTheme: Int = 0,
    @IntRange(from = 1, to = 100)
    var outputJpegQuality: Int? = null,
    var forceUseCamera1Impl: Boolean = false,
) : Parcelable {

    constructor() : this(facingBack = true) //explicit "empty" constructor, as seen by Java.

    fun setCameraFacingBack(facingBack: Boolean) = apply { this.facingBack = facingBack }
    fun setChangeCameraAllowed(changeCameraAllowed: Boolean) = apply { this.changeCameraAllowed = changeCameraAllowed }
    fun setAllowMultipleImages(allowMultipleImages: Boolean) = apply { this.allowMultipleImages = allowMultipleImages }
    fun setLockRotate(lockRotate: Boolean) = apply { this.lockRotate = lockRotate }
    fun setFullScreenMode(fullScreenMode: Boolean) = apply { this.fullScreenMode = fullScreenMode }
    fun setFitMode(fitMode: Boolean) = apply { this.fitMode = fitMode }
    fun setPreviewImage(previewImage: Boolean) = apply { this.previewImage = previewImage }
    fun setMaxImageSize(maxImageSize: Int) = apply { this.maxImageSize = maxImageSize }
    fun setUseSnapshot(useSnapshot: Boolean) = apply { this.useSnapshot = useSnapshot }
    fun setForceUseCamera1Impl(forceUseCamera1Impl: Boolean) = apply { this.forceUseCamera1Impl = forceUseCamera1Impl }
    fun setRequestCode(requestCode: Int) = apply { this.requestCode = requestCode }
    fun setDialogTheme(@AnyRes dialogTheme: Int) = apply { this.dialogTheme = dialogTheme }
    fun setOutputJpegQuality(@IntRange(from = 1, to = 100) outputJpegQuality: Int) = apply { this.outputJpegQuality = outputJpegQuality }
    fun setSaveTo(saveTo: SaveTo) = apply { this.saveTo = saveTo }

    fun start(activity: Activity) {
        activity.startActivityForResult(CameraActivity.getIntent(activity, this), getCode())
    }

    //TODO Добавить нормальный старт активити
    fun start(fragment: Fragment) {
        fragment.startActivityForResult(CameraActivity.getIntent(fragment.requireContext(), this), getCode())
    }

    private fun getCode() = if (requestCode == 0) REQUEST_IMAGE_CAPTURE else requestCode

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, onSuccess: Consumer<List<Uri>>, onCancel: Runnable? = null) {
        if (requestCode == getCode()) {
            Companion.onActivityResult(resultCode, data, onSuccess, onCancel)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 7411   //random number

        @Suppress("UNCHECKED_CAST")
        fun onActivityResult(resultCode: Int, data: Intent?, onSuccess: Consumer<List<Uri>>, onCancel: Runnable? = null) {
            when (resultCode) {
                Activity.RESULT_CANCELED -> onCancel?.run()
                Activity.RESULT_OK -> {
                    val uris = data?.extras?.get(CameraActivity.photosExtraName)?.let {
                        (it as Array<*>).toList().filterIsInstance<Uri>()
                    }
                    onSuccess.accept(uris)
                }
            }
        }
    }
}