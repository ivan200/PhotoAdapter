package com.ivan200.photoadapter.base

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.ivan200.photoadapter.CameraBuilder

/**
 * @author ivan200
 * @since 24.02.2022
 */
interface CameraDelegate {
    val state: LiveData<CameraViewState>

    /**
     * fit camera into view (false = fill) fill by default
     */
    fun setScaleType(scale: ScaleDelegate)
    val scaleType: ScaleDelegate

    fun setLifecycleOwner(owner: LifecycleOwner?)
    fun setCameraBuilder(cameraBuilder: CameraBuilder)
    val cameraInfo: LiveData<SimpleCameraInfo>
    val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>>
    fun changeFacing()
    fun changeSameFacingCamera()
    fun selectCamera(camera: SimpleCameraInfo)
    fun restart()
    fun takePicture()
    val takePictureResult: LiveData<TakePictureResult>
    fun setFlash(flash: FlashDelegate.HasFlash)
    val orientationChanged: LiveData<Int>
    val isBlurring: Boolean
}
