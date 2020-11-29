package com.ivan200.photoadapter.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ivan200.photoadapter.*
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.utils.*
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Engine
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash


//
// Created by Ivan200 on 15.10.2019.
//
class CameraFragment : Fragment(R.layout.fragment_camera), ApplyInsetsListener {
    private val flashView get() = requireView().findViewById<View>(R.id.flashView)
    private val cameraView get() = requireView().findViewById<CameraView>(R.id.cameraView)
    private val cameraFrame get() = requireView().findViewById<LinearLayout>(R.id.cameraFrame)
    private val statusView get() = requireView().findViewById<View>(R.id.statusView)
    private val switchCamera get() = requireView().findViewById<ImageButton>(R.id.switch_camera)
    private val capture get() = requireView().findViewById<ImageButton>(R.id.capture)
    private val torchSwitch get() = requireView().findViewById<ImageButton>(R.id.torch_switch)
    private val actionLayout get() = requireView().findViewById<RelativeLayout>(R.id.action_layout)
    private val resultImage get() = requireView().findViewById<ImageButton>(R.id.result)

    private var toast: Toast? = null

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(activity as CameraActivity).get(CameraViewModel::class.java)
    }

    private var supportedFlash: List<Flash> = listOf(Flash.OFF)
    private var currentFlash = Flash.OFF
    private lateinit var cameraBuilder: CameraBuilder

    private var insets: WindowInsetsCompat? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraBuilder = (activity as? CameraActivity)?.cameraBuilder ?: CameraBuilder()

        cameraViewModel.pictures.observeVal(requireActivity()) {
            if (!cameraBuilder.previewImage) {
                if (!cameraBuilder.allowMultipleImages && it.isNotEmpty()) {
                    cameraViewModel.success()
                } else {
                    loadThumbImage(it.firstOrNull())
                }
            } else if (it.size == 0) {
                resultImage.hide()
            }
        }

        cameraViewModel.curPageLoaded.observeVal(requireActivity()) {
            if (cameraBuilder.previewImage) {
                loadThumbImage(it)
            }
        }

        if (cameraBuilder.fullScreenMode) {
            //Очищаем значение обозначающее что вью камеры должно быть над панелью кнопок
            (cameraFrame.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                arrayOf(RelativeLayout.BELOW, RelativeLayout.ABOVE, RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF).forEach {
                    addRule(it, 0)
                }
            }
        }
        if (cameraBuilder.fitMode) {
            cameraView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }



        cameraView.setLifecycleOwner(requireActivity())
        cameraView.addCameraListener(Listener())

        cameraBuilder.maxImageSize?.let {
            cameraView.snapshotMaxWidth = it
            cameraView.snapshotMaxHeight = it
        }
        if (ImageUtils.allowCamera2Support(requireActivity())) {
            cameraView.engine = Engine.CAMERA2
        }

        cameraView.facing = if (cameraBuilder.facingBack) Facing.BACK else Facing.FRONT
        switchCamera.showIf { cameraBuilder.changeCameraAllowed && ImageUtils.hasDifferentFacings(requireActivity()) }
        switchCamera.onClick {
            setFlash(Flash.OFF)
            cameraView.toggleFacing()
        }

        cameraViewModel.showCamera.observeVal(requireActivity()) {
            if (it) {
                setFlash(currentFlash)
            } else {
                setFlash(Flash.OFF)
            }
        }

        cameraViewModel.rotate.observeVal(requireActivity()) {
            rotateItems(it, capture, torchSwitch, resultImage, switchCamera)
        }

        cameraViewModel.volumeKeyPressed.observeVal(requireActivity()) {
            if (cameraViewModel.showCamera.value!!) {
                capture.simulateClick()
            }
        }

        insets?.let {
            setWindowInsets(it)
        }

        capture.onClick(this::takePicture)
        torchSwitch.onClick(this::nextFlash)
        resultImage.onClick(this::showGallery)
    }

    private fun loadThumbImage(pictureInfo: PictureInfo?) {
        if (cameraBuilder.allowMultipleImages && pictureInfo != null) {
            resultImage.show()
            Glide.with(requireActivity())
                .load(pictureInfo.thumbFile ?: pictureInfo.file)
                .apply(RequestOptions.circleCropTransform())
                .into(resultImage)
        } else {
            resultImage.hide()
        }
    }

    override fun onApplyInsets(insets: WindowInsetsCompat) {
        insets.let {
            this.insets = it
            if (view != null) {
                setWindowInsets(it)
            }
        }
    }

    private fun setWindowInsets(insets: WindowInsetsCompat) {
        actionLayout.padBottomViewWithInsets(insets)
        if (!cameraBuilder.fullScreenMode) {
            statusView.padTopViewWithInsets(insets)
        }
    }

    private fun takePicture() {
        if (cameraView.isTakingPicture) return
        toast?.cancel()
        if (cameraBuilder.useSnapshot) {
            cameraView.takePictureSnapshot()
        } else {
            cameraView.takePicture()
        }
        if (!cameraBuilder.previewImage && cameraBuilder.allowMultipleImages) {
            flashView.postDelayed({
                flashView.show()
                flashView.postDelayed({ flashView.hide() }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    private fun showGallery() {
        toast?.cancel()
        cameraViewModel.changeFragment(false)
    }

    private fun nextFlash() {
        var index = supportedFlash.indexOf(currentFlash) + 1
        if (index >= supportedFlash.size) index = 0

        setFlash(supportedFlash[index])
    }

    private fun setFlash(flash: Flash) {
        cameraView.flash = flash
        this.currentFlash = flash
        torchSwitch.setImageResource(
            when (flash) {
                Flash.OFF -> R.drawable.ic_photo_flash_off
                Flash.ON -> R.drawable.ic_photo_flash_on
                Flash.AUTO -> R.drawable.ic_photo_flash_auto
                Flash.TORCH -> R.drawable.ic_photo_flash_torch
            }
        )
    }

    override fun onStart() {
        super.onStart()
        if (cameraBuilder.lockRotate) {
            requireActivity().lockOrientation()
        }
    }

    override fun onStop() {
        super.onStop()

        toast?.cancel()
        if (cameraBuilder.lockRotate) {
            requireActivity().unlockOrientation()
        }
    }

    //вызывается после снятия фотки, при провале проверки картинки
    //для показа диалога что фото не очень
    @SuppressLint("InflateParams")
    fun onVerificationFailed() {
        toast = Toast.makeText(requireActivity(), "", Toast.LENGTH_SHORT)
            .apply {
                val padd = requireActivity().resources.getDimensionPixelOffset(R.dimen.height_toolbar)
                setGravity(Gravity.TOP, 0, padd)
                view = layoutInflater.inflate(R.layout.toast_custom, null)
            }
        toast?.show()
    }

    inner class Listener : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
            supportedFlash = options.supportedFlash.sortedBy { it.ordinal }.toList()
            torchSwitch.showIf { options.supportedFlash.size > 1 }
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            ResultCheckSaver(requireActivity(), result, cameraBuilder, this::onCheckSaved)
                .checkSave()
        }

        private fun onCheckSaved(resultCheckSaver: ResultCheckSaver) {
            if (!resultCheckSaver.checkResult) {
                onVerificationFailed()
            } else {
                cameraViewModel.onFileSaved(resultCheckSaver.photoFile, resultCheckSaver.thumbsFile)
            }
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            AlertDialog.Builder(activity, cameraBuilder.dialogTheme)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(exception.localizedMessage)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        //Rotate items on screen if orientation locked
        override fun onOrientationChanged(orientation: Int) {
            super.onOrientationChanged(orientation)
            if (!cameraBuilder.lockRotate) return

            val invertAngle = when (orientation) {
                90 -> 270; 270 -> 90; else -> orientation
            }
            var rotAngle = cameraViewModel.rotate.value!!
            when (invertAngle) {
                (rotAngle + 90) % 360 -> rotAngle += 90
                (rotAngle - 90 + 360) % 360 -> rotAngle -= 90
                else -> rotAngle = invertAngle
            }
            cameraViewModel.rotate(rotAngle)
        }
    }
}