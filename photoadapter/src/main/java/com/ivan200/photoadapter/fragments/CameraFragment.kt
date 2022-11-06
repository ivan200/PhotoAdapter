package com.ivan200.photoadapter.fragments

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ivan200.photoadapter.CameraActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.CameraViewModel
import com.ivan200.photoadapter.PictureInfo
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.base.CameraView
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.base.TakePictureResult
import com.ivan200.photoadapter.utils.ANIMATION_FAST_MILLIS
import com.ivan200.photoadapter.utils.ANIMATION_SLOW_MILLIS
import com.ivan200.photoadapter.utils.ApplyInsetsListener
import com.ivan200.photoadapter.utils.lockOrientation
import com.ivan200.photoadapter.utils.onClick
import com.ivan200.photoadapter.utils.padBottomViewWithInsets
import com.ivan200.photoadapter.utils.padTopViewWithInsets
import com.ivan200.photoadapter.utils.rotateItems
import com.ivan200.photoadapter.utils.simulateClick
import com.ivan200.photoadapter.utils.unlockOrientation
import java.lang.ref.WeakReference

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
    private val buttonFit get() = requireView().findViewById<ImageButton>(R.id.btn_fit)
    private val camerasRecycler get() = requireView().findViewById<RecyclerView>(R.id.select_camera_recycler)

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(activity as CameraActivity)[CameraViewModel::class.java]
    }

    private var currentFlash: FlashDelegate = FlashDelegate.NoFlash
    private lateinit var cameraBuilder: CameraBuilder

    private var insets: WindowInsetsCompat? = null

    private val camerasAdapter = CamerasAdapter(this::selectCamera)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraBuilder = (activity as? CameraActivity)?.cameraBuilder ?: CameraBuilder()

        CameraView.cameraSelector = cameraBuilder.cameraImplSelector
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraViewModel.pictures.observe(requireActivity()) {
            if (!cameraBuilder.previewImage) {
                if (!cameraBuilder.allowMultipleImages && it.isNotEmpty()) {
                    cameraViewModel.success()
                } else {
                    loadThumbImage(it.firstOrNull())
                }
            } else if (it.size == 0) {
                resultImage.isVisible = false
            }
        }

        if (cameraBuilder.fullScreenMode) {
            // Очищаем значение обозначающее что вью камеры должно быть над панелью кнопок
            (cameraFrame.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                arrayOf(RelativeLayout.BELOW, RelativeLayout.ABOVE, RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF).forEach {
                    addRule(it, 0)
                }
            }
        }

        cameraViewModel.curPageLoaded.observe(requireActivity()) {
            if (cameraBuilder.previewImage) {
                loadThumbImage(it)
            }
        }

        cameraView.state.observe(viewLifecycleOwner) {
            when (it) {
                is CameraViewState.Error -> {
                    // TODO Обработать соостояния ошибок
                    initText.isVisible = false
                    val dialog = AlertDialog.Builder(requireActivity(), cameraBuilder.dialogTheme)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(it.error.name)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                    dialog.show()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    }
                }
                CameraViewState.Initializing -> {
                    initText.isVisible = true
//                    switchCamera.isVisible = false
//                    torchSwitch.isVisible = false
                }
                CameraViewState.NoPermissions -> {
                    // TODO не срабатывает обработка пермишенов при разворачивании
                    initText.isVisible = false
                }
                CameraViewState.NotInitialized -> initText.isVisible = false
                CameraViewState.Streaming -> initText.isVisible = false
            }
        }

        cameraView.cameraInfo.observe(viewLifecycleOwner) {
            val list = cameraView.cameraInfoList

            switchCamera.isVisible = list.size > 1

            val sameFacingCameras = list[it.cameraFacing]!!
            if (sameFacingCameras.size > 1) {
                camerasRecycler.isVisible = true
                camerasAdapter.update(sameFacingCameras, it)
            } else {
                camerasRecycler.isVisible = false
            }

            switchCamera.setImageResource(it.cameraFacing.iconRes)
            switchCamera.contentDescription = getString(it.cameraFacing.descriptionRes)
            switchCamera.setOnClickListener {
                cameraView.changeFacing()
            }
            torchSwitch.isVisible = it.hasFlashUnit
            currentFlash = if (it.supportedFlash.isNotEmpty()) {
                it.supportedFlash.first()
            } else {
                FlashDelegate.NoFlash
            }
        }

        cameraView.setLifecycleOwner(requireActivity())
        cameraView.setCameraBuilder(cameraBuilder)

        cameraView.takePictureResult.observe(requireActivity(), this::onPictureTaken)

        camerasRecycler.adapter = camerasAdapter
        camerasRecycler.itemAnimator = null


//        switchCamera.showIf { cameraBuilder.changeCameraAllowed && ImageUtils.hasDifferentFacings(requireActivity()) }
//        switchCamera.onClick {
//            currentFlash = Flash.OFF
//            setFlash(currentFlash)
//            cameraView.toggleFacing()
//        }

        buttonFit.setOnClickListener {
            cameraView.setFitMode(!cameraView.isFit)
        }

        cameraViewModel.fragmentState.observe(requireActivity()) {
            setFlash(if (it != FragmentChangeState.GALLERY) currentFlash else FlashDelegate.NoFlash)
        }

        cameraViewModel.rotate.observe(requireActivity()) {
            rotateItems(it, capture, torchSwitch, resultImage, switchCamera)
        }

        cameraViewModel.volumeKeyPressed.observe(requireActivity()) {
            if (cameraViewModel.fragmentState.value == FragmentChangeState.CAMERA) {
                capture.simulateClick()
            }
        }

        cameraViewModel.restartCamera.observe(requireActivity()) {
            it.get()?.apply {
                cameraView.restart()
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
            resultImage.isVisible = true
            Glide.with(requireActivity())
                .load(pictureInfo.file)
                .apply(RequestOptions.circleCropTransform())
                .into(resultImage)
        } else {
            resultImage.isVisible = false
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
        cameraView.takePicture()
        cameraViewModel.changeState(FragmentChangeState.WAITING_FOR_IMAGE)
        if (!cameraBuilder.previewImage && cameraBuilder.allowMultipleImages) {
            val flashWeakRef = WeakReference(flashView)
            flashView.postDelayed({
                flashWeakRef.get()?.apply {
                    isVisible = true
                    postDelayed({ flashWeakRef.get()?.isVisible = false }, ANIMATION_FAST_MILLIS)
                }
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    private fun selectCamera(camera: SimpleCameraInfo) {
        cameraView.selectCamera(camera)
    }

    private fun showGallery() {
        cameraViewModel.changeState(FragmentChangeState.GALLERY)
    }

    private fun nextFlash() {
        val flashes = cameraView.cameraInfo.value?.supportedFlash ?: emptyList()
        if (flashes.isNotEmpty()) {
            var index = when (val flash = currentFlash) {
                FlashDelegate.NoFlash -> 0
                is FlashDelegate.HasFlash -> flashes.indexOf(flash) + 1
            }
            if (index >= flashes.size) index = 0
            currentFlash = flashes[index]
            setFlash(currentFlash)
        }
    }

    private fun setFlash(flash: FlashDelegate) {
        when (flash) {
            FlashDelegate.NoFlash -> torchSwitch.isVisible = false
            is FlashDelegate.HasFlash -> {
                cameraView.setFlash(flash)
                torchSwitch.isVisible = true
                torchSwitch.setImageResource(flash.iconRes)
                torchSwitch.contentDescription = getString(flash.descriptionRes)
            }
        }
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

    fun onPictureTaken(result: TakePictureResult) {
        when (result) {
            is TakePictureResult.ImageTakeException -> {
                val dialog = AlertDialog.Builder(requireActivity(), cameraBuilder.dialogTheme)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(result.ex?.localizedMessage) // TODO добавить тексты
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }
            is TakePictureResult.ImageTaken -> {
                cameraViewModel.onFileSaved(result.file)
            }
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
//        override fun onOrientationChanged(orientation: Int) { TODO Обработать поворот экрана
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
