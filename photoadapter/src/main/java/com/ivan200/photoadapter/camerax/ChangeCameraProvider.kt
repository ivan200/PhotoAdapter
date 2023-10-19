package com.ivan200.photoadapter.camerax

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.internal.Camera2CameraInfoImpl
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivan200.photoadapter.CameraBuilder
import com.ivan200.photoadapter.base.FacingDelegate
import com.ivan200.photoadapter.base.FlashDelegate
import com.ivan200.photoadapter.base.SimpleCameraInfo
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @author ivan200
 * @since 24.02.2022
 */
@Suppress("unused")
@TargetApi(21)
class ChangeCameraProvider {

    var cameraInfoList: Map<FacingDelegate, List<SimpleCameraInfo>> = mapOf()
        private set

    private val _cameraInfo = MutableLiveData<SimpleCameraInfo>()
    val cameraInfo: LiveData<SimpleCameraInfo> = _cameraInfo

    private val selectedList: MutableMap<FacingDelegate, SimpleCameraInfo> = mutableMapOf()
    private var currentCameraInfo: SimpleCameraInfo? = null
    private var provider: ProcessCameraProvider? = null
    private var cameraParams: CameraBuilder? = null

    fun setCameraProvider(provider: ProcessCameraProvider, cameraParams: CameraBuilder) {
        this.provider = provider
        this.cameraParams = cameraParams
        cameraInfoList = getFilledCameraInfoList()
        if (currentCameraInfo == null) {
            currentCameraInfo = selectFirstCamera(cameraParams)
        }
        updateCameraInfo()
    }

    private fun selectFirstCamera(cameraParams: CameraBuilder): SimpleCameraInfo? {
        cameraInfoList.forEach { (facing, infoList) ->
            selectedList[facing] = infoList.minByOrNull { it.cameraId }!!
        }
        return when (cameraParams.facingBack) {
            true -> selectedList[FacingDelegate.BACK] ?: selectedList[FacingDelegate.FRONT] ?: selectedList[FacingDelegate.EXTERNAL]
            false -> selectedList[FacingDelegate.FRONT] ?: selectedList[FacingDelegate.BACK] ?: selectedList[FacingDelegate.EXTERNAL]
        }
    }

    private fun updateCameraInfo() {
        currentCameraInfo?.let {
            selectedList[it.cameraFacing] = it
            _cameraInfo.postValue(it)
        }
    }

    fun hasAnyCamera(): Boolean = cameraInfoList.isNotEmpty()

    fun toggleFacing() {
        if (selectedList.size <= 1) return
        val newCamera = when (currentCameraInfo?.cameraFacing) {
            FacingDelegate.BACK -> selectedList[FacingDelegate.FRONT] ?: selectedList[FacingDelegate.EXTERNAL]
            FacingDelegate.FRONT -> selectedList[FacingDelegate.EXTERNAL] ?: selectedList[FacingDelegate.BACK]
            FacingDelegate.EXTERNAL -> selectedList[FacingDelegate.BACK] ?: selectedList[FacingDelegate.FRONT]
            null -> null
        }
        selectCamera(newCamera)
    }

    fun toggleSameFacingCamera() {
        val sameFacingCameraList = cameraInfoList[currentCameraInfo?.cameraFacing] ?: return

        val index = sameFacingCameraList.indexOf(currentCameraInfo)
        val nextIndex = if (index < sameFacingCameraList.size - 1) index + 1 else 0
        val newCamera = sameFacingCameraList[nextIndex]
        selectCamera(newCamera)
    }

    fun selectSameFacingCameraByIndex(index: Int) {
        val sameFacingCameraList = cameraInfoList[currentCameraInfo?.cameraFacing] ?: return
        val newCamera = sameFacingCameraList.getOrNull(index) ?: return
        selectCamera(newCamera)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun selectCamera(newCamera: SimpleCameraInfo?) {
        if (newCamera != null && newCamera != currentCameraInfo) {
            currentCameraInfo = newCamera
            updateCameraInfo()
        }
    }

    @Suppress("unused")
    fun toggleCamera() {
        val allCameraList = cameraInfoList.values.flatten()
        val index = allCameraList.indexOf(currentCameraInfo)
        val nextIndex = if (index < allCameraList.size - 1) index + 1 else 0
        currentCameraInfo = allCameraList[nextIndex]
        updateCameraInfo()
    }

    @SuppressLint("RestrictedApi")
    internal fun getCameraSelector(): CameraSelector {
        return CameraSelector.Builder().addCameraFilter { cameraList: MutableList<CameraInfo> ->
            return@addCameraFilter cameraList.filter {
                (it as? CameraInfoInternal)?.cameraId == currentCameraInfo?.cameraId
            }
        }.build()
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun getFilledCameraInfoList(): Map<FacingDelegate, List<SimpleCameraInfo>> {
        val cameras = mutableListOf<SimpleCameraInfo>()
        provider?.availableCameraInfos?.forEach {
            val cameraInfo = it as? CameraInfoInternal
            val characteristics = (it as? Camera2CameraInfoImpl)?.cameraCharacteristicsCompat

            val supportedFlash: List<FlashDelegate.HasFlash> = if (it.hasFlashUnit()) {
                if (cameraParams?.useSnapshot == true) {
                    listOf(
                        FlashDelegate.HasFlash.Off,
                        FlashDelegate.HasFlash.Torch
                    )
                } else {
                    listOf(
                        FlashDelegate.HasFlash.Off,
                        FlashDelegate.HasFlash.On,
                        FlashDelegate.HasFlash.Auto,
                        FlashDelegate.HasFlash.Torch
                    )
                }
            } else emptyList()

            cameras.add(
                SimpleCameraInfo(
                    cameraId = cameraInfo?.cameraId.orEmpty(),
                    cameraFacing = getFacing(cameraInfo),
                    hasFlashUnit = it.hasFlashUnit(),
                    physicalSize = getPhysicalSize(characteristics),
                    supportedFlash = supportedFlash.sortedBy(FlashDelegate.HasFlash::orderValue),
                    fov = getCameraFov(characteristics),
                    focal = getFocalLength(characteristics),
                    supportedFps = getAvailableFps(characteristics),
                    name = cameraInfo?.cameraId.orEmpty(),
                    nameSelected = cameraInfo?.cameraId.orEmpty(),
                )
            )
        }


        //enable flash in other cameras
        if (cameras.any { it.hasFlashUnit == true && it.cameraFacing == FacingDelegate.BACK }
            && cameras.any { it.hasFlashUnit == false && it.cameraFacing == FacingDelegate.BACK }) {
            val cameraWithFlashUnit = cameras.first { it.hasFlashUnit == true && it.cameraFacing == FacingDelegate.BACK }

            for ((index, simpleCameraInfo) in cameras.withIndex()) {
                if (simpleCameraInfo.hasFlashUnit == false && simpleCameraInfo.cameraFacing == FacingDelegate.BACK) {
                    cameras[index] = cameras[index].copy(
                        hasFlashUnit = true,
                        supportedFlash = listOf(
                            FlashDelegate.HasFlash.OffOnMainCamera(cameraWithFlashUnit.cameraId),
                            FlashDelegate.HasFlash.TorchOnMainCamera(cameraWithFlashUnit.cameraId)
                        )
                    )
                }
            }
        }

        return cameras.groupBy { it.cameraFacing }.mapValues { cameraList ->
            if (cameraList.value.all { it.fov > 0 }) {
                cameraList.value.sortedByDescending { it.fov }.let { mapWithNames(it) }
            } else {
                cameraList.value.sortedBy { it.cameraId }
            }
        }
    }

    private fun mapWithNames(sameFacingCameras: List<SimpleCameraInfo>): List<SimpleCameraInfo> {
        val mainCamera = sameFacingCameras.minByOrNull { it.cameraId }!!
        val canCountZoom: Boolean = sameFacingCameras.all { it.focal > 0 && it.physicalSize.x > 0 && it.physicalSize.y > 0 }
        val mainZoomValue: Float = if (canCountZoom) mainCamera.zoomValue() else 1f
        return sameFacingCameras.mapIndexed { index, simpleCameraInfo ->
            val cameraZoom = simpleCameraInfo.zoomValue()
            val name = if (canCountZoom) getCameraName(cameraZoom, mainZoomValue) else (index + 1).toString()
            val nameSelected = if (canCountZoom) getCameraNameSelected(cameraZoom, mainZoomValue) else (index + 1).toString()

            simpleCameraInfo.copy(
                name = name,
                nameSelected = nameSelected
            )
        }
    }

    /**
     * Get the intended camera name by knowing the camera parameters
     * so name will be: "1","2","3" or ".5",".8" or "1.5","2.2"
     */
    private fun getCameraName(cameraZoom: Float, mainZoomValue: Float): String {
        val result = cameraZoom / mainZoomValue
        val bd1 = BigDecimal(result.toDouble()).setScale(1, RoundingMode.HALF_UP)
        val bd2 = BigDecimal(result.toDouble()).setScale(0, RoundingMode.HALF_UP)
        return when {
            bd1.toFloat() == bd2.toFloat() -> bd2.toString()
            bd1 < (1).toBigDecimal() && bd1 > BigDecimal.ZERO -> bd1.toString().substring(1)
            else -> bd1.toString()
        }
    }

    /**
     * Get the selected camera name by knowing the camera parameters
     * so name will be: "1×","2×","3×" or "0.5×","0.8×" or "1.5×","2.2×"
     */
    private fun getCameraNameSelected(cameraZoom: Float, mainZoomValue: Float): String {
        val result = cameraZoom / mainZoomValue
        val bd1 = BigDecimal(result.toDouble()).setScale(1, RoundingMode.HALF_UP)
        val bd2 = BigDecimal(result.toDouble()).setScale(0, RoundingMode.HALF_UP)
        val name = if (bd1.toFloat() == bd2.toFloat()) bd2.toString() else bd1.toString()
        return "$name×"
    }

    /**
     * Calculation of the initial camera zoom
     * The magnification factor of the lens is the ratio of the maximum value
     * of the focal length of the camera lens (in mm) to the length of the diagonal of the frame (in mm)
     */
    private fun SimpleCameraInfo.zoomValue(): Float {
        val diagonal = sqrt(physicalSize.x.pow(2) + physicalSize.y.pow(2))
        return focal / diagonal
    }

    @SuppressLint("RestrictedApi")
    private fun getFacing(info: CameraInfoInternal?): FacingDelegate {
        if (info == null) return FacingDelegate.BACK
        return when (info.lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> FacingDelegate.FRONT
            CameraSelector.LENS_FACING_BACK -> FacingDelegate.BACK
            else -> FacingDelegate.EXTERNAL
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getPhysicalSize(cameraCharacteristics: CameraCharacteristicsCompat?): PointF {
        val configs = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        if (configs != null) {
            return PointF(configs.width, configs.height)
        }
        return PointF(0f, 0f)
    }

    @SuppressLint("RestrictedApi")
    private fun getAvailableFps(cameraCharacteristics: CameraCharacteristicsCompat?): List<IntRange> {
        val configs = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        if (configs != null) {
            return configs.map {
                IntRange(it.lower, it.upper)
            }
        }
        return emptyList()
    }

    @SuppressLint("RestrictedApi")
    private fun getCameraFov(cameraCharacteristics: CameraCharacteristicsCompat?): Float {
        val fArr = cameraCharacteristics?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.asList()
        if (!fArr.isNullOrEmpty()) {
            val focal = fArr[0].toDouble()
            val sizeF = cameraCharacteristics[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
            if (sizeF != null) {
                val size = sizeF.width.toDouble()
                if (size > 0.0f) {
                    return (Math.toDegrees(atan((size / 2.0) / focal)) * 2.0).toFloat()
                }
            }
        }
        return 0.0f
    }

    @SuppressLint("RestrictedApi")
    private fun getFocalLength(cameraCharacteristics: CameraCharacteristicsCompat?): Float {
        val fArr = cameraCharacteristics?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.asList()
        return if (!fArr.isNullOrEmpty()) {
            fArr[0]
        } else 0f
    }

}
