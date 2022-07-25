package com.ivan200.photoadapter.base

import android.graphics.PointF

/**
 * @author ivan200
 * @since 24.02.2022
 */
data class SimpleCameraInfo(
    val cameraId: String,
    val cameraFacing: FacingDelegate,
    val hasFlashUnit: Boolean,
    val physicalSize: PointF,
    val fov: Float,
    val focal: Float,
    val name: String
)
