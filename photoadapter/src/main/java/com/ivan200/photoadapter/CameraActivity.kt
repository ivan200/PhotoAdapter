package com.ivan200.photoadapter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.permission.PermissionsDelegate
import com.ivan200.photoadapter.permission.ResultType
import com.ivan200.photoadapter.utils.ApplyInsetsListener
import com.ivan200.photoadapter.utils.SaveUtils
import com.ivan200.photoadapter.utils.SavedStateUtils.lazySavedStateViewModel
import com.ivan200.photoadapter.utils.hideSystemUI

@Suppress("MemberVisibilityCanBePrivate")
class CameraActivity : AppCompatActivity() {

    private val cameraViewModel: CameraViewModel by lazySavedStateViewModel(this, this) {
        CameraViewModel(it)
    }

    val container: View get() = findViewById(R.id.content_frame)
    val frameCamera: FrameLayout get() = findViewById(R.id.frame_camera)
    val frameGallery: FrameLayout get() = findViewById(R.id.frame_gallery)

    val cameraBuilder: CameraBuilder by lazy {
        IntentCompat.getParcelableExtra(intent, KEY_CAMERA_BUILDER, CameraBuilder::class.java)!!
    }

    lateinit var permissionsDelegate: PermissionsDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (cameraBuilder.fullScreenMode) R.style.AppThemePhoto_FullScreen else R.style.AppThemePhoto)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
            supportFragmentManager.fragments.forEach {
                if (it is ApplyInsetsListener) it.onApplyInsets(insets)
            }
            return@setOnApplyWindowInsetsListener insets
        }

        cameraViewModel.fragmentState.observe(this, changeFragmentsObserver)
        cameraViewModel.curPageLoaded.observe(this, pageLoadedObserver)
        cameraViewModel.success.observe(this, successCalledObserver)

        permissionsDelegate = PermissionsDelegate(this, savedInstanceState) {
            when (it) {
                is ResultType.Denied -> cancel(false)
                ResultType.Allow.AlreadyHas -> Unit
                is ResultType.Allow -> cameraViewModel.restartCamera()
            }
        }
        permissionsDelegate.initWithBuilder(cameraBuilder)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@CameraActivity.handleOnBackPressed()
            }
        })
    }

    val pageLoadedObserver = Observer<PictureInfo> {
        if (cameraBuilder.allowPreviewResult && cameraViewModel.fragmentState.value == FragmentChangeState.WAITING_FOR_IMAGE) {
            cameraViewModel.changeState(FragmentChangeState.GALLERY)
        }
    }

    val changeFragmentsObserver = Observer<FragmentChangeState> {
        when (it) {
            FragmentChangeState.CAMERA -> showCamera()
            FragmentChangeState.WAITING_FOR_IMAGE -> Unit
            FragmentChangeState.GALLERY -> showGallery()
        }
    }

    fun handleOnBackPressed() {
        if (cameraViewModel.fragmentState.value!! == FragmentChangeState.GALLERY) {
            cameraViewModel.backPressed()
        } else {
            cancel(true)
        }
    }

    fun showCamera() {
        frameCamera.isVisible = true
        frameGallery.isInvisible = true
    }

    fun showGallery() {
        frameGallery.isVisible = true
        frameCamera.isInvisible = true
    }

    override fun onStart() {
        super.onStart()
        permissionsDelegate.queryPermissionsOnStart()

        if (cameraBuilder.fullScreenMode) {
            this.hideSystemUI()
        }
    }

    fun cancel(allowDismissDialog: Boolean) {
        if (cameraViewModel.pictures.value!!.isNotEmpty()) {
            showConfirmSaveDialog(cameraViewModel::success, this::cancelActivity, allowDismissDialog)
        } else {
            cancelActivity()
        }
    }

    fun showConfirmSaveDialog(onYes: () -> Unit, onNo: () -> Unit, allowDismiss: Boolean) {
        val dialog = AlertDialog.Builder(this, cameraBuilder.dialogTheme)
            .setTitle(R.string.title_confirm)
            .setMessage(R.string.save_photos_dialog)
            .setPositiveButton(R.string.button_yes) { dialog, _ ->
                onYes.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.button_no) { dialog, _ ->
                onNo.invoke()
                dialog.dismiss()
            }
            .setCancelable(allowDismiss)
            .create()
            .apply {
                setCancelable(allowDismiss)
            }
        dialog.show()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    fun cancelActivity() {
        cameraViewModel.deleteAllFiles()

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    val successCalledObserver = Observer<Unit?> {
        cameraViewModel.pictures.value!!.let { pics ->
            val uris = SaveUtils.moveImagesToGallery(this, pics.map { it.file }, cameraBuilder.saveTo)
            intent.putExtra(photosExtraName, uris.toTypedArray())
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        permissionsDelegate.saveInstanceState(outState)
    }

    companion object {
        private const val KEY_CAMERA_BUILDER = "KEY_CAMERA_BUILDER"

        internal fun getIntent(context: Context, builder: CameraBuilder) =
            Intent(context, CameraActivity::class.java)
                .putExtra(KEY_CAMERA_BUILDER, builder)

        const val photosExtraName = "photos"
    }
}
