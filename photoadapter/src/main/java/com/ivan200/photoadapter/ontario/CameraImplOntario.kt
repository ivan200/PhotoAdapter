package com.ivan200.photoadapter.ontario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.Camera
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.base.CameraDelegate
import com.ivan200.photoadapter.base.CameraError
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.FacingDelegate
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.utils.ImageUtils
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

/**
 * @author ivan200
 * @since 24.07.2022
 */
class CameraImplOntario @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CameraView(context, attrs), CameraDelegate {

    private val _cameraInfo = MutableLiveData<SimpleCameraInfo?>()
    override val cameraInfo: LiveData<SimpleCameraInfo?> = _cameraInfo
    private val facings: Set<FacingDelegate> = ImageUtils.getFacings(context)

    private val _cameraInfoList: MutableMap<FacingDelegate, List<SimpleCameraInfo>> = facings.groupBy { it }
        .mapValues { emptyList<SimpleCameraInfo>() }.toMutableMap()
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = _cameraInfoList

    private var lifecycleOwner: LifecycleOwner? = null

    private val listener = Listener()

    init {
        audio = Audio.OFF
        setAutoFocusMarker(DefaultAutoFocusMarker())
        engine = if (ImageUtils.allowCamera2Support(context)) Engine.CAMERA2 else Engine.CAMERA1
        facing = Facing.BACK
        flash = Flash.OFF
        mapGesture(Gesture.LONG_TAP, GestureAction.NONE)
        mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.NONE)
        mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.EXPOSURE_CORRECTION)
        mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)
        grid = Grid.OFF
        mode = Mode.PICTURE
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        playSounds = false
        preview = Preview.GL_SURFACE
        setRequestPermissions(false)
    }

    private val _state = MutableLiveData<CameraViewState>(CameraViewState.NotInitialized)
    override val state: LiveData<CameraViewState> = _state

    var builder: CameraBuilder? = null

    override fun setCameraBuilder(cameraBuilder: CameraBuilder) {
        this.builder = cameraBuilder
        val selectors = mutableListOf<SizeSelector>()
        if (cameraBuilder.fitMode) {
            setFitMode(true)
        }
        if (cameraBuilder.fullScreenMode) {
            selectors.add(
                SizeSelectors.aspectRatio(
                    AspectRatio.of(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels), 0.1f
                )
            )
        }
        cameraBuilder.maxImageSize?.let {
            snapshotMaxWidth = it
            snapshotMaxHeight = it

            selectors.add(SizeSelectors.and(SizeSelectors.maxWidth(it), SizeSelectors.maxHeight(it)))
        }
        if (selectors.isNotEmpty()) {
            setPictureSize(SizeSelectors.and(*selectors.toTypedArray()))
        }
        facing = if (cameraBuilder.facingBack) Facing.BACK else Facing.FRONT
    }

    override fun setFitMode(fit: Boolean) {
        layoutParams = if (fit) {
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        } else {
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

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
        if (isTakingPicture) return
        if (builder?.useSnapshot == true) {
            takePictureSnapshot()
        } else {
            super.takePicture()
        }
    }

    override fun changeFacing() {
        if(facings.size > 1){
            super.toggleFacing()
        }
    }

    override fun changeSameFacingCamera() {  }

    override fun selectSameFacingCameraByIndex(index: Int) { }

    inner class Listener : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            _state.postValue(CameraViewState.Streaming)

            val facing = when {
                options.supportedFacing.contains(Facing.BACK) -> FacingDelegate.BACK
                options.supportedFacing.contains(Facing.FRONT) -> FacingDelegate.FRONT
                else -> FacingDelegate.BACK
            }

            val hasFlash = options.supportedFlash.contains(Flash.ON)
                    || options.supportedFlash.contains(Flash.AUTO)
                    || options.supportedFlash.contains(Flash.TORCH)

            val cameraInfo = SimpleCameraInfo(
                cameraIdMap.get(facing)!!,
                facing,
                hasFlash,
                PointF(0f, 0f),
                0f,
                0f,
                cameraIdMap.get(facing)!!
            )

            _cameraInfoList[facing] = listOf(cameraInfo)
            _cameraInfo.postValue(cameraInfo)
        }

        override fun onCameraClosed() {
            _state.postValue(CameraViewState.Paused)
            _cameraInfo.postValue(null)
        }

        override fun onCameraError(exception: CameraException) {
            val reason = cameraErrorMap[exception.reason] ?: CameraError.CAMERA_UNKNOWN_ERROR
            _state.postValue(CameraViewState.Error(reason, exception))
        }

        override fun onPictureTaken(result: PictureResult) {

        }
    }


    private companion object{

        //since camera options in ontario does not contain camera id on an open map, i prefer to set a virtual id
        @Suppress("DEPRECATION")
        private val cameraIdMap = hashMapOf<FacingDelegate, String>(
            FacingDelegate.BACK  to Camera.CameraInfo.CAMERA_FACING_BACK.toString(),
            FacingDelegate.FRONT to Camera.CameraInfo.CAMERA_FACING_FRONT.toString()
        )

        private val cameraErrorMap = hashMapOf<Int, CameraError>(
            CameraException.REASON_UNKNOWN                  to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraException.REASON_FAILED_TO_CONNECT        to CameraError.CAMERA_DISCONNECTED,
            CameraException.REASON_FAILED_TO_START_PREVIEW  to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraException.REASON_DISCONNECTED             to CameraError.CAMERA_DISCONNECTED,
            CameraException.REASON_PICTURE_FAILED           to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraException.REASON_VIDEO_FAILED             to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraException.REASON_NO_CAMERA                to CameraError.NO_CAMERA,
        )
    }
}
