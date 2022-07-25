package com.ivan200.photoadapter.fragments

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ivan200.photoadapter.CameraActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.CameraViewModel
import com.ivan200.photoadapter.PictureInfo
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.base.CameraView
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.utils.ApplyInsetsListener
import com.ivan200.photoadapter.utils.hide
import com.ivan200.photoadapter.utils.lockOrientation
import com.ivan200.photoadapter.utils.onClick
import com.ivan200.photoadapter.utils.padBottomViewWithInsets
import com.ivan200.photoadapter.utils.padTopViewWithInsets
import com.ivan200.photoadapter.utils.rotateItems
import com.ivan200.photoadapter.utils.show
import com.ivan200.photoadapter.utils.simulateClick
import com.ivan200.photoadapter.utils.unlockOrientation
import com.otaliastudios.cameraview.controls.Flash


//
// Created by Ivan200 on 15.10.2019.
//
@Suppress("unused")
class CameraFragment : Fragment(R.layout.fragment_camera), ApplyInsetsListener {

    private val flashView get() = requireView().findViewById<View>(R.id.flashView)
    private val cameraView get() = requireView().findViewById<CameraView>(R.id.cameraView)
    private val cameraFrame get() = requireView().findViewById<FrameLayout>(R.id.cameraFrame)
    private val initText get() = requireView().findViewById<TextView>(R.id.initText)
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

        cameraViewModel.pictures.observe(requireActivity()) {
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

        cameraViewModel.curPageLoaded.observe(requireActivity()) {
            if (cameraBuilder.previewImage) {
                loadThumbImage(it)
            }
        }

        cameraView.state.observe(viewLifecycleOwner){
            initText.isVisible = it == CameraViewState.Initializing
        }

        cameraView.cameraInfo.observe(viewLifecycleOwner) {
            if(it != null){
                val list = cameraView.cameraInfoList

                switchCamera.isVisible = list.size > 1
                switchCamera.setImageResource(it.cameraFacing.iconRes)
                switchCamera.contentDescription = getString(it.cameraFacing.descriptionRes)
                switchCamera.setOnClickListener {
                    cameraView.changeFacing()
                }
                torchSwitch.isVisible = it.hasFlashUnit
            } else {
                switchCamera.isVisible = false
                torchSwitch.isVisible = false
            }
        }


        cameraView.setLifecycleOwner(requireActivity())
        cameraView.setCameraBuilder(cameraBuilder)

//        switchCamera.showIf { cameraBuilder.changeCameraAllowed && ImageUtils.hasDifferentFacings(requireActivity()) }
//        switchCamera.onClick {
//            currentFlash = Flash.OFF
//            setFlash(currentFlash)
//            cameraView.toggleFacing()
//        }

        cameraViewModel.showCamera.observe(requireActivity()) {
            setFlash(if (it) currentFlash else Flash.OFF)
        }

        cameraViewModel.rotate.observe(requireActivity()) {
            rotateItems(it, capture, torchSwitch, resultImage, switchCamera)
        }

        cameraViewModel.volumeKeyPressed.observe(requireActivity()) {
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
//        if (cameraView.isTakingPicture) return
//        toast?.cancel()
//        if (cameraBuilder.useSnapshot) {
//            cameraView.takePictureSnapshot()
//        } else {
//            cameraView.takePicture()
//        }
//        if (!cameraBuilder.previewImage && cameraBuilder.allowMultipleImages) {
//            flashView.postDelayed({
//                flashView.show()
//                flashView.postDelayed({ flashView.hide() }, ANIMATION_FAST_MILLIS)
//            }, ANIMATION_SLOW_MILLIS)
//        }
    }

    private fun showGallery() {
        toast?.cancel()
        cameraViewModel.changeFragment(false)
    }

    private fun nextFlash() {
        var index = supportedFlash.indexOf(currentFlash) + 1
        if (index >= supportedFlash.size) index = 0

        currentFlash = supportedFlash[index]
        setFlash(currentFlash)
    }

    private fun setFlash(flash: Flash) {
//        cameraView.flash = flash
//        torchSwitch.setImageResource(
//            when (flash) {
//                Flash.OFF -> R.drawable.ic_photo_flash_off
//                Flash.ON -> R.drawable.ic_photo_flash_on
//                Flash.AUTO -> R.drawable.ic_photo_flash_auto
//                Flash.TORCH -> R.drawable.ic_photo_flash_torch
//            }
//        )
    }

    override fun onStart() {
        super.onStart()
        if (cameraBuilder.lockRotate) {
            requireActivity().lockOrientation()
        }
    }

    override fun onStop() {
        super.onStop()

        if (cameraBuilder.lockRotate) {
            requireActivity().unlockOrientation()
        }
    }


//    inner class Listener : CameraListener() {
//        override fun onCameraOpened(options: CameraOptions) {
//            super.onCameraOpened(options)
//            supportedFlash = options.supportedFlash.sortedBy { it.ordinal }.toList()
//            torchSwitch.showIf { options.supportedFlash.size > 1 }
//        }
//
//        override fun onPictureTaken(result: PictureResult) {
//            super.onPictureTaken(result)
//            ResultCheckSaver(requireActivity(), result, cameraBuilder, this::onCheckSaved)
//                .checkSave()
//        }
//
//        private fun onCheckSaved(resultCheckSaver: ResultCheckSaver) {
//            if (!resultCheckSaver.checkResult) {
//                onVerificationFailed()
//            } else {
//                cameraViewModel.onFileSaved(resultCheckSaver.photoFile, resultCheckSaver.thumbsFile)
//            }
//        }
//
//        override fun onCameraError(exception: CameraException) {
//            super.onCameraError(exception)
//            AlertDialog.Builder(activity, cameraBuilder.dialogTheme)
//                .setTitle(android.R.string.dialog_alert_title)
//                .setMessage(exception.localizedMessage)
//                .setPositiveButton(android.R.string.ok) { dialog, _ ->
//                    dialog.dismiss()
//                }
//                .create()
//                .show()
//        }
//
//        //Rotate items on screen if orientation locked
//        override fun onOrientationChanged(orientation: Int) {
//            super.onOrientationChanged(orientation)
//            if (!cameraBuilder.lockRotate) return
//
//            val invertAngle = when (orientation) {
//                90 -> 270; 270 -> 90; else -> orientation
//            }
//            var rotAngle = cameraViewModel.rotate.value!!
//            when (invertAngle) {
//                (rotAngle + 90) % 360 -> rotAngle += 90
//                (rotAngle - 90 + 360) % 360 -> rotAngle -= 90
//                else -> rotAngle = invertAngle
//            }
//            cameraViewModel.rotate(rotAngle)
//        }
//    }
}