package com.ivan200.photoadapter.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.alpha
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ivan200.photoadapter.CameraActivity
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.CameraViewModel
import com.ivan200.photoadapter.PictureInfo
import com.ivan200.photoadapter.R
import com.ivan200.photoadapter.base.CameraError
import com.ivan200.photoadapter.base.CameraView
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.FragmentChangeState
import com.ivan200.photoadapter.base.ScaleDelegate
import com.ivan200.photoadapter.base.ScaleDelegate.*
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.base.TakePictureResult
import com.ivan200.photoadapter.utils.ANIMATION_FAST_MILLIS
import com.ivan200.photoadapter.utils.ANIMATION_SLOW_MILLIS
import com.ivan200.photoadapter.utils.ApplyInsetsListener
import com.ivan200.photoadapter.utils.animateFadeVisibility
import com.ivan200.photoadapter.utils.getColorCompat
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
    private val btnBack get() = requireView().findViewById<ImageButton>(R.id.btn_back)
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
            if (!cameraBuilder.allowPreviewResult) {
                if (!cameraBuilder.allowMultipleImages && it.isNotEmpty()) {
                    cameraViewModel.success()
                } else {
                    loadThumbImage(it.firstOrNull())
                }
            } else if (it.isEmpty()) {
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
            switchCamera.background.alpha = view.context.getColorCompat(R.color.circle_icon_fullscreen_transparency).alpha
        }

        cameraViewModel.curPageLoaded.observe(requireActivity()) {
            if (cameraBuilder.allowPreviewResult) {
                loadThumbImage(cameraViewModel.pictures.value?.firstOrNull())
            }
        }

        cameraView.state.observe(viewLifecycleOwner) {
            when (it) {
                is CameraViewState.Error -> {
                    initText.isVisible = false
                    val dialog = AlertDialog.Builder(requireActivity(), cameraBuilder.dialogTheme)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(getString(it.error.messageRes))
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .apply {
                            if (it.error == CameraError.CAMERA_UNAVAILABLE_DO_NOT_DISTURB && SDK_INT >= LOLLIPOP) {
                                setNeutralButton(getString(R.string.camera_go_to_settings)) { dialog, _ ->
                                    try {
                                        val intent = Intent("android.settings.ZEN_MODE_SETTINGS")
                                        startActivity(intent)
                                    } catch (ex: ActivityNotFoundException) {
                                        //do nothing
                                    }
                                    dialog.dismiss()
                                }
                            }
                        }
                        .create()
                    dialog.show()
                    if (SDK_INT < LOLLIPOP) {
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    }
                }
                CameraViewState.Initializing -> {
                    if (cameraBuilder.blurOnSwitch) {
                        initText.isVisible = !cameraView.isBlurring
                    } else {
                        initText.isVisible = true
                    }
                }
                CameraViewState.NoPermissions -> {
                    initText.isVisible = false
                }
                CameraViewState.NotInitialized -> initText.isVisible = false
                CameraViewState.Streaming -> initText.isVisible = false
            }
        }

        cameraView.cameraInfo.observe(viewLifecycleOwner) {
            if (cameraBuilder.allowChangeCamera) {
                val list = cameraView.cameraInfoList

                val sameFacingCameras = list[it.cameraFacing]!!
                if (sameFacingCameras.size > 1) {
                    camerasAdapter.update(sameFacingCameras, it, cameraViewModel.rotate.value!!)
                }
                camerasRecycler.animateFadeVisibility(sameFacingCameras.size > 1)

                switchCamera.animateFadeVisibility(list.size > 1) { cameraView.changeFacing() }
                switchCamera.setImageResource(it.cameraFacing.iconRes)
                switchCamera.contentDescription = getString(it.cameraFacing.descriptionRes)

                torchSwitch.animateFadeVisibility(it.hasFlashUnit) { this.nextFlash() }

                currentFlash = if (it.supportedFlash.isNotEmpty()) {
                    it.supportedFlash.first()
                } else {
                    FlashDelegate.NoFlash
                }
            } else {
                switchCamera.isVisible = false
                camerasRecycler.isVisible = false
            }
        }

        cameraView.setLifecycleOwner(requireActivity())
        cameraView.setCameraBuilder(cameraBuilder)

        cameraView.takePictureResult.observe(requireActivity(), this::onPictureTaken)

        cameraView.orientationChanged.observe(requireActivity(), this::onOrientationChanged)

        camerasRecycler.adapter = camerasAdapter
        camerasRecycler.itemAnimator = null

        if (cameraBuilder.allowToggleFit) {
            buttonFit.isVisible = true
            setScaleTypeIcon(cameraView.scaleType)
            buttonFit.setOnClickListener {
                val newScaleType = when (cameraView.scaleType) {
                    FIT -> FILL
                    FILL -> FIT
                }
                cameraView.setScaleType(newScaleType)
                setScaleTypeIcon(newScaleType)
            }
        }

        cameraViewModel.fragmentState.observe(requireActivity()) {
            setFlash(if (it != FragmentChangeState.GALLERY) currentFlash else FlashDelegate.NoFlash)
        }

        cameraViewModel.rotate.observe(requireActivity()) {
            rotateItems(it, capture, torchSwitch, resultImage, switchCamera, buttonFit, btnBack)

            if (camerasRecycler.isVisible) {
                val layoutManager = camerasRecycler.layoutManager as LinearLayoutManager
                val itemFirst = layoutManager.findFirstVisibleItemPosition()
                val itemLast = layoutManager.findLastVisibleItemPosition()
                for (i in itemFirst..itemLast) {
                    val vh = camerasRecycler.findViewHolderForAdapterPosition(i)
                    (vh as CamerasAdapter.CamerasViewHolder).rotateItem(it)
                }
            }
        }

        cameraViewModel.captureSimulate.observe(requireActivity()) {
            it?.let {
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

        if (cameraBuilder.showBackButton) {
            btnBack.isVisible = true
            btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        } else {
            btnBack.isVisible = false
        }
    }

    private fun setScaleTypeIcon(scaleType: ScaleDelegate) {
        buttonFit.setImageResource(scaleType.iconRes)
        buttonFit.contentDescription = getString(scaleType.descriptionRes)
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
        this.insets = insets
        view?.let { setWindowInsets(insets) }
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
        if (!cameraBuilder.allowPreviewResult && cameraBuilder.allowMultipleImages) {
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

    private fun onPictureTaken(result: TakePictureResult) {
        when (result) {
            is TakePictureResult.ImageTakeException -> {
                val message = getString(result.error.messageRes)
                val messageTryAgain = getString(R.string.capture_error_try_again)
                val sumMessage = "$message\n$messageTryAgain"
                val dialog = AlertDialog.Builder(requireActivity(), cameraBuilder.dialogTheme)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(sumMessage)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
                if (SDK_INT < LOLLIPOP) {
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }
            is TakePictureResult.ImageTaken -> {
                cameraViewModel.onFileSaved(result.file)
            }
        }
    }

    private fun onOrientationChanged(orientation: Int) {
        if (!cameraBuilder.lockRotate) return

        val updatedRotation = when (orientation) {
            90 -> 270; 270 -> 90; else -> orientation
        }
        val lastRotation = cameraViewModel.rotate.value!!

        val newRotation = when {
            (lastRotation + 90) % 360 == updatedRotation -> lastRotation + 90
            (lastRotation - 90 + 360) % 360 == updatedRotation -> lastRotation - 90
            (lastRotation + 180) % 360 == updatedRotation && lastRotation < 0 -> lastRotation + 180
            (lastRotation + 180) % 360 == updatedRotation && lastRotation > 0 -> lastRotation - 180
            else -> updatedRotation
        }
        cameraViewModel.rotate(newRotation)
    }
}
