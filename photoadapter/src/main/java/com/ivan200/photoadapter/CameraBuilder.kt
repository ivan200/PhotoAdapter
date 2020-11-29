package com.ivan200.photoadapter

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.utils.PhotoChecker
import kotlinx.android.parcel.Parcelize
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
 * @property hasThumbnails         allow to save thumbnails along with photos
 * @property thumbnailsPath        specify thumbnails path, if need
 * @property photosPath            specify photos path, if need
 * @property maxImageSize          preferred result image size
 * @property useSnapshot           take picture from snapshot (faster, no sound)
 * @property requestCode           specify code for starting activity
 * @property fixJpegBytes          extra fix first and last 2 bytes of saved jpeg images
 * @property photoChecker          check photo if it must be approved (by tensorflow or other)
 * @property dialogTheme           customize alert dialog theme
 *
 * Created by Ivan200 on 11.10.2019.
 */
@Suppress("unused")
@Parcelize
data class CameraBuilder(
    var facingBack: Boolean = true,
    var changeCameraAllowed: Boolean = true,
    var previewImage: Boolean = true,
    var allowMultipleImages: Boolean = true,
    var lockRotate: Boolean = true,
    var galleryName: String? = null,
    var fullScreenMode: Boolean = false,
    var fitMode: Boolean = false,
    var hasThumbnails: Boolean = false,
    var thumbnailsPath: File? = null,
    var photosPath: File? = null,
    var maxImageSize: Int? = 1920,
    var useSnapshot: Boolean = true,
    var requestCode: Int = 0,
    var fixJpegBytes: Boolean = false,
    var photoChecker: PhotoChecker? = null,
    var dialogTheme: Int = 0
) : Parcelable {

    constructor() : this(facingBack = true) //explicit "empty" constructor, as seen by Java.

    fun withCameraFacingBack(facingBack: Boolean) = apply { this.facingBack = facingBack }
    fun withChangeCameraAllowed(changeCameraAllowed: Boolean) = apply { this.changeCameraAllowed = changeCameraAllowed }
    fun withAllowMultipleImages(allowMultipleImages: Boolean) = apply { this.allowMultipleImages = allowMultipleImages }
    fun withLockRotate(lockRotate: Boolean) = apply { this.lockRotate = lockRotate }
    fun withSavePhotoToGallery(galleryName: String?) = apply { this.galleryName = galleryName }
    fun withFullScreenMode(fullScreenMode: Boolean) = apply { this.fullScreenMode = fullScreenMode }
    fun withFitMode(fitMode: Boolean) = apply { this.fitMode = fitMode }
    fun withPreviewImage(previewImage: Boolean) = apply { this.previewImage = previewImage }
    fun withThumbnails(hasThumbnails: Boolean) = apply { this.hasThumbnails = hasThumbnails }
    fun withThumbnailsPath(thumbnailsPath: File) = apply { this.thumbnailsPath = thumbnailsPath }
    fun withPhotosPath(photosPath: File) = apply { this.photosPath = photosPath }
    fun withMaxImageSize(maxImageSize: Int) = apply { this.maxImageSize = maxImageSize }
    fun withUseSnapshot(useSnapshot: Boolean) = apply { this.useSnapshot = useSnapshot }
    fun withRequestCode(requestCode: Int) = apply { this.requestCode = requestCode }
    fun withFixJpegBytes(fixJpegBytes: Boolean) = apply { this.fixJpegBytes = fixJpegBytes }
    fun withDialogTheme(@AnyRes dialogTheme: Int) = apply { this.dialogTheme = dialogTheme }
    fun withPhotoChecker(photoChecker: PhotoChecker?) = apply { this.photoChecker = photoChecker }

    fun start(activity: Activity) {
        activity.startActivityForResult(CameraActivity.getIntent(activity, this), getCode())
    }

    fun start(fragment: Fragment) {
        fragment.startActivityForResult(CameraActivity.getIntent(fragment.requireContext(), this), getCode())
    }

    private fun getCode() = if (requestCode == 0) REQUEST_IMAGE_CAPTURE else requestCode


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, onSuccess: Consumer<List<String>>, onCancel: Runnable? = null) {
        if (requestCode == getCode()) {
            Companion.onActivityResult(resultCode, data, onSuccess, onCancel)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 7411   //random number

        fun onActivityResult(resultCode: Int, data: Intent?, onSuccess: Consumer<List<String>>, onCancel: Runnable? = null) {
            when (resultCode) {
                Activity.RESULT_CANCELED -> onCancel?.run()
                Activity.RESULT_OK -> {
                    val stringArrayExtra = data?.getStringArrayExtra(CameraActivity.photosExtraName)
                    onSuccess.accept(stringArrayExtra?.toList() ?: arrayListOf())
                }
            }
        }
    }
}