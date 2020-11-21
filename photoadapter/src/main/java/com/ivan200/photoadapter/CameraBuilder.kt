package com.ivan200.photoadapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.AnyRes
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.utils.PhotoChecker
import kotlinx.android.parcel.Parcelize
import java.io.File

//
// Created by Ivan200 on 11.10.2019.
//

@Suppress("unused")
@Parcelize
data class CameraBuilder(
    var facingBack: Boolean = true,             //camera facing: back (normal) or front (selfie)
    var changeCameraAllowed: Boolean = true,    //allow to flip camera facing
    var previewImage: Boolean = true,           //show result image after each photo taken
    var allowMultipleImages: Boolean = true,    //allow take more than one image per session
    var lockRotate: Boolean = true,             //lock auto rotating activity to disable view recreating
    var galleryName: String? = null,            //allow to save images to phone gallery with specified name
    var fullScreenMode: Boolean = false,        //full screen (16/9) or normal (4/3) mode
    var hasThumbnails: Boolean = false,         //allow to save thumbnails along with photos
    var thumbnailsPath: File? = null,           //specify thumbnails path, if need
    var photosPath: File? = null,               //specify photos path, if need
    var maxImageSize: Int? = 1200,              //preferred result image size
    var useSnapshot: Boolean = true,            //take picture from snapshot (faster, no sound)
    var requestCode: Int = 0,                   //specify code for starting activity
    var fixJpegBytes: Boolean = false,          //extra fix first and last 2 bytes of saved jpeg images
    var photoChecker: PhotoChecker? = null,     //check photo if it must be approved (by tensorflow or other)
    //customize alert dialog theme
    var dialogTheme: Int = 0
) : Parcelable {

    fun withCameraFacingBack(facingBack: Boolean) = apply { this.facingBack = facingBack }
    fun withChangeCameraAllowed(changeCameraAllowed: Boolean) = apply { this.changeCameraAllowed = changeCameraAllowed }
    fun withAllowMultipleImages(allowMultipleImages: Boolean) = apply { this.allowMultipleImages = allowMultipleImages }
    fun withLockRotate(lockRotate: Boolean) = apply { this.lockRotate = lockRotate }
    fun withSavePhotoToGallery(galleryName: String?) = apply { this.galleryName = galleryName }
    fun withFullScreenMode(fullScreenMode: Boolean) = apply { this.fullScreenMode = fullScreenMode }
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
        activity.startActivityForResult(getIntent(activity), getCode())
    }

    fun start(fragment: Fragment) {
        fragment.startActivityForResult(getIntent(fragment.context), getCode())
    }

    private fun getCode() = if (requestCode == 0) REQUEST_IMAGE_CAPTURE else requestCode

    private fun getIntent(context: Context?) =
        Intent(context, CameraActivity::class.java).putExtra(this::class.java.simpleName, this)

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, onSuccess: Consumer<List<String>>, onCancel: Runnable? = null) {
        if (requestCode == getCode()) {
            Companion.onActivityResult(resultCode, data, onSuccess, onCancel)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 7411   //number generated randomly

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