@file:Suppress("DEPRECATION")

package com.ivan200.photoadapter.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PointF
import android.hardware.Camera
import android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
import com.ivan200.photoadapter.base.FacingDelegate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

//
// Created by Ivan200 on 21.10.2019.
//

@Suppress("MemberVisibilityCanBePrivate")
object ImageUtils {

    fun allowCamera2Support(activity: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false

        (activity.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.apply {
            try {
                cameraIdList.firstOrNull()?.let {
                    val characteristics = getCameraCharacteristics(it)
                    val support = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                        support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
                    ) {
                        return true
                    }
                }
            } catch (ex: Throwable) {
                return false
            }
        }
        return false
    }

    @Suppress("DEPRECATION")
    fun getFacingsOldWay(): Set<FacingDelegate> {
        val facings = mutableSetOf<FacingDelegate>()
        val cameraInfo = Camera.CameraInfo()
        val numberOfCameras: Int = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CAMERA_FACING_BACK) facings.add(FacingDelegate.BACK)
            if (cameraInfo.facing == CAMERA_FACING_FRONT) facings.add(FacingDelegate.FRONT)
        }
        return facings
    }

    fun getFacings(context: Context): Set<FacingDelegate> {
        if (!isCameraAvailable(context)) {
            return emptySet()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return getFacingsOldWay()
        }
        val facings = mutableSetOf<FacingDelegate>()
        (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)?.apply {
            try {
                cameraIdList.forEach {
                    val characteristics = getCameraCharacteristics(it)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> facings.add(FacingDelegate.BACK)
                        CameraCharacteristics.LENS_FACING_FRONT -> facings.add(FacingDelegate.FRONT)
                        else -> Unit // Ontario is not support external cameras
                    }
                }
            } catch (ex: Throwable) {
                return getFacingsOldWay()
            }
        }
        return facings
    }

    fun isCameraAvailable(context: Context): Boolean {
        val hasBack = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        if (hasBack) return true

        val hasFront = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        if (hasFront) return true

        // external only can be used with camerax implementation, and it only used with camera2 support
        val hasExternal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL) && // Added in API level 20
            allowCamera2Support(context)
        if (hasExternal) return true

//      dont use PackageManager.FEATURE_CAMERA_ANY, because it addad in api 20,
//      and on api 20 with only external camera (without front and back) it will return true
//      and ontario implementation is not support external camera at all

        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun aspectRatio(size: PointF?): Int {
        if (size == null || size.x <= 0f || size.y <= 0f) {
            return DEFAULT_ASPECT_RATIO
        }
        val previewRatio = max(size.x, size.y).toDouble() / min(size.x, size.y)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private const val RATIO_4_3_VALUE: Double = 4.0 / 3.0
    private const val RATIO_16_9_VALUE: Double = 16.0 / 9.0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private const val DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun targetSize(cameraRatio: Int, targetSideSize: Int): Size = when (cameraRatio) {
        AspectRatio.RATIO_16_9 -> Size((targetSideSize / RATIO_16_9_VALUE).toInt(), targetSideSize)
        else -> Size((targetSideSize / RATIO_4_3_VALUE).toInt(), targetSideSize)
    }

    fun Point.scaleDown(maxX: Int?, maxY: Int?): Point {
        val setX = maxX ?: x
        val setY = maxY ?: y

        if (setX < 0 || setY < 0 || (x <= setX && y <= setY)) {
            return this
        }
        if (x == 0 || y == 0) {
            return Point(0, 0)
        }
        val ratio = x.toFloat() / y.toFloat()
        return when {
            ratio >= 1 -> Point(setX, (setY / ratio).toInt())
            else -> Point((setX * ratio).toInt(), setY)
        }
    }

    @Suppress("DEPRECATION")
    val Context.displayCompat: Display?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
            ?: ContextCompat.getSystemService(this, WindowManager::class.java)?.defaultDisplay

    /** Checking whether the system orientation of the device is landscape */
    fun isDefaultOrientationLandscape(context: Context): Boolean {
        val config = context.resources.configuration
        val rotation = context.displayCompat?.rotation
        val defaultLandscapeAndIsInLandscape = (rotation == ROTATION_0 || rotation == ROTATION_180) &&
            config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val defaultLandscapeAndIsInPortrait = (rotation == ROTATION_90 || rotation == ROTATION_270) &&
            config.orientation == Configuration.ORIENTATION_PORTRAIT
        return defaultLandscapeAndIsInLandscape || defaultLandscapeAndIsInPortrait
    }

    fun getExifByRotation(rotation: Int): Int = when (rotation) {
        ROTATION_0 -> ORIENTATION_NORMAL
        ROTATION_90 -> ORIENTATION_ROTATE_90
        ROTATION_180 -> ORIENTATION_ROTATE_180
        ROTATION_270 -> ORIENTATION_ROTATE_270
        else -> ORIENTATION_UNDEFINED
    }
}
