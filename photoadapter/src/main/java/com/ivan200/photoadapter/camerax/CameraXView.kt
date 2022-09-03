package com.ivan200.photoadapter.camerax

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.base.CameraDelegate
import com.ivan200.photoadapter.base.CameraError
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.CaptureError
import com.ivan200.photoadapter.base.FacingDelegate
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.base.TakePictureResult
import com.ivan200.photoadapter.camerax.touch.TouchHandler
import com.ivan200.photoadapter.utils.ImageUtils
import com.ivan200.photoadapter.utils.ImageUtils.dpToPx
import com.ivan200.photoadapter.utils.SaveUtils
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * @author ivan200
 * @since 24.02.2022
 */
@TargetApi(21)
class CameraXView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), CameraDelegate {

    var builder: CameraBuilder = CameraBuilder()

    private val _state = MutableLiveData<CameraViewState>(CameraViewState.NotInitialized)
    override val state: LiveData<CameraViewState> = _state

    private val cameraLifecycleObserver = CameraLifecycleObserver()
    private var lifecycleOwner: LifecycleOwner? = null

    private var analysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    val rotationDetector = DisplayRotationDetector(context, this::onDisplayRotated)
    private val changeCameraProvider = ChangeCameraProvider()

    private val cameraProviderFutureListener = CameraProviderFutureListener()
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    private val _torchState = MutableLiveData<Boolean>(false)
    val torchState: LiveData<Boolean> = _torchState

    private val _takePictureResult = MutableLiveData<TakePictureResult>()
    override val takePictureResult: LiveData<TakePictureResult> = _takePictureResult

    val takePictureExecutor = Executors.newSingleThreadExecutor()

    init {
        this.background = ColorDrawable(Color.BLACK)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    val viewFinder = PreviewView(context, attrs, defStyleAttr, defStyleRes).also {
        it.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        it.scaleType = PreviewView.ScaleType.FILL_CENTER
        this.addView(it)
    }

    val focusView = FocusView(context, attrs, defStyleAttr, defStyleRes).also {
        val size = 72.dpToPx(context).toInt()
        it.layoutParams = ViewGroup.LayoutParams(size, size)
        this.addView(it)
    }

    override fun setFlash(flash: FlashDelegate.HasFlash) {
        if (_torchState.value == true && flash != FlashDelegate.HasFlash.Torch) {
            camera?.cameraControl?.enableTorch(false)
        }

        when (flash) {
            FlashDelegate.HasFlash.Auto -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
            FlashDelegate.HasFlash.Off -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
            FlashDelegate.HasFlash.On -> imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
            FlashDelegate.HasFlash.Torch -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(true)
            }
        }
    }

    override fun setFitMode(fit: Boolean) {
        if (fit) {
            viewFinder.scaleType = PreviewView.ScaleType.FIT_CENTER
        } else {
            viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    override val isFit: Boolean
        get() = viewFinder.scaleType == PreviewView.ScaleType.FIT_CENTER

    override fun setLifecycleOwner(owner: LifecycleOwner?) {
        if (!ImageUtils.isCameraAvailable(context)) {
            _state.postValue(CameraViewState.Error(CameraError.NO_CAMERA))
            return
        }

        lifecycleOwner?.lifecycle?.removeObserver(cameraLifecycleObserver)
        owner?.let {
            it.lifecycle.addObserver(cameraLifecycleObserver)
            lifecycleOwner = it

            changeCameraProvider.cameraInfo.observe(it) { info: SimpleCameraInfo? ->
                if (info != null) {
                    try {
                        bindCameraUseCases(info)
                    } catch (e: Exception) {
                        processCameraException(e)
                    }
                }
            }
        }
    }

    override fun setCameraBuilder(cameraBuilder: CameraBuilder) {
        this.builder = cameraBuilder
    }

    private fun onDisplayRotated() {
        imageCapture?.targetRotation = rotationDetector.deviceOrientation
    }

    fun setUiOnPermission() {
        if (lifecycleOwner == null) return

        val hasCameraPermission = ContextCompat
            .checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            _state.postValue(CameraViewState.Initializing)
            initCamera()
        } else {
            _state.postValue(CameraViewState.NoPermissions)
        }
    }

    private fun initCamera() {
        cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(context)
        } catch (e: Exception) {
            processCameraException(e)
            null
        }
        cameraProviderFuture?.addListener(cameraProviderFutureListener, ContextCompat.getMainExecutor(context))
    }

    private inner class CameraProviderFutureListener() : Runnable {
        override fun run() {
            try {
                cameraProvider = cameraProviderFuture!!.get()
                changeCameraProvider.setCameraProvider(cameraProvider!!, builder)
                if (!changeCameraProvider.hasAnyCamera()) {
                    _state.postValue(CameraViewState.Error(CameraError.NO_CAMERA))
                }
                // bindCameraUseCases() will called at updating changeCameraProvider.cameraInfo
            } catch (e: Exception) {
                processCameraException(e)
            }
        }
    }

    private inner class CameraLifecycleObserver() : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    rotationDetector.enable()
                    setUiOnPermission()
                }
                Lifecycle.Event.ON_STOP -> rotationDetector.disable()
                else -> Unit
            }
        }
    }

    override fun restart() {
        setUiOnPermission()
    }

    private fun processCameraException(ex: Exception) {
        val cameraEx = when (ex) {
            is InitializationException -> ex.cause as? CameraUnavailableException
            is ExecutionException -> ex.cause as? CameraUnavailableException
            is CameraUnavailableException -> ex
            else -> null
        }

        val reason = cameraErrorMap[cameraEx?.reason] ?: CameraError.CAMERA_UNKNOWN_ERROR
        _state.postValue(CameraViewState.Error(reason, ex))
    }

    private fun bindCameraUseCases(cameraInfo: SimpleCameraInfo) {
        if (lifecycleOwner == null) return

        val dimObtained = cameraInfo.physicalSize.let { it.x > 0f && it.y > 0f }
        val cameraSize = if (dimObtained) cameraInfo.physicalSize else getPreviewSize()
        val cameraRatio = ImageUtils.aspectRatio(cameraSize)

        val rotation = rotationDetector.deviceOrientation

        preview = Preview.Builder().apply {
            setTargetAspectRatio(cameraRatio)
            setTargetRotation(rotation)
        }.build().apply {
            setSurfaceProvider(viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .apply {
                builder.outputJpegQuality?.let {
                    setJpegQuality(it)
                }
                val maxSize = builder.maxImageSize
                if (maxSize != null && maxSize > 0) {
                    setTargetResolution(ImageUtils.targetSize(cameraRatio, maxSize))
                } else {
                    setTargetAspectRatio(cameraRatio)
                }
            }
            .build()

        cameraProvider?.let {
            it.unbindAll()
            val useCases: MutableList<UseCase> = mutableListOf(preview!!, imageCapture!!)
            camera = it.bindToLifecycle(
                lifecycleOwner!!,
                changeCameraProvider.getCameraSelector(),
                *(useCases.toTypedArray())
            )
        }

        viewFinder.previewStreamState.observe(lifecycleOwner!!) {
            if (it == PreviewView.StreamState.STREAMING) {
                _state.postValue(CameraViewState.Streaming)
            } else {
                if (_state.value is CameraViewState.Streaming) {
                    _state.postValue(CameraViewState.Initializing)
                }
            }
        }
        camera?.cameraInfo?.torchState?.observe(lifecycleOwner!!) {
            when (it) {
                TorchState.ON -> _torchState.postValue(true)
                TorchState.OFF -> _torchState.postValue(false)
            }
        }
        viewFinder.setOnTouchListener(TouchHandler(camera, viewFinder, focusView))
    }

    private fun getPreviewSize(): PointF? {
        val size = PointF(measuredWidth.toFloat(), measuredHeight.toFloat())
        return if (size.x > 0 && size.y > 0) size else null
    }

    override fun takePicture() {
        if (builder.useSnapshot) {
            takeSnapshot()
            return
        }

        // Setup image capture metadata
        val metadata = Metadata().apply {
            isReversedHorizontal = when {
                // by default we always flip front, but if requested flipping, we dont flip it (inverse logic)
                builder.flipFrontResult -> false
                // Mirror image when using the front camera
                changeCameraProvider.cameraInfo.value?.cameraFacing == FacingDelegate.FRONT -> true
                else -> false
            }
        }

        val photoFile = SaveUtils.createImageFile(context, builder.saveTo)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        imageCapture?.takePicture(
            outputOptions,
            takePictureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    _takePictureResult.postValue(TakePictureResult.ImageTaken(outputFileResults.savedUri!!.toFile()))
                }

                override fun onError(exception: ImageCaptureException) {
                    val reason = takePictureErrorMap[exception.imageCaptureError] ?: CaptureError.ERROR_UNKNOWN
                    _takePictureResult.postValue(TakePictureResult.ImageTakeException(reason, exception))
                }
            }
        )
    }

    private fun onSnapshotSaved(photoFile: File) {
        _takePictureResult.postValue(TakePictureResult.ImageTaken(photoFile))
    }

    private fun onSnapshotSaveError(ex: Throwable) {
        _takePictureResult.postValue(
            TakePictureResult.ImageTakeException(
                CaptureError.ERROR_FILE_IO,
                ex
            )
        )
    }

    fun takeSnapshot() {
        viewFinder.bitmap?.let {
            val exif = ImageUtils.getExifByRotation(rotationDetector.sumOrientation)
            val photoFile = SaveUtils.createImageFile(context, builder.saveTo)
            val jpegQuality = builder.outputJpegQuality ?: DEFAULT_JPEG_QUALITY
            BitmapSaver(photoFile, it, exif, builder.maxImageSize, jpegQuality, this::onSnapshotSaved, this::onSnapshotSaveError).save()
        }
    }

    override val cameraInfo: LiveData<SimpleCameraInfo?> get() = changeCameraProvider.cameraInfo
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = changeCameraProvider.cameraInfoList
    override fun changeFacing() = changeCameraProvider.toggleFacing()
    override fun changeSameFacingCamera() = changeCameraProvider.toggleSameFacingCamera()
    override fun selectSameFacingCameraByIndex(index: Int) = changeCameraProvider.selectSameFacingCameraByIndex(index)

    companion object {
        private val cameraErrorMap = hashMapOf<Int, CameraError>(
            CameraUnavailableException.CAMERA_UNKNOWN_ERROR to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraUnavailableException.CAMERA_DISABLED to CameraError.CAMERA_DISABLED,
            CameraUnavailableException.CAMERA_DISCONNECTED to CameraError.CAMERA_DISCONNECTED,
            CameraUnavailableException.CAMERA_ERROR to CameraError.CAMERA_ERROR,
            CameraUnavailableException.CAMERA_IN_USE to CameraError.CAMERA_IN_USE,
            CameraUnavailableException.CAMERA_MAX_IN_USE to CameraError.CAMERA_MAX_IN_USE,
            CameraUnavailableException.CAMERA_UNAVAILABLE_DO_NOT_DISTURB to CameraError.CAMERA_UNAVAILABLE_DO_NOT_DISTURB
        )
        private val takePictureErrorMap = hashMapOf<Int, CaptureError>(
            ImageCapture.ERROR_UNKNOWN to CaptureError.ERROR_UNKNOWN,
            ImageCapture.ERROR_FILE_IO to CaptureError.ERROR_FILE_IO,
            ImageCapture.ERROR_CAPTURE_FAILED to CaptureError.ERROR_CAPTURE_FAILED,
            ImageCapture.ERROR_CAMERA_CLOSED to CaptureError.ERROR_CAMERA_CLOSED,
            ImageCapture.ERROR_INVALID_CAMERA to CaptureError.ERROR_INVALID_CAMERA
        )

        private const val DEFAULT_JPEG_QUALITY = 95
    }
}
