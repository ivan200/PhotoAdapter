package com.ivan200.photoadapter.base

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivan200.photoadapter.CameraBuilder

/**
 * @author ivan200
 * @since 24.02.2022
 */
class CameraDummy : CameraDelegate {
    override val state: LiveData<CameraViewState> = MutableLiveData<CameraViewState>()
    override fun setFitMode(fit: Boolean){}
    override fun setLifecycleOwner(owner: LifecycleOwner?){}
    override fun setCameraBuilder(cameraBuilder: CameraBuilder){}
    override val cameraInfo: LiveData<SimpleCameraInfo?> = MutableLiveData<SimpleCameraInfo?>()
    override val cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> = emptyMap()
    override fun changeFacing() {}
    override fun changeSameFacingCamera() {}
    override fun selectSameFacingCameraByIndex(index: Int) {}
}
