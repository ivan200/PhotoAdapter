package com.ivan200.photoadapter.camerax

import android.annotation.TargetApi
import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.OrientationEventListener
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import androidx.core.content.ContextCompat
import com.ivan200.photoadapter.utils.ImageUtils.displayCompat

/**
 * @author ivan200
 * @since 24.02.2022
 */
@TargetApi(21)
class DisplayRotationDetector(
    val context: Context,
    var onOrientationChangedListener: Runnable? = null
) : DisplayManager.DisplayListener {

    var sensorOrientation: Int = ROTATION_0
        private set
    var deviceOrientation: Int = getOrientation()
        private set
    var sumOrientation: Int = deviceOrientation
        private set

    private var orientationListener: OrientationEventListener? = null
    private var displayManager: DisplayManager? = null

    fun enable() {
        if (orientationListener == null) {
            orientationListener = OrientationListener(context)
        }
        if (displayManager == null) {
            displayManager = ContextCompat.getSystemService(context, DisplayManager::class.java)
        }
        displayManager?.registerDisplayListener(this, null)
        orientationListener?.enable()
    }

    fun disable() {
        orientationListener?.disable()
        displayManager?.unregisterDisplayListener(this)
    }

    private fun onDisplayOrOrientationChanged(newDeviceOrientation: Int, newSensorOrientation: Int) {
        if (newDeviceOrientation != deviceOrientation || newSensorOrientation != sensorOrientation) {
            deviceOrientation = newDeviceOrientation
            sensorOrientation = newSensorOrientation
            sumOrientation = getSumOrientation(newDeviceOrientation, newSensorOrientation)
            onOrientationChangedListener?.run()
        }
    }

    private fun getSumOrientation(deviceOr: Int, sensorOr: Int): Int {
        val deviceDeg = rotationToDegree.getValue(deviceOr)
        val sensorDeg = rotationToDegree.getValue(sensorOr)
        val sumDegree = (deviceDeg + sensorDeg) % 360
        return degreeToRotation.getValue(sumDegree)
    }

    override fun onDisplayAdded(displayId: Int) = Unit
    override fun onDisplayRemoved(displayId: Int) = Unit
    override fun onDisplayChanged(displayId: Int) {
        context.displayCompat?.let {
            if (displayId == it.displayId) {
                onDisplayOrOrientationChanged(it.rotation, sensorOrientation)
            }
        }
    }

    private inner class OrientationListener(context: Context) :
        OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(orientation: Int) {
            val newSensorOrientation = when (orientation) {
                ORIENTATION_UNKNOWN -> null
                in 45..134 -> ROTATION_90
                in 135..224 -> ROTATION_180
                in 225..314 -> ROTATION_270
                else -> ROTATION_0   // orientation >= 315 || orientation < 45 -> 0
            }
            if (newSensorOrientation != null) {
                onDisplayOrOrientationChanged(deviceOrientation, newSensorOrientation)
            }
        }
    }

    private fun getOrientation() = context.displayCompat?.rotation ?: ROTATION_0

    private companion object {
        val rotationToDegree = mapOf(
            ROTATION_0 to 0,
            ROTATION_90 to 90,
            ROTATION_180 to 180,
            ROTATION_270 to 270
        )
        val degreeToRotation = rotationToDegree.map { it.value to it.key }.toMap()
    }
}
