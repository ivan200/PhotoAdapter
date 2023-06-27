package com.ivan200.photoadapter.base

import android.graphics.PointF

/**
 * Simple data information about camera
 *
 * @param cameraId       id of camera
 * @param cameraFacing   facing of camera
 * @param hasFlashUnit   is camera has flash unit
 * @param supportedFlash supported flash units of camera
 * @param physicalSize   the physical dimensions of the full pixel camera size
 * @param fov            camera field of view
 * @param focal          camera focal distance
 * @param name           camera name for display
 * @param nameSelected   camera selected name for display
 *
 * @author               ivan200
 * @since                24.02.2022
 */
data class SimpleCameraInfo(
    val cameraId: String,
    val cameraFacing: FacingDelegate,
    val hasFlashUnit: Boolean,
    val supportedFlash: List<FlashDelegate.HasFlash>,
    val physicalSize: PointF,
    val fov: Float,
    val focal: Float,
    val supportedFps: List<IntRange>,
    val name: String,
    val nameSelected: String
)
