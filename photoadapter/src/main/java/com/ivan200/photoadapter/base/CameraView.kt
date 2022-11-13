package com.ivan200.photoadapter.base

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.camerax.CameraXView
import com.ivan200.photoadapter.ontario.CameraImplOntario
import com.ivan200.photoadapter.utils.CameraImplSelector

/**
 * @author ivan200
 * @since 24.02.2022
 */
@Suppress("UNUSED_PARAMETER")
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CameraDelegate {

    private val impl: CameraDelegate = if (
        isInEditMode || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && cameraSelector.isImplCamera2(context))
    ) {
        CameraXView(context, attrs, defStyleAttr)
    } else {
        CameraImplOntario(context, attrs)
    }.also {
        addView(it)
    }

    override val state: LiveData<CameraViewState> get() = impl.state
    override val cameraInfo: LiveData<SimpleCameraInfo> get() = impl.cameraInfo
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = impl.cameraInfoList
    override fun setFitMode(fit: Boolean) = impl.setFitMode(fit)
    override fun setLifecycleOwner(owner: LifecycleOwner?) = impl.setLifecycleOwner(owner)
    override fun setCameraBuilder(cameraBuilder: CameraBuilder) = impl.setCameraBuilder(cameraBuilder)
    override fun changeFacing() = impl.changeFacing()
    override fun changeSameFacingCamera() = impl.changeSameFacingCamera()
    override fun selectCamera(camera: SimpleCameraInfo) = impl.selectCamera(camera)
    override fun takePicture() = impl.takePicture()
    override val takePictureResult: LiveData<TakePictureResult> get() = impl.takePictureResult
    override val isFit: Boolean get() = impl.isFit
    override fun setFlash(flash: FlashDelegate.HasFlash) = impl.setFlash(flash)
    override fun restart() = impl.restart()
    override val orientationChanged: LiveData<Int> = impl.orientationChanged
    override val isBlurring: Boolean get() = impl.isBlurring

    companion object {
        /** this is workaround to chose implementation before view is created */
        var cameraSelector: CameraImplSelector = CameraImplSelector.Camera2IfAnyFullSupport
    }
}
