package com.ivan200.photoadapter.base

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.camerax.CameraXView
import com.ivan200.photoadapter.ontario.CameraImplOntario
import com.ivan200.photoadapter.utils.ImageUtils

/**
 * @author ivan200
 * @since 24.02.2022
 */
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CameraDelegate {

    val impl: CameraDelegate = if (forceUseCamera1Impl || !ImageUtils.allowCamera2Support(context)) {
        CameraImplOntario(context, attrs)
    } else {
        CameraXView(context, attrs, defStyleAttr)
    }.also {
        addView(it)
    }

    override val state: LiveData<CameraViewState> get() = impl.state
    override val cameraInfo: LiveData<SimpleCameraInfo?> get() = impl.cameraInfo
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> get() = impl.cameraInfoList
    override fun setFitMode(fit: Boolean) = impl.setFitMode(fit)
    override fun setLifecycleOwner(owner: LifecycleOwner?) = impl.setLifecycleOwner(owner)
    override fun setCameraBuilder(cameraBuilder: CameraBuilder) = impl.setCameraBuilder(cameraBuilder)
    override fun changeFacing() = impl.changeFacing()
    override fun changeSameFacingCamera() = impl.changeSameFacingCamera()
    override fun selectSameFacingCameraByIndex(index: Int) = impl.selectSameFacingCameraByIndex(index)
    override fun takePicture() = impl.takePicture()
    override val takePictureResult: LiveData<TakePictureResult> get() = impl.takePictureResult
    override val isFit: Boolean get() = impl.isFit
    override fun setFlash(flash: FlashDelegate.HasFlash) = impl.setFlash(flash)
    override fun restart() = impl.restart()
    companion object {
        /** this is workaround to chose implementation before it is created */
        var forceUseCamera1Impl = false
    }
}
