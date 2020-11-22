package com.ivan200.photoadapter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ivan200.photoadapter.utils.*


@Suppress("MemberVisibilityCanBePrivate")
class CameraActivity : AppCompatActivity() {

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(this@CameraActivity).get(CameraViewModel::class.java)
    }

    val container: View get() = findViewById(R.id.content_frame)
    val frameCamera: FrameLayout get() = findViewById(R.id.frame_camera)
    val frameGallery: FrameLayout get() = findViewById(R.id.frame_gallery)

    val cameraBuilder: CameraBuilder by lazy { intent.getParcelableExtra(KEY_CAMERA_BUILDER)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (cameraBuilder.fullScreenMode) R.style.AppThemePhoto_FullScreen else R.style.AppThemePhoto)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            supportFragmentManager.fragments.forEach {
                if (it is ApplyInsetsListener) it.onApplyInsets(insets)
            }
            return@setOnApplyWindowInsetsListener insets.consumeSystemWindowInsets()
        }

        cameraViewModel.showCamera.observe(this, changeFragmentsObserver)
        cameraViewModel.curPageLoaded.observe(this, pageLoadedObserver)
        cameraViewModel.success.observe(this, successCalledObserver)
    }

    var pageLoadedObserver = Observer<PictureInfo> {
        if (cameraBuilder.previewImage) {
            cameraViewModel.changeFragment(showCamera = false)
        }
    }

    var changeFragmentsObserver = Observer<Boolean> { showCamera ->
        if (showCamera) showCamera() else showGallery()
    }

    fun showCamera() {
        frameCamera.show()
        frameGallery.invisible()
    }

    fun showGallery() {
        frameGallery.show()
        frameCamera.invisible()
    }

    override fun onStart() {
        super.onStart()

        if (cameraBuilder.fullScreenMode) {
            container.postDelayed({
                container.hideSystemUI()
            }, IMMERSIVE_FLAG_TIMEOUT)
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        if (!cameraViewModel.showCamera.value!!) {
            cameraViewModel.backPressed()
        } else {
            if (cameraViewModel.pictures.value!!.isNotEmpty()) {
                showConfirmSaveDialog(cameraViewModel::success, this::cancelActivity)
            } else {
                cancelActivity()
            }
        }
    }

    fun showConfirmSaveDialog(onYes: () -> Unit, onNo: () -> Unit) {
        AlertDialog.Builder(this, cameraBuilder.dialogTheme)
            .setTitle(R.string.title_confirm)
            .setMessage(R.string.save_photos_dialog)
            .setPositiveButton(R.string.button_yes) { dialog, id ->
                onYes.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.button_no) { dialog, id ->
                onNo.invoke()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun cancelActivity() {
        cameraViewModel.deleteAllFiles()

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    var successCalledObserver = Observer<Unit?> {
        cameraViewModel.pictures.value!!.let { pics ->
            val resultPhotos = pics.map { it.file.absolutePath }.toTypedArray()
            val resultThumbs = pics.map { it.thumbFile?.absolutePath }.toTypedArray()
            intent.putExtra(photosExtraName, resultPhotos)
            intent.putExtra(thumbsExtraName, resultThumbs)

            try {
                ImageUtils.copyImagesToGallery(this, pics.map { it.file }.toTypedArray(), cameraBuilder.galleryName)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraViewModel.volumeKeyPressed()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        cameraViewModel.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraViewModel.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_CAMERA_BUILDER = "KEY_CAMERA_BUILDER"

        fun getIntent(context: Context, builder: CameraBuilder) =
            Intent(context, CameraActivity::class.java)
                .putExtra(KEY_CAMERA_BUILDER, builder)

        const val photosExtraName = "photos"
        const val thumbsExtraName = "thumbs"
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }
}