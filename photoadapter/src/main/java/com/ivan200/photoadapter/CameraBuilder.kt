package com.ivan200.photoadapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntRange
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import com.ivan200.photoadapter.utils.CameraImplSelector
import com.ivan200.photoadapter.utils.SaveTo
import kotlinx.parcelize.Parcelize

/**
 * Camera builder
 *
 * @param facingBack          camera facing: back (normal) or front (selfie)
 * @param allowChangeCamera   allow to flip camera facing
 * @param allowPreviewResult  show result image after each photo taken
 * @param allowMultipleImages allow take more than one image per session
 * @param lockRotate          lock auto rotating activity to disable view recreating
 * @param fullScreenMode      full screen (16/9) or normal (4/3) mode
 * @param fillPreview         fill camera surfaceView into screen
 * @param allowToggleFit      allow toggle between fit preview/fill preview
 * @param showBackButton      show back button in top left corner
 * @param saveTo              where will the images be saved
 * @param maxWidth            preferred result image max width
 * @param maxHeight           preferred result image max height
 * @param useSnapshot         take picture from snapshot (faster, no sound)
 * @param blurOnSwitch        whenever switching cameras, show blurred image, instead of black screen
 * @param dialogTheme         customize alert dialog theme
 * @param outputJpegQuality   quality for saving jpeg image
 * @param cameraImplSelector  selector of camera implementation
 * @param flipFrontResult     does flipping of front image are enabled
 *
 * @author Ivan200
 * @since  11.10.2019
 */
@Suppress("unused")
@Parcelize
data class CameraBuilder constructor(
    var facingBack: Boolean = true,
    var allowChangeCamera: Boolean = true,
    var allowPreviewResult: Boolean = true,
    var allowMultipleImages: Boolean = true,
    var lockRotate: Boolean = true,
    var fullScreenMode: Boolean = false,
    var fillPreview: Boolean = true,
    var allowToggleFit: Boolean = true,
    var showBackButton: Boolean = true,
    var saveTo: SaveTo = SaveTo.OnlyInternal,
    var maxWidth: Int? = null,
    var maxHeight: Int? = null,
    var useSnapshot: Boolean = true,
    var blurOnSwitch: Boolean = true,
    var dialogTheme: Int = 0,
    @IntRange(from = 1, to = 100)
    var outputJpegQuality: Int? = null,
    var cameraImplSelector: CameraImplSelector = CameraImplSelector.Camera2FromApi21,
    var flipFrontResult: Boolean = true
) : Parcelable {

    /** Explicit "empty" constructor, as seen by Java. */
    constructor() : this(facingBack = true)

    /**
     * Register a request to start an activity for result from fragment
     *
     * @param fragment fragment for register request
     * @param callback callback on result of taking picture
     */
    fun registerForResult(fragment: Fragment, callback: ImagesTakenCallback): ActivityResultLauncher<Intent> {
        return fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), Callback(callback))
    }

    /**
     * Register a request to start an activity for result from activity
     *
     * @param activity activity for register request
     * @param callback callback on result of taking picture
     */
    fun registerForResult(
        activity: ComponentActivity,
        callback: ImagesTakenCallback
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(), Callback(callback))
    }

    /**
     * Get intent of camera activity to launch it
     *
     * @param context for creating intent
     */
    fun getTakePictureIntent(context: Context): Intent {
        return CameraActivity.getIntent(context, this)
    }

    /**
     * Manually call on onActivityResult to get images uri
     *
     * @param resultCode the integer result code returned by CameraActivity through its setResult().
     * @param data       an Intent, which can return result data to the caller
     * @param callback   callback on result of taking picture
     */
    fun onActivityResult(resultCode: Int, data: Intent?, callback: ImagesTakenCallback) {
        Callback(callback).onActivityResult(ActivityResult(resultCode, data))
    }

    private inner class Callback(private val callback: ImagesTakenCallback) : ActivityResultCallback<ActivityResult> {
        override fun onActivityResult(result: ActivityResult) {
            when (result.resultCode) {
                Activity.RESULT_CANCELED -> callback.onCancel()
                Activity.RESULT_OK -> {
                    val uris = result.data
                        ?.let { IntentCompat.getParcelableArrayExtra(it, CameraActivity.photosExtraName, Uri::class.java) }
                        ?.filterIsInstance<Uri>().orEmpty()
                    callback.onImagesTaken(uris)
                }

                else -> Unit
            }
        }
    }

    /**
     * Callback on result of taking pictures
     */
    interface ImagesTakenCallback {
        /**
         * Callback on images was taken
         *
         * @param images list of images uri
         */
        fun onImagesTaken(images: List<Uri>)

        /** Callback on back button was pressed */
        fun onCancel() {
            //Empty by default
        }
    }
}
