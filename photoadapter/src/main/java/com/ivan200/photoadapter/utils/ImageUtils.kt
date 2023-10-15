@file:Suppress("DEPRECATION")

package com.ivan200.photoadapter.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.hardware.Camera
import android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
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
        if (!isCameraAvailable(context, false)) {
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
                    when (characteristics[CameraCharacteristics.LENS_FACING]) {
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

    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    fun isCameraAvailable(context: Context, useCamera2: Boolean): Boolean {
        val hasBack = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        if (hasBack) return true

        val hasFront = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        if (hasFront) return true

        //ontario implementation is not support external camera at all
        if (useCamera2) {
            val hasAny = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                    && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            if (hasAny) return true

            // external only can be used with camerax implementation, and it only used with camera2 support
            val hasExternal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                    && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL) // Added in API level 20
            if (hasExternal) return true
        }
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

    fun getExifByRotation(rotation: Int): Int = when (rotation) {
        ROTATION_0 -> ORIENTATION_NORMAL
        ROTATION_90 -> ORIENTATION_ROTATE_90
        ROTATION_180 -> ORIENTATION_ROTATE_180
        ROTATION_270 -> ORIENTATION_ROTATE_270
        else -> ORIENTATION_UNDEFINED
    }

    fun scaleBitmap(maxWidth: Int?, maxHeight: Int?, source: Bitmap): Bitmap {
        try {
            val bitmapSize = Point(source.width, source.height)
            val scaledSize = bitmapSize.scaleDown(maxWidth, maxHeight)
            val needScale = scaledSize != bitmapSize
            if (!needScale) {
                return source
            }
            val matrix = Matrix().apply {
                setScale(scaledSize.x / source.width.toFloat(), scaledSize.y / source.height.toFloat())
            }
            val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            source.recycle()
            return bitmap
        } catch (ex: Exception) {
            return source
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun blurBitmap(bitmap: Bitmap?, applicationContext: Context, radius: Float): Bitmap? {
        if (bitmap == null) return null
        try {
            // Create the output bitmap
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)

            // Blur the image
            val rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)
            val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
            val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)
            val theIntrinsic = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
            theIntrinsic.apply {
                setRadius(radius)
                theIntrinsic.setInput(inAlloc)
                theIntrinsic.forEach(outAlloc)
            }
            outAlloc.copyTo(output)
            rsContext.finish()
            return output
        } catch (ex: Exception) {
            return null
        }
    }
}
