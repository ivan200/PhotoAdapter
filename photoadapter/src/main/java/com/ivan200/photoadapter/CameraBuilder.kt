package com.ivan200.photoadapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.annotation.IntRange
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.utils.CameraImplSelector
import com.ivan200.photoadapter.utils.SaveTo
import kotlinx.parcelize.Parcelize

/**
 * Camera builder
 *
 * @param facingBack             camera facing: back (normal) or front (selfie)
 * @param changeCameraAllowed    allow to flip camera facing
 * @param previewImage           show result image after each photo taken
 * @param allowMultipleImages    allow take more than one image per session
 * @param lockRotate             lock auto rotating activity to disable view recreating
 * @param fullScreenMode         full screen (16/9) or normal (4/3) mode
 * @param fitMode                fit camera surfaceView into screen
 * @param saveTo                 where will the images be saved
 * @param maxWidth               preferred result image max width
 * @param maxHeight              preferred result image max height
 * @param useSnapshot            take picture from snapshot (faster, no sound)
 * @param requestCode            specify code for starting activity
 * @param dialogTheme            customize alert dialog theme
 * @param outputJpegQuality      quality for saving jpeg image
 * @param cameraImplSelector     selector of camera implementation
 * @param flipFrontResult        does flipping of front image are enabled
 *
 * Created by Ivan200 on 11.10.2019.
 */
@Suppress("unused")
@Parcelize
data class CameraBuilder constructor(
    var facingBack: Boolean = true,
    var changeCameraAllowed: Boolean = true,
    var previewImage: Boolean = true,
    var allowMultipleImages: Boolean = true,
    var lockRotate: Boolean = true,
    var fullScreenMode: Boolean = false,
    var fitMode: Boolean = false,
    var saveTo: SaveTo = SaveTo.OnlyInternal,
    var maxWidth: Int? = null,
    var maxHeight: Int? = null,
    var useSnapshot: Boolean = true,
    var requestCode: Int = 0,
    var dialogTheme: Int = 0,
    @IntRange(from = 1, to = 100)
    var outputJpegQuality: Int? = null,
    var cameraImplSelector: CameraImplSelector = CameraImplSelector.Camera2IfAnyFullSupport,
    var flipFrontResult: Boolean = true
) : Parcelable {

    constructor() : this(facingBack = true) // explicit "empty" constructor, as seen by Java.

    fun setCameraFacingBack(facingBack: Boolean) = apply { this.facingBack = facingBack }
    fun setChangeCameraAllowed(changeCameraAllowed: Boolean) = apply { this.changeCameraAllowed = changeCameraAllowed }
    fun setAllowMultipleImages(allowMultipleImages: Boolean) = apply { this.allowMultipleImages = allowMultipleImages }
    fun setLockRotate(lockRotate: Boolean) = apply { this.lockRotate = lockRotate }
    fun setFullScreenMode(fullScreenMode: Boolean) = apply { this.fullScreenMode = fullScreenMode }
    fun setFitMode(fitMode: Boolean) = apply { this.fitMode = fitMode }
    fun setPreviewImage(previewImage: Boolean) = apply { this.previewImage = previewImage }
    fun setMaxWidth(maxWidth: Int) = apply { this.maxWidth = maxWidth }
    fun setMaxHeight(maxHeight: Int) = apply { this.maxHeight = maxHeight }
    fun setUseSnapshot(useSnapshot: Boolean) = apply { this.useSnapshot = useSnapshot }
    fun setCameraImplSelector(cameraImplSelector: CameraImplSelector) = apply { this.cameraImplSelector = cameraImplSelector }
    fun setRequestCode(requestCode: Int) = apply { this.requestCode = requestCode }
    fun setDialogTheme(@AnyRes dialogTheme: Int) = apply { this.dialogTheme = dialogTheme }
    fun setOutputJpegQuality(@IntRange(from = 1, to = 100) outputJpegQuality: Int) = apply { this.outputJpegQuality = outputJpegQuality }
    fun setFlipFrontResult(flipFrontalPicture: Boolean) = apply { this.flipFrontResult = flipFrontalPicture }
    fun setSaveTo(saveTo: SaveTo) = apply { this.saveTo = saveTo }

    fun start(activity: Activity) {
        activity.startActivityForResult(CameraActivity.getIntent(activity, this), getCode())
    }

    // TODO Добавить нормальный старт активити
    fun start(fragment: Fragment) {
        fragment.startActivityForResult(CameraActivity.getIntent(fragment.requireContext(), this), getCode())
    }

    private fun getCode() = if (requestCode == 0) REQUEST_IMAGE_CAPTURE else requestCode

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, onSuccess: Consumer<List<Uri>>, onCancel: Runnable? = null) {
        if (requestCode == getCode()) {
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

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 7411 // random number
    }
}
