package com.ivan200.photoadapter.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.AttributeSet
import android.util.Range
import android.util.Size
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
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
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.base.CameraDelegate
import com.ivan200.photoadapter.base.CameraError
import com.ivan200.photoadapter.base.CameraViewState
import com.ivan200.photoadapter.base.CaptureError
import com.ivan200.photoadapter.base.FacingDelegate
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.ScaleDelegate
import com.ivan200.photoadapter.base.ScaleDelegate.FILL
import com.ivan200.photoadapter.base.ScaleDelegate.FIT
import com.ivan200.photoadapter.base.SimpleCameraInfo
import com.ivan200.photoadapter.base.TakePictureResult
import com.ivan200.photoadapter.camerax.touch.TouchHandler
import com.ivan200.photoadapter.utils.ImageUtils
import com.ivan200.photoadapter.utils.SaveUtils
import com.ivan200.photoadapter.utils.SimpleRequestListener
import com.ivan200.photoadapter.utils.dpToPx
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * @author ivan200
 * @since 24.02.2022
 */
@TargetApi(21)
@Suppress("MemberVisibilityCanBePrivate", "unused")
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
    var currentFlash: FlashDelegate? = null

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

    private val _torchState = MutableLiveData(false)
    val torchState: LiveData<Boolean> = _torchState

    private val _takePictureResult = MutableLiveData<TakePictureResult>()
    override val takePictureResult: LiveData<TakePictureResult> = _takePictureResult

    private val _orientationChanged = MutableLiveData<Int>()
    override val orientationChanged: LiveData<Int> = _orientationChanged

    val takePictureExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    val blurView = ImageView(context, attrs, defStyleAttr, defStyleRes).also {
        it.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        it.scaleType = ImageView.ScaleType.CENTER_CROP
        it.isVisible = false
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

            is FlashDelegate.HasFlash.TorchOnMainCamera -> toggleMainFlash(true, flash.cameraId)
            is FlashDelegate.HasFlash.OffOnMainCamera -> toggleMainFlash(false, flash.cameraId)
        }
        currentFlash = flash
    }

    fun toggleMainFlash(enabled: Boolean, cameraId: String) = runCatching {
        ContextCompat.getSystemService(context, CameraManager::class.java)?.apply {
            setTorchMode(cameraId, enabled)
        }
    }


    override fun setScaleType(scale: ScaleDelegate) {
        when (scale) {
            FIT -> {
                viewFinder.scaleType = PreviewView.ScaleType.FIT_CENTER
                blurView.scaleType = ImageView.ScaleType.FIT_CENTER
            }

            FILL -> {
                viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
                blurView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override val scaleType: ScaleDelegate
        get() = if (viewFinder.scaleType == PreviewView.ScaleType.FIT_CENTER) FIT else FILL

    override fun setLifecycleOwner(owner: LifecycleOwner?) {
        if (!ImageUtils.isCameraAvailable(context, true)) {
            _state.postValue(CameraViewState.Error(CameraError.NO_CAMERA))
            return
        }

        lifecycleOwner?.lifecycle?.removeObserver(cameraLifecycleObserver)
        owner?.let {
            it.lifecycle.addObserver(cameraLifecycleObserver)
            lifecycleOwner = it

            changeCameraProvider.cameraInfo.observe(it) { info: SimpleCameraInfo ->
                try {
                    bindCameraUseCases(info)
                } catch (e: Exception) {
                    processCameraException(e)
                }
            }
        }
    }

    override fun setCameraBuilder(cameraBuilder: CameraBuilder) {
        this.builder = cameraBuilder

        setScaleType(if (cameraBuilder.fillPreview) FILL else FIT)
    }

    private fun onDisplayRotated() {
        if (builder.lockRotate) {
            val invert = when (rotationDetector.sensorOrientation) {
                ROTATION_90 -> ROTATION_270
                ROTATION_270 -> ROTATION_90
                else -> rotationDetector.sensorOrientation
            }
            imageCapture?.targetRotation = invert
        } else {
            imageCapture?.targetRotation = rotationDetector.deviceOrientation
        }

        _orientationChanged.postValue(DisplayRotationDetector.rotationToDegree[rotationDetector.sumOrientation])
    }

    fun setUiOnPermission() {
        if (lifecycleOwner == null) return

        val hasCameraPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

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

    private inner class CameraProviderFutureListener : Runnable {
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

    private inner class CameraLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    rotationDetector.enable()
                    setUiOnPermission()
                }

                Lifecycle.Event.ON_STOP -> {
                    rotationDetector.disable()
                    checkDisableFlash()
                }

                else -> Unit
            }
        }
    }

    fun checkDisableFlash() {
        val flash = currentFlash
        if (flash is FlashDelegate.HasFlash.TorchOnMainCamera) toggleMainFlash(false, flash.cameraId)
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

    fun aspectRatioSelector(@AspectRatio.Ratio preferredAspectRatio: Int): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(preferredAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .build()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(cameraInfo: SimpleCameraInfo) {
        if (lifecycleOwner == null) return

        val dimObtained = cameraInfo.physicalSize.let { it.x > 0f && it.y > 0f }
        val cameraSize = if (dimObtained) cameraInfo.physicalSize else getPreviewSize()
        val cameraRatio = ImageUtils.aspectRatio(cameraSize)

        val rotation = rotationDetector.deviceOrientation


        val previewBuilder = Preview.Builder().apply {
            setResolutionSelector(aspectRatioSelector(cameraRatio))
            setTargetRotation(rotation)
        }

        val fps60 = cameraInfo.supportedFps.filter { (it.first in 31..60) || (it.last in 31..60) }
        if (fps60.isNotEmpty()) {
            val sorted = fps60.sortedWith(compareByDescending(IntRange::last).thenByDescending(IntRange::first))
            val maxRange = sorted.first()

            Camera2Interop.Extender(previewBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(maxRange.first, maxRange.last))
        }

        preview = previewBuilder.build().apply {
            surfaceProvider = viewFinder.surfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF).apply {
                builder.outputJpegQuality?.let {
                    setJpegQuality(it)
                }
                val pictureRatio = ImageUtils.aspectRatio(cameraSize)
                setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            AspectRatioStrategy(pictureRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                        )
                        .apply {
                            if (builder.maxWidth != null
                                && builder.maxHeight != null
                                && builder.maxWidth!! > 0
                                && builder.maxHeight!! > 0
                            ) {
                                setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(builder.maxWidth!!, builder.maxHeight!!),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                    )
                                )
                            }
                        }
                        .build()
                )
            }.build()

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
                if (_state.value !is CameraViewState.Streaming) {
                    _state.postValue(CameraViewState.Streaming)
                    blurView.isVisible = false
                }
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
            // Mirror image when using the front camera
            isReversedHorizontal = builder.flipFrontResult && changeCameraProvider.cameraInfo.value?.cameraFacing == FacingDelegate.FRONT
        }

        val photoFile = SaveUtils.createImageFile(context, builder.saveTo)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build()

        val viewWeakRef = WeakReference(this)
        imageCapture?.takePicture(
            outputOptions,
            takePictureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewWeakRef.get()?.onPictureSaved(outputFileResults.savedUri!!.toFile())
                }

                override fun onError(exception: ImageCaptureException) {
                    val reason = takePictureErrorMap[exception.imageCaptureError]
                    viewWeakRef.get()?.onPictureSaveError(exception, reason)
                }
            }
        )
    }

    private fun onPictureSaved(photoFile: File) {
        _takePictureResult.postValue(TakePictureResult.ImageTaken(photoFile))
    }

    private fun onPictureSaveError(ex: Throwable, error: CaptureError?) {
        _takePictureResult.postValue(
            TakePictureResult.ImageTakeException(
                error ?: CaptureError.ERROR_UNKNOWN,
                ex
            )
        )
    }

    fun takeSnapshot() {
        viewFinder.bitmap?.let {
            val exif = ImageUtils.getExifByRotation(rotationDetector.sumOrientation)
            val photoFile = try {
                SaveUtils.createImageFile(context, builder.saveTo)
            } catch (ex: IOException) {
                onPictureSaveError(ex, CaptureError.ERROR_FILE_IO)
                return
            }
            val jpegQuality = builder.outputJpegQuality ?: DEFAULT_JPEG_QUALITY

            val viewWeakRef = WeakReference(this)
            BitmapSaver(
                photoFile = photoFile,
                result = it,
                exif = exif,
                maxWidth = builder.maxWidth,
                maxHeight = builder.maxHeight,
                jpegQuality = jpegQuality,
                onSaved = { file -> viewWeakRef.get()?.onPictureSaved(file) },
                onSavedError = { ex -> viewWeakRef.get()?.onPictureSaveError(ex, CaptureError.ERROR_FILE_IO) }
            ).save()
        }
    }

    override val cameraInfo: LiveData<SimpleCameraInfo> get() = changeCameraProvider.cameraInfo
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = changeCameraProvider.cameraInfoList

    override fun changeFacing() {
        showBlur(changeCameraProvider::toggleFacing)
        checkDisableFlash()
    }

    override fun changeSameFacingCamera() {
        changeCameraProvider.toggleSameFacingCamera()
        checkDisableFlash()
    }

    override fun selectCamera(camera: SimpleCameraInfo) {
        if (camera != changeCameraProvider.cameraInfo.value) {
            showBlur {
                changeCameraProvider.selectCamera(camera)
            }
            checkDisableFlash()
        }
    }


    override val isBlurring: Boolean get() = blurView.isVisible

    fun showBlur(onNext: () -> Unit) {
        if (!builder.blurOnSwitch) {
            onNext.invoke()
            return
        }

        val bitmap = viewFinder.bitmap
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
            val size = min(measuredWidth, measuredHeight) / 2
            val scaled = ImageUtils.scaleBitmap(size, size, bitmap)
            val blur = ImageUtils.blurBitmap(scaled, context, 25f)
            if (blur == null) {
                onNext.invoke()
            } else {
                blurView.isVisible = true

                //TODO Отделить Glide от либы

                Glide.with(context)
                    .load(blur)
                    .listener(SimpleRequestListener {
                        blurView.isVisible = it
                        onNext.invoke()
                    })
                    .into(blurView)
            }
        } else {
            onNext.invoke()
        }
    }

    companion object {
        // @formatter:off
        private val cameraErrorMap = hashMapOf(
            CameraUnavailableException.CAMERA_UNKNOWN_ERROR              to CameraError.CAMERA_UNKNOWN_ERROR,
            CameraUnavailableException.CAMERA_DISABLED                   to CameraError.CAMERA_DISABLED,
            CameraUnavailableException.CAMERA_DISCONNECTED               to CameraError.CAMERA_DISCONNECTED,
            CameraUnavailableException.CAMERA_ERROR                      to CameraError.CAMERA_ERROR,
            CameraUnavailableException.CAMERA_IN_USE                     to CameraError.CAMERA_IN_USE,
            CameraUnavailableException.CAMERA_MAX_IN_USE                 to CameraError.CAMERA_MAX_IN_USE,
            CameraUnavailableException.CAMERA_UNAVAILABLE_DO_NOT_DISTURB to CameraError.CAMERA_UNAVAILABLE_DO_NOT_DISTURB
        )
        private val takePictureErrorMap = hashMapOf(
            ImageCapture.ERROR_UNKNOWN        to CaptureError.ERROR_UNKNOWN,
            ImageCapture.ERROR_FILE_IO        to CaptureError.ERROR_FILE_IO,
            ImageCapture.ERROR_CAPTURE_FAILED to CaptureError.ERROR_CAPTURE_FAILED,
            ImageCapture.ERROR_CAMERA_CLOSED  to CaptureError.ERROR_CAMERA_CLOSED,
            ImageCapture.ERROR_INVALID_CAMERA to CaptureError.ERROR_INVALID_CAMERA
        )
        // @formatter:on

        private const val DEFAULT_JPEG_QUALITY = 95
    }
}
