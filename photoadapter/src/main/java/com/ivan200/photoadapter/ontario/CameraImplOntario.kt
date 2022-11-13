package com.ivan200.photoadapter.ontario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.base.CameraDelegate
import com.ivan200.photoadapter.base.CameraError
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.CaptureError
import com.ivan200.photoadapter.base.FacingDelegate
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.base.TakePictureResult
import com.ivan200.photoadapter.utils.ImageUtils
import com.ivan200.photoadapter.utils.SaveUtils
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Engine
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Grid
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.Preview
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import com.otaliastudios.cameraview.markers.DefaultAutoFocusMarker
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.SizeSelector
import com.otaliastudios.cameraview.size.SizeSelectors
import java.lang.ref.WeakReference

/**
 * @author ivan200
 * @since 24.07.2022
 */
class CameraImplOntario @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CameraView(context, attrs), CameraDelegate {

    private val _cameraInfo = MutableLiveData<SimpleCameraInfo>()
    override val cameraInfo: LiveData<SimpleCameraInfo> = _cameraInfo
    private val facings: Set<FacingDelegate> = ImageUtils.getFacings(context)

    private val _cameraInfoList: MutableMap<FacingDelegate, List<SimpleCameraInfo>> = facings.groupBy { it }
        .mapValues { emptyList<SimpleCameraInfo>() }.toMutableMap()
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = _cameraInfoList

    private val listener = Listener()
    private var isPictureSaving = false

    private val _takePictureResult = MutableLiveData<TakePictureResult>()
    override val takePictureResult: LiveData<TakePictureResult> = _takePictureResult

    private val _orientationChanged = MutableLiveData<Int>()
    override val orientationChanged: LiveData<Int> = _orientationChanged

    init {
        audio = Audio.OFF
        setAutoFocusMarker(DefaultAutoFocusMarker())
        engine = Engine.CAMERA1
        facing = Facing.BACK
        flash = Flash.OFF
        mapGesture(Gesture.LONG_TAP, GestureAction.NONE)
        mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.NONE)
        mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.EXPOSURE_CORRECTION)
        mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        grid = Grid.OFF
        mode = Mode.PICTURE
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
            gravity = CENTER
        }
        playSounds = false
        preview = Preview.GL_SURFACE
        setRequestPermissions(false)
    }

    private val _state = MutableLiveData<CameraViewState>(CameraViewState.NotInitialized)
    override val state: LiveData<CameraViewState> = _state

    var builder = CameraBuilder()

    override fun setCameraBuilder(cameraBuilder: CameraBuilder) {
        this.builder = cameraBuilder
        val selectors = mutableListOf<SizeSelector>()
        setFitMode(!cameraBuilder.fillPreview)
        if (cameraBuilder.fullScreenMode) {
            selectors.add(
                SizeSelectors.aspectRatio(AspectRatio.of(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels), 0.1f)
            )
        } else {
            val aspect = resources.displayMetrics.let {
                if (it.widthPixels < it.heightPixels) AspectRatio.of(3, 4) else AspectRatio.of(4, 3)
            }
            selectors.add(SizeSelectors.aspectRatio(aspect, 0.1f))
        }

        cameraBuilder.maxWidth?.let {
            snapshotMaxWidth = it
            selectors.add(SizeSelectors.maxWidth(it))
        }
        cameraBuilder.maxHeight?.let {
            snapshotMaxHeight = it
            selectors.add(SizeSelectors.maxHeight(it))
        }
        selectors.add(SizeSelectors.biggest())

        setPictureSize(SizeSelectors.and(*selectors.toTypedArray()))
        facing = if (cameraBuilder.facingBack) Facing.BACK else Facing.FRONT
    }

    override fun setFitMode(fit: Boolean) {
        layoutParams.apply {
            width = if (fit) WRAP_CONTENT else MATCH_PARENT
            height = if (fit) WRAP_CONTENT else MATCH_PARENT
        }
        requestLayout()
    }

    override val isFit: Boolean
        get() = layoutParams.width == WRAP_CONTENT

    override fun setLifecycleOwner(owner: LifecycleOwner?) {
        if (!ImageUtils.isCameraAvailable(context) || facings.isEmpty()) {
            _state.postValue(CameraViewState.Error(CameraError.NO_CAMERA))
            return
        }
        val hasCameraPermission = ContextCompat
            .checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            _state.postValue(CameraViewState.Initializing)
            super.setLifecycleOwner(owner)

            removeCameraListener(listener)
            addCameraListener(listener)
        } else {
            _state.postValue(CameraViewState.NoPermissions)
        }
    }

    override fun takePicture() {
        if (isTakingPicture || isPictureSaving) return
        if (builder.useSnapshot) {
            takePictureSnapshot()
        } else {
            super.takePicture()
        }
    }

    override fun changeFacing() {
        if (facings.size > 1) {
            super.toggleFacing()
        }
    }

    override fun restart() {
        this.open()
    }

    override fun changeSameFacingCamera() {
        // ontario does not support multiple same facing cameras
    }

    //since ontario have no callback at camera starts streaming, we can not blur previous snapshot
    override val isBlurring: Boolean
        get() = false

    override fun selectCamera(camera: SimpleCameraInfo) {
        val currentFacing = cameraInfo.value?.cameraFacing
        if (currentFacing != null && camera.cameraFacing != currentFacing) {
            changeFacing()
        }
    }

    override fun setFlash(flash: FlashDelegate.HasFlash) {
        val newFlash = when (flash) {
            FlashDelegate.HasFlash.Auto -> Flash.AUTO
            FlashDelegate.HasFlash.Off -> Flash.OFF
            FlashDelegate.HasFlash.On -> Flash.ON
            FlashDelegate.HasFlash.Torch -> Flash.TORCH
        }
        this.flash = newFlash
    }

    inner class Listener : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            _state.postValue(CameraViewState.Streaming)

            val facing = when {
                options.supportedFacing.contains(Facing.BACK) -> FacingDelegate.BACK
                options.supportedFacing.contains(Facing.FRONT) -> FacingDelegate.FRONT
                else -> FacingDelegate.BACK
            }

            val hasFlash = options.supportedFlash.contains(Flash.ON) ||
                    options.supportedFlash.contains(Flash.AUTO) ||
                    options.supportedFlash.contains(Flash.TORCH)

            val supportedFlash: List<FlashDelegate.HasFlash> = if (hasFlash) {
                if (builder.useSnapshot) {
                    listOf(FlashDelegate.HasFlash.Off, FlashDelegate.HasFlash.Torch)
                } else {
                    options.supportedFlash.map {
                        when (it!!) {
                            Flash.OFF -> FlashDelegate.HasFlash.Off
                            Flash.ON -> FlashDelegate.HasFlash.On
                            Flash.AUTO -> FlashDelegate.HasFlash.Auto
                            Flash.TORCH -> FlashDelegate.HasFlash.Torch
                        }
                    }
                }
            } else {
                emptyList()
            }

            val cameraInfo = SimpleCameraInfo(
                cameraId = cameraIdMap[facing]!!,
                cameraFacing = facing,
                hasFlashUnit = hasFlash,
                supportedFlash = supportedFlash.sortedBy { it.orderValue },
                physicalSize = PointF(0f, 0f),
                fov = 0f,
                focal = 0f,
                name = cameraIdMap[facing]!!,
                nameSelected = cameraIdMap[facing]!!
            )

            _cameraInfoList[facing] = listOf(cameraInfo)
            _cameraInfo.postValue(cameraInfo)
        }

        override fun onCameraClosed() {
            _state.postValue(CameraViewState.Initializing)
        }

        override fun onCameraError(exception: CameraException) {
            val takePictureError = when (exception.reason) {
                CameraException.REASON_PICTURE_FAILED -> CaptureError.ERROR_CAPTURE_FAILED
                CameraException.REASON_VIDEO_FAILED -> CaptureError.ERROR_CAPTURE_FAILED
                else -> null
            }
            if (takePictureError != null) {
                _takePictureResult.postValue(TakePictureResult.ImageTakeException(takePictureError, exception))
            } else {
                val cameraError = when (exception.reason) {
                    CameraException.REASON_UNKNOWN -> CameraError.CAMERA_UNKNOWN_ERROR
                    CameraException.REASON_FAILED_TO_CONNECT -> CameraError.CAMERA_DISCONNECTED
                    CameraException.REASON_FAILED_TO_START_PREVIEW -> CameraError.CAMERA_ERROR
                    CameraException.REASON_DISCONNECTED -> CameraError.CAMERA_DISCONNECTED
                    CameraException.REASON_NO_CAMERA -> CameraError.NO_CAMERA
                    else -> CameraError.CAMERA_UNKNOWN_ERROR
                }
                _state.postValue(CameraViewState.Error(cameraError, exception))
            }
        }

        override fun onPictureTaken(result: PictureResult) {
            val photoFile = SaveUtils.createImageFile(context, builder.saveTo)
            isPictureSaving = true

            val viewWeakRef = WeakReference(this@CameraImplOntario)
            ResultSaver(
                photoFile = photoFile,
                flipFrontRequested = builder.flipFrontResult,
                result = result,
                onSaved = {
                    viewWeakRef.get()?.apply {
                        isPictureSaving = false
                        _takePictureResult.postValue(TakePictureResult.ImageTaken(it))
                    }
                },
                onSavedError = {
                    viewWeakRef.get()?.apply {
                        isPictureSaving = false
                        _takePictureResult.postValue(TakePictureResult.ImageTakeException(CaptureError.ERROR_FILE_IO, it))
                    }
                }
            ).save()
        }

        override fun onOrientationChanged(orientation: Int) {
            _orientationChanged.postValue(orientation)
        }
    }

    private companion object {

        // since camera options in ontario does not contain camera id on an open map, i prefer to set a virtual id
        @Suppress("DEPRECATION")
        private val cameraIdMap = hashMapOf(
            FacingDelegate.BACK to android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK.toString(), // Noncompliant
            FacingDelegate.FRONT to android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT.toString() // Noncompliant
        )
    }
}
